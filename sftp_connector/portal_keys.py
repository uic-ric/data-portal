#!/usr/bin/env python

import sys
import logging
import mysql.connector
import argparse
import ConfigParser
import traceback

# Setup logging for the SFTP server
logging.basicConfig(level='INFO')
log = logging.getLogger(__name__)

def print_fetch_keys(sql, key_hash=None):
    cursor = sql.cursor()
    query = "SELECT u.userid, s.ssh_key FROM users u JOIN ssh_keys s ON(u.userid = s.userid) WHERE s.ssh_key IS NOT NULL"
    if key_hash != None:
        if key_hash.startswith('SHA256:'):
            query = query + ' AND s.sha256_hash=%s'
            key_hash = key_hash[7:]
        else:
            if key_hash.startswith('MD5:'):
                key_hash = key_hash[4:]
            query = query + ' AND s.md5_hash=%s'
        cursor.execute(query, (key_hash, ))
    else:
        cursor.execute(query)

    print_keys(cursor, "/usr/local/bin/portal_sftp.py")
    cursor.close() 

def print_keys(cursor, command):
    for (userid, key) in cursor:
        print('command="{} {}" {}'.format(command, userid, key))

def print_upload_keys(sql, key_hash=None):
    cursor = sql.cursor()

    cursor.close() 

if __name__ == "__main__":
    try:
        # Setup the command line parser
        parser = argparse.ArgumentParser(description="Generate SSH keys from portal database")
        parser.add_argument('-c', '--configuration', help="Configuration file", default='/etc/portal-sftp.ini')
        parser.add_argument('-u', '--user', help="Portal user")
        parser.add_argument('-k', '--key_hash', help="Key hash")

        # Parse arguments from command line
        opts = parser.parse_args()

        # Parse configuration file (INI)
        # configuration file should have two sectons ovirt and report
        #
        # database section should have....
        # user as username for database
        # password as password for user
        # database database to use
        #
        config = ConfigParser.ConfigParser()
        config.read(opts.configuration)

        sql = mysql.connector.connect(user=config.get('database','user'), password=config.get('database','password'), 
            database=config.get('database','database'), host=config.get('database','host'), charset='utf8')

        if opts.user == 'fetch':
            print_fetch_keys(sql, opts.key_hash)
        elif opts.user == 'upload':
            print_upload_keys(sql, opts.key_hash)

    except Exception as e:
        traceback.print_exc(file=sys.stderr)

