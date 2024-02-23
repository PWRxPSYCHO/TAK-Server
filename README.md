# TAK Server Development
*Requires Java 17*

* Linux or MacOS is recommended for development. If using Windows, replace "gradlew" with "gradlew.bat" in commands below. An x86-64 architecture CPU is required to build from source, including on MacOS. M1 or M2 Apple silicon is not supported.

Links:
 * [Test Execution](src/takserver-takcl-core/docs/testing.md)
 * [Test Architecture and Development](src/takserver-takcl-core/docs/Development.md)
 * [Publishing](src/docs/publishing.md)

---
Clean and Build TAK Server, including war, retention service, plugin manager, user manager and schema manager.
```
cd src
./gradlew clean bootWar bootJar shadowJar
```

In Eclipse, choose File -> Import -> Gradle -> Existing Gradle Project

Navigate to `takserver/src`

Select Finish. The TAK Server parent project, and all subprojects, will be imported into Eclipse.

Install PostgreSQL + PostGIS extension locally on your workstation, or run the docker container as described below. If installing locally, use 

Start the Postres server.

To run a local PostgreSQL + PostGIS container, follow the commands below using the official PostGIS database docker container as follows, and changing the environment variables supplied to the container as necessary. Note the '--rm' means the container will be destroyed when it is stopped.

```
docker run -it -d --rm --name TakserverServer0DB \
    --env POSTGRES_PASSWORD=e815f795745e \
    --env POSTGRES_HOST_AUTH_METHOD=trust \
    --env POSTGRES_USER=martiuser \
    --env POSTGRES_DB=cot \
    -p 5432 postgis/postgis:15-3.3

echo SQL SERVER IP: `docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' TakserverServer0DB`
```

Setup Local Database. If the postgis container was used, only the last two lines should be necessary.  
```
- cd src/takserver-schemamanager
- psql -d postgres  -c "CREATE ROLE martiuser LOGIN ENCRYPTED PASSWORD 'md564d5850dcafc6b4ddd03040ad1260bc2' SUPERUSER INHERIT CREATEDB NOCREATEROLE;"
- createdb --owner=martiuser cot
- ../gradlew shadowJar
- java -jar build/libs/schemamanager-<version>-uber.jar upgrade # Make sure that the CoreConfig.xml is in the current directory
```

Configure Local CoreConfig and Certs
```
cd takserver-core/example
```

This is the CoreConfig that takserver war will look for when running from the takserver-core/example directory. From this point, just follow the instructions at takserver/src/docs/TAK_Server_Configuration_Guide.pdf to set up the CoreConfig and Certs. Make sure that the CoreConfig now points to the directory where the certs were generated locally.

See appendix B in src/docs/TAK_Server_Configuration_Guide.pdf for cert generation instructions.

### Build TAK server to run locally for development

Note that due to Java 17, there are a lot of '--add-opens' arguments in the JDK_JAVA_OPTIONS
```
cd takserver-core
../gradlew clean bootWar bootJar
cd example
export IGNITE_HOME="$PWD/ignite"
export JDK_JAVA_OPTIONS="-Dloader.path=WEB-INF/lib-provided,WEB-INF/lib,WEB-INF/classes,file:lib/ -Djava.net.preferIPv4Stack=true -Djava.security.egd=file:/dev/./urandom -DIGNITE_UPDATE_NOTIFIER=false -DIGNITE_QUIET=true -Dio.netty.tmpdir=$PWD -Djava.io.tmpdir=$PWD -Dio.netty.native.workdir=$PWD -Djdk.tls.client.protocols=TLSv1.2  --add-opens=java.base/sun.security.pkcs=ALL-UNNAMED --add-opens=java.base/sun.security.pkcs10=ALL-UNNAMED --add-opens=java.base/sun.security.util=ALL-UNNAMED --add-opens=java.base/sun.security.x509=ALL-UNNAMED --add-opens=java.base/sun.security.tools.keytool=ALL-UNNAMED --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED --add-opens=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED --add-opens=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED --add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.math=ALL-UNNAMED --add-opens=java.sql/java.sql=ALL-UNNAMED --add-opens=java.base/javax.net.ssl=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=jdk.unsupported/sun.misc=ALL-UNNAMED --add-opens=java.base/java.lang.ref=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/java.security.ssl=ALL-UNNAMED --add-opens=java.base/java.security.cert=ALL-UNNAMED --add-opens=java.base/sun.security.rsa=ALL-UNNAMED --add-opens=java.base/sun.security.ssl=ALL-UNNAMED --add-opens=java.base/sun.security.x500=ALL-UNNAMED --add-opens=java.base/sun.security.pkcs12=ALL-UNNAMED --add-opens=java.base/sun.security.provider=ALL-UNNAMED --add-opens=java.base/javax.security.auth.x500=ALL-UNNAMED"

