#!/usr/bin/env python
#
# This code is modified from the custom_sftp code by Steven Fernandez
#
# https://gist.github.com/lonetwin/3b5982cf88c598c0e169
#

import os
import logging
import sys
import re
import stat
import errno
import arvados
from arvados.collection import Collection
from paramiko import SFTPServerInterface, SFTPServer, SFTPAttributes, SFTP_OK, SFTPHandle

project_name_re = re.compile("\(([a-z0-9\-]+)\)$")

class StubSFTPHandle (SFTPHandle):
    def stat(self):
        try:
            return SFTPAttributes.from_stat(os.fstat(self.readfile.fileno()))
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)

    def chattr(self, attr):
        # python doesn't have equivalents to fchown or fchmod, so we have to
        # use the stored filename
        try:
            SFTPServer.set_file_attr(self.filename, attr)
            return SFTP_OK
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)

#
# The PortalSFTPServer class handles SFTP downloads from the data portal
#
class PortalSFTPServer(SFTPServerInterface):

    # When initializing the SFTP server the following arguments should be passed
    #
    # mysql_conn - an initialized mysql.connection object
    # userid - ID of the portal user that is connecting
    # arv - initialized arvados.api object
    #
    def __init__(server, *largs, **kwargs):
        server.mysql_conn = kwargs['mysql_conn']
        server.userid = kwargs['userid']
        server.arv = kwargs['arv']
        server.collections = {}
        log = logging.getLogger()
        log.debug("PortalSFTPServer __init__()") 

    def _dir_attr(self, mtime=0):
        attr = SFTPAttributes()
        attr.st_size = 1
        attr.st_uid = os.getuid()
        attr.st_gid = os.getgid()
        attr.st_mtime = mtime
        attr.st_atime = mtime
        attr.st_ctime = mtime
        attr.st_mode = stat.S_IRUSR + stat.S_IXUSR + stat.S_IFDIR
        return attr

    def _file_attr(self, mtime=0):
        attr = SFTPAttributes()
        attr.st_uid = os.getuid()
        attr.st_gid = os.getgid()
        attr.st_mode = stat.S_IRUSR + stat.S_IFREG
        attr.st_mtime = mtime
        attr.st_atime = mtime
        attr.st_ctime = mtime
        return attr


    def _get_project_id(self, part):
        re_match = project_name_re.search(part)
        if re_match != None:
            return str(re_match.group(1))
        else:
            return part

    # Method to retrieve a collection from arvados
    # In order to make lookups faster a dictionary is used to cache the Collection objects
    def _get_collection(self, uuid):
        if not uuid in self.collections:
            self.collections[uuid] = Collection(uuid, api_client=self.arv)
        return self.collections[uuid]

    # Method to retrieve an ArvFile object for a file in an Arvados collection
    def _get_file(self, uuid, filepath):
        collection = self._get_collection(uuid)
        return collection.find(filepath)

    # Method to check if the user has permission to access the given project
    def _is_allowed(self, projectid):
        cursor = self.mysql_conn.cursor()
        query = "SELECT role from users WHERE userid=%s"
        cursor.execute(query, (self.userid, ))
        row = cursor.fetchone()
        if row[0] == "admin":
            cursor.close()
            return True

        query = "SELECT projectid FROM project_membership WHERE userid=%s AND projectid=%s"
        cursor.execute(query, (self.userid, str(projectid)))
        row = cursor.fetchone()
        cursor.close()
        if row == None:
            raise OSError(errno.EACCES, "Permission denied")

        return True

    def list_folder(self, path):
        logging.debug(repr(path))
        out = [ ]
        try:
            cursor = self.mysql_conn.cursor()
            logging.debug("list_folder PATH: {}".format(path))
            if path == "/":
                # This is the root path.  Print a list of projects.
                query = """SELECT p.projectid,UNIX_TIMESTAMP(MAX(r.release_date)) FROM project_membership p 
                    JOIN project_files f ON(f.projectid = p.projectid) JOIN project_release r ON(r.releaseid = f.releaseid) 
                    WHERE p.userid=%s GROUP BY p.projectid"""
                cursor.execute(query, (self.userid, ))

                for (projectid, m_time) in cursor:
                    attr = self._dir_attr(m_time)
                    project = self.arv.groups().get(uuid=projectid).execute()
                    filename = "{} ({})".format(project['name'], projectid)
                    attr.filename = filename
                    logging.debug("Folder attrs: {}".format(str(attr)))
                    out.append(attr)

            else:
                parts = path.strip("/").split("/", 3)
                logging.debug("list_folder PARTS: {}".format(repr(parts)))

                parts[0] = self._get_project_id(parts[0])
                logging.debug("Project ID:{}".format(parts[0]))

                self._is_allowed(parts[0])

                if len(parts) == 1:
                    # If there is only one part to the path then list the datatypes for the collection
                    # Perhaps may change this to a release view? Will need to decide how to make paths within the project unique.
                    query = """SELECT f.type,UNIX_TIMESTAMP(MAX(r.release_date)) FROM project_files f 
                        JOIN project_release r ON(f.releaseid = r.releaseid) WHERE f.projectid=%s 
                        GROUP BY f.type ORDER BY f.type"""
                    cursor.execute(query, (str(parts[0]), ))

                    for (filetype, mtime) in cursor:
                        attr = self._dir_attr(mtime)
                        attr.filename = filetype
                        out.append(attr)

                else:
                    # Otherwise find the subpath and files
                    pathquery = "/" + ("/".join(parts[2:]) + "/" if len(parts) > 2 else "")

                    # First check if this a real directory
                    query = """SELECT COUNT(filename) FROM project_files WHERE projectid=%s AND type=%s AND filepath=%s"""
                    cursor.execute(query, (str(parts[0]), str(parts[1]), pathquery))
                    row = cursor.fetchone()
                    if row is None:
                        raise OSError(errno.ENOENT, "No such directory")

                    # First find the subdirectories and list them
                    query = """SELECT SUBSTRING_INDEX(SUBSTR(f.filepath, LENGTH(%s)+1),'/',1) as subpath, UNIX_TIMESTAMP(MAX(r.release_date))
                        FROM project_files f JOIN project_release r ON(f.releaseid = r.releaseid)
                        WHERE f.projectid=%s AND f.type=%s AND f.filepath LIKE %s AND LENGTH(f.filepath) > LENGTH(%s) GROUP BY subpath ORDER BY f.filepath"""

                    cursor.execute(query, (pathquery, str(parts[0]), str(parts[1]), pathquery + '%', pathquery))
                    logging.debug(cursor.statement)

                    for (dirname, mtime) in cursor:
                        attr = self._dir_attr(mtime)
                        attr.filename = dirname
                        out.append(attr)
                        logging.debug("DIR: {}".format(str(attr)))

                    # Then find the files and list those
                    query = """SELECT f.filename, UNIX_TIMESTAMP(r.release_date), f.fileid 
                        FROM project_files f JOIN project_release r ON (f.releaseid = r.releaseid) 
                        WHERE f.projectid=%s AND f.type=%s AND f.filepath=%s"""

                    cursor.execute(query, (str(parts[0]), str(parts[1]), pathquery))

                    for (filename, mtime, fileid) in cursor:
                        file_parts = fileid.split('/',1)
                        arv_file = self._get_file(file_parts[0], file_parts[1])
                        attr = self._file_attr(mtime)
                        attr.filename = os.path.basename(filename)
                        attr.st_size = arv_file.size()
                        out.append(attr)
                        logging.debug("FILE: {}".format(str(attr)))

            cursor.close()
            return out
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)

    def stat(self, path):
        try:
            if path == "/":
                cursor = self.mysql_conn.cursor()
                query = """SELECT UNIX_TIMESTAMP(MAX(r.release_date)) FROM project_membership p 
                    JOIN project_files f ON(f.projectid = p.projectid) JOIN project_release r ON(r.releaseid = f.releaseid) 
                    WHERE p.userid=%s"""
                cursor.execute(query, (self.userid, ))
                row = cursor.fetchone()
                return self._dir_attr(row[0])
            else:
                logging.debug("stat PATH: {}".format(path))
                parts = str(path).strip("/").split("/", 2)
                if parts[len(parts) - 1] == "..":
                    parts.pop()

                parts[0] = self._get_project_id(parts[0])
    
                logging.debug("stat PARTS: {}".format(repr(parts)))
                cursor = self.mysql_conn.cursor()
                # First check if this is a file
                if len(parts) > 2:
                    parts[2] = '/' + parts[2]
                    query = """SELECT UNIX_TIMESTAMP(r.release_date) FROM project_files f
                        JOIN project_release r ON(r.releaseid = f.releaseid) 
                        WHERE f.projectid=%s AND f.type=%s AND CONCAT(f.filepath,f.filename)=%s"""

                    cursor.execute(query, (parts[0], parts[1], parts[2]))
                    row = cursor.fetchone()
                    if cursor.with_rows and row is not None:
                        # this is a file.  Return a file attr 
                        attr = self._file_attr(row[0])
                    	logging.debug("STAT: {}".format(repr(attr)))
                        cursor.close()
                        return attr

                    logging.debug("STAT: is directory")

                query = """SELECT UNIX_TIMESTAMP(MAX(r.release_date)) FROM project_files f
                    JOIN project_release r ON(r.releaseid = f.releaseid) 
                    WHERE f.projectid=%s"""

                if len(parts) == 3:
                    logging.debug(repr(parts))
                    query = query + " AND f.type=%s AND f.filepath=%s"
                    if not parts[2].endswith('/'):
                    	parts[2] = parts[2] + "/"
                    cursor.execute(query, (parts[0], parts[1], parts[2]))
                elif len(parts) > 1:
                    query = query + " AND f.type=%s"
                    cursor.execute(query, (parts[0], parts[1]))
                else:
                    cursor.execute(query, (parts[0], ))

                row = cursor.fetchone()
                logging.debug("STAT return: {} {}".format(repr(cursor.statement), repr(row)))
                if cursor.with_rows and row[0] is not None:
                    mtime = row[0]
                    cursor.close()
                    return self._dir_attr(mtime)
                else:
                    cursor.close()
                    raise OSError(errno.ENOENT, "No such file or directory")

        except OSError as e:
            return SFTPServer.convert_errno(e.errno)

    def lstat(self, path):
        return self.stat(path) 

    def open(self, path, flags, attr):
        parts = path.strip("/").split("/", 2)
        re_match = project_name_re.search(parts[0])
        if re_match != None:
            parts[0] = re_match.group(1)

        logging.debug("OPEN FILE: {} -> {}".format(path, repr(parts)))
        if flags == 0 and len(parts) > 1:
            self._is_allowed(parts[0])

            cursor = self.mysql_conn.cursor()
            query = "SELECT fileid FROM project_files WHERE projectid=%s AND type=%s AND CONCAT(filepath, filename)=%s"
            cursor.execute(query, (str(parts[0]), str(parts[1]), "/" + parts[2]))
            row = cursor.fetchone()
            cursor.close()

            file_parts = row[0].split('/',1)
            collection = self._get_collection(file_parts[0])
            fobj = StubSFTPHandle(flags)
            fobj.readfile = collection.open(file_parts[1])
            return fobj
        else:
            return SFTPServer.convert_errno(errno.E_ACCES)

    # For all the following actions.  Send a permission denied error
    def remove(self, path):
        return SFTPServer.convert_errno(errno.E_ACCES)

    def rename(self, oldpath, newpath):
        return SFTPServer.convert_errno(errno.E_ACCES)

    def mkdir(self, path, attr):
        return SFTPServer.convert_errno(errno.E_ACCES)

    def rmdir(self, path):
        return SFTPServer.convert_errno(errno.E_ACCES)

    def chattr(self, path, attr):
        return SFTPServer.convert_errno(errno.E_ACCES)

