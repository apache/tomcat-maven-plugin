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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
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
    public List<String> calculateClassPathEntries( MavenProject project, Set<Artifact> dependencies, Log log )
        throws TomcatRunException
    {
        List<String> classLoaderEntries = new ArrayList<String>();
        // add classes directories to loader

        try
        {
            @SuppressWarnings( "unchecked" ) List<String> classPathElements = project.getCompileClasspathElements();
            if ( classPathElements != null )
            {
                for ( String classPathElement : classPathElements )
                {
                    File classPathElementFile = new File( classPathElement );
                    if ( classPathElementFile.exists() && classPathElementFile.isDirectory() )
                    {
                        log.debug( "adding classPathElementFile " + classPathElementFile.toURI().toString() );
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
        if ( dependencies != null )
        {
            for ( Artifact artifact : dependencies )
            {
                String scope = artifact.getScope();

                // skip provided and test scoped artifacts
                if ( !Artifact.SCOPE_PROVIDED.equals( scope ) && !Artifact.SCOPE_TEST.equals( scope ) )
                {
                    log.debug(
                        "add dependency to webapploader " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":"
                            + artifact.getVersion() + ":" + artifact.getScope() );
                    if ( !isInProjectReferences( artifact, project ) )
                    {
                        classLoaderEntries.add( artifact.getFile().toURI().toString() );
                    }
                    else
                    {
                        log.debug( "skip adding artifact " + artifact.getArtifactId() + " as it's in reactors" );
                    }
                }
            }
        }
        return classLoaderEntries;
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
