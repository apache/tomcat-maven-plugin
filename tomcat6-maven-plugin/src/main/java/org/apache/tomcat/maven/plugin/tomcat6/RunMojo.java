package org.apache.tomcat.maven.plugin.tomcat6;

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

import org.apache.catalina.Context;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Embedded;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tomcat.maven.common.run.ClassLoaderEntriesCalculator;
import org.apache.tomcat.maven.common.run.ClassLoaderEntriesCalculatorRequest;
import org.apache.tomcat.maven.common.run.ClassLoaderEntriesCalculatorResult;
import org.apache.tomcat.maven.common.run.TomcatRunException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;

/**
 * Runs the current project as a dynamic web application using an embedded Tomcat server.
 *
 * @author Jurgen Lust
 * @author Mark Hobson <markhobson@gmail.com>
 * @version $Id$
 * @goal run
 * @execute phase="compile"
 * @requiresDependencyResolution test
 */
public class RunMojo
    extends AbstractRunMojo
{
    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------


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
     * @parameter default-value="${basedir}/src/main/webapp" expression = "${tomcat.warSourceDirectory}"
     */
    private File warSourceDirectory;


    /**
     * Set the "follow standard delegation model" flag used to configure our ClassLoader.
     *
     * @parameter expression = "${tomcat.delegate}" default-value="true"
     * @see http://tomcat.apache.org/tomcat-6.0-doc/api/org/apache/catalina/loader/WebappLoader.html#setDelegate(boolean)
     * @since 1.0
     */
    private final boolean delegate = true;

    /**
     * represents the delay in seconds between each classPathScanning change invocation
     *
     * @parameter expression="${maven.tomcat.backgroundProcessorDelay}" default-value="-1"
     * @see <a href="http://tomcat.apache.org/tomcat-6.0-doc/config/context.html">http://tomcat.apache.org/tomcat-6.0-doc/config/context.html</a>
     */
    protected int backgroundProcessorDelay = -1;

    /**
     * @readonly
     * @component
     * @since 2.0
     */
    private ClassLoaderEntriesCalculator classLoaderEntriesCalculator;

    /**
     * will add /WEB-INF/lib/*.jar and /WEB-INF/classes from war dependencies in the webappclassloader
     *
     * @parameter expression="${maven.tomcat.addWarDependenciesInClassloader}" default-value="true"
     * @since 2.0
     */
    private boolean addWarDependenciesInClassloader;

    /**
     * will use the test classpath rather than the compile one and will add test dependencies too
     *
     * @parameter expression="${maven.tomcat.useTestClasspath}" default-value="false"
     * @since 2.0
     */
    private boolean useTestClasspath;

    /**
     * Additional optional directories to add to the embedded tomcat classpath.
     *
     * @parameter alias = "additionalClassesDirs"
     * @since 2.0-beta-1
     */
    private List<String> additionalClasspathDirs;

    // ----------------------------------------------------------------------
    // AbstractRunMojo Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
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
     *
     * @throws MojoExecutionException
     */
    @Override
    protected WebappLoader createWebappLoader()
        throws IOException, MojoExecutionException
    {
        WebappLoader loader = super.createWebappLoader();
        if ( useSeparateTomcatClassLoader )
        {
            loader.setDelegate( delegate );
        }

        try
        {
            ClassLoaderEntriesCalculatorRequest request =
                new ClassLoaderEntriesCalculatorRequest().setDependencies( dependencies ).setLog(
                    getLog() ).setMavenProject( project ).setAddWarDependenciesInClassloader(
                    addWarDependenciesInClassloader ).setUseTestClassPath( useTestClasspath );
            ClassLoaderEntriesCalculatorResult classLoaderEntriesCalculatorResult =
                classLoaderEntriesCalculator.calculateClassPathEntries( request );
            List<String> classLoaderEntries = classLoaderEntriesCalculatorResult.getClassPathEntries();
            final List<File> tmpDirectories = classLoaderEntriesCalculatorResult.getTmpDirectories();

            Runtime.getRuntime().addShutdownHook( new Thread()
            {
                @Override
                public void run()
                {
                    for ( File tmpDir : tmpDirectories )
                    {
                        try
                        {
                            FileUtils.deleteDirectory( tmpDir );
                        }
                        catch ( IOException e )
                        {
                            // ignore
                        }
                    }
                }
            } );

            if ( classLoaderEntries != null )
            {
                for ( String classLoaderEntry : classLoaderEntries )
                {
                    loader.addRepository( classLoaderEntry );
                }
            }
            if ( additionalClasspathDirs != null && !additionalClasspathDirs.isEmpty() )
            {
                for ( String additionalClasspathDir : additionalClasspathDirs )
                {
                    if ( StringUtils.isNotBlank( additionalClasspathDir ) )
                    {
                        File file = new File( additionalClasspathDir );
                        if ( file.exists() )
                        {
                            String fileUri = file.toURI().toString();
                            getLog().debug( "add file:" + fileUri + " as a additionalClasspathDir" );
                            loader.addRepository( fileUri );
                        }
                    }
                }
            }
        }
        catch ( TomcatRunException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }

        return loader;
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
    protected File getContextFile()
        throws MojoExecutionException
    {
        File temporaryContextFile = null;

        //----------------------------------------------------------------------------
        // context attributes backgroundProcessorDelay reloadable cannot be modified at runtime.
        // It looks only values from the file ared used
        // so here we create a temporary file with values modified
        //----------------------------------------------------------------------------
        FileReader fr = null;
        FileWriter fw = null;
        StringWriter sw = new StringWriter();
        try
        {
            temporaryContextFile = File.createTempFile( "tomcat-maven-plugin", "temp-ctx-file" );
            temporaryContextFile.deleteOnExit();
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
                    StringBuilder sb = new StringBuilder( "<Context " ).append( "backgroundProcessorDelay=\"" ).append(
                        Integer.toString( backgroundProcessorDelay ) ).append( "\"" ).append(
                        " reloadable=\"" + Boolean.toString( isContextReloadable() ) + "\"/>" );

                    getLog().debug( " generated context file " + sb.toString() );

                    fw.write( sb.toString() );
                }
                else
                {
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
        }
        finally
        {
            IOUtil.close( fw );
            IOUtil.close( fr );
            IOUtil.close( sw );
        }

        return temporaryContextFile;
    }

}
