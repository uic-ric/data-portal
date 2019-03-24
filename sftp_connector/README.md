README
======
Data portal SFTP connector


---
## Usage

These scripts allow integration of SFTP with the data portal in order to allow users to download/upload data via SFTP.

###

### portal_sftp.py

Main SFTP script.  The portal_keys.py script will generate authorized_keys file that will properly invoke the portal_sftp.py script.  The following is an example...

command="/usr/local/bin/portal_sftp.py gchlip2@uic.edu" ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDPJwu/YoBSgPhSfCOiQTvJqGLo4AHAENwFJqGKRLIRiEyNCYcbQwBFmxWXrtsZXR4i9XX1VXWs7gYS8/iqtcsqaxfwQBx3rPOtYolQrQZf0D+1a5jFCaWoSs9ceDFobzJlOZoccJgb2DFFfMmicpJ0pmXz+/nqoeSsg8zx6WHo++i0zey9biyVcIXM/hUYMs0Rn1jyrKKhkp87NZYsLy/1Dkp0LQ6CWEYTu/c48q6mIGLLyTPNS2UXvaR4MDdQPzg1rffooZnK+u4K03mBzZp+rqS02bqpkjluqPwo8U3NOOXeUXwcsjab0ZHzTyCHVxviVztK2eqYJPaekfVOnktJ George Chlipala <gchlip2@uic.edu>

Using the portal_keys.py script will dynamically generate the authorized_keys information for sshd.

### sftp_implementation.py

SFTPServer implementation using Paramiko SFTPServer

### portal_keys.py

The script reads SSH keys from the data portal database to dynamically generate authorized keys file for "fetch" user.  
This script requires an INI file (/etc/portal-sftp.ini) that contains the parameters for portal database and arvados installation.  This file should be readable by the AuthorizedKeysCommandUser.  The following in an example of the INI file.

[database]
user = <db user>
password = <password>
database = portaldb
host = <db host>

[arvados]
host = <arvados API host>
token = <super user token>
 
The following are example lines for the sshd_config file.

AuthorizedKeysCommand /usr/local/bin/portal_keys.py -u %u -k %f
AuthorizedKeysCommandUser fetch

Match User fetch
        X11Forwarding no
        AllowTcpForwarding no
        PermitTTY no
	PasswordAuthentication no

---
## References

- Arvados
	- [Main page](http://arvados.org)
	- [API documentation](http://doc.arvados.org/api/index.html)

