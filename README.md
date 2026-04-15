# Apache Tomcat Maven Plugin

## Overview

The Apache Tomcat Maven Plugin provides goals to manipulate WAR projects within the Apache Tomcat servlet container. It supports:
- Running web applications with an embedded Tomcat server
- Deploying/undeploying web applications to a running Tomcat server
- Creating self-contained executable WAR/JAR files with embedded Tomcat

The plugin supports Tomcat 9, Tomcat 10, and Tomcat 11.

## Prerequisites

- Java 8 or higher
- Maven 3.8.1 or higher

## Build

Edit the main pom.xml to set the main <version> which should correspond
to the version of Apache Tomcat that is going to be used.

```bash
mvn clean install
```

To run integration tests:

```bash
mvn clean install -Prun-its
```

Override default ports for integration tests:

```bash
mvn clean install -Prun-its -Dits.http.port=8080 -Dits.ajp.port=8009
```

## Basic Usage

Add the plugin to your `pom.xml`:

```xml
<plugin>
  <groupId>org.apache.tomcat.maven</groupId>
  <artifactId>tomcat-maven-plugin</artifactId>
  <version>${project.version}</version>
</plugin>
```

## Common Goals

### Run Goals

**run** - Start Tomcat and run your web application:

```bash
mvn tomcat:run
```

**run-war** - Run the current project as a packaged web application:

```bash
mvn tomcat:run-war
```

**exec-war** - Create a self-executable JAR file with embedded Tomcat:

```bash
mvn tomcat:exec-war
```

**standalone-war** - Create an executable WAR file with embedded Tomcat:

```bash
mvn tomcat:standalone-war
```

**shutdown** - Shut down all embedded Tomcat servers:

```bash
mvn tomcat:shutdown
```

### Run Goals (No Lifecycle Forking)

The following goals are similar to the above but don't fork the Maven lifecycle:

**run-war-only** - Run the current project as a packaged web application without forking the package phase:

```bash
mvn tomcat:run-war-only
```

**exec-war-only** - Create a self-executable JAR file without forking the package phase:

```bash
mvn tomcat:exec-war-only
```

**standalone-war-only** - Create an executable WAR file without forking the package phase:

```bash
mvn tomcat:standalone-war-only
```

### Deploy Goals

**deploy** - Deploy a WAR to Tomcat:

```bash
mvn tomcat:deploy
```

**undeploy** - Undeploy a WAR from Tomcat:

```bash
mvn tomcat:undeploy
```

**redeploy** - Redeploy an existing WAR:

```bash
mvn tomcat:redeploy
```

**deploy-only** - Deploy a WAR to Tomcat without forking the package lifecycle:

```bash
mvn tomcat:deploy-only
```

**redeploy-only** - Redeploy a WAR without forking the package lifecycle:

```bash
mvn tomcat:redeploy-only
```

### Container Goals

**list** - List all deployed applications:

```bash
mvn tomcat:list
```

**serverinfo** - Get server information:

```bash
mvn tomcat:serverinfo
```

**resources** - List JNDI resources:

```bash
mvn tomcat:resources
```

**reload** - Reload a web application:

```bash
mvn tomcat:reload
```

**sessions** - List session information for a web application:

```bash
mvn tomcat:sessions
```

**start** - Start a web application:

```bash
mvn tomcat:start
```

**stop** - Stop a web application:

```bash
mvn tomcat:stop
```

## Configuration

### Deploy to Tomcat

Configure server credentials in `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>tomcat</id>
      <username>admin</username>
      <password>password</password>
    </server>
  </servers>
</settings>
```

Configure the plugin in your `pom.xml`:

```xml
<plugin>
  <groupId>org.apache.tomcat.maven</groupId>
  <artifactId>tomcat-maven-plugin</artifactId>
  <version>${project.version}</version>
  <configuration>
    <url>http://localhost:8080/manager/text</url>
    <server>tomcat</server>
    <path>/myapp</path>
  </configuration>
</plugin>
```

### Run with Custom Port

Configure custom HTTP port in your `pom.xml`:

