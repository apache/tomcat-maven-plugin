Build Apache Tomcat Maven Plugin
--------------------------------
To build this project you must Apache Maven at least 2.2.1 .
mvn clean install will install the mojos without running integration tests.
As there are some hardcoded integration tests with http port 1973, ajp 2001 and 2008, you could have some port allocation issues (if you don't know why those values ask olamy :-) )
mvn clean install -Prun-its will run integration tests too: to override the default used http port you can use -Dits.http.port= -Dits.ajp.port=

Snapshots deployment
---------------------
To deploy a snaphot version to https://repository.apache.org/content/repositories/snapshots/, you must run : mvn clean deploy .
Note you need some configuration in ~/.m2/settings.xml:
    <server>
      <id>apache.snapshots.https</id>
      <username>your asf id</username>
      <password>your asf paswword</password>
    </server

NOTE: a Jenkins job deploys SNAPSHOT automatically https://builds.apache.org/job/TomcatMavenPlugin-mvn3.x/.
So no real need to deploy manually, just commit and Jenkins will do the job for you.

Site deployment
-----------------

Checkstyle: this project uses the Apache Maven checkstyle configuration for ide codestyle files see http://maven.apache.org/developers/committer-environment.html .

Site: to test site generation, just run: mvn site. If you want more reporting (javadoc, pmd, checkstyle, jxr, changelog from jira entries), use: mvn site -Preporting.

To deploy site, use: mvn clean site-deploy -Preporting. The site will be deployed to http://tomcat.apache.org/maven-plugin-${project.version}

Note you need some configuration in ~/.m2/settings.xml:
    <server>
      <id>apache.website</id>
      <username>your asf id</username>
      <filePermissions>664</filePermissions>
      <directoryPermissions>775</directoryPermissions>
    </server>

If you have a nice ssh key in ~/.ssh/ no need of configuring password, privateKey, passphrase.

Releasing
----------
For release your ~/.m2/settings.xml must contains :

    <server>
      <id>apache.releases.https</id>
      <username>asf id</username>
      <password>asf password</password>
    </server>

And run: mvn release:prepare release:perform -Dusername= -Dpassword=   (username/password are your Apache svn authz)

Test staged Tomcat artifacts
----------------------------
To test staging artifacts for a vote process.
* activate a profile: tc-staging
* pass staging repository as parameter: -DtcStagedReleaseUrl=
* pass tomcat version as parameter: -Dtomcat7Version=

Sample for tomcat7 artifacts: mvn clean install -Prun-its -Ptc-staging -DtcStagedReleaseUrl=stagingrepositoryurl -Dtomcat7Version=7.x

Sample for tomcat6 artifacts: mvn clean install -Prun-its -Ptc-staging -DtcStagedReleaseUrl=stagingrepositoryurl -Dtomcat6Version=6.x