#
# The PortalUploadSFTPServer class handles SFTP downloads from the data portal
# The class inherits the PortalSFTPServer class to use a number of the portal 
# methods
#
class PortalUploadSFTPServer(PortalSFTPServer):
    # Set the default upload root directory to /tmp.
    # The actual root directory should be passed when the object it initialized
    ROOT = '/tmp'

    # When initializing the SFTP server the following arguments should be passed
    #
    # mysql_conn - an initialized mysql.connection object
    # userid - ID of the portal user that is connecting
    # arv - initialized arvados.api object
    # root - root directory for uploads
    #
    def __init__(server, *largs, **kwargs):
        super(PortalUploadSFTPServer, server).__init__(*args, **kwargs)
        self.ROOT = kwargs['root']

    def _realpath(self, path):
        return self.ROOT + self.canonicalize(path)

    def list_folder(self, path):
        try:
            out = [ ]
            if path == "/":
                # This is the root path.  Print a list of projects.
                cursor = self.mysql_conn.cursor()
                query = "SELECT projectid FROM project_membership WHERE userid=%s"
                cursor.execute(query, (self.userid, ))

                for (projectid, ) in cursor:
                    attr = self._dir_attr()
                    attr.filename = projectid
                    out.append(attr)
            else:
                parent = os.path.split(path)[0]
                path = self._realpath(path)

                # If the parent is '/' then this is a project dir.  Check if the directory does not exist send nothing.
                if parent == '/' and (not os.path.exists(path)):
                    return out

                flist = os.listdir(path)
                for fname in flist:
                    attr = SFTPAttributes.from_stat(os.stat(os.path.join(path, fname)))
                    attr.filename = fname
                    out.append(attr)
            return out
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)

    def stat(self, path):
        try:
            # If the root path send a simple directory attribute object
            if path == "/":
                return self._dir_attr()
            else:
                parent = os.path.split(path)[0]
                if parent == '/':
                    return self._dir_attr()
                else:
                    path = self._realpath(path)
                    return SFTPAttributes.from_stat(os.stat(path))
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)

    def lstat(self, path):
        try:
            if path == "/":
                return self._dir_attr()
            else:
                parent = os.path.split(path)[0]
                if parent == '/':
                    return self._dir_attr()
                else:
                    path = self._realpath(path)
                    return SFTPAttributes.from_stat(os.lstat(path))
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)

    def open(self, path, flags, attr):
        path = self._realpath(path)
        try:
            binary_flag = getattr(os, 'O_BINARY',  0)
            flags |= binary_flag
            mode = getattr(attr, 'st_mode', None)
            if mode is not None:
                fd = os.open(path, flags, mode)
            else:
                # os.open() defaults to 0777 which is
                # an odd default mode for files
                fd = os.open(path, flags, 0o666)
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)
        if (flags & os.O_CREAT) and (attr is not None):
            attr._flags &= ~attr.FLAG_PERMISSIONS
            SFTPServer.set_file_attr(path, attr)
        if flags & os.O_WRONLY:
            if flags & os.O_APPEND:
                fstr = 'ab'
            else:
                fstr = 'wb'
        elif flags & os.O_RDWR:
            if flags & os.O_APPEND:
                fstr = 'a+b'
            else:
                fstr = 'r+b'
        else:
            # O_RDONLY (== 0)
            fstr = 'rb'
        try:
            f = os.fdopen(fd, fstr)
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)
        fobj = StubSFTPHandle(flags)
        fobj.filename = path
        fobj.readfile = f
        fobj.writefile = f
        return fobj

    # Should decide if allowing 
    def remove(self, path):
        path = self._realpath(path)
        try:
            if path == "/":
                return SFTPServer.convert_errno(errno.E_ACCES)
            else:
                parent = os.path.split(path)[0]
                if parent == '/':
                    return SFTPServer.convert_errno(errno.E_ACCES)
                else:
                    os.remove(path)
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)
        return SFTP_OK

    def rename(self, oldpath, newpath):
        oldpath = self._realpath(oldpath)
        newpath = self._realpath(newpath)
        try:
            os.rename(oldpath, newpath)
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)
        return SFTP_OK

    def mkdir(self, path, attr):

        path = self._realpath(path)
        try:
            os.mkdir(path)
            if attr is not None:
                SFTPServer.set_file_attr(path, attr)
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)
        return SFTP_OK

    def rmdir(self, path):
        path = self._realpath(path)
        try:
            os.rmdir(path)
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)
        return SFTP_OK

    def chattr(self, path, attr):
        path = self._realpath(path)
        try:
            SFTPServer.set_file_attr(path, attr)
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)
        return SFTP_OK

    def symlink(self, target_path, path):
        path = self._realpath(path)
        if (len(target_path) > 0) and (target_path[0] == '/'):
            # absolute symlink
            target_path = os.path.join(self.ROOT, target_path[1:])
            if target_path[:2] == '//':
                # bug in os.path.join
                target_path = target_path[1:]
        else:
            # compute relative to path
            abspath = os.path.join(os.path.dirname(path), target_path)
            if abspath[:len(self.ROOT)] != self.ROOT:
                # this symlink isn't going to work anyway -- just break it immediately
                target_path = '<error>'
        try:
            os.symlink(target_path, path)
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)
        return SFTP_OK

    def readlink(self, path):
        path = self._realpath(path)
        try:
            symlink = os.readlink(path)
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)
        # if it's absolute, remove the root
        if os.path.isabs(symlink):
            if symlink[:len(self.ROOT)] == self.ROOT:
                symlink = symlink[len(self.ROOT):]
                if (len(symlink) == 0) or (symlink[0] != '/'):
                    symlink = '/' + symlink
            else:
                symlink = '<error>'
        return symlink

