package org.apache.tomcat.maven.plugin.tomcat7.run;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.tomcat.maven.plugin.tomcat7.AbstractTomcat7Mojo;
import org.apache.tomcat.maven.runner.Tomcat7Runner;
import org.apache.tomcat.maven.runner.Tomcat7RunnerCli;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Olivier Lamy
 * @since 2.0
 */
public abstract class AbstractExecWarMojo
    extends AbstractTomcat7Mojo
{


    /**
     * @parameter default-value="${project.artifact}"
     * @required
     * @readonly
     */
    private Artifact projectArtifact;

    /**
     * The maven project.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter default-value="${plugin.artifacts}"
     * @required
     */
    private List<Artifact> pluginArtifacts;

    /**
     * @parameter default-value="${project.build.directory}"
     */
    private File buildDirectory;

    /**
     * @parameter default-value="src/main/tomcatconf" expression="${maven.tomcat.exec.war.tomcatConf}"
     */
    private File tomcatConfigurationFilesDirectory;

    /**
     * @parameter default-value="src/main/tomcatconf/server.xml" expression="${maven.tomcat.exec.war.serverXml}"
     */
    private File serverXml;


    /**
     * Name of the generated exec JAR.
     *
     * @parameter expression="${tomcat.jar.finalName}" default-value="${project.artifactId}-${project.version}-war-exec.jar"
     * @required
     */
    private String finalName;

    /**
     * The webapp context path to use for the web application being run.
     * The name to store webapp in exec jar. Do not use /
     *
     * @parameter expression="${maven.tomcat.path}" default-value="${project.artifactId}"
     * @required
     */
    protected String path;

    /**
     * @parameter
     */
    protected List<WarRunDependency> warRunDependencies;

    /**
     * @component
     */
    protected ArtifactResolver artifactResolver;

    /**
     * Maven Artifact Factory component.
     *
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * Location of the local repository.
     *
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    private ArtifactRepository local;

    /**
     * List of Remote Repositories used by the resolver
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    protected List<ArtifactRepository> remoteRepos;

    /**
     * @component
     * @required
     * @readonly
     */
    private MavenProjectHelper projectHelper;

    /**
     * Attach or not the generated artifact to the build (use true if you want to install or deploy it)
     *
     * @parameter expression="${maven.tomcat.exec.war.attachArtifact}" default-value="true"
     * @required
     */
    private boolean attachArtifact;


    /**
     * the classifier to use for the attached/generated artifact
     *
     * @parameter expression="${maven.tomcat.exec.war.attachArtifactClassifier}" default-value="exec-war"
     * @required
     */
    private String attachArtifactClassifier;


    /**
     * the type to use for the attached/generated artifact
     *
     * @parameter expression="${maven.tomcat.exec.war.attachArtifactType}" default-value="jar"
     * @required
     */
    private String attachArtifactClassifierType;

    /**
     * to enable naming when starting tomcat
     *
     * @parameter expression="${maven.tomcat.exec.war.enableNaming}" default-value="false"
     * @required
     */
    private boolean enableNaming;

    /**
     * see http://tomcat.apache.org/tomcat-7.0-doc/config/valve.html
     *
     * @parameter expression="${maven.tomcat.exec.war.accessLogValveFormat}" default-value="%h %l %u %t "%r" %s %b %I %D"
     * @required
     */
    private String accessLogValveFormat;

    /**
     * list of extra dependencies to add in the standalone tomcat jar: your jdbc driver, mail.jar etc..
     * <b>Those dependencies will be in root classloader.</b>
     *
     * @parameter
     */
    private List<Dependency> extraDependencies;

    /**
     * Main class to use for starting the standalone jar.
     *
     * @parameter expression="${maven.tomcat.exec.war.mainClass}" default-value="org.apache.tomcat.maven.runner.Tomcat7RunnerCli"
     * @required
     */
    private String mainClass;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        //project.addAttachedArtifact(  );
        File warExecFile = new File( buildDirectory, finalName );
        if ( warExecFile.exists() )
        {
            warExecFile.delete();
        }

        File execWarJar = new File( buildDirectory, finalName );

        FileOutputStream execWarJarOutputStream = null;
        ArchiveOutputStream os = null;
        File tmpPropertiesFile = null;
        File tmpManifestFile = null;
        FileOutputStream tmpPropertiesFileOutputStream = null;
        PrintWriter tmpManifestWriter = null;

        try
        {

            tmpPropertiesFile = new File( buildDirectory, "war-exec.properties" );
            if ( tmpPropertiesFile.exists() )
            {
                tmpPropertiesFile.delete();
            }
            tmpPropertiesFile.getParentFile().mkdirs();

            tmpManifestFile = new File( buildDirectory, "war-exec.manifest" );
            if ( tmpManifestFile.exists() )
            {
                tmpManifestFile.delete();
            }
            tmpPropertiesFileOutputStream = new FileOutputStream( tmpPropertiesFile );
            execWarJar.getParentFile().mkdirs();
            execWarJar.createNewFile();
            execWarJarOutputStream = new FileOutputStream( execWarJar );

            tmpManifestWriter = new PrintWriter( tmpManifestFile );

            // store :
            //* wars in the root: foo.war
            //* tomcat jars
            //* file tomcat.standalone.properties with possible values :
            //   * useServerXml=true/false to use directly the one provided
            //   * enableNaming=true/false
            //   * wars=foo.war|contextpath;bar.war  ( |contextpath is optionnal if empty use the war name )
            //   * accessLogValveFormat=
            //* optionnal: conf/ with usual tomcat configuration files
            //* MANIFEST with Main-Class

            Properties properties = new Properties();

            properties.put( Tomcat7Runner.ENABLE_NAMING_KEY, Boolean.toString( enableNaming ) );
            properties.put( Tomcat7Runner.ACCESS_LOG_VALVE_FORMAT_KEY, accessLogValveFormat );

            os = new ArchiveStreamFactory().createArchiveOutputStream( ArchiveStreamFactory.JAR,
                                                                       execWarJarOutputStream );

            if ( "war".equals( project.getPackaging() ) )
            {

                os.putArchiveEntry( new JarArchiveEntry( StringUtils.removeStart( path, "/" ) + ".war" ) );
                IOUtils.copy( new FileInputStream( projectArtifact.getFile() ), os );
                os.closeArchiveEntry();
                properties.put( Tomcat7Runner.WARS_KEY, StringUtils.removeStart( path, "/" ) + ".war|" + path );
            }

            if ( "pom".equals( project.getPackaging() ) && ( warRunDependencies != null
                && !warRunDependencies.isEmpty() ) )
            {
                for ( WarRunDependency warRunDependency : warRunDependencies )
                {
                    if ( warRunDependency.dependency != null )
                    {
                        Dependency dependency = warRunDependency.dependency;
                        // String groupId, String artifactId, String version, String scope, String type
                        Artifact artifact =
                            artifactFactory.createArtifact( dependency.getGroupId(), dependency.getArtifactId(),
                                                            dependency.getVersion(), dependency.getScope(),
                                                            dependency.getType() );

                        artifactResolver.resolve( artifact, this.remoteRepos, this.local );
                        File warFile = new File( buildDirectory, artifact.getFile().getName() );
                        String warFileName = artifact.getFile().getName();
                        FileUtils.copyFile( artifact.getFile(), warFile );
                        if ( warRunDependency.contextXml != null )
                        {
                            warFile = addContextXmlToWar( warRunDependency.contextXml, warFile );
                        }
                        os.putArchiveEntry( new JarArchiveEntry( warFileName ) );
                        IOUtils.copy( new FileInputStream( warFile ), os );
                        os.closeArchiveEntry();
                        String propertyWarValue = properties.getProperty( Tomcat7Runner.WARS_KEY );
                        // FIXME check contextPath is not empty or at least only / for root app
                        if ( propertyWarValue != null )
                        {
                            properties.put( Tomcat7Runner.WARS_KEY,
                                            propertyWarValue + ";" + warFileName + "|" + warRunDependency.contextPath );
                        }
                        else
                        {
                            properties.put( Tomcat7Runner.WARS_KEY, warFileName + "|" + warRunDependency.contextPath );
                        }
                    }
                }
            }

            // FIXME if no war has been added here we must stop with a human readable and user friendly error message

            if ( serverXml != null && serverXml.exists() )
            {
                os.putArchiveEntry( new JarArchiveEntry( "conf/server.xml" ) );
                IOUtils.copy( new FileInputStream( serverXml ), os );
                os.closeArchiveEntry();
                properties.put( Tomcat7Runner.USE_SERVER_XML_KEY, Boolean.TRUE.toString() );
            }
            else
            {
                properties.put( Tomcat7Runner.USE_SERVER_XML_KEY, Boolean.FALSE.toString() );
            }

            properties.store( tmpPropertiesFileOutputStream, "created by Apache Tomcat Maven plugin" );

            tmpPropertiesFileOutputStream.flush();
            tmpPropertiesFileOutputStream.close();

            os.putArchiveEntry( new JarArchiveEntry( Tomcat7RunnerCli.STAND_ALONE_PROPERTIES_FILENAME ) );
            IOUtils.copy( new FileInputStream( tmpPropertiesFile ), os );
            os.closeArchiveEntry();

            // add tomcat classes
            for ( Artifact pluginArtifact : pluginArtifacts )
            {
                if ( StringUtils.equals( "org.apache.tomcat", pluginArtifact.getGroupId() ) || StringUtils.equals(
                    "org.apache.tomcat.embed", pluginArtifact.getGroupId() ) || StringUtils.equals(
                    "org.eclipse.jdt.core.compiler", pluginArtifact.getGroupId() ) || StringUtils.equals( "commons-cli",
                                                                                                          pluginArtifact.getArtifactId() )
                    || StringUtils.equals( "tomcat7-war-runner", pluginArtifact.getArtifactId() ) )
                {
                    JarFile jarFile = new JarFile( pluginArtifact.getFile() );
                    Enumeration<JarEntry> jarEntries = jarFile.entries();
                    while ( jarEntries.hasMoreElements() )
                    {
                        JarEntry jarEntry = jarEntries.nextElement();
                        InputStream jarEntryIs = jarFile.getInputStream( jarEntry );

                        os.putArchiveEntry( new JarArchiveEntry( jarEntry.getName() ) );
                        IOUtils.copy( jarEntryIs, os );
                        os.closeArchiveEntry();
                    }
                }
            }

            // add extra dependencies
            if ( extraDependencies != null && !extraDependencies.isEmpty() )
            {
                for ( Dependency dependency : extraDependencies )
                {
                    // String groupId, String artifactId, String version, String scope, String type
                    Artifact artifact =
                        artifactFactory.createArtifact( dependency.getGroupId(), dependency.getArtifactId(),
                                                        dependency.getVersion(), dependency.getScope(),
                                                        dependency.getType() );

                    artifactResolver.resolve( artifact, this.remoteRepos, this.local );
                    JarFile jarFile = new JarFile( artifact.getFile() );
                    Enumeration<JarEntry> jarEntries = jarFile.entries();
                    while ( jarEntries.hasMoreElements() )
                    {
                        JarEntry jarEntry = jarEntries.nextElement();
                        InputStream jarEntryIs = jarFile.getInputStream( jarEntry );

                        os.putArchiveEntry( new JarArchiveEntry( jarEntry.getName() ) );
                        IOUtils.copy( jarEntryIs, os );
                        os.closeArchiveEntry();
                    }
                }
            }

            Manifest manifest = new Manifest();

            Manifest.Attribute mainClassAtt = new Manifest.Attribute();
            mainClassAtt.setName( "Main-Class" );
            mainClassAtt.setValue( mainClass );
            manifest.addConfiguredAttribute( mainClassAtt );

            manifest.write( tmpManifestWriter );
            tmpManifestWriter.flush();
            tmpManifestWriter.close();

            os.putArchiveEntry( new JarArchiveEntry( "META-INF/MANIFEST.MF" ) );
            IOUtils.copy( new FileInputStream( tmpManifestFile ), os );
            os.closeArchiveEntry();

            if ( attachArtifact )
            {
                //MavenProject project, String artifactType, String artifactClassifier, File artifactFile
                projectHelper.attachArtifact( project, attachArtifactClassifierType, attachArtifactClassifier,
                                              execWarJar );
            }
        }
        catch ( ManifestException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( ArchiveException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        finally
        {
            IOUtils.closeQuietly( os );
            IOUtils.closeQuietly( tmpManifestWriter );
            IOUtils.closeQuietly( execWarJarOutputStream );
            IOUtils.closeQuietly( tmpPropertiesFileOutputStream );
        }
    }


    /**
     * return file can be deleted
     */
    private File addContextXmlToWar( File contextXmlFile, File warFile )
        throws IOException, ArchiveException
    {
        ArchiveOutputStream os = null;
        OutputStream warOutputStream = null;
        File tmpWar = File.createTempFile( "tomcat", "war-exec" );
        tmpWar.deleteOnExit();

        try
        {
            warOutputStream = new FileOutputStream( tmpWar );
            os = new ArchiveStreamFactory().createArchiveOutputStream( ArchiveStreamFactory.JAR, warOutputStream );
            os.putArchiveEntry( new JarArchiveEntry( "META-INF/context.xml" ) );
            IOUtils.copy( new FileInputStream( contextXmlFile ), os );
            os.closeArchiveEntry();

            JarFile jarFile = new JarFile( warFile );
            Enumeration<JarEntry> jarEntries = jarFile.entries();
            while ( jarEntries.hasMoreElements() )
            {
                JarEntry jarEntry = jarEntries.nextElement();
                os.putArchiveEntry( new JarArchiveEntry( jarEntry.getName() ) );
                IOUtils.copy( jarFile.getInputStream( jarEntry ), os );
                os.closeArchiveEntry();
            }
            os.flush();
        }
        finally
        {
            IOUtils.closeQuietly( os );
            IOUtils.closeQuietly( warOutputStream );
        }
        return tmpWar;
    }
}
