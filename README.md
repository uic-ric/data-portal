Data portal
===========

The data portal application was developed to allow core facility personnel to share data stored in Arvados with end users. The data portal application allows sharing select files, with descriptors, from a project with the end users and bundle these files into releases. The data portal application also allows selected end users to be designated as “owners” who can add and remove other end users from a project.

A SFTP connector is also included that allows end users to download their data, in a self-service fashion, using a public key authentication.  Users are able to manage their SSH keys via the main web application.

# Requirements

- Tomcat v8
- MySQL/MariaDB v5.5
- Arvados

## SFTP connector

- OpenSSH
- Python 2.7
- Paramiko python packages
- mysql.connector python packages
- Arvados python API

# Tomcat Context Configuration

# Datasource

A MySQL datasource with the name `jdbc/portaldb` should be defined for the servlet context for the application.  This datasource configuration should reference an empty database that will used to store the release information for the data portal.

# Authentication Realm

The following Realm should be defined in the servlet Context for the application.

```
<Realm className="org.apache.catalina.realm.DataSourceRealm" dataSourceName="jdbc/portaldb" localDataSource="true" roleNameCol="role" userCredCol="pass" userNameCol="userid" userRoleTable="users" userTable="users">
	<CredentialHandler className="org.apache.catalina.realm.MessageDigestCredentialHandler" algorithm="sha"/>
</Realm>
```

# Mail session

A mail session resource must be defined to allow the application to send notices.  If the application is running on a Linux system, the following configuration should work.

```
<Resource auth="Container" mail.smtp.host="localhost" name="mail/Session" type="javax.mail.Session"/>
```

## Context Parameters

The following context parameters can be added to the context configuration to alter the behavior of the web application.  

### Required

- `ARVADOS_API_HOST` - Arvados API host
- `ARVADOS_API_TOKEN` - Superuser Arvados API token, required to allow end users to access project information and download released files.
- `KEEP_WEB` - Base URL for keep web, e.g. https://download.<uuid_prefix>.<your.domain>. Refer the installation details of your Arvados instance.
- `CONTACT_EMAIL` - Email to include in text of email notices.
- `FILE_PATH` - Path to use as staging area for uploads

# Optional

- `CONTACT_NAME` - Name to use in signature line of email notices.  Default will be "<BRAND> Administrators"
- `BRAND` - Brand name for web application.  Default is "Data Portal"
- `SFTP_HOST` - Host name of server running SFTP connector.  Default is to use the same hostname for the web application.
- `SFTP_USER` - SFTP user. Default is "fetch".
- `MAX_UPLOAD_FILE_SIZE` - Maximum allowed upload size, in bytes.  Default is 2 MB (2097152).

# Compiling

The code was compiled using WebApp libraries for Tomcat v8.

If using Eclipse...

1. Import the code as a "Dynamic Web Project".  Java source code is in `src` directory and web content in `WebContent` directory.  
2. Convert to Maven project by right clicking on project folder then select "Configure" > "Convert to Maven project".  
3. Have Maven "Update Project" to load dependent libraries from the Maven repositories.
4. Build the project.
5. Export the project as a WAR. 