```xml
<plugin>
  <groupId>org.apache.tomcat.maven</groupId>
  <artifactId>tomcat-maven-plugin</artifactId>
  <version>${project.version}</version>
  <configuration>
    <port>9090</port>
    <path>/</path>
  </configuration>
</plugin>
```

## Snapshots Deployment

To deploy a snapshot version to https://repository.apache.org/content/repositories/snapshots/, you must run:

```bash
mvn clean deploy
```

Note you need some configuration in `~/.m2/settings.xml`:

```xml
<server>
  <id>apache.snapshots.https</id>
  <username>your asf id</username>
  <password>your asf password</password>
</server>
```

**NOTE:** A Jenkins job deploys SNAPSHOT automatically https://builds.apache.org/job/TomcatMavenPlugin/. So no real need to deploy manually, just commit and Jenkins will do the job for you.

## Site Deployment

Checkstyle: this project uses the Apache Maven checkstyle configuration for IDE code style files. See http://maven.apache.org/developers/committer-environment.html.

Site: to test site generation, just run `mvn site`. If you want more reporting (javadoc, pmd, checkstyle, jxr, changelog from jira entries), use `mvn site -Preporting`.

To deploy site, use:

```bash
mvn clean site-deploy scm-publish:publish-scm -Dusername=$svnuid -Dpassword=$svnpwd -Preporting
```

The site will be deployed to http://tomcat.apache.org/maven-plugin-trunk ($svnuid is your asf id, $svnpwd is your asf password).

When releasing, deploy with `-Psite-release`.

## Releasing

For release, your `~/.m2/settings.xml` must contain:

```xml
<server>
  <id>apache.releases.https</id>
  <username>asf id</username>
  <password>asf password</password>
</server>
```

And run:

```bash
mvn release:prepare release:perform -Dusername= -Dpassword=
```

(username/password are your Apache svn authz)

## Test Staged Tomcat Artifacts

To test staging artifacts for a vote process:

* Activate a profile: `tc-staging`
* Pass staging repository as parameter: `-DtcStagedReleaseUrl=`
* Pass Tomcat version as parameter: `-DtomcatVersion=`

## Examples

### Example 1: Simple WAR Project

Create a simple WAR project and run it with Tomcat:

```bash
mkdir -p /tmp/tomcat-test
cd /tmp/tomcat-test

cat > pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>simple-webapp</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>war</packaging>
    
    <dependencies>
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>javax.servlet-api</artifactId>
            <version>4.0.1</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.tomcat.maven</groupId>
                <artifactId>tomcat-maven-plugin</artifactId>
                <version>${project.version}</version>
            </plugin>
        </plugins>
    </build>
</project>
EOF

mkdir -p src/main/webapp/WEB-INF
cat > src/main/webapp/index.jsp << 'EOF'
<!DOCTYPE html>
<html>
<head><title>Test App</title></head>
<body><h1>Hello from Tomcat!</h1></body>
</html>
EOF

# Run the application
mvn tomcat:run

# In another terminal, test it:
curl http://localhost:8080/simple-webapp-1.0-SNAPSHOT/
```

### Example 2: Deploy to Remote Tomcat

```bash
mkdir -p /tmp/tomcat-remote
cd /tmp/tomcat-remote

cat > pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>remote-deploy</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>war</packaging>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.tomcat.maven</groupId>
                <artifactId>tomcat-maven-plugin</artifactId>
                <version>${project.version}</version>
                <configuration>
                    <url>http://localhost:8080/manager/text</url>
                    <server>tomcat</server>
                    <path>/myapp</path>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOF

mkdir -p src/main/webapp
cat > src/main/webapp/index.jsp << 'EOF'
<!DOCTYPE html>
<html>
<head><title>Remote Deploy Test</title></head>
<body><h1>Deployed to remote Tomcat!</h1></body>
</html>
EOF

# Deploy (requires Tomcat running with manager app)
mvn package tomcat:deploy
```

## Mailing Lists

- User List: users@tomcat.apache.org
- Dev List: dev@tomcat.apache.org

## License

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0