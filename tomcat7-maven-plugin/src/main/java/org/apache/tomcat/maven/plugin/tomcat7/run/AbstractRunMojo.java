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

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Catalina;
import org.apache.catalina.startup.CatalinaProperties;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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
import org.apache.naming.NamingEntry;
import org.apache.naming.resources.FileDirContext;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.maven.common.config.AbstractWebapp;
import org.apache.tomcat.maven.common.run.EmbeddedRegistry;
import org.apache.tomcat.maven.common.run.ExternalRepositoriesReloadableWebappLoader;
import org.apache.tomcat.maven.plugin.tomcat7.AbstractTomcat7Mojo;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author Olivier Lamy
 * @since 2.0
 */
public abstract class AbstractRunMojo
    extends AbstractTomcat7Mojo
{
    // ---------------------------------------------------------------------
    // Mojo Components
    // ---------------------------------------------------------------------

    /**
     * Used to look up Artifacts in the remote repository.
     */
    @Component
    protected ArtifactFactory factory;

    /**
     * Location of the local repository.
     */
    @Parameter( defaultValue = "${localRepository}", required = true, readonly = true )
    private ArtifactRepository local;

    /**
     * Used to look up Artifacts in the remote repository.
     */
    @Component
    protected ArtifactResolver resolver;

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The packaging of the Maven project that this goal operates upon.
     */
    @Parameter( defaultValue = "${project.packaging}", required = true, readonly = true )
    private String packaging;

    /**
     * The directory to create the Tomcat server configuration under.
     */
    @Parameter( defaultValue = "${project.build.directory}/tomcat" )
    private File configurationDir;

    /**
     * The port to run the Tomcat server on.
     * Will be exposed as System props and session.executionProperties with key tomcat.maven.http.port
     */
    @Parameter( property = "maven.tomcat.port", defaultValue = "8080" )
    private int port;

    /**
     *
     * this IP address will be used on all ports
     * @since 2.2
     */
    @Parameter( property = "maven.tomcat.address")
    private String address;

    /**
     * The AJP port to run the Tomcat server on.
     * By default it's 0 this means won't be started.
     * The ajp connector will be started only for value > 0.
     * Will be exposed as System props and session.executionProperties with key tomcat.maven.ajp.port
     *
     * @since 2.0
     */
    @Parameter( property = "maven.tomcat.ajp.port", defaultValue = "0" )
    private int ajpPort;

    /**
     * The AJP protocol to run the Tomcat server on.
     * By default it's ajp.
     * NOTE The ajp connector will be started only if {@link #ajpPort} > 0.
     * possible values are:
     * <ul>
     * <li>org.apache.coyote.ajp.AjpProtocol - new blocking Java connector that supports an executor</li>
     * <li>org.apache.coyote.ajp.AjpAprProtocol - the APR/native connector.</li>
     * </ul>
     *
     * @since 2.0
     */
    @Parameter( property = "maven.tomcat.ajp.protocol", defaultValue = "org.apache.coyote.ajp.AjpProtocol" )
    private String ajpProtocol;

    /**
     * The https port to run the Tomcat server on.
     * By default it's 0 this means won't be started.
     * The https connector will be started only for value > 0.
     * Will be exposed as System props and session.executionProperties with key tomcat.maven.https.port
     *
     * @since 1.0
     */
    @Parameter( property = "maven.tomcat.httpsPort", defaultValue = "0" )
    private int httpsPort;

    /**
     * The character encoding to use for decoding URIs.
     *
     * @since 1.0
     */
    @Parameter( property = "maven.tomcat.uriEncoding", defaultValue = "ISO-8859-1" )
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
    @Parameter( property = "maven.tomcat.additionalConfigFilesDir", defaultValue = "${basedir}/src/main/tomcatconf" )
    private File additionalConfigFilesDir;

    /**
     * server.xml to use <b>Note if you use this you must configure in this file your webapp paths</b>.
     *
     * @since 1.0-alpha-2
     */
    @Parameter( property = "maven.tomcat.serverXml" )
    private File serverXml;

    /**
     * overriding the providing web.xml to run tomcat
     * <b>This override the global Tomcat web.xml located in $CATALINA_HOME/conf/</b>
     *
     * @since 1.0-alpha-2
     */
    @Parameter( property = "maven.tomcat.webXml" )
    private File tomcatWebXml;

    /**
     * Set this to true to allow Maven to continue to execute after invoking
     * the run goal.
     *
     * @since 1.0
     */
    @Parameter( property = "maven.tomcat.fork", defaultValue = "false" )
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
    @Parameter( property = "maven.tomcat.addContextWarDependencies", defaultValue = "false" )
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
    @Component
    private ArchiverManager archiverManager;

    /**
     * if <code>true</code> a new classLoader separated from maven core will be created to start tomcat.
     *
     * @since 1.0
     */
    @Parameter( property = "tomcat.useSeparateTomcatClassLoader", defaultValue = "false" )
    protected boolean useSeparateTomcatClassLoader;

    /**
     * @since 1.0
     */
    @Parameter( defaultValue = "${plugin.artifacts}", required = true )
    private List<Artifact> pluginArtifacts;

    /**
     * If set to true ignore if packaging of project is not 'war'.
     *
     * @since 1.0
     */
    @Parameter( property = "tomcat.ignorePackaging", defaultValue = "false" )
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
    @Parameter( defaultValue = "JKS" )
    private String keystoreType;

    /**
     * <p>
     * Enables or disables naming support for the embedded Tomcat server.
     * </p>
     * <p>
     * <strong>Note:</strong> This setting is ignored if you provide a <code>server.xml</code> for your
     * Tomcat. Instead please configure naming in the <code>server.xml</code>.
     * </p>
     *
     * @see <a href="http://tomcat.apache.org/tomcat-6.0-doc/api/org/apache/catalina/startup/Embedded.html">org.apache.catalina.startup.Embedded</a>
     * @see <a href="http://tomcat.apache.org/tomcat-7.0-doc/api/org/apache/catalina/startup/Tomcat.html">org.apache.catalina.startup.Tomcat</a>
     * @since 2.0
     */
    @Parameter( property = "maven.tomcat.useNaming", defaultValue = "true" )
    private boolean useNaming;

    /**
     * Force context scanning if you don't use a context file with reloadable = "true".
     * The other way to use contextReloadable is to add attribute reloadable = "true"
     * in your context file.
     *
     * @since 2.0
     */
    @Parameter( property = "maven.tomcat.contextReloadable", defaultValue = "false" )
    protected boolean contextReloadable;

    /**
     * represents the delay in seconds between each classPathScanning change invocation
     *
     * @see <a href="http://tomcat.apache.org/tomcat-7.0-doc/config/context.html">http://tomcat.apache.org/tomcat-7.0-doc/config/context.html</a>
     */
    @Parameter( property = "maven.tomcat.backgroundProcessorDelay", defaultValue = "-1" )
    protected int backgroundProcessorDelay = -1;


    /**
     * <p>The path of the Tomcat context XML file.</p>
     * <p>Since release 2.0, the file is filtered as a maven resource so you can use
     * interpolation tokens ${ }</p>
     */
    @Parameter( property = "maven.tomcat.contextFile" )
    protected File contextFile;

    /**
     * The default context file to check for if contextFile not configured.
     * If no contextFile configured and the below default not present, no
     * contextFile will be sent to Tomcat, resulting in the latter's default
     * context configuration being used instead.
     */
    @Parameter( defaultValue = "${project.build.directory}/${project.build.finalName}/META-INF/context.xml",
                readonly = true )
    private File defaultContextFile;

    /**
     * The protocol to run the Tomcat server on.
     * By default it's HTTP/1.1.
     * See possible values <a href="http://tomcat.apache.org/tomcat-7.0-doc/config/http.html">HTTP Connector</a>
     * protocol attribute
     *
     * @since 2.0
     */
    @Parameter( property = "maven.tomcat.protocol", defaultValue = "HTTP/1.1" )
    private String protocol;

    /**
     * The path of the Tomcat users XML file.
     *
     * @since 2.0
     */
    @Parameter( property = "maven.tomcat.tomcatUsers.file" )
    private File tomcatUsers;

    /**
     * The path of the Tomcat logging configuration.
     *
     * @since 2.0
     */
    @Parameter( property = "maven.tomcat.tomcatLogging.file" )
    private File tomcatLoggingFile;

    /**
     * Skip execution
     *
     * @since 2.0
     */
    @Parameter( property = "maven.tomcat.skip", defaultValue = "false" )
    protected boolean skip;

    /**
     * Collection of webapp artifacts to be deployed. Elements are &lt;webapp&gt; and contain
     * usual GAVC plus contextPath and/or contextFile elements.<p>
     * @see {@link Webapp}
     * @since 2.0
     */
    @Parameter
    private List<Webapp> webapps;

    /**
     * The static context
     *
     * @since 2.0
     */
    @Parameter( property = "maven.tomcat.staticContextPath", defaultValue = "/" )
    private String staticContextPath;

    /**
     * The static context docroot base fully qualified path
     * if <code>null</code> static context won't be added
     *
     * @since 2.0
     */
    @Parameter( property = "maven.tomcat.staticContextDocbase" )
    private String staticContextDocbase;

    /**
     * Class loader class to set.
     *
     * @since 2.0
     */
    @Parameter
    protected String classLoaderClass;

    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    protected MavenSession session;

    /**
     * Will dump port in a properties file (see ports for property names).
     * If empty no file generated
     */
    @Parameter( property = "maven.tomcat.propertiesPortFilePath" )
    protected String propertiesPortFilePath;

    /**
     * configure host name
     *
     * @since 2.0
     */
    @Parameter( property = "maven.tomcat.hostName", defaultValue = "localhost" )
    protected String hostName;

    /**
     * configure aliases
     * see <a href="http://tomcat.apache.org/tomcat-7.0-doc/config/host.html#Host_Name_Aliases">Host Name aliases</a>
     *
     * @since 2.0
     */
    @Parameter
    protected String[] aliases;

    /**
     * enable client authentication for https (if configured)
     * see <a href="http://tomcat.apache.org/tomcat-7.0-doc/config/http.html#SSL_Support_-_BIO_and_NIO">http://tomcat.apache.org/tomcat-7.0-doc/config/http.html#SSL_Support_-_BIO_and_NIO</a>
     *
     * @since 2.1
     */
    @Parameter( property = "maven.tomcat.https.clientAuth", defaultValue = "false" )
    protected String clientAuth = "false";

    @Component( role = MavenFileFilter.class, hint = "default" )
    protected MavenFileFilter mavenFileFilter;


    /**
     * In case a module in your reactors has some web-fragments they will be read.
     * If you don't need that for performance reasons, you can deactivate it.
     *
     * @since 2.2
     */
    @Parameter( property = "maven.tomcat.jarScan.allDirectories", defaultValue = "true" )
    protected boolean jarScanAllDirectories = true;

    /**
     *
     * @since 2.2
     */
    @Parameter( property = "maven.tomcat.useBodyEncodingForURI", defaultValue = "false" )
    protected boolean useBodyEncodingForURI;

    /**
     *
     * @since 2.2
     */
    @Parameter
    protected String trustManagerClassName;

    /**
     *
     * @since 2.2
     */
    @Parameter
    protected String trustMaxCertLength;

    /**
     *
     * @since 2.2
     */
    @Parameter
    protected String truststoreAlgorithm;

    /**
     *
     * @since 2.2
     */
    @Parameter
    protected String truststoreFile;

    /**
     *
     * @since 2.2
     */
    @Parameter
    protected String  truststorePass;

    /**
     *
     * @since 2.2
     */
    @Parameter
    protected String truststoreProvider;

    /**
     *
     * @since 2.2
     */
    @Parameter
    protected String truststoreType;

    // ----------------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------------

    /**
     * @since 1.0
     */
    private ClassRealm tomcatRealm;

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
        // ensure project is a web application
        if ( !isWar() && !addContextWarDependencies && getAdditionalWebapps().isEmpty() )
        {
            getLog().info( messagesProvider.getMessage( "AbstractRunMojo.nonWar" ) );
            return;
        }
        ClassLoader originalClassLoader = null;
        if ( useSeparateTomcatClassLoader )
        {
            originalClassLoader = Thread.currentThread().getContextClassLoader();
        }
        try
        {
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
        catch ( ServletException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
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
    protected Context createContext( Tomcat container )
        throws IOException, MojoExecutionException, ServletException
    {
        String contextPath = getPath();

        String baseDir = getDocBase().getAbsolutePath();

        File overriddenContextFile = getContextFile();

        StandardContext standardContext = null;

        if ( overriddenContextFile != null && overriddenContextFile.exists() )
        {
            standardContext = parseContextFile( overriddenContextFile );
        }
        else if ( defaultContextFile.exists() )
        {
            standardContext = parseContextFile( defaultContextFile );
        }

        if ( standardContext != null )
        {
            if ( standardContext.getPath() != null )
            {
                contextPath = standardContext.getPath();
            }
            if ( standardContext.getDocBase() != null )
            {
                baseDir = standardContext.getDocBase();
            }
        }

        contextPath = "/".equals( contextPath ) ? "" : contextPath;

        getLog().info( "create webapp with contextPath: " + contextPath );

        Context context = container.addWebapp( contextPath, baseDir );

        context.setResources(
            new MyDirContext( new File( project.getBuild().getOutputDirectory() ).getAbsolutePath() ) );

        if ( useSeparateTomcatClassLoader )
        {
            context.setParentClassLoader( getTomcatClassLoader() );
        }

        final WebappLoader loader = createWebappLoader();

        context.setLoader( loader );

        if ( overriddenContextFile != null )
        {
            // here, send file to Tomcat for it to complain if missing
            context.setConfigFile( overriddenContextFile.toURI().toURL() );
        }
        else if ( defaultContextFile.exists() )
        {
            // here, only sending default file if it indeed exists
            // otherwise Tomcat will create a default context
            context.setConfigFile( defaultContextFile.toURI().toURL() );
        }

        if ( classLoaderClass != null )
        {
            loader.setLoaderClass( classLoaderClass );
        }

        // https://issues.apache.org/jira/browse/MTOMCAT-239
        // get the jar scanner to configure scanning directories as we can run a jar or a reactor project with a jar so
        // the entries is a directory (target/classes)
        JarScanner jarScanner = context.getJarScanner();

        // normally this one only but just in case ...
        if ( jarScanner instanceof StandardJarScanner )
        {
            ( (StandardJarScanner) jarScanner ).setScanAllDirectories( jarScanAllDirectories );
        }

        return context;

    }

    protected StandardContext parseContextFile( File file )
        throws MojoExecutionException
    {
        try
        {
            StandardContext standardContext = new StandardContext();
            XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader( new FileInputStream( file ) );

            int tag = reader.next();

            while ( true )
            {
                if ( tag == XMLStreamConstants.START_ELEMENT && StringUtils.equals( "Context", reader.getLocalName() ) )
                {
                    String path = reader.getAttributeValue( null, "path" );
                    if ( StringUtils.isNotBlank( path ) )
                    {
                        standardContext.setPath( path );
                    }

                    String docBase = reader.getAttributeValue( null, "docBase" );
                    if ( StringUtils.isNotBlank( docBase ) )
                    {
                        standardContext.setDocBase( docBase );
                    }
                }
                if ( !reader.hasNext() )
                {
                    break;
                }
                tag = reader.next();
            }

            return standardContext;
        }
        catch ( XMLStreamException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( FileNotFoundException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }


    private static class MyDirContext
        extends FileDirContext
    {
        String buildOutputDirectory;

        MyDirContext( String buildOutputDirectory )
        {
            this.buildOutputDirectory = buildOutputDirectory;
        }

        @Override
        protected List<NamingEntry> doListBindings( String name )
            throws NamingException
        {
            if ( "/WEB-INF/classes".equals( name ) )
            {
                if ( !new File( buildOutputDirectory ).exists() )
                {
                    return Collections.emptyList();
                }
                FileDirContext fileDirContext = new FileDirContext();
                fileDirContext.setDocBase( buildOutputDirectory );
                NamingEntry namingEntry = new NamingEntry( "/WEB-INF/classes", fileDirContext, -1 );
                return Collections.singletonList( namingEntry );
            }

            return super.doListBindings( name );
        }
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
    protected abstract File getDocBase()
        throws IOException;

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
     * @throws java.net.MalformedURLException if the running webapp URL is invalid
     */
    private URL getWebappUrl()
        throws MalformedURLException
    {
        return new URL( "http", "localhost", port, getPath() );
    }

    /**
     * FIXME not sure we need all of those files with tomcat7
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

            if ( tomcatLoggingFile != null )
            {
                FileUtils.copyFile( tomcatLoggingFile, new File( confDir, "logging.properties" ) );
            }
            else
            {
                copyFile( "/conf/logging.properties", new File( confDir, "logging.properties" ) );
            }

            copyFile( "/conf/tomcat-users.xml", new File( confDir, "tomcat-users.xml" ) );
            if ( tomcatWebXml != null )
            {
                if ( !tomcatWebXml.exists() )
                {
                    throw new MojoExecutionException( " tomcatWebXml " + tomcatWebXml.getPath() + " not exists" );
                }
                //MTOMCAT-42  here it's a real file resources not a one coming with the mojo
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
        throws IOException, LifecycleException, MojoExecutionException, ServletException
    {
        String previousCatalinaBase = System.getProperty( "catalina.base" );

        try
        {

            // Set the system properties
            setupSystemProperties();

            System.setProperty( "catalina.base", configurationDir.getAbsolutePath() );

            if ( serverXml != null )
            {
                if ( !serverXml.exists() )
                {
                    throw new MojoExecutionException( serverXml.getPath() + " not exists" );
                }

                Catalina container = new Catalina();

                if ( useSeparateTomcatClassLoader )
                {
                    Thread.currentThread().setContextClassLoader( getTomcatClassLoader() );
                    container.setParentClassLoader( getTomcatClassLoader() );
                }

                container.setUseNaming( this.useNaming );
                container.setConfig( serverXml.getAbsolutePath() );
                container.start();
                EmbeddedRegistry.getInstance().register( container );
            }
            else
            {

                System.setProperty( "java.util.logging.manager", "org.apache.juli.ClassLoaderLogManager" );
                System.setProperty( "java.util.logging.config.file",
                                    new File( configurationDir, "conf/logging.properties" ).toString() );

                // Trigger loading of catalina.properties
                CatalinaProperties.getProperty( "foo" );

                Tomcat embeddedTomcat = new ExtendedTomcat( configurationDir );

                embeddedTomcat.setBaseDir( configurationDir.getAbsolutePath() );
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

                embeddedTomcat.setDefaultRealm( memoryRealm );

                Context ctx = createContext( embeddedTomcat );

                if ( useNaming )
                {
                    embeddedTomcat.enableNaming();
                }

                embeddedTomcat.getHost().setAppBase( new File( configurationDir, "webapps" ).getAbsolutePath() );

                if ( hostName != null )
                {
                    embeddedTomcat.getHost().setName( hostName );
                }
                if ( aliases != null )
                {
                    for ( String alias : aliases )
                    {
                        embeddedTomcat.getHost().addAlias( alias );
                    }

                }
                createStaticContext( embeddedTomcat, ctx, embeddedTomcat.getHost() );

                Connector connector = new Connector( protocol );
                connector.setPort( port );

                if ( httpsPort > 0 )
                {
                    connector.setRedirectPort( httpsPort );
                }

                if ( address != null)
                {
                    connector.setAttribute( "address", address );
                }

                connector.setURIEncoding( uriEncoding );

                connector.setUseBodyEncodingForURI( this.useBodyEncodingForURI );

                embeddedTomcat.getService().addConnector( connector );

                embeddedTomcat.setConnector( connector );

                AccessLogValve alv = new AccessLogValve();
                alv.setDirectory( new File( configurationDir, "logs" ).getAbsolutePath() );
                alv.setPattern( "%h %l %u %t \"%r\" %s %b %I %D" );
                embeddedTomcat.getHost().getPipeline().addValve( alv );

                // create https connector
                Connector httpsConnector = null;
                if ( httpsPort > 0 )
                {
                    httpsConnector = new Connector( protocol );
                    httpsConnector.setPort( httpsPort );
                    httpsConnector.setSecure( true );
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

                    if( trustManagerClassName != null )
                    {
                        httpsConnector.setAttribute( "trustManagerClassName", trustManagerClassName );
                    }

                    if( trustMaxCertLength != null )
                    {
                        httpsConnector.setAttribute( "trustMaxCertLength", trustMaxCertLength );
                    }

                    if( truststoreAlgorithm != null )
                    {
                        httpsConnector.setAttribute( "truststoreAlgorithm", truststoreAlgorithm );
                    }

                    if( truststoreFile != null )
                    {
                        httpsConnector.setAttribute( "truststoreFile", truststoreFile );
                    }

                    if( truststorePass != null )
                    {
                        httpsConnector.setAttribute( "truststorePass", truststorePass );
                    }

                    if( truststoreProvider != null )
                    {
                        httpsConnector.setAttribute( "truststoreProvider", truststoreProvider );
                    }


                    if( truststoreType != null )
                    {
                        httpsConnector.setAttribute( "truststoreType", truststoreType );
                    }

                    httpsConnector.setAttribute( "clientAuth", clientAuth );

                    httpsConnector.setUseBodyEncodingForURI( this.useBodyEncodingForURI );

                    if ( address != null)
                    {
                        httpsConnector.setAttribute( "address", address );
                    }

                    embeddedTomcat.getEngine().getService().addConnector( httpsConnector );

                }

                // create ajp connector
                Connector ajpConnector = null;
                if ( ajpPort > 0 )
                {
                    ajpConnector = new Connector( ajpProtocol );
                    ajpConnector.setPort( ajpPort );
                    ajpConnector.setURIEncoding( uriEncoding );
                    ajpConnector.setUseBodyEncodingForURI( this.useBodyEncodingForURI );
                    if ( address != null)
                    {
                        ajpConnector.setAttribute( "address", address );
                    }
                    embeddedTomcat.getEngine().getService().addConnector( ajpConnector );
                }

                if ( addContextWarDependencies || !getAdditionalWebapps().isEmpty() )
                {
                    createDependencyContexts( embeddedTomcat );
                }

                if ( useSeparateTomcatClassLoader )
                {
                    Thread.currentThread().setContextClassLoader( getTomcatClassLoader() );
                    embeddedTomcat.getEngine().setParentClassLoader( getTomcatClassLoader() );
                }

                embeddedTomcat.start();

                Properties portProperties = new Properties();

                portProperties.put( "tomcat.maven.http.port", Integer.toString( connector.getLocalPort() ) );

                session.getExecutionProperties().put( "tomcat.maven.http.port",
                                                      Integer.toString( connector.getLocalPort() ) );
                System.setProperty( "tomcat.maven.http.port", Integer.toString( connector.getLocalPort() ) );

                if ( httpsConnector != null )
                {
                    session.getExecutionProperties().put( "tomcat.maven.https.port",
                                                          Integer.toString( httpsConnector.getLocalPort() ) );
                    portProperties.put( "tomcat.maven.https.port", Integer.toString( httpsConnector.getLocalPort() ) );
                    System.setProperty( "tomcat.maven.https.port", Integer.toString( httpsConnector.getLocalPort() ) );
                }

                if ( ajpConnector != null )
                {
                    session.getExecutionProperties().put( "tomcat.maven.ajp.port",
                                                          Integer.toString( ajpConnector.getLocalPort() ) );
                    portProperties.put( "tomcat.maven.ajp.port", Integer.toString( ajpConnector.getLocalPort() ) );
                    System.setProperty( "tomcat.maven.ajp.port", Integer.toString( ajpConnector.getLocalPort() ) );
                }
                if ( propertiesPortFilePath != null )
                {
                    File propertiesPortsFile = new File( propertiesPortFilePath );
                    if ( propertiesPortsFile.exists() )
                    {
                        propertiesPortsFile.delete();
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream( propertiesPortsFile );
                    try
                    {
                        portProperties.store( fileOutputStream, "Apache Tomcat Maven plugin port used" );
                    }
                    finally
                    {
                        IOUtils.closeQuietly( fileOutputStream );
                    }
                }

                EmbeddedRegistry.getInstance().register( embeddedTomcat );

            }


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

            for ( @SuppressWarnings( "rawtypes" ) Iterator i = pluginArtifacts.iterator(); i.hasNext(); )
            {
                Artifact pluginArtifact = (Artifact) i.next();
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

    @SuppressWarnings( "unchecked" )
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
     * @return dependency tomcat contexts of warfiles in scope "tomcat"
     */
    private Collection<Context> createDependencyContexts( Tomcat container )
        throws MojoExecutionException, MalformedURLException, ServletException, IOException
    {
        getLog().info( "Deploying dependency wars" );
        // Let's add other modules
        List<Context> contexts = new ArrayList<Context>();

        ScopeArtifactFilter filter = new ScopeArtifactFilter( "tomcat" );
        @SuppressWarnings( "unchecked" ) Set<Artifact> artifacts = project.getArtifacts();
        for ( Artifact artifact : artifacts )
        {

            // Artifact is not yet registered and it has neither test, nor a
            // provided scope, not is it optional
            if ( "war".equals( artifact.getType() ) && !artifact.isOptional() && filter.include( artifact ) )
            {
                addContextFromArtifact( container, contexts, artifact, "/" + artifact.getArtifactId(), null, false );
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
                                    additionalWebapp.getContextFile(), additionalWebapp.isAsWebapp() );
        }
        return contexts;
    }


    private void addContextFromArtifact( Tomcat container, List<Context> contexts, Artifact artifact,
                                         String contextPath, File contextXml, boolean asWebApp )
        throws MojoExecutionException, ServletException, IOException
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
        // TODO make that configurable ?
        //WebappLoader webappLoader = new WebappLoader( Thread.currentThread().getContextClassLoader() );
        WebappLoader webappLoader = createWebappLoader();
        Context context = null;
        if ( asWebApp )
        {
            context = container.addWebapp( contextPath, artifactWarDir.getAbsolutePath() );
        }
        else
        {
            context = container.addContext( contextPath, artifactWarDir.getAbsolutePath() );
        }
        context.setLoader( webappLoader );

        File contextFile = contextXml != null ? contextXml : getContextFile();
        if ( contextFile != null )
        {
            context.setConfigFile( contextFile.toURI().toURL() );
        }

        contexts.add( context );
//        container.getHost().addChild(context);
    }

    private void createStaticContext( final Tomcat container, Context context, Host host )
    {
        if ( staticContextDocbase != null )
        {
            Context staticContext = container.addContext( staticContextPath, staticContextDocbase );
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
            artifact =
                factory.createDependencyArtifact( additionalWebapp.getGroupId(), additionalWebapp.getArtifactId(), vr,
                                                  additionalWebapp.getType(), null, Artifact.SCOPE_COMPILE );
        }
        else
        {
            artifact =
                factory.createDependencyArtifact( additionalWebapp.getGroupId(), additionalWebapp.getArtifactId(), vr,
                                                  additionalWebapp.getType(), additionalWebapp.getClassifier(),
                                                  Artifact.SCOPE_COMPILE );
        }

        try
        {
            resolver.resolve( artifact, project.getRemoteArtifactRepositories(), this.local );
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