```
### Running TAK server locally for development

TAK server consists of three processes: Configuration, Messaging and API. 

The configuration process needs to be running first in order for the Messaging, API or any other services to retrieve the centralized configuration.  This is separate from the TAKIgniteConfiguration that is loaded **per service** using defaults or the overridden values in TAKIgniteConfig.xml.  

The messaging process can run independently, but the API process may need to connect to the ignite server that runs as a part of the messaging process if it is not configured to run its own Ignite server. For both processes, -Xmx should always be specified.

Note - These commands include the **duplicatelogs** profile. This turns off the filter that blocks duplicated log messages that cause log spam in operational deployments of TAK Server.

#### Run Configuration Microservice
```
java -server -XX:+AlwaysPreTouch -XX:+UseG1GC -XX:+ScavengeBeforeFullGC -XX:+DisableExplicitGC -Xmx<value> -Dspring.profiles.active=config,duplicatelogs -jar ../build/libs/takserver-core-xyz.war
```
#### Run Messaging Microservice
```
java -server -XX:+AlwaysPreTouch -XX:+UseG1GC -XX:+ScavengeBeforeFullGC -XX:+DisableExplicitGC -Xmx<value> -Dspring.profiles.active=messaging,duplicatelogs -jar ../build/libs/takserver-core-xyz.war
```
#### Run API Microservice
```
java -server -XX:+AlwaysPreTouch -XX:+UseG1GC -XX:+ScavengeBeforeFullGC -XX:+DisableExplicitGC -Xmx<value> -Dspring.profiles.active=api,duplicatelogs -Dkeystore.pkcs12.legacy -jar ../build/libs/takserver-core-xyz.war
```

#### Run Plugin Manager Microservice (optional - useful when working on plugin capability)
```
java -server -XX:+AlwaysPreTouch -XX:+UseG1GC -XX:+ScavengeBeforeFullGC -XX:+DisableExplicitGC -Xmx<value> -jar ../../takserver-plugin-manager/build/libs/takserver-plugin-manager-xyz.jar 
```

### RPM Generation
Separate RPMs are generated to install the following components of TAK server:

* api
* messaging
* database

To build all RPMs:
```
cd <repo-home>/src
./gradlew clean buildRpm
```

Subproject RPMs may be built individually using the following commands:
 
* takserver-package:api:buildRpm
* takserver-package:messaging:buildRpm
* takserver-package:database:buildRpm
* takserver-package:launcher:buildRpm
* takserver-package:takserver:buildRpm

## Certificates
TAK Server uses client and server certificates, TLS and X.509 mutual authentication and for channel encryption. Scripts for generating a private security enclave, including a Certificate Authority (CA), and certs for use by TAK Server and clients are located in /utils/misc/certs.

See the TAK Server configuration guide (docs/TAK_Server_Configuration_Guide.pdf) for additional information about TAK Server's capabilities.

## Logging
Logging levels for loggers at the class or package level can be set on startup:
```
java -Xmx<value> -Dspring.profiles.active=messaging -jar ../build/libs/takserver-core-1.3.13-DEV-xyz.war --logging.level.com.bbn.marti.sync=DEBUG --logging.level.marti_data_access_audit_log=OFF
```

turn down log level of all logs:
```
java -jar takserver.war $@ --logging.level.root=ERROR
```

turn down log level for subscriptions:
```
java -jar takserver.war $@ --logging.level.com.bbn.marti.service.Subscription=ERROR
```

turn off logs just for subscriptions:
```
java -jar takserver.war $@ --logging.level.com.bbn.marti.service.Subscription=OFF
```

entirely disable most logging:
```
java -jar takserver.war $@ --logging.level.root=OFF
```

The default log level for most things is INFO. Possible levels are INFO, WARN, ERROR, OFF (in order of decreasing log frequency)


These levels can be applied globally with this option 

```--logging.level.root=<level>```

i.e.

```--logging.level.root=ERROR```

The TAK Server log files can be found in the _logs_ subdirectory:

1. _takserver-config.log_ - Execution-level information about the configuration process including setup, error messages and warnings.
2. _takserver-messaging.log_ - Execution-level information about the messaging process, including client connection events, error messages and warnings.
3. _takserver-api.log_ - Execution-level information about the API process, including error messages and warnings.
4. _takserver-config-console.log_ - Java Virtual Machine (JVM) informational messages and errors, for the config process.
5. _takserver-messaging-console.log_ - Java Virtual Machine (JVM) informational messages and errors, for the messaging process.
6. _takserver-api-console.log_ - Java Virtual Machine (JVM) informational messages and errors, for the API process.

## Swagger
https://localhost:8443/swagger-ui.html

## TAK Server CI

### Integration Tests

Integration tests are executed against master nightly. In addition to this, they can be executed on any branch as follows:  
1.  Navigate to the [TAKServer Dashboard](https://git.tak.gov/core/takserver).  
2.  On the sidebar, hover over 'CI/CD' and select 'Pipelines'.  
3.  Find your commit from the list and tap the Play button to the right, and select the test suite you would like to execute.  The Main suites are what is executed nightly and execute all the tests.  


Build takserver and plugin manager

```
cd <repo-home>/src
./gradlew clean build bootWar bootJar
```



