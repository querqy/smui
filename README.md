[![Travis CI build status](https://travis-ci.org/pbartusch/smui.svg?branch=master)](https://travis-ci.org/pbartusch/smui)

# Search Management UI (SMUI)

![SMUI v1.5.0 screenshot](20190103_screenshot_SMUI_v1-5-0.png)

SMUI is a tool for managing Solr-based onsite search. It provides a web user interface for maintaining rules for query rewriting based on the Querqy Solr plugin for query rewriting. Please see [querqy](https://github.com/renekrie/querqy) for the installation of Querqy.

## RELEASE NOTES

### Major changes in v3.5

* SMUI provides a docker only runtime environment (including a safe `smui:smui` user). Installing SMUI from an RPM image is not supported (and therefore not documented) any more.

### Major changes in v3 (compared to v2)

* Auto-DECORATE do not exist any more. Please migrate any usage to Auto-Log-Rule-ID
* SMUI for that feature depends now on v3.3 of [querqy](https://github.com/renekrie/querqy)

### Major changes in v2 (compared to v1)

* Database schema definition (SQL) implemented more lightweight, so that SMUI can be operated with every standard SQL database (entity IDs therefore needed to be adjusted, see "Migrate pre-v2 SMUI databases" for details)

## INSTALLATION

### Step 1: Create and configure database (SQL level)

SMUI needs a database backend in order to manage and store search management rules.

Supported (tested) databases:

Generally SMUI database connection implementation is based on JDBC and only standard SQL is used, so technically every database management system supported by JDBC should feasible when using SMUI. However as database management systems potentially come with specific features, SMUI explicity is tested (and/or productively used) only with the following database management systems:

* MySQL & MariaDB
* PostgreSQL
* SQLite
* HSQLDB

You can decide, where your database backend application should be run and host its data. In an productive environment, e.g. you could run a docker based database application (e.g. [https://hub.docker.com/_/mariadb](official dockerhub MariaDB image)) within the same (docker) network like SMUI. However, for the sake of simplicity, the following sections assumes you have a local (MariaDB) database application running on the host environment.

Create SMUI database, user and assign according permissions. Example script (SQL, MariaDB / MySQL):

```
CREATE USER 'smui'@'localhost' IDENTIFIED BY 'smui';
CREATE DATABASE smui;
GRANT ALL PRIVILEGES ON smui.* TO 'smui'@'localhost' WITH GRANT OPTION;
```

#### Migrate pre-v2 SMUI databases

As of version 3.3 it has become possible to migrate prior version's search management input and rules via the `rules.txt` file. See "Import existing rules.txt" for details.

### Step 2: Install SMUI application (using docker image or Docker Hub repository)

#### Use SMUI docker image from Docker Hub (recommended)

SMUI is also integrated into a Travis CI build pipeline, that provides a Docker Hub SMUI image. You can pull the latest SMUI (master branch) from its public dockerhub repository, e.g. (command line):

```
docker pull pbartusch/smui:latest
```

#### Manually build the SMUI docker container

SMUI provides a [Makefile](Makefile) to help you with the manual docker build process. You can use `make` to build as a docker container, e.g. (command line):

```
make docker-build-only
```

NOTE: If you are not having `make` available, you can manually reproduce the according `docker build` command.

### Step 3: Minimum SMUI configuration and start of the application

SMUI is configured passing environment variables to the docker container SMUI runs on. The following section describes all parameters, that you can configure SMUI with. Mappings of config keys to environment variables can be found in [application.conf](conf/application.conf) (e.g. `SMUI_DB_JDBC_DRIVER` environment variable sets `db.default.driver`).

NOTE: Environment variables are the preferred way to configure your production environment. In contrast, while developing (outside a docker environment) it is possible to use a local `smui-dev.conf` file (see "DEVELOPMENT SETUP").

The following sections describe application configs in more detail.

#### Configure basic settings

The following settings can (and should) be overwritten on application.conf in your own `smui-prod.conf` level:

config key | description | default
--- | --- | ---
`db.default.driver` | JDBC database driver | MySQL database on localhost for smui:smui
`db.default.url` | Database host and optional connection parameters (JDBC connection string) | MySQL database on localhost for smui:smui.
`db.default.username` and `db.default.password` | Database credentials | MySQL database on localhost for smui:smui.
`smui2solr.SRC_TMP_FILE` | Path to temp file (when rules.txt generation happens) | local /tmp file in docker container (recommended: leave default). WARNING: Deprecated as of v3.4, will be replaced soon.
`smui2solr.DST_CP_FILE_TO` | LIVE `rules.txt` destination file for the default deployment script. See "Details on rules.txt deployment" for more info. WARNING: Deprecated as of v3.4, will be replaced soon. | `/usr/bin/solr/defaultCore/conf/rules.txt`
`smui2solr.SOLR_HOST` | Solr host | Virtual local Solr instance. WARNING: Deprecated as of v3.4, will be replaced soon.
`play.http.secret.key` | Encryption key for server/client communication (Play 2.6 standard) | unsecure default.

#### Start SMUI (docker) application

Using the config key's environment variable equivalents (as defined in the [application.conf](conf/application.conf)), the following start command can be used to bootstrap the SMUI (docker) application.

NOTE: For security reasons, within the docker container, SMUI is run as `smui` user (group: `smui`) with a `uid` of `1024`. For rules.txt deployment onto the host file system, you need to make sure, that an according user (`uid`) exists on the host (see "Details on rules.txt deployment" for more info).

A minimum start command can look like this (working with the default setup as described above) running SMUI on its default port 9000, e.g. (command line):

```
docker run \
  -p 9000:9000 \
  -v /tmp/smui_deployment_path:/usr/bin/solr/defaultCore/conf \
  pbartusch/smui
```

This will deploy a `rules.txt` to the `/tmp/smui_deployment_path` of the host (if user and permission requirements are set accordingly).

NOTE: In a productive scenario, you can as well use a `docker-compose.yml` to define the SMUI (docker) runtime environment.

### Step 4: Full feature configuration for SMUI

The following sections describe:

* Configuration of the application behaviour / feature toggles (e.g. rule tagging)
* Details and options for the deployment (of Querqy's `rules.txt` file)
* Configuration of authentication

#### Configure application behaviour / feature toggles

Optional. The following settings in the `application.conf` define its (frontend) behaviour:

config key | description | default
--- | --- | ---
`toggle.ui-concept.updown-rules.combined` | Show UP(+++) fields instead of separated rule and intensity fields. | `true`
`toggle.ui-concept.all-rules.with-solr-fields` | Offer a separated "Solr Field" input to the user (UP/DOWN, FILTER). | `true`
`toggle.rule-deployment.log-rule-id` | With every exported search input, add an additional @_log line that identifies the ID of the rule (if info logging in the search-engine / Solr for querqy is activated, see `querqy.infoLogging=on`, it is being communicated in the search-engine's / Solr response). | `false`
`toggle.rule-deployment.split-decompound-rule-txt` | Separate decompound synonyms (SOME* => SOME $1) into an own rules.txt file. WARNING: Activating this results in the need of having the second special-purpose-DST_CP_FILE_TO configured (see below). Temp file path for this purpose will be generated by adding a `-2` to `smui2solr.SRC_TMP_FILE`. WARNING: Deprecated as of v3.4, will be replaced soon. | `false`
`toggle.rule-deployment.split-decompound-rule-txt-DST_CP_FILE_TO` | Path to productive querqy decompound-rules.txt (within Solr context). WARNING: Deprecated as of v3.4, will be replaced soon. | Example content. Needs to be adjusted, if split for decompound rules.txt has been activated.
`toggle.rule-deployment.pre-live.present` | Make separated deployments PRELIVE vs. LIVE possible (and display a button for that on the frontend). | `false`
`smui2solr.deploy-prelive-fn-rules-txt` | PRELIVE `rules.txt` destination file for the default deployment script. See "Details on rules.txt deployment" for more info. | `/usr/bin/solr/defaultCore/conf/rules.txt`
`smui2solr.deploy-prelive-solr-host` | Host and port (e.g. `localhost:8983`) of Solr PRELIVE instance. If left empty, the default deployment script will not trigger a core reload after deployment. | Empty. In case core reload on PRELIVE deployments should be triggered, this needs to be set.
`smui2solr.deploy-prelive-fn-decompound-txt` | Separate decompound synonyms for PRELIVE (see above). | `/usr/bin/solr/defaultCore/conf/rules-decompound.txt`
`toggle.rule-deployment.custom-script` | If set to `true` the below custom script (path) is used for deploying the rules.txt files. | `false`
`toggle.rule-deployment.custom-script-SMUI2SOLR-SH_PATH` | Path to an optional custom script (see above). | Example content. Needs to be adjusted, if a custom deployment script is activated.
`toggle.rule-tagging` | Should tagging feature be activated. | `false`
`toggle.predefined-tags-file` | Path to optional file, that provides pre-defined rule tags (see "Configure predefined rule tags"). | ``
`smui.auth.ui-concept.simple-logout-button-target-url` | Target URL of simple logout button (see "Configure Authentication"). | ``

NOTE: The above described feature toggles are passed to SMUI's docker container using according environment variables. The mappings can be found in the [application.conf](conf/application.conf).

##### Configure predefined rule tags (optional)

Optional. You can define pre-defined rule tags, that can be used by the search manager to organise or even adjust the rules exported to the rules.txt. See [TestPredefinedTags.json](test/resources/TestPredefinedTags.json) for structure.

#### Details and options for the deployment (`rules.txt`)

The default deployment script supports `cp` or `scp` file transfer method to deploy the `rules.txt` and triggers a Solr core on the target system, if configured accordingly. Its behaviour is controlled using the config variables above, e.g.:

```
docker run \
  ...
  -e SMUI_2SOLR_DST_CP_FILE_TO=remote_user:remote_pass@remote_host:/path/to/live/solr/defaultCore/conf/rules.txt \
  -e SMUI_2SOLR_SOLR_HOST=remote_solr_host:8983 \
  -e SMUI_DEPLOY_PRELIVE_FN_RULES_TXT=/mnt/prelive_solr_depl/rules.txt \
  -e SMUI_DEPLOY_PRELIVE_SOLR_HOST=docker_host:8983 \
  ...
  -v /path/to/prelive/solr/defaultCore/conf:/mnt/prelive_solr_depl
  ...
  pbartusch/smui
```
(config parameters are expressed as according environment variable names, like applicable in a docker setup, see [application.conf](conf/application.conf))

In this particular example, the LIVE instance of Solr runs on `remote_solr_host` and can be reached by `remote_user` on `remote_host` for `rules.txt` deployment (NOTE: `remote_host` as well as `remote_solr_host` might even be the same instance, but just have differing network names). `scp` will be chosen by the default deployment script.
In contrast to that, the PRELIVE instance of Solr resides on the `docker_host`. File deployment is ensured using an according docker volume mount. `cp` will be chosen.

NOTE: The example above also accounts for `SMUI_TOGGLE_DEPL_DECOMPOUND_DST` and `SMUI_DEPLOY_PRELIVE_FN_DECOMPOUND_TXT`, when `SMUI_TOGGLE_DEPL_SPLIT_DECOMPOUND` is set to `true`.

#### Configuration of authentication

SMUI is shipped with HTTP Basic and JWT Authentication support.

##### Basic Authentication
This is telling every controller method (Home and ApiController) to use the according authentication method as well as it tells SMUI's `BasicAuthAuthenticatedAction` username and password it should use. 
Basic Auth can be turned on in the extension by configuring an `smui.authAction` in the config file, e.g.:

```
# For Basic Auth authentication, use SMUI's BasicAuthAuthenticatedAction (or leave it blanked / commented out for no authentication), e.g.:
smui.authAction = controllers.auth.BasicAuthAuthenticatedAction
smui.BasicAuthAuthenticatedAction.user = smui_user
smui.BasicAuthAuthenticatedAction.pass = smui_pass
```

##### JWT Authentication

```
smui.authAction="controllers.auth.JWTJsonAuthenticatedAction"
```

config key | description | default
--- | --- | ---
`smui.JWTJsonAuthenticatedAction.login.url` | The URL to the login page (e.g. https://loginexample.com/login.html?callback=https://redirecturl.com)" | ``
`smui.JWTJsonAuthenticatedAction.cookie.name` | Name of cookie that contains the Json Web Token (JWT) | `jwt_token`
`smui.JWTJsonAuthenticatedAction.public.key` | The public key to verify the token signature | ``
`smui.JWTJsonAuthenticatedAction.algorithm` | The algorithms that should be used for decoding (options: 'rsa', 'hmac', 'asymmetric', 'ecdsa') | `rsa`
`smui.JWTJsonAuthenticatedAction.authorization.active` | Activation of authorization check | `false`
`smui.JWTJsonAuthenticatedAction.authorization.json.path` | The JSON path to the roles saved in the JWT | `$.roles`
`smui.JWTJsonAuthenticatedAction.authorization.roles` | Roles (comma separated) of roles that are authorized to access SMUI | `admin`

Example of decoded Json Web Token:

```json
{
  "user": "Test Admin",
  "roles": [
    "admin"
  ]
}
```  

##### Logout
In this setup SMUI can provide a simple logout button, that simply sends the user to a configured target URL:

```
smui.auth.ui-concept.simple-logout-button-target-url="https://www.example.com/logoutService/"
```

##### Custom Authentication
You can also implement a custom authentication action and tell SMUI to decorate its controllers with that, e.g.:

```
smui.authAction = myOwnPackage.myOwnAuthenticatedAction
```

See "Developing Custom Authentication" for details.

### Step 4: Create SMUI admin data initially (REST interface)

Once the database scheme has been established, the initial data can be inserted. SMUI supports a REST interface to PUT admin entities (like the following) into the database.

#### Solr Collections to maintain Search Management rules for

There must exist a minimum of 1 Solr Collection (or querqy/`rules.txt` deployment target), that Search Management rules are maintained for. This must be created before the application can be used. Example `curl` (relative to `localhost:9000`):

```
curl -X PUT -H "Content-Type: application/json" -d '{"name":"core_name1", "description":"Solr Search Index/Core #1"}' http://localhost:9000/api/v1/solr-index
[...]
```

NOTE: `solr-index/name` (in this case `core_name1`) will be used as the name of the Solr core, when performing a Core Reload (see `smui2solr.sh`).

#### Initial Solr Fields

Optional. Example `curl` (relative to `localhost:9000`):

```
curl -X PUT -H "Content-Type: application/json" -d '{"name":"solr-field-1"}' http://localhost:9000/api/v1/{SOLR_INDEX_ID}/suggested-solr-field
[...]
```

Where `solr-field-1` refers to the field in your configured Solr schema you would like to make addressable to the Search Manager. `{SOLR_INDEX_ID}` refers to the index ID created by the `solr-index` call above.

Refresh Browser window and you should be ready to go.

## USING SMUI

### Search rules

SMUI supports the following search rules, that can be deployed to a Querqy supporting search engine (like [Solr](https://lucene.apache.org/solr/)):

* `SYNONYM` (directed & undirected)
* `UP` / `DOWN`
* `FILTER`
* `DELETE`

Please see [Querqy](https://github.com/renekrie/querqy) for a description of those rules.

Furthermore, SMUI comes with built in `DECORATE` rules for certain use cases:

* `REDIRECT` (as Querqy/`DECORATE`) to a specific target URL

SMUI might as well leverages querqy's `@_log` property to communicate SMUI's rule ID back to the search-engine (Solr) querying instance.

### Import existing rules (rules.txt)

As of version 3.3 SMUI supports importing an existing rules.txt file and adding its content to the SMUI database. The following steps outline the procedure

* uses an existing Solr index or create a new one
* uses the new `import-from-rules-txt` endpoint to upload / import a rules.txt file

e.g.:
```
curl -X PUT  -H "Content-Type: application/json" -d '{"name": "mySolrCore", "description": "My Solr Core"}' http://localhost:9000/api/v1/solr-index
#> {"result":"OK","message":"Adding Search Input 'mySolrCore' successful.","returnId":"a4aaf472-c0c0-49ac-8e34-c70fef9aa8a9"}
#> a4aaf472-c0c0-49ac-8e34-c70fef9aa8a9 is the Id of new Solr index
curl -F 'rules_txt=@/path/to/local/rules.txt' http://localhost:9000/api/v1/a4aaf472-c0c0-49ac-8e34-c70fef9aa8a9/import-from-rules-txt
```

NOTE: If you have configured SMUI with authentication, you need to pass authentication information (e.g. BasicAuth header) along the `curl` request.

WARNING: As of version 3.3 the rules.txt import endpoint only supports `SYNONYM`, `UP` / `DOWN`, `FILTER` and `DELETE` rules. Redirects, other `DECORATE`s, as well as Input Tags will be omitted, and not be migrated using the import endpoint.

### Use SMUI's REST interface to create an search input with according rules

Like SMUI's (angular) frontend, you are capable of leveraging SMUI's REST interface to create and update search management rules programmatically. Rules have corresponding search inputs, that they are working on. If you want to create rules programmatically it is therefore important to keep track of the input the rules should refer to. As processing relies on parsing JSON input and output, the python script under [docs/example_rest_crud.py](docs/example_rest_crud.py) will create one search input, that will be updated with one `SYNONYM` and one `FILTER` rule as an example.

### Monitor SMUI's log file

SMUI's log file is located under the following path (in the SMUI docker container):

```
/smui/logs/application.log
```

Server logs can be watched using `docker exec`, e.g. (command line):

```
docker exec -it <CONTAINER_PS_ID> tail -f /smui/logs/application.log
```

## DEVELOPMENT SETUP

For developing new features and test the application with different type of configuration, it is recommended to create a local development configuration of the application (instead of the productive one described above). There is the `smui-dev.conf` being excluded from version control through the `.gitignore`, so that you can safely create a local development configuration in the project's root (naming it `smui-dev.conf`). Here is an example being used on a local development machine adjusting some features:

```
include "application.conf"

db.default.url="jdbc:mysql://localhost/smui?autoReconnect=true&useSSL=false"
db.default.username="local_dev_db_user"
db.default.password="local_dev_db_pass"

smui2solr.SRC_TMP_FILE="/PATH/TO/LOCAL_DEV/TMP/FILE.tmp"
smui2solr.DST_CP_FILE_TO="PATH/TO/LOCAL_DEV/SOLR/CORE/CONF/rules.txt"
smui2solr.SOLR_HOST="localhost:8983"

toggle.ui-concept.updown-rules.combined=true
toggle.ui-concept.all-rules.with-solr-fields=true
toggle.rule-deployment.log-rule-id=true
toggle.rule-deployment.split-decompound-rules-txt=true
toggle.rule-deployment.split-decompound-rules-txt-DST_CP_FILE_TO="/PATH/TO/LOCAL_DEV/SOLR/CORE/CONF/decompound-rules.txt"
toggle.rule-deployment.pre-live.present=true
toggle.rule-deployment.custom-script=true
toggle.rule-deployment.custom-script-SMUI2SOLR-SH_PATH="/PATH/TO/LOCAL_DEV/smui2solr-dev.sh"
toggle.rule-tagging=true
toggle.predefined-tags-file="/PATH/TO/LOCAL_DEV/predefined-tags.json"

...

play.http.secret.key="<generated local play secret>"

# smui.authAction = controllers.auth.BasicAuthAuthenticatedAction
# smui.BasicAuthAuthenticatedAction.user = smui_dev_user
# smui.BasicAuthAuthenticatedAction.pass = smui_dev_pass
```

As you can see, for development purposes you are recommended to have a local Solr installation running as well.

For running The SMUI application locally on your development machine pass the above config file when starting the application in `sbt`, e.g.:

```
run -Dconfig.file=./smui-dev.conf 9000
```

Furthermore, above's configuration points to a deviant development version of the `smui2solr.sh`-script. The file `smui2solr-dev.sh` is as well excluded from the version control. The following example provides a simple custom deployment script approach, that basically just delegates the script call to the main `smui2solr.sh` one:

```
echo "In smui2solr-dev.sh - DEV wrapper for smui2solr.sh, proving custom scripts work"

BASEDIR=$(dirname "$0")
$BASEDIR/conf/smui2solr.sh "$@"
exit $?
```

It can be used as a basis for extension.

NOTE: Remember to give it a `+x` permission for being executable to the application.

### Developing Custom Authentication

#### Authentication Backend

If you want to extend SMUI's authentication behaviour, you can do so by supplying your own authentication implementation into the classpath of SMUI's play application instance and referencing it in the `application.conf`. Your custom authentication action offers a maximum of flexibility as it is based upon play's `ActionBuilderImpl`. In addition your custom action gets the current environment's `appConfig`, so it can use configurations defined there as well. Comply with the following protocol:

```
import play.api.Configuration
import play.api.mvc._
import scala.concurrent.ExecutionContext
class myOwnAuthenticatedAction(parser: BodyParsers.Default,
                               appConfig: Configuration)(implicit ec: ExecutionContext) extends ActionBuilderImpl(parser) {
override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
	...
}
```

As an example implementation, you can check [BasicAuthAuthenticatedAction.scala](app/controllers/auth/BasicAuthAuthenticatedAction.scala) as well.

#### Frontend Behaviour for Authentication

The Angular frontend comes with a built-in HTTP request authentication interceptor. Every API request is observed for returned 401 status codes. In case the backend returns 401, the backend can pass an behaviour instruction to the frontend by complying with spec defined by `SmuiAuthViolation` within [http-auth-interceptor.ts](app/assets/app/http-auth-interceptor.ts), e.g.:

```
{
  "action": "redirect",
  "params": "https://www.example.com/loginService/?urlCallback={{CURRENT_SMUI_URL}}"
}
```

NOTE: The authentication interceptor only joins the game, in case the Angular application is successfully bootstrap'ed. So for SMUI's `/` route, your custom authentication method might choose a different behaviour (e.g. 302).

Within exemplary `redirect` action above, you can work with the `{{CURRENT_SMUI_URL}}` placeholder, that SMUI will replace with its current location as an absolute URL before the redirect gets executed. Through this, it becomes possible for the remote login service to redirect back to SMUI once the login has succeeded.

## License
Search Management UI (SMUI) is licensed under the [Apache License, Version 2](http://www.apache.org/licenses/LICENSE-2.0.html).

### Contributors

 - [Paul M. Bartusch](https://github.com/pbartusch), Committer/Maintainer
 - [Michael Gottschalk](https://github.com/migo)
 - [Matthias Krüger](https://github.com/mkr)
 - [Gunnar Busch](https://github.com/gunnarbusch)
