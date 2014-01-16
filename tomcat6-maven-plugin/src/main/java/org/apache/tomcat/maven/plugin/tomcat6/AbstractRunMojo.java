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
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Catalina;
import org.apache.catalina.startup.Embedded;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFileFilterRequest;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.tomcat.maven.common.config.AbstractWebapp;
import org.apache.tomcat.maven.common.run.EmbeddedRegistry;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract goal that provides common configuration for embedded Tomcat goals.
 *
 * @author Jurgen Lust
 * @author Mark Hobson <markhobson@gmail.com>
 */
public abstract class AbstractRunMojo
    extends AbstractI18NTomcat6Mojo
{
    // ---------------------------------------------------------------------
    // Mojo Components
    // ---------------------------------------------------------------------

    /**
     * Used to look up Artifacts in the remote repository.
     */
    @Component(role = ArtifactFactory.class)
    protected ArtifactFactory artifactFactory;

    /**
     * Location of the local repository.
     */
    @Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
    private ArtifactRepository artifactRepository;

    /**
     * Used to look up Artifacts in the remote repository.
     */
    @Component(role = ArtifactResolver.class)
    protected ArtifactResolver artifactResolver;

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The packaging of the Maven project that this goal operates upon.
     */
    @Parameter(defaultValue = "${project.packaging}", required = true, readonly = true)
    private String packaging;

    /**
     * The directory to create the Tomcat server configuration under.
     */
    @Parameter(defaultValue = "${project.build.directory}/tomcat")
    private File configurationDir;

    /**
     * The port to run the Tomcat server on.
     */
    @Parameter(property = "maven.tomcat.port", defaultValue = "8080")
    private int port;

    /**
     * this IP address will be used on all ports.
     *
     * @since 2.2
     */
    @Parameter(property = "maven.tomcat.address")
    private String address;

    /**
     * The AJP port to run the Tomcat server on.
     * By default it's 0 this means won't be started.
     * The ajp connector will be started only for value > 0.
     *
     * @since 2.0
     */
    @Parameter(property = "maven.tomcat.ajp.port", defaultValue = "0")
    private int ajpPort;

    /**
     * The AJP protocol to run the Tomcat server on.
     * By default it's ajp.
     * NOTE The ajp connector will be started only if {@link #ajpPort} > 0.
     *
     * @since 2.0
     */
    @Parameter(property = "maven.tomcat.ajp.protocol", defaultValue = "ajp")
    private String ajpProtocol;

    /**
     * The https port to run the Tomcat server on.
     * By default it's 0 this means won't be started.
     * The https connector will be started only for value > 0.
     *
     * @since 1.0
     */
    @Parameter(property = "maven.tomcat.httpsPort", defaultValue = "0")
    private int httpsPort;

    /**
     * The max post size to run the Tomcat server on.
     * By default it's 2097152 bytes. That's the default Tomcat configuration.
     * Set this value to 0 or less to disable the post size limit.
     *
     * @since 2.3
     */
    @Parameter(property = "maven.tomcat.maxPostSize", defaultValue = "2097152")
    private int maxPostSize;

    /**
     * The character encoding to use for decoding URIs.
     *
     * @since 1.0
     */
    @Parameter(property = "maven.tomcat.uriEncoding", defaultValue = "ISO-8859-1")
    private String uriEncoding;

    /**
     * List of System properties to pass to the Tomcat Server.
     *
     * @since 1.0-alpha-2
     */
    @Parameter
    private Map<String, String> systemProperties;

    /**
     * The directory contains additional configuration Files that copied in the Tomcat conf Directory.
     *
     * @since 1.0-alpha-2
     */
    @Parameter(property = "maven.tomcat.additionalConfigFilesDir", defaultValue = "${basedir}/src/main/tomcatconf")
    private File additionalConfigFilesDir;

    /**
     * server.xml to use <b>Note if you use this you must configure in this file your webapp paths</b>.
     *
     * @since 1.0-alpha-2
     */
    @Parameter(property = "maven.tomcat.serverXml")
    private File serverXml;

    /**
     * overriding the providing web.xml to run tomcat
     * <b>This override the global Tomcat web.xml located in $CATALINA_HOME/conf/</b>
     *
     * @since 1.0-alpha-2
     */
    @Parameter(property = "maven.tomcat.webXml")
    private File tomcatWebXml;

    /**
     * Set this to true to allow Maven to continue to execute after invoking
     * the run goal.
     *
     * @since 1.0
     */
    @Parameter(property = "maven.tomcat.fork", defaultValue = "false")
    private boolean fork;

    /**
     * Will create a tomcat context for each dependencies of war type with 'scope' set to 'tomcat'.
     * In other words, dependencies with:
     * <pre>
     *    &lt;type&gt;war&lt;/type&gt;
     *    &lt;scope&gt;tomcat&lt;/scope&gt;
     * </pre>
     * To preserve backward compatibility it's false by default.
     *
     * @since 1.0
     * @deprecated use webapps instead
     */
    @Parameter(property = "maven.tomcat.addContextWarDependencies", defaultValue = "false")
    private boolean addContextWarDependencies;

    /**
     * The maven project.
     *
     * @since 1.0
     */
    @Component
    protected MavenProject project;

    /**
     * The archive manager.
     *
     * @since 1.0
     */
    @Component(role = ArchiverManager.class)
    private ArchiverManager archiverManager;

    /**
     * if <code>true</code> a new classLoader separated from maven core will be created to start tomcat.
     *
     * @since 1.0
     */
    @Parameter(property = "tomcat.useSeparateTomcatClassLoader", defaultValue = "false")
    protected boolean useSeparateTomcatClassLoader;

    /**
     * @since 1.0
     */
    @Parameter(defaultValue = "${plugin.artifacts}", required = true)
    private List<Artifact> pluginArtifacts;

    /**
     * If set to true ignore if packaging of project is not 'war'.
     *
     * @since 1.0
     */
    @Parameter(property = "tomcat.ignorePackaging", defaultValue = "false")
    private boolean ignorePackaging;

    /**
     * Override the default keystoreFile for the HTTPS connector (if enabled)
     *
     * @since 1.1
     */
    @Parameter
    private String keystoreFile;

    /**
     * Override the default keystorePass for the HTTPS connector (if enabled)
     *
     * @since 1.1
     */
    @Parameter
    private String keystorePass;

    /**
     * Override the type of keystore file to be used for the server certificate. If not specified, the default value is "JKS".
     *
     * @since 2.0
     */
    @Parameter(defaultValue = "JKS")
    private String keystoreType;

    /**
     * Override the default truststoreFile for the HTTPS connector (if enabled)
     *
     * @since 2.2
     */
    @Parameter
    private String truststoreFile;

    /**
     * Override the default truststorePass for the HTTPS connector (if enabled)
     *
     * @since 2.2
     */
    @Parameter
    private String truststorePass;

    /**
     * Override the default truststoreType for the HTTPS connector (if enabled)
     *
     * @since 2.2
     */
    @Parameter
    private String truststoreType;

    /**
     * Override the default truststoreProvider for the HTTPS connector (if enabled)
     *
     * @since 2.2
     */
    @Parameter
    private String truststoreProvider;

    /**
     * <p>
     * Enables or disables naming support for the embedded Tomcat server. By default the embedded Tomcat
     * in Tomcat 6 comes with naming enabled. In contrast to this the embedded Tomcat 7 comes with
     * naming disabled by default.
     * </p>
     * <p>
     * <strong>Note:</strong> This setting is ignored if you provide a <code>server.xml</code> for your
     * Tomcat. Instead please configure naming in the <code>server.xml</code>.
     * </p>
     *
     * @see <a href="http://tomcat.apache.org/tomcat-6.0-doc/api/org/apache/catalina/startup/Embedded.html">org.apache.catalina.startup.Embedded</a>
     * @since 2.0
     */
    @Parameter(property = "maven.tomcat.useNaming", defaultValue = "true")
    private boolean useNaming;

    /**
     * Force context scanning if you don't use a context file with reloadable = "true".
     * The other way to use contextReloadable is to add attribute reloadable = "true"
     * in your context file.
     *
     * @since 2.0
     */
    @Parameter(property = "maven.tomcat.contextReloadable", defaultValue = "false")
    protected boolean contextReloadable;

    /**
     * represents the delay in seconds between each classPathScanning change invocation
     *
     * @see <a href="http://tomcat.apache.org/tomcat-6.0-doc/config/context.html">http://tomcat.apache.org/tomcat-6.0-doc/config/context.html</a>
     */
    @Parameter(property = "maven.tomcat.backgroundProcessorDelay", defaultValue = "-1")
    protected int backgroundProcessorDelay = -1;

    /**
     * The path of the Tomcat context XML file.
     */
    @Parameter(defaultValue = "src/main/webapp/META-INF/context.xml")
    protected File contextFile;

    /**
     * The protocol to run the Tomcat server on.
     * By default it's HTTP/1.1.
     * See possible values <a href="http://tomcat.apache.org/tomcat-6.0-doc/config/http.html">HTTP Connector</a>
     * protocol attribute
     *
     * @since 2.0
     */
    @Parameter(property = "maven.tomcat.protocol", defaultValue = "HTTP/1.1")
    private String protocol;

    /**
     * The path of the Tomcat users XML file.
     */
    @Parameter(property = "maven.tomcat.tomcatUsers.file")
    private File tomcatUsers;

    /**
     * to install a manager in your embeded tomcat
     *
     * @since 2.0
     */
    @Parameter
    private File managerWarPath;


    /**
     * Skip execution
     *
     * @since 2.0
     */
    @Parameter(property = "maven.tomcat.skip", defaultValue = "false")
    protected boolean skip;

    /**
     * @see {@link Webapp}
     * @since 2.0
     */
    @Parameter
    private List<Webapp> webapps;

    /**
     * configure host name
     *
     * @since 2.1
     */
    @Parameter(property = "maven.tomcat.hostName", defaultValue = "localhost")
    protected String hostName;

    /**
     * configure aliases
     * see <a href="http://tomcat.apache.org/tomcat-6.0-doc/config/host.html#Host_Name_Aliases">Host Name aliases</a>
     *
     * @since 2.1
     */
    @Parameter
    protected String[] aliases;

    // ----------------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------------

    /**
     * @since 1.0
     */
    private ClassRealm tomcatRealm;

    /**
     * The static context
     *
     * @since 2.0
     */
    @Parameter(property = "maven.tomcat.staticContextPath", defaultValue = "/")
    private String staticContextPath;

    /**
     * The static context docroot base fully qualified path.
     * if <code>null</code> static context won't be added
     *
     * @since 2.0
     */
    @Parameter(property = "maven.tomcat.staticContextDocbase")
    private String staticContextDocbase;

    /**
     * Class loader class to set.
     *
     * @since 2.0
     */
    @Parameter
    protected String classLoaderClass;

    /**
     * @since 2.2
     */
    @Parameter(property = "maven.tomcat.useBodyEncodingForURI", defaultValue = "false")
    protected boolean useBodyEncodingForURI;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    @Component(role = MavenFileFilter.class, hint = "default")
    protected MavenFileFilter mavenFileFilter;

    // ----------------------------------------------------------------------
    // Mojo Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skip )
        {
            getLog().info( "skip execution" );
            return;
        }
        // ensure project is a web application and we have at least additionnal webapps to run
        if ( !isWar() && !addContextWarDependencies && getAdditionalWebapps().isEmpty() )
        {
            getLog().info( messagesProvider.getMessage( "AbstractRunMojo.nonWar" ) );
            return;
        }
        ClassLoader originalClassLoader = null;
        try
        {

            if ( useSeparateTomcatClassLoader )
            {
                originalClassLoader = Thread.currentThread().getContextClassLoader();
            }
            getLog().info( messagesProvider.getMessage( "AbstractRunMojo.runningWar", getWebappUrl() ) );

            initConfiguration();
            startContainer();
            if ( !fork )
            {
                waitIndefinitely();
            }
        }
        catch ( LifecycleException exception )
        {
            throw new MojoExecutionException( messagesProvider.getMessage( "AbstractRunMojo.cannotStart" ), exception );
        }
        catch ( IOException exception )
        {
            throw new MojoExecutionException(
                messagesProvider.getMessage( "AbstractRunMojo.cannotCreateConfiguration" ), exception );
        }
        catch ( MavenFilteringException e )
        {
            throw new MojoExecutionException( "filtering issue: " + e.getMessage(), e );
        }
        finally
        {
            if ( useSeparateTomcatClassLoader )
            {
                Thread.currentThread().setContextClassLoader( originalClassLoader );
            }
        }
    }

    // ----------------------------------------------------------------------
    // Protected Methods
    // ----------------------------------------------------------------------

    /**
     * Gets the webapp context path to use for the web application being run.
     *
     * @return the webapp context path
     */
    protected String getPath()
    {
        return path;
    }

    /**
     * Gets the context to run this web application under for the specified embedded Tomcat.
     *
     * @param container the embedded Tomcat container being used
     * @return the context to run this web application under
     * @throws IOException            if the context could not be created
     * @throws MojoExecutionException in case of an error creating the context
     */
    protected Context createContext( Embedded container )
        throws IOException, MojoExecutionException
    {
        String contextPath = getPath();
        Context context =
            container.createContext( "/".equals( contextPath ) ? "" : contextPath, getDocBase().getAbsolutePath() );

        if ( useSeparateTomcatClassLoader )
        {
            context.setParentClassLoader( getTomcatClassLoader() );
        }

        final WebappLoader webappLoader = createWebappLoader();

        if ( classLoaderClass != null )
        {
            webappLoader.setLoaderClass( classLoaderClass );
        }

        context.setLoader( webappLoader );
        File contextFile = getContextFile();
        if ( contextFile != null )
        {
            context.setConfigFile( getContextFile().getAbsolutePath() );
        }
        return context;
    }

    /**
     * Gets the webapp loader to run this web application under.
     *
     * @return the webapp loader to use
     * @throws IOException            if the webapp loader could not be created
     * @throws MojoExecutionException in case of an error creating the webapp loader
     */
    protected WebappLoader createWebappLoader()
        throws IOException, MojoExecutionException
    {
        if ( useSeparateTomcatClassLoader )
        {
            return ( isContextReloadable() )
                ? new ExternalRepositoriesReloadableWebappLoader( getTomcatClassLoader(), getLog() )
                : new WebappLoader( getTomcatClassLoader() );
        }

        return ( isContextReloadable() )
            ? new ExternalRepositoriesReloadableWebappLoader( Thread.currentThread().getContextClassLoader(), getLog() )
            : new WebappLoader( Thread.currentThread().getContextClassLoader() );
    }

    /**
     * Determine whether the passed context.xml file declares the context as reloadable or not.
     *
     * @return false by default, true if  reloadable="true" in context.xml.
     */
    protected boolean isContextReloadable()
        throws MojoExecutionException
    {
        if ( contextReloadable || backgroundProcessorDelay > 0 )
        {
            return true;
        }
        // determine whether to use a reloadable Loader or not (default is false).
        boolean reloadable = false;
        try
        {
            if ( contextFile != null && contextFile.exists() )
            {
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document contextDoc = builder.parse( contextFile );
                contextDoc.getDocumentElement().normalize();

                NamedNodeMap nodeMap = contextDoc.getDocumentElement().getAttributes();
                Node reloadableAttribute = nodeMap.getNamedItem( "reloadable" );

                reloadable =
                    ( reloadableAttribute != null ) ? Boolean.valueOf( reloadableAttribute.getNodeValue() ) : false;
            }
            getLog().debug( "context reloadable: " + reloadable );
        }
        catch ( IOException ioe )
        {
            getLog().error( "Could not parse file: [" + contextFile.getAbsolutePath() + "]", ioe );
        }
        catch ( ParserConfigurationException pce )
        {
            getLog().error( "Could not configure XML parser", pce );
        }
        catch ( SAXException se )
        {
            getLog().error( "Could not parse file: [" + contextFile.getAbsolutePath() + "]", se );
        }

        return reloadable;
    }


    /**
     * Gets the webapp directory to run.
     *
     * @return the webapp directory
     */
    protected abstract File getDocBase();

    /**
     * Gets the Tomcat context XML file to use.
     *
     * @return the context XML file
     */
    protected abstract File getContextFile()
        throws MojoExecutionException;

    // ----------------------------------------------------------------------
    // Private Methods
    // ----------------------------------------------------------------------

    /**
     * Gets whether this project uses WAR packaging.
     *
     * @return whether this project uses WAR packaging
     */
    protected boolean isWar()
    {
        return "war".equals( packaging ) || ignorePackaging;
    }

    /**
     * Gets the URL of the running webapp.
     *
     * @return the URL of the running webapp
     * @throws MalformedURLException if the running webapp URL is invalid
     */
    private URL getWebappUrl()
        throws MalformedURLException
    {
        return new URL( "http", "localhost", port, getPath() );
    }

    /**
     * Creates the Tomcat configuration directory with the necessary resources.
     *
     * @throws IOException            if the Tomcat configuration could not be created
     * @throws MojoExecutionException if the Tomcat configuration could not be created
     */
    private void initConfiguration()
        throws IOException, MojoExecutionException, MavenFilteringException
    {
        if ( configurationDir.exists() )
        {
            getLog().info( messagesProvider.getMessage( "AbstractRunMojo.usingConfiguration", configurationDir ) );
        }
        else
        {
            getLog().info( messagesProvider.getMessage( "AbstractRunMojo.creatingConfiguration", configurationDir ) );

            configurationDir.mkdirs();

            File confDir = new File( configurationDir, "conf" );
            confDir.mkdir();

            copyFile( "/conf/tomcat-users.xml", new File( confDir, "tomcat-users.xml" ) );

            if ( tomcatWebXml != null )
            {
                if ( !tomcatWebXml.exists() )
                {
                    throw new MojoExecutionException( " tomcatWebXml " + tomcatWebXml.getPath() + " not exists" );
                }
                //MTOMCAT-42  here it's a real file resources not a one coming with the mojo 
                FileUtils.copyFile( tomcatWebXml, new File( confDir, "web.xml" ) );
                //MTOMCAT-128 apply filtering
                MavenFileFilterRequest mavenFileFilterRequest = new MavenFileFilterRequest();
                mavenFileFilterRequest.setFrom( tomcatWebXml );
                mavenFileFilterRequest.setTo( new File( confDir, "web.xml" ) );
                mavenFileFilterRequest.setMavenProject( project );
                mavenFileFilterRequest.setMavenSession( session );
                mavenFileFilterRequest.setFiltering( true );

                mavenFileFilter.copyFile( mavenFileFilterRequest );
            }
            else
            {
                copyFile( "/conf/web.xml", new File( confDir, "web.xml" ) );
            }

            File logDir = new File( configurationDir, "logs" );
            logDir.mkdir();

            File webappsDir = new File( configurationDir, "webapps" );
            webappsDir.mkdir();
            if ( managerWarPath != null && managerWarPath.exists() )
            {
                FileUtils.copyFileToDirectory( managerWarPath, webappsDir );
            }

            if ( additionalConfigFilesDir != null && additionalConfigFilesDir.exists() )
            {
                DirectoryScanner scanner = new DirectoryScanner();
                scanner.addDefaultExcludes();
                scanner.setBasedir( additionalConfigFilesDir.getPath() );
                scanner.scan();

                String[] files = scanner.getIncludedFiles();

                if ( files != null && files.length > 0 )
                {
                    getLog().info( "Coping additional tomcat config files" );

                    for ( int i = 0; i < files.length; i++ )
                    {
                        File file = new File( additionalConfigFilesDir, files[i] );

                        getLog().info( " copy " + file.getName() );

                        FileUtils.copyFileToDirectory( file, confDir );
                    }
                }
            }
        }
    }

    /**
     * Copies the specified class resource to the specified file.
     *
     * @param fromPath the path of the class resource to copy
     * @param toFile   the file to copy to
     * @throws IOException if the file could not be copied
     */
    private void copyFile( String fromPath, File toFile )
        throws IOException
    {
        URL fromURL = getClass().getResource( fromPath );

        if ( fromURL == null )
        {
            throw new FileNotFoundException( fromPath );
        }

        FileUtils.copyURLToFile( fromURL, toFile );
    }

    /**
     * Starts the embedded Tomcat server.
     *
     * @throws IOException            if the server could not be configured
     * @throws LifecycleException     if the server could not be started
     * @throws MojoExecutionException if the server could not be configured
     */
    private void startContainer()
        throws IOException, LifecycleException, MojoExecutionException
    {
        String previousCatalinaBase = System.getProperty( "catalina.base" );

        try
        {

            // Set the system properties
            setupSystemProperties();

            System.setProperty( "catalina.base", configurationDir.getAbsolutePath() );
            System.setProperty( "catalina.home", configurationDir.getAbsolutePath() );

            File catalinaPolicy = new File( configurationDir, "conf/catalina.policy" );

            if ( catalinaPolicy.exists() )
            {
                // FIXME restore previous value ?
                System.setProperty( "java.security.policy", catalinaPolicy.getAbsolutePath() );
            }

            final Embedded container;
            if ( serverXml != null )
            {
                if ( !serverXml.exists() )
                {
                    throw new MojoExecutionException( serverXml.getPath() + " not exists" );
                }

                container = new Catalina();
                container.setCatalinaHome( configurationDir.getAbsolutePath() );
                container.setCatalinaBase( configurationDir.getAbsolutePath() );
                ( (Catalina) container ).setConfigFile( serverXml.getPath() );
                ( (Catalina) container ).setRedirectStreams( true );
                ( (Catalina) container ).setUseNaming( this.useNaming );

                container.start();
            }
            else
            {
                // create server
                container = new Embedded();
                container.setCatalinaHome( configurationDir.getAbsolutePath() );
                MemoryRealm memoryRealm = new MemoryRealm();

                if ( tomcatUsers != null )
                {
                    if ( !tomcatUsers.exists() )
                    {
                        throw new MojoExecutionException( " tomcatUsers " + tomcatUsers.getPath() + " not exists" );
                    }
                    getLog().info( "use tomcat-users.xml from " + tomcatUsers.getAbsolutePath() );
                    memoryRealm.setPathname( tomcatUsers.getAbsolutePath() );

                }

                container.setRealm( memoryRealm );
                container.setUseNaming( useNaming );

                //container.createLoader( getTomcatClassLoader() ).

                // create context
                Context context = createContext( container );

                // create host
                String appBase = new File( configurationDir, "webapps" ).getAbsolutePath();
                Host host = container.createHost( "localHost", appBase );

                if ( hostName != null )
                {
                    host.setName( hostName );
                }
                if ( aliases != null )
                {
                    for ( String alias : aliases )
                    {
                        host.addAlias( alias );
                    }
                }

                host.addChild( context );
                createStaticContext( container, context, host );
                if ( addContextWarDependencies || !getAdditionalWebapps().isEmpty() )
                {
                    Collection<Context> dependencyContexts = createDependencyContexts( container );
                    for ( Context extraContext : dependencyContexts )
                    {
                        host.addChild( extraContext );
                    }
                }

                // create engine
                Engine engine = container.createEngine();
                engine.setName( "localEngine-" + port );
                engine.addChild( host );
                engine.setDefaultHost( host.getName() );
                container.addEngine( engine );

                getLog().debug( "start tomcat instance on http port:" + port + " and protocol: " + protocol );

                // create http connector
                Connector httpConnector = container.createConnector( (InetAddress) null, port, protocol );
                httpConnector.setMaxPostSize( maxPostSize );
                if ( httpsPort > 0 )
                {
                    httpConnector.setRedirectPort( httpsPort );
                }
                httpConnector.setURIEncoding( uriEncoding );
                httpConnector.setUseBodyEncodingForURI( this.useBodyEncodingForURI );

                if ( address != null )
                {
                    httpConnector.setAttribute( "address", address );
                }

                container.addConnector( httpConnector );

                // create https connector
                if ( httpsPort > 0 )
                {
                    Connector httpsConnector = container.createConnector( (InetAddress) null, httpsPort, true );
                    httpsConnector.setSecure( true );
                    httpsConnector.setMaxPostSize( maxPostSize );
                    httpsConnector.setProperty( "SSLEnabled", "true" );
                    // should be default but configure it anyway
                    httpsConnector.setProperty( "sslProtocol", "TLS" );
                    if ( keystoreFile != null )
                    {
                        httpsConnector.setAttribute( "keystoreFile", keystoreFile );
                    }
                    if ( keystorePass != null )
                    {
                        httpsConnector.setAttribute( "keystorePass", keystorePass );
                    }
                    if ( keystoreType != null )
                    {
                        httpsConnector.setAttribute( "keystoreType", keystoreType );
                    }

                    if ( truststoreFile != null )
                    {
                        httpsConnector.setAttribute( "truststoreFile", truststoreFile );
                    }

                    if ( truststorePass != null )
                    {
                        httpsConnector.setAttribute( "truststorePass", truststorePass );
                    }

                    if ( truststoreType != null )
                    {
                        httpsConnector.setAttribute( "truststoreType", truststoreType );
                    }

                    if ( truststoreProvider != null )
                    {
                        httpsConnector.setAttribute( "truststoreProvider", truststoreProvider );
                    }

                    httpsConnector.setUseBodyEncodingForURI( this.useBodyEncodingForURI );

                    if ( address != null )
                    {
                        httpsConnector.setAttribute( "address", address );
                    }

                    container.addConnector( httpsConnector );

                }

                // create ajp connector
                if ( ajpPort > 0 )
                {
                    Connector ajpConnector = container.createConnector( (InetAddress) null, ajpPort, ajpProtocol );
                    ajpConnector.setURIEncoding( uriEncoding );
                    ajpConnector.setUseBodyEncodingForURI( this.useBodyEncodingForURI );
                    if ( address != null )
                    {
                        ajpConnector.setAttribute( "address", address );
                    }
                    container.addConnector( ajpConnector );
                }
                if ( useSeparateTomcatClassLoader )
                {
                    Thread.currentThread().setContextClassLoader( getTomcatClassLoader() );
                    engine.setParentClassLoader( getTomcatClassLoader() );
                }
                container.start();
            }

            EmbeddedRegistry.getInstance().register( container );
        }
        finally
        {
            if ( previousCatalinaBase != null )
            {
                System.setProperty( "catalina.base", previousCatalinaBase );
            }
        }
    }

    private List<Webapp> getAdditionalWebapps()
    {
        if ( webapps == null )
        {
            return Collections.emptyList();
        }
        return webapps;
    }

    protected ClassRealm getTomcatClassLoader()
        throws MojoExecutionException
    {
        if ( this.tomcatRealm != null )
        {
            return tomcatRealm;
        }
        try
        {
            ClassWorld world = new ClassWorld();
            ClassRealm root = world.newRealm( "tomcat", Thread.currentThread().getContextClassLoader() );

            for ( Artifact pluginArtifact : pluginArtifacts )
            {
                // add all plugin artifacts see https://issues.apache.org/jira/browse/MTOMCAT-122
                if ( pluginArtifact.getFile() != null )
                {
                    root.addURL( pluginArtifact.getFile().toURI().toURL() );
                }

            }
            tomcatRealm = root;
            return root;
        }
        catch ( DuplicateRealmException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( MalformedURLException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    @SuppressWarnings("unchecked")
    public Set<Artifact> getProjectArtifacts()
    {
        return project.getArtifacts();
    }

    /**
     * Causes the current thread to wait indefinitely. This method does not return.
     */
    private void waitIndefinitely()
    {
        Object lock = new Object();

        synchronized ( lock )
        {
            try
            {
                lock.wait();
            }
            catch ( InterruptedException exception )
            {
                getLog().warn( messagesProvider.getMessage( "AbstractRunMojo.interrupted" ), exception );
            }
        }
    }


    /**
     * Set the SystemProperties from the configuration.
     */
    private void setupSystemProperties()
    {
        if ( systemProperties != null && !systemProperties.isEmpty() )
        {
            getLog().info( "setting SystemProperties:" );

            for ( String key : systemProperties.keySet() )
            {
                String value = systemProperties.get( key );

                if ( value != null )
                {
                    getLog().info( " " + key + "=" + value );
                    System.setProperty( key, value );
                }
                else
                {
                    getLog().info( "skip sysProps " + key + " with empty value" );
                }
            }
        }
    }


    /**
     * Allows the startup of additional webapps in the tomcat container by declaration with scope
     * "tomcat".
     *
     * @param container tomcat
     * @return dependency tomcat contexts of warfiles in scope "tomcat" and those from webapps
     */
    private Collection<Context> createDependencyContexts( Embedded container )
        throws MojoExecutionException
    {
        getLog().info( "Deploying dependency wars" );
        // Let's add other modules
        List<Context> contexts = new ArrayList<Context>();

        ScopeArtifactFilter filter = new ScopeArtifactFilter( "tomcat" );
        @SuppressWarnings("unchecked") Set<Artifact> artifacts = project.getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            // Artifact is not yet registered and it has neither test, nor a
            // provided scope, not is it optional
            if ( "war".equals( artifact.getType() ) && !artifact.isOptional() && filter.include( artifact ) )
            {
                addContextFromArtifact( container, contexts, artifact, "/" + artifact.getArtifactId(), null );
            }
        }

        for ( AbstractWebapp additionalWebapp : getAdditionalWebapps() )
        {
            String contextPath = additionalWebapp.getContextPath();
            if ( !contextPath.startsWith( "/" ) )
            {
                contextPath = "/" + contextPath;
            }
            addContextFromArtifact( container, contexts, getArtifact( additionalWebapp ), contextPath,
                                    additionalWebapp.getContextFile() );
        }
        return contexts;
    }


    private void addContextFromArtifact( Embedded container, List<Context> contexts, Artifact artifact,
                                         String contextPath, File contextXml )
        throws MojoExecutionException
    {
        getLog().info( "Deploy warfile: " + String.valueOf( artifact.getFile() ) + " to contextPath: " + contextPath );
        File webapps = new File( configurationDir, "webapps" );
        File artifactWarDir = new File( webapps, artifact.getArtifactId() );
        if ( !artifactWarDir.exists() )
        {
            //dont extract if exists
            artifactWarDir.mkdir();
            try
            {
                UnArchiver unArchiver = archiverManager.getUnArchiver( "zip" );
                unArchiver.setSourceFile( artifact.getFile() );
                unArchiver.setDestDirectory( artifactWarDir );

                // Extract the module
                unArchiver.extract();
            }
            catch ( NoSuchArchiverException e )
            {
                getLog().error( e );
                return;
            }
            catch ( ArchiverException e )
            {
                getLog().error( e );
                return;
            }
        }
        WebappLoader webappLoader = new WebappLoader( Thread.currentThread().getContextClassLoader() );
        Context context = container.createContext( contextPath, artifactWarDir.getAbsolutePath() );
        context.setLoader( webappLoader );

        File contextFile = contextXml != null ? contextXml : getContextFile();
        if ( contextFile != null )
        {
            context.setConfigFile( contextFile.getAbsolutePath() );
        }
        contexts.add( context );
    }


    private void createStaticContext( final Embedded container, Context context, Host host )
    {
        if ( staticContextDocbase != null )
        {
            Context staticContext = container.createContext( staticContextPath, staticContextDocbase );
            staticContext.setPrivileged( true );
            Wrapper servlet = context.createWrapper();
            servlet.setServletClass( DefaultServlet.class.getName() );
            servlet.setName( "staticContent" );
            staticContext.addChild( servlet );
            staticContext.addServletMapping( "/", "staticContent" );
            host.addChild( staticContext );
        }
    }


    /**
     * Resolves the Artifact from the remote repository if necessary. If no version is specified, it will be retrieved
     * from the dependency list or from the DependencyManagement section of the pom.
     *
     * @param additionalWebapp containing information about artifact from plugin configuration.
     * @return Artifact object representing the specified file.
     * @throws MojoExecutionException with a message if the version can't be found in DependencyManagement.
     */
    protected Artifact getArtifact( AbstractWebapp additionalWebapp )
        throws MojoExecutionException
    {

        Artifact artifact;
        VersionRange vr;
        try
        {
            vr = VersionRange.createFromVersionSpec( additionalWebapp.getVersion() );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            getLog().warn( "fail to create versionRange from version: " + additionalWebapp.getVersion(), e );
            vr = VersionRange.createFromVersion( additionalWebapp.getVersion() );
        }

        if ( StringUtils.isEmpty( additionalWebapp.getClassifier() ) )
        {
            artifact = artifactFactory.createDependencyArtifact( additionalWebapp.getGroupId(),
                                                                 additionalWebapp.getArtifactId(), vr,
                                                                 additionalWebapp.getType(), null,
                                                                 Artifact.SCOPE_COMPILE );
        }
        else
        {
            artifact = artifactFactory.createDependencyArtifact( additionalWebapp.getGroupId(),
                                                                 additionalWebapp.getArtifactId(), vr,
                                                                 additionalWebapp.getType(),
                                                                 additionalWebapp.getClassifier(),
                                                                 Artifact.SCOPE_COMPILE );
        }

        try
        {
            artifactResolver.resolve( artifact, project.getRemoteArtifactRepositories(), this.artifactRepository );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Unable to resolve artifact.", e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException( "Unable to find artifact.", e );
        }

        return artifact;
    }
}
