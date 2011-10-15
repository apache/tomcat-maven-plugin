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
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
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
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 *
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
    private Artifact artifact;
    
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
     * @parameter
     */    
    private List<Dependency> extraDependencies;

    /**
     * @parameter default-value="${project.build.directory}"
     */
    private File buildDirectory;

    /**
     * @parameter default-value="src/main/tomcatconf" expression="${tomcat.exec.war.tomcatConf}"
     */
    private File tomcatConfigurationFilesDirectory;

    /**
     * @parameter default-value="src/main/tomcatconf/server.xml" expression="${tomcat.exec.war.serverXml}"
     */
    private File serverXml;


    /**
     * Name of the generated exec JAR.
     *
     * @parameter expression="${tomcat.jar.finalName}" default-value="${project.artifactId}-${project.version}-war-exec.jar"
     * @required
     */
    private String finalName;

    
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        
        //project.addAttachedArtifact(  );
        File warExecFile = new File( buildDirectory, finalName );
        if (warExecFile.exists())
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

            tmpPropertiesFile = File.createTempFile( "war-exec", "properties" );
            tmpPropertiesFile.deleteOnExit();
            tmpManifestFile = File.createTempFile( "war-exec", "manifest" );
            tmpManifestFile.deleteOnExit();
            tmpPropertiesFileOutputStream = new FileOutputStream( tmpPropertiesFile );
            execWarJar.createNewFile();
            execWarJarOutputStream =  new FileOutputStream( execWarJar );

            tmpManifestWriter = new PrintWriter( tmpManifestFile );


            // store :
            //* wars in the root: foo.war
            //* tomcat jars
            //* file tomcat.standalone.properties with possible values :
            //   * useServerXml=true/false to use directly the one provided
            //   * wars=foo.war;bar.war
            //* optionnal: conf/ with usual tomcat configuration files
            //* MANIFEST with Main-Class

            os =
                new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.JAR, execWarJarOutputStream);


            // TODO control project packaging is war
            os.putArchiveEntry( new JarArchiveEntry( project.getBuild().getFinalName() + ".war" ) );
            IOUtils.copy( new FileInputStream(artifact.getFile()), os );
            os.closeArchiveEntry();

            Properties properties = new Properties(  );


            if (serverXml != null && serverXml.exists() )
            {
                os.putArchiveEntry( new JarArchiveEntry( "conf/server.xml") );
                IOUtils.copy( new FileInputStream(serverXml), os );
                os.closeArchiveEntry();
                properties.put(Tomcat7Runner.USE_SERVER_XML_KEY, Boolean.TRUE.toString() );
            } else
            {
                properties.put(Tomcat7Runner.USE_SERVER_XML_KEY, Boolean.FALSE.toString() );
            }

            properties.put( Tomcat7Runner.WARS_KEY , project.getBuild().getFinalName() + ".war" );
            properties.store( tmpPropertiesFileOutputStream, "created by Apache Tomcat Maven plugin" );

            tmpPropertiesFileOutputStream.flush();
            tmpPropertiesFileOutputStream.close();

            os.putArchiveEntry( new JarArchiveEntry( Tomcat7RunnerCli.STAND_ALONE_PROPERTIES_FILENAME ) );
            IOUtils.copy( new FileInputStream(tmpPropertiesFile), os );
            os.closeArchiveEntry();
            

            // add tomcat classes
            for (Artifact pluginArtifact : pluginArtifacts)
            {
                if ( StringUtils.equals( "org.apache.tomcat", pluginArtifact.getGroupId() )
                    || StringUtils.equals( "org.apache.tomcat.embed", pluginArtifact.getGroupId() )
                    || StringUtils.equals( "org.eclipse.jdt.core.compiler", pluginArtifact.getGroupId() )
                    || StringUtils.equals( "commons-cli", pluginArtifact.getArtifactId() )
                    || StringUtils.equals( "tomcat7-war-runner", pluginArtifact.getArtifactId() ) )
                {
                    JarFile jarFile = new JarFile( pluginArtifact.getFile() );
                    Enumeration<JarEntry> jarEntries = jarFile.entries();
                    while ( jarEntries.hasMoreElements() )
                    {
                        JarEntry jarEntry = jarEntries.nextElement();
                        InputStream jarEntryIs = jarFile.getInputStream(jarEntry);

                        os.putArchiveEntry( new JarArchiveEntry( jarEntry.getName() ) );
                        IOUtils.copy( jarEntryIs, os );
                        os.closeArchiveEntry();
                    }
                }
            }
            Manifest manifest = new Manifest( );

            Manifest.Attribute mainClassAtt = new Manifest.Attribute( );
            mainClassAtt.setName( "Main-Class");
            mainClassAtt.setValue( Tomcat7RunnerCli.class.getName() );
            manifest.addConfiguredAttribute( mainClassAtt );

            manifest.write( tmpManifestWriter );
            tmpManifestWriter.flush();
            tmpManifestWriter.close();

            os.putArchiveEntry( new JarArchiveEntry( "META-INF/MANIFEST.MF" ) );
            IOUtils.copy( new FileInputStream( tmpManifestFile ), os );
            os.closeArchiveEntry();
        } catch ( ManifestException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        } catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        } catch ( ArchiveException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        } finally {
            IOUtils.closeQuietly( os );
            IOUtils.closeQuietly( tmpManifestWriter );
            IOUtils.closeQuietly( execWarJarOutputStream );
            IOUtils.closeQuietly(tmpPropertiesFileOutputStream);
            FileUtils.deleteQuietly(tmpPropertiesFile);
            FileUtils.deleteQuietly( tmpManifestFile );
        }
    }
}
