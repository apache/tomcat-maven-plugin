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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tomcat.maven.runner.Tomcat7Runner;
import org.apache.tomcat.maven.runner.Tomcat7RunnerCli;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.jar.JarFile;

/**
 * Abstract Mojo for building deployable and executable war files
 *
 * @since 2.1
 */
public abstract class AbstractStandaloneWarMojo
    extends AbstractExecWarMojo
{

    /**
     * Name of the generated WAR.
     */
    @Parameter(property = "tomcat.jar.finalName",
               defaultValue = "${project.artifactId}-${project.version}-standalone.war", required = true)
    protected String finalName;

    /**
     * the classifier to use for the attached/generated artifact
     */
    @Parameter(property = "maven.tomcat.exec.war.attachArtifactClassifier", defaultValue = "standalone",
               required = true)
    protected String attachArtifactClassifier;

    /**
     * the type to use for the attached/generated artifact
     *
     * @since 2.2
     */
    @Parameter(property = "maven.tomcat.exec.war.attachArtifactType", defaultValue = "war", required = true)
    protected String attachArtifactClassifierType;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !"war".equals( project.getPackaging() ) )
        {
            throw new MojoFailureException( "Pacakaging must be of type war for standalone-war goal." );
        }

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
            //   * codeSourceContextPath=path parameter, default is project.artifactId
            //* optionnal: conf/ with usual tomcat configuration files
            //* MANIFEST with Main-Class

            Properties properties = new Properties();

            properties.put( Tomcat7Runner.ARCHIVE_GENERATION_TIMESTAMP_KEY,
                            Long.toString( System.currentTimeMillis() ) );
            properties.put( Tomcat7Runner.ENABLE_NAMING_KEY, Boolean.toString( enableNaming ) );
            properties.put( Tomcat7Runner.ACCESS_LOG_VALVE_FORMAT_KEY, accessLogValveFormat );
            properties.put( Tomcat7Runner.HTTP_PROTOCOL_KEY, connectorHttpProtocol );
            properties.put( Tomcat7Runner.CODE_SOURCE_CONTEXT_PATH, path );

            os = new ArchiveStreamFactory().createArchiveOutputStream( ArchiveStreamFactory.JAR,
                                                                       execWarJarOutputStream );

            extractJarToArchive( new JarFile( projectArtifact.getFile() ), os, null );

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
                    extractJarToArchive( jarFile, os, excludes );
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
}
