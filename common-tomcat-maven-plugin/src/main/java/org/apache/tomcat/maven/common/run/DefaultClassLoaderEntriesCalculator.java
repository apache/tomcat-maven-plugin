package org.apache.tomcat.maven.common.run;

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

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Olivier Lamy
 * @since 2.0
 */
@Component( role = ClassLoaderEntriesCalculator.class )
public class DefaultClassLoaderEntriesCalculator
    implements ClassLoaderEntriesCalculator
{

    @Requirement
    private ArchiverManager archiverManager;


    public List<String> calculateClassPathEntries( ClassLoaderEntriesCalculatorRequest request )
        throws TomcatRunException
    {
        Set<String> classLoaderEntries = new HashSet<String>();

        // add classes directories to loader
        try
        {
            @SuppressWarnings( "unchecked" ) List<String> classPathElements = request.isUseTestClassPath()
                ? request.getMavenProject().getTestClasspathElements()
                : request.getMavenProject().getCompileClasspathElements();
            if ( classPathElements != null )
            {
                for ( String classPathElement : classPathElements )
                {
                    File classPathElementFile = new File( classPathElement );
                    if ( classPathElementFile.exists() && classPathElementFile.isDirectory() )
                    {
                        request.getLog().debug(
                            "adding classPathElementFile " + classPathElementFile.toURI().toString() );
                        classLoaderEntries.add( classPathElementFile.toURI().toString() );
                    }
                }
            }
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new TomcatRunException( e.getMessage(), e );
        }

        // add artifacts to loader
        if ( request.getDependencies() != null )
        {
            for ( Artifact artifact : request.getDependencies() )
            {
                String scope = artifact.getScope();

                // skip provided and test scoped artifacts
                if ( !Artifact.SCOPE_PROVIDED.equals( scope ) && ( !Artifact.SCOPE_TEST.equals( scope )
                    || request.isUseTestClassPath() ) )
                {
                    request.getLog().debug(
                        "add dependency to webapploader " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":"
                            + artifact.getVersion() + ":" + artifact.getScope() );
                    if ( !isInProjectReferences( artifact, request.getMavenProject() ) )
                    {
                        classLoaderEntries.add( artifact.getFile().toURI().toString() );
                    }
                    else
                    {
                        request.getLog().debug(
                            "skip adding artifact " + artifact.getArtifactId() + " as it's in reactors" );
                    }
                }
                // in case of war dependency we must add /WEB-INF/lib/*.jar in entries and WEB-INF/classes
                if ( "war".equals( artifact.getType() ) && request.isAddWarDependenciesInClassloader() )
                {
                    File tmpDir = null;
                    try
                    {
                        tmpDir = Files.createTempDir();
                        tmpDir.deleteOnExit();
                        File warFile = artifact.getFile();
                        UnArchiver unArchiver = archiverManager.getUnArchiver( "jar" );
                        unArchiver.setSourceFile( warFile );
                        unArchiver.setDestDirectory( tmpDir );
                        unArchiver.extract();
                        File libsDirectory = new File( tmpDir, "WEB-INF/lib" );
                        if ( libsDirectory.exists() )
                        {
                            String[] jars = libsDirectory.list( new FilenameFilter()
                            {
                                public boolean accept( File file, String s )
                                {
                                    return s.endsWith( ".jar" );
                                }
                            } );
                            for ( String jar : jars )
                            {
                                classLoaderEntries.add( new File( libsDirectory, jar ).toURI().toString() );
                            }
                        }
                        File classesDirectory = new File( tmpDir, "WEB-INF/classes" );
                        if ( classesDirectory.exists() )
                        {
                            classLoaderEntries.add( classesDirectory.toURI().toString() );
                        }
                    }
                    catch ( NoSuchArchiverException e )
                    {
                        throw new TomcatRunException( e.getMessage(), e );
                    }
                    catch ( ArchiverException e )
                    {
                        request.getLog().error(
                            "fail to extract war file " + artifact.getFile() + ", reason:" + e.getMessage(), e );
                        throw new TomcatRunException( e.getMessage(), e );
                    }
                    finally
                    {
                        deleteDirectory( tmpDir, request.getLog() );
                    }
                }
            }
        }
        return new ArrayList<String>( classLoaderEntries );
    }

    private void deleteDirectory( File directory, Log log )
        throws TomcatRunException
    {
        try
        {
            FileUtils.deleteDirectory( directory );
        }
        catch ( IOException e )
        {
            log.error( "fail to delete directory file " + directory + ", reason:" + e.getMessage(), e );
            throw new TomcatRunException( e.getMessage(), e );
        }
    }

    protected boolean isInProjectReferences( Artifact artifact, MavenProject project )
    {
        if ( project.getProjectReferences() == null || project.getProjectReferences().isEmpty() )
        {
            return false;
        }
        @SuppressWarnings( "unchecked" ) Collection<MavenProject> mavenProjects =
            project.getProjectReferences().values();
        for ( MavenProject mavenProject : mavenProjects )
        {
            if ( StringUtils.equals( mavenProject.getId(), artifact.getId() ) )
            {
                return true;
            }
        }
        return false;
    }
}
