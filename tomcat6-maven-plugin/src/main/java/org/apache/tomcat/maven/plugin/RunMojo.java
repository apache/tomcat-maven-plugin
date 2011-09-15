package org.apache.tomcat.maven.plugin;

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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.catalina.Context;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Embedded;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Runs the current project as a dynamic web application using an embedded Tomcat server.
 * 
 * @goal run
 * @execute phase="compile"
 * @requiresDependencyResolution runtime
 * @author Jurgen Lust
 * @author Mark Hobson <markhobson@gmail.com>
 * @version $Id: RunMojo.java 13551 2011-02-09 16:05:47Z olamy $
 */
public class RunMojo
    extends AbstractRunMojo 
{
    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The classes directory for the web application being run.
     * 
     * @parameter expression = "${project.build.outputDirectory}"
     */
    private File classesDir;

    /**
     * The set of dependencies for the web application being run.
     * 
     * @parameter default-value = "${project.artifacts}"
     * @required
     * @readonly
     */
    private Set<Artifact> dependencies;

    /**
     * The web resources directory for the web application being run.
     * 
     * @parameter expression="${basedir}/src/main/webapp"
     */
    private File warSourceDirectory;

    
    /**
     * Set the "follow standard delegation model" flag used to configure our ClassLoader.
     * @see http://tomcat.apache.org/tomcat-6.0-doc/api/org/apache/catalina/loader/WebappLoader.html#setDelegate(boolean)
     * @parameter expression = "${tomcat.delegate}" default-value="true"
     * @since 1.0
     */    
    private boolean delegate = true;
    
    /**
     * represents the delay in seconds between each classPathScanning change invocation
     * @see <a href="http://tomcat.apache.org/tomcat-6.0-doc/config/context.html">http://tomcat.apache.org/tomcat-6.0-doc/config/context.html</a>
     * @parameter expression="${maven.tomcat.backgroundProcessorDelay}" default-value="-1"
     */
    protected int backgroundProcessorDelay = -1;    
    
    private File temporaryContextFile = null;

    // ----------------------------------------------------------------------
    // AbstractRunMojo Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * @throws MojoExecutionException 
     */
    @Override
    protected Context createContext( Embedded container )
        throws IOException, MojoExecutionException
    {
        Context context = super.createContext( container );

        context.setReloadable( isContextReloadable() );
        
        return context;
    }

    /**
     * {@inheritDoc}
     * @throws MojoExecutionException 
     */
    @Override
    protected WebappLoader createWebappLoader()
        throws IOException, MojoExecutionException
    {
        WebappLoader loader = super.createWebappLoader();
        //super.project.
        if ( useSeparateTomcatClassLoader )
        {
            loader.setDelegate( delegate );
        }
                
        // add classes directories to loader
        if ( classesDir != null )
        {
            try
            {
                @SuppressWarnings("unchecked")
                List<String> classPathElements = project.getCompileClasspathElements();
                for (String classPathElement : classPathElements)
                {
                    File classPathElementFile = new File(classPathElement);
                    if (classPathElementFile.exists() && classPathElementFile.isDirectory())
                    {
                        getLog().debug( "adding classPathElementFile " + classPathElementFile.toURI().toString() );
                        loader.addRepository( classPathElementFile.toURI().toString() );
                    }
                }
            }
            catch ( DependencyResolutionRequiredException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }
            
            //loader.addRepository( classesDir.toURI().toString() );
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
                    getLog().debug(
                                    "add dependency to webapploader " + artifact.getGroupId() + ":"
                                        + artifact.getArtifactId() + ":" + artifact.getVersion() + ":"
                                        + artifact.getScope() );
                    if ( !isInProjectReferences( artifact ) )
                    {
                        loader.addRepository( artifact.getFile().toURI().toString() );
                    }
                    else
                    {
                        getLog().debug( "skip adding artifact " + artifact.getArtifactId() + " as it's in reactors" );
                    }
                }
            }
        }

        return loader;
    }
    
    protected boolean isInProjectReferences(Artifact artifact)
    {
        if ( project.getProjectReferences() == null || project.getProjectReferences().isEmpty() )
        {
            return false;
        }
        @SuppressWarnings("unchecked")
        Collection<MavenProject> mavenProjects = project.getProjectReferences().values();
        for ( MavenProject mavenProject : mavenProjects )
        {
            if (StringUtils.equals( mavenProject.getId(), artifact.getId() ))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected File getDocBase()
    {
        return warSourceDirectory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected File getContextFile() throws MojoExecutionException
    {
        if ( temporaryContextFile != null )
        {
            return temporaryContextFile;
        }
        //----------------------------------------------------------------------------
        // context attributes backgroundProcessorDelay reloadable cannot be modifiec at runtime.
        // It looks only values from the file ared used
        // so here we create a temporary file with values modified
        //----------------------------------------------------------------------------
        FileReader fr = null;
        FileWriter fw = null;
        StringWriter sw = new StringWriter();
        try
        {
            temporaryContextFile = File.createTempFile( "tomcat-maven-plugin", "temp-ctx-file" );
            fw = new FileWriter( temporaryContextFile );
            // format to modify/create <Context backgroundProcessorDelay="5" reloadable="false">
            if ( contextFile != null && contextFile.exists() )
            {
                fr = new FileReader( contextFile );
                Xpp3Dom xpp3Dom = Xpp3DomBuilder.build( fr );
                xpp3Dom.setAttribute( "backgroundProcessorDelay", Integer.toString( backgroundProcessorDelay ) );
                xpp3Dom.setAttribute( "reloadable", Boolean.toString( isContextReloadable() ) );
                Xpp3DomWriter.write( fw, xpp3Dom );
                Xpp3DomWriter.write( sw, xpp3Dom );
                getLog().debug( " generated context file " + sw.toString() );
            }
            else
            {
                if ( contextReloadable )
                {
                    // don't care about using a complicated xml api to create one xml line :-)
                    StringBuilder sb = new StringBuilder( "<Context " ).append( "backgroundProcessorDelay=\"" )
                        .append( Integer.toString( backgroundProcessorDelay ) ).append( "\"" )
                        .append( " reloadable=\"" + Boolean.toString( isContextReloadable() ) + "\"/>" );
                    
                    getLog().debug( " generated context file " + sb.toString() );
                    
                    fw.write( sb.toString() );
                } else {
                    // no user context file and contextReloadable false so no need about creating a hack one 
                    return null;
                }
            }
        }
        catch ( IOException e )
        {
            getLog().error( "error creating fake context.xml : " + e.getMessage(), e );
            throw new MojoExecutionException( "error creating fake context.xml : " + e.getMessage(), e );
        }
        catch ( XmlPullParserException e )
        {
            getLog().error( "error creating fake context.xml : " + e.getMessage(), e );
            throw new MojoExecutionException( "error creating fake context.xml : " + e.getMessage(), e );
        } finally {
            IOUtil.close( fw );
            IOUtil.close( fr );
            IOUtil.close( sw );
        }        
        
        return temporaryContextFile;
    }

}
