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
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.tomcat.maven.plugin.tomcat7.AbstractTomcat7Mojo;
import org.apache.tomcat.maven.runner.Tomcat7Runner;
import org.apache.tomcat.maven.runner.Tomcat7RunnerCli;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.SelectorUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
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

    @Parameter( defaultValue = "${project.artifact}", required = true, readonly = true )
    protected Artifact projectArtifact;

    /**
     * The maven project.
     */
    @Parameter( defaultValue = "${project}", required = true, readonly = true )
    protected MavenProject project;

    @Parameter( defaultValue = "${plugin.artifacts}", required = true )
    protected List<Artifact> pluginArtifacts;

    @Parameter( defaultValue = "${project.build.directory}" )
    protected File buildDirectory;

    /**
     * Path under {@link #buildDirectory} where this mojo may do temporary work.
     */
    @Parameter( defaultValue = "${project.build.directory}/tomcat7-maven-plugin-exec" )
    private File pluginWorkDirectory;

    @Parameter( property = "maven.tomcat.exec.war.tomcatConf", defaultValue = "src/main/tomcatconf" )
    protected File tomcatConfigurationFilesDirectory;

    @Parameter( defaultValue = "src/main/tomcatconf/server.xml", property = "maven.tomcat.exec.war.serverXml" )
    protected File serverXml;

    /**
     * Name of the generated exec JAR.
     */
    @Parameter( property = "tomcat.jar.finalName",
                defaultValue = "${project.artifactId}-${project.version}-war-exec.jar", required = true )
    protected String finalName;

    /**
     * Skip the execution
     *
     * @since 2.2
     */
    @Parameter( property = "maven.tomcat.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * The webapp context path to use for the web application being run.
     * The name to store webapp in exec jar. Do not use /
     */
    @Parameter( property = "maven.tomcat.path", defaultValue = "${project.artifactId}", required = true )
    protected String path;

    @Parameter
    protected List<WarRunDependency> warRunDependencies;

    @Component
    protected ArtifactResolver artifactResolver;

    /**
     * Maven Artifact Factory component.
     */
    @Component
    protected ArtifactFactory artifactFactory;

    /**
     * Location of the local repository.
     */
    @Parameter( defaultValue = "${localRepository}", required = true, readonly = true )
    protected ArtifactRepository local;

    /**
     * List of Remote Repositories used by the resolver
     */
    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", required = true, readonly = true )
    protected List<ArtifactRepository> remoteRepos;

    @Component
    protected MavenProjectHelper projectHelper;

    /**
     * Attach or not the generated artifact to the build (use true if you want to install or deploy it)
     */
    @Parameter( property = "maven.tomcat.exec.war.attachArtifact", defaultValue = "true", required = true )
    protected boolean attachArtifact;


    /**
     * the classifier to use for the attached/generated artifact
     */
    @Parameter( property = "maven.tomcat.exec.war.attachArtifactClassifier", defaultValue = "exec-war",
                required = true )
    protected String attachArtifactClassifier;


    /**
     * the type to use for the attached/generated artifact
     */
    @Parameter( property = "maven.tomcat.exec.war.attachArtifactType", defaultValue = "jar", required = true )
    protected String attachArtifactClassifierType;

    /**
     * to enable naming when starting tomcat
     */
    @Parameter( property = "maven.tomcat.exec.war.enableNaming", defaultValue = "false", required = true )
    protected boolean enableNaming;

    /**
     * see http://tomcat.apache.org/tomcat-7.0-doc/config/valve.html
     */
    @Parameter( property = "maven.tomcat.exec.war.accessLogValveFormat", defaultValue = "%h %l %u %t %r %s %b %I %D",
                required = true )
    protected String accessLogValveFormat;

    /**
     * list of extra dependencies to add in the standalone tomcat jar: your jdbc driver, mail.jar etc..
     * <b>Those dependencies will be in root classloader.</b>
     */
    @Parameter
    protected List<ExtraDependency> extraDependencies;

    /**
     * list of extra resources to add in the standalone tomcat jar: your logger configuration etc
     */
    @Parameter
    protected List<ExtraResource> extraResources;

    /**
     * Main class to use for starting the standalone jar.
     */
    @Parameter( property = "maven.tomcat.exec.war.mainClass",
                defaultValue = "org.apache.tomcat.maven.runner.Tomcat7RunnerCli", required = true )
    protected String mainClass;

    /**
     * which connector protocol to use HTTP/1.1 or org.apache.coyote.http11.Http11NioProtocol
     */
    @Parameter( property = "maven.tomcat.exec.war.connectorHttpProtocol", defaultValue = "HTTP/1.1", required = true )
    protected String connectorHttpProtocol;

    /**
     * configure a default http port for the standalone jar
     *
     * @since 2.2
     */
    @Parameter( property = "maven.tomcat.exec.war.httpPort" )
    protected String httpPort;

    /**
     * File patterns to exclude from extraDependencies
     *
     * @since 2.2
     */
    @Parameter
    protected String[] excludes;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( this.skip )
        {
            getLog().info( "skip execution" );
            return;
        }
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
            //   * connectorhttpProtocol: HTTP/1.1 or org.apache.coyote.http11.Http11NioProtocol
            //* optionnal: conf/ with usual tomcat configuration files
            //* MANIFEST with Main-Class

            Properties properties = new Properties();

            properties.put( Tomcat7Runner.ARCHIVE_GENERATION_TIMESTAMP_KEY,
                            Long.toString( System.currentTimeMillis() ) );
            properties.put( Tomcat7Runner.ENABLE_NAMING_KEY, Boolean.toString( enableNaming ) );
            properties.put( Tomcat7Runner.ACCESS_LOG_VALVE_FORMAT_KEY, accessLogValveFormat );
            properties.put( Tomcat7Runner.HTTP_PROTOCOL_KEY, connectorHttpProtocol );

            if ( httpPort != null )
            {
                properties.put( Tomcat7Runner.HTTP_PORT_KEY, httpPort );
            }

            os = new ArchiveStreamFactory().createArchiveOutputStream( ArchiveStreamFactory.JAR,
                                                                       execWarJarOutputStream );

            if ( "war".equals( project.getPackaging() ) )
            {

                os.putArchiveEntry( new JarArchiveEntry( StringUtils.removeStart( path, "/" ) + ".war" ) );
                IOUtils.copy( new FileInputStream( projectArtifact.getFile() ), os );
                os.closeArchiveEntry();

                properties.put( Tomcat7Runner.WARS_KEY, StringUtils.removeStart( path, "/" ) + ".war|" + path );
            }
            else if ( warRunDependencies != null && !warRunDependencies.isEmpty() )
            {
                for ( WarRunDependency warRunDependency : warRunDependencies )
                {
                    if ( warRunDependency.dependency != null )
                    {
                        Dependency dependency = warRunDependency.dependency;
                        String version = dependency.getVersion();
                        if ( StringUtils.isEmpty( version ) )
                        {
                            version = findArtifactVersion( dependency );
                        }

                        if ( StringUtils.isEmpty( version ) )
                        {
                            throw new MojoExecutionException(
                                "Dependency '" + dependency.getGroupId() + "':'" + dependency.getArtifactId()
                                    + "' does not have version specified" );
                        }
                        Artifact artifact = artifactFactory.createArtifactWithClassifier( dependency.getGroupId(),
                                                                                          dependency.getArtifactId(),
                                                                                          version,
                                                                                          dependency.getType(),
                                                                                          dependency.getClassifier() );

                        artifactResolver.resolve( artifact, this.remoteRepos, this.local );

                        File warFileToBundle = new File( resolvePluginWorkDir(), artifact.getFile().getName() );
                        FileUtils.copyFile( artifact.getFile(), warFileToBundle );

                        if ( warRunDependency.contextXml != null )
                        {
                            warFileToBundle = addContextXmlToWar( warRunDependency.contextXml, warFileToBundle );
                        }
                        final String warFileName = artifact.getFile().getName();
                        os.putArchiveEntry( new JarArchiveEntry( warFileName ) );
                        IOUtils.copy( new FileInputStream( warFileToBundle ), os );
                        os.closeArchiveEntry();
                        String propertyWarValue = properties.getProperty( Tomcat7Runner.WARS_KEY );
                        String contextPath =
                            StringUtils.isEmpty( warRunDependency.contextPath ) ? "/" : warRunDependency.contextPath;
                        if ( propertyWarValue != null )
                        {
                            properties.put( Tomcat7Runner.WARS_KEY,
                                            propertyWarValue + ";" + warFileName + "|" + contextPath );
                        }
                        else
                        {
                            properties.put( Tomcat7Runner.WARS_KEY, warFileName + "|" + contextPath );
                        }
                    }
                }
            }

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

            os.putArchiveEntry( new JarArchiveEntry( "conf/web.xml" ) );
            IOUtils.copy( getClass().getResourceAsStream( "/conf/web.xml" ), os );
            os.closeArchiveEntry();

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
                    extractJarToArchive( jarFile, os, null );
                }
            }

            // add extra dependencies
            if ( extraDependencies != null && !extraDependencies.isEmpty() )
            {
                for ( Dependency dependency : extraDependencies )
                {
                    String version = dependency.getVersion();
                    if ( StringUtils.isEmpty( version ) )
                    {
                        version = findArtifactVersion( dependency );
                    }

                    if ( StringUtils.isEmpty( version ) )
                    {
                        throw new MojoExecutionException(
                            "Dependency '" + dependency.getGroupId() + "':'" + dependency.getArtifactId()
                                + "' does not have version specified" );
                    }

                    // String groupId, String artifactId, String version, String scope, String type
                    Artifact artifact =
                        artifactFactory.createArtifact( dependency.getGroupId(), dependency.getArtifactId(), version,
                                                        dependency.getScope(), dependency.getType() );

                    artifactResolver.resolve( artifact, this.remoteRepos, this.local );
                    JarFile jarFile = new JarFile( artifact.getFile() );
                    extractJarToArchive( jarFile, os, this.excludes );
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

            if ( extraResources != null )
            {
                for ( ExtraResource extraResource : extraResources )
                {

                    DirectoryScanner directoryScanner = new DirectoryScanner();
                    directoryScanner.setBasedir( extraResource.getDirectory() );
                    directoryScanner.addDefaultExcludes();
                    directoryScanner.setExcludes( toStringArray( extraResource.getExcludes() ) );
                    directoryScanner.setIncludes( toStringArray( extraResource.getIncludes() ) );
                    directoryScanner.scan();
                    for ( String includeFile : directoryScanner.getIncludedFiles() )
                    {
                        getLog().debug( "include file:" + includeFile );
                        os.putArchiveEntry( new JarArchiveEntry( includeFile ) );
                        IOUtils.copy( new FileInputStream( new File( extraResource.getDirectory(), includeFile ) ),
                                      os );
                        os.closeArchiveEntry();
                    }
                }
            }

            if ( tomcatConfigurationFilesDirectory != null && tomcatConfigurationFilesDirectory.exists() )
            {
                // Because its the tomcat default dir for configs
                String aConfigOutputDir = "conf/";
                copyDirectoryContentIntoArchive( tomcatConfigurationFilesDirectory, aConfigOutputDir, os );
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

    protected String findArtifactVersion( Dependency dependency )
    {
        // search in project.dependencies
        for ( Dependency projectDependency : (List<Dependency>) this.project.getDependencies() )
        {
            if ( sameDependencyWithoutVersion( dependency, projectDependency ) )
            {
                return projectDependency.getVersion();
            }
        }

        // search in project.dependencies
        for ( Dependency projectDependency : (List<Dependency>) this.project.getDependencyManagement().getDependencies() )
        {
            if ( sameDependencyWithoutVersion( dependency, projectDependency ) )
            {
                return projectDependency.getVersion();
            }
        }

        return null;
    }

    protected boolean sameDependencyWithoutVersion( Dependency that, Dependency dependency )
    {
        return StringUtils.equals( that.getGroupId(), dependency.getGroupId() ) && StringUtils.equals(
            that.getArtifactId(), dependency.getArtifactId() );
    }

    protected void copyDirectoryContentIntoArchive( File sourceFolder, String destinationPath,
                                                    ArchiveOutputStream archiveOutputStream )
        throws IOException
    {

        // Scan the directory
        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir( sourceFolder );
        directoryScanner.addDefaultExcludes();
        directoryScanner.scan();

        // Each File
        for ( String includeFileName : directoryScanner.getIncludedFiles() )
        {
            getLog().debug( "include configuration file : " + destinationPath + includeFileName );
            File inputFile = new File( sourceFolder, includeFileName );

            FileInputStream sourceFileInputStream = null;
            try
            {
                sourceFileInputStream = new FileInputStream( inputFile );

                archiveOutputStream.putArchiveEntry( new JarArchiveEntry( destinationPath + includeFileName ) );
                IOUtils.copy( sourceFileInputStream, archiveOutputStream );
                archiveOutputStream.closeArchiveEntry();
            }
            finally
            {
                IOUtils.closeQuietly( sourceFileInputStream );
            }
        }

    }

    /**
     * Resolves the plugin work dir as a sub directory of {@link #buildDirectory}, creating it if it does not exist.
     *
     * @return File representing the resolved plugin work dir
     * @throws MojoExecutionException if the plugin work dir cannot be created
     */
    protected File resolvePluginWorkDir()
        throws MojoExecutionException
    {
        if ( !pluginWorkDirectory.exists() && !pluginWorkDirectory.mkdirs() )
        {
            throw new MojoExecutionException(
                "Could not create plugin work directory at " + pluginWorkDirectory.getAbsolutePath() );
        }

        return pluginWorkDirectory;

    }

    protected String[] toStringArray( List list )
    {
        if ( list == null || list.isEmpty() )
        {
            return new String[0];
        }
        List<String> res = new ArrayList<String>( list.size() );

        for ( Iterator ite = list.iterator(); ite.hasNext(); )
        {
            res.add( (String) ite.next() );
        }
        return res.toArray( new String[res.size()] );
    }


    /**
     * return file can be deleted
     */
    protected File addContextXmlToWar( File contextXmlFile, File warFile )
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
            extractJarToArchive( jarFile, os, null );
            os.flush();
        }
        finally
        {
            IOUtils.closeQuietly( os );
            IOUtils.closeQuietly( warOutputStream );
        }
        return tmpWar;
    }

    /**
     * Copy the contents of a jar file to another archive
     *
     * @param file The input jar file
     * @param os   The output archive
     * @throws IOException
     */
    protected void extractJarToArchive( JarFile file, ArchiveOutputStream os, String[] excludes )
        throws IOException
    {
        Enumeration<? extends JarEntry> entries = file.entries();
        while ( entries.hasMoreElements() )
        {
            JarEntry j = entries.nextElement();

            if ( excludes != null && excludes.length > 0 )
            {
                for ( String exclude : excludes )
                {
                    if ( SelectorUtils.match( exclude, j.getName() ) )
                    {
                        continue;
                    }
                }
            }

            if ( StringUtils.equalsIgnoreCase( j.getName(), "META-INF/MANIFEST.MF" ) )
            {
                continue;
            }
            os.putArchiveEntry( new JarArchiveEntry( j.getName() ) );
            IOUtils.copy( file.getInputStream( j ), os );
            os.closeArchiveEntry();
        }
        if ( file != null )
        {
            file.close();
        }
    }
}
