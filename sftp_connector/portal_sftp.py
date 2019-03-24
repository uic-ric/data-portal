#!/usr/bin/env python
# A more detailed explaination will come as a blog post soon, but in brief,
# here is the problem that led to this:
#
# For various reasons we have a requirement to implement an 'sftp' like
# abstraction/interface for a service that we provide. Basically we need to
# present objects in our application as a filesystem accessible over sftp.
#
# The way we have decided to go about this is to replace the 'standard' openssh
# sftp subsystem command entry in /etc/ssh/sshd_config on our servers with our
# own customized sftp server.
#
# This would allow openssh to handle all the heavy lifting of authentication
# and setting up the transport and we'd ^just^ have to implement the a sftp
# server which would communicate over its stdin/stdout with the openssh
# established connection.
#
# I decided to use paramiko to help me build the sftp server side of things,
# but in the process had to only adapt stdin/stdout for a socket api
# (SocketAdapter below) -- neat stuff !!

import sys
import logging
import mysql.connector
import arvados
import ConfigParser

from paramiko.server import ServerInterface
from paramiko.transport import Transport
from paramiko.sftp_server import SFTPServer, SFTPServerInterface

# - an example implementation that might provide an abstraction of a filesystem
# this one just serves the actual filesystem, but you could choose to implement
# whatever is required
from sftp_implementation import PortalSFTPServer

# Setup logging for the SFTP server
import logging
from logging.handlers import SysLogHandler
log = logging.getLogger()
log.setLevel('DEBUG')
handler = SysLogHandler(address='/dev/log', facility=SysLogHandler.LOG_USER)
handler.setFormatter(logging.Formatter('portal_sftp: [%(levelname)s] [pid: %(process)d] %(msg)s in %(module)s::%(funcName)s'))
log.addHandler(handler)

# This class adapts the sys.std{in,out} to the socket interface for providing
# the recv() and send() api which paramiko calls to interact with the connected
# client channel
class SocketAdapter(object):
    """ Class that adapts stdout and stdin to the socket api to keep paramiko
        happy
    """
    def __init__(self, stdin, stdout):
        self._stdin  = stdin
        self._stdout = stdout
        self._transport = None

    def send(self, data, flags=0):
        self._stdout.flush()
        self._stdout.write(data)
        self._stdout.flush()
        return len(data)

    def recv(self, bufsize, flags=0):
        data = self._stdin.read(bufsize)
        return data

    def close(self):
        self._stdin.close()
        self._stdout.close()

    def settimeout(self, ignored):
        pass

    def get_name(self):
        # required at https://github.com/paramiko/paramiko/blob/master/paramiko/sftp_server.py#L86-L91
        return 'sftp'

    def get_transport(self):
        if not self._transport:
            self._transport = Transport(self)
        return self._transport



# - the main entry point
def start_server(params):
    # - choose the implementation of the filesystem abstraction, defaults to
    # the 'normal' local filesystem as implemented in localfs.py
    # fs_type = params[1] if len(params) > 1 else 'local'
    # server_type = {'local' : PortalSFTPServer }
    server_type = PortalSFTPServer

    # Parse configuration file (INI)
    # configuration file should have two sectons ovirt and report
    #
    # database section should have....
    # user as username for database
    # password as password for user
    # database database to use
    #
    # arvados section should have...
    # host - API hostname
    # token - API token to use
    #
    config = ConfigParser.ConfigParser()
    config.read('/etc/portal-sftp.ini')

    log.debug('going to setup adapter...')
    server_socket = SocketAdapter(sys.stdin, sys.stdout)

    log.info('portal SFTP session for user {}'.format(params[1]))

    sql = mysql.connector.connect(user=config.get('database','user'), password=config.get('database','password'), database=config.get('database','database'), charset='utf8', host=config.get('database','host'))

    arv = arvados.api(host=config.get('arvados','host'), token=config.get('arvados','token'), cache=False, insecure=True)

    log.debug('going to setup server...')
    si = ServerInterface()
    sftp_server = SFTPServer(server_socket, 'sftp', server=si, sftp_si=server_type, userid=params[1], mysql_conn=sql, arv=arv)

    log.debug('going to start server...')
    try:
        sftp_server.start()
    except Exception as e:
        log.error("ERROR: {}".format(str(e)))

# If you'd like to quickly try this out, save this file someplace, replace the
# Subsystem entry in your /etc/ssh/sshd_config file to point to the full path
# of this file and restart sshd
if __name__ == '__main__':
    log.debug('starting the sftp_server subsystem...')
    try:
        start_server( sys.argv )
    except Exception as e:
        log.error("ERROR: {}".format(str(e)))
