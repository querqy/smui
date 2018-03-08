# Search Management UI (SMUI) - Manual version 0.9.4

## INSTALLATION

Please follow the above steps in this order.

### Step 1: Install RPM

Example script (command line):

```
rpm -i PATH/search-management-ui-VERSION.noarch.rpm
```

* Ensure user running search-management-ui can read binary, configs, etc. and write PID file (usually within the folder `/usr/share/search-management-ui`).
* Ensure the user has permission to access and write onto the log file as well (usually `/var/log/search-management-ui/*`).
* Ensure the user has permission to access and write onto the `/tmp` directory as well.
* Ensure `search-management-ui` service is being included to your Server's start up sequence (e.g. `init.d`).

It might be necessary to execute command with root rights.

### Step 2: Create and configure database (SQL level)

Create MariaDB- or MySQL-database, user and assign according permissions. Example script (SQL):

```
CREATE USER 'smui'@'localhost' IDENTIFIED BY 'smui';
CREATE DATABASE smui;
GRANT ALL PRIVILEGES ON smui.* TO 'smui'@'localhost' WITH GRANT OPTION;
```

### Step 3: Adjust configuration

#### rules.txt copy- and update-script (smui2solr.sh)

The script should be located under `/usr/share/search-management-ui/conf/smui2solr.sh`. Adjust the `DST_CP_FILE_TO` to the Solr/querqy rules.txt file's path:

```
[...]
DST_CP_FILE_TO="/todo/path/to/rules.txt"
[...]
```

#### application.conf (of JAVA/Play web application)

The .conf file is usually located under:

```/usr/share/search-management-ui/conf/application.conf```

Adjust database URL:

```
[...]
db.default.url="jdbc:mysql://localhost/smui?autoReconnect=true&useSSL=false"
[...]
```

* Define host (`localhost` or else).
* Define, if SSL should be used for SQL-connection (`useSSL`).

Then first time start the service. Example script (command line):

```
search-management-ui &
```

Or via `service` command, or automatic startup after reboot respectively (see Step 1).

### Step 4: Create initial data (SQL level)

#### Solr Collections to maintain Search Management rules for

There must exist a minimum of 1 Solr Collection, that Search Management rules are maintained for. This must be created before the application can be used. Example script (SQL):

```
INSERT INTO solr_index (name, description) VALUES ('index_name1', 'Solr Search Index');
[...]
```

Hint: `solr_index.name` (in this case `index_name1`) will be used as the name of the Solr core, when performing a Core Reload (see `smui2solr.sh`).

#### Initial Solr Fields

Optional. Example script (SQL):

```
INSERT INTO suggested_solr_field (name, solr_index_id) values ('microline1', 1);
[...]
```

Refresh Browser window and you should be ready to go.

## MAINTENANCE

## Log data

The Log file(s) is/are located under the following path:

```
/var/log/search-management-ui/
```

Server log can be watched by example script (command line):

```
tail -f /var/log/search-management-ui/search-management-ui.log
```

### Add a new Solr Collection (SQL level)

See "Step 5". Example script (SQL):

```
INSERT INTO solr_index (name, description) VALUES ('added_index_name2', 'Added Index Description #2');
INSERT INTO solr_index (name, description) VALUES ('added_index_name3', 'Added Index Description #3');
[...]
```

### Add new Solr Fields (SQL level)

See "Step 5". Example script (SQL):

```
INSERT INTO suggested_solr_field (name, solr_index_id) values ('added_solr_field2', 1);
INSERT INTO suggested_solr_field (name, solr_index_id) values ('added_solr_field3', 1);
[...]
```
