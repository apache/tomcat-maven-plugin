package org.apache.tomcat.maven.runner;
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
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Catalina;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.juli.ClassLoaderLogManager;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.LogManager;

/**
 * FIXME add junit for that but when https://issues.apache.org/bugzilla/show_bug.cgi?id=52028 fixed
 * Main class used to run the standalone wars in a Apache Tomcat instance.
 *
 * @author Olivier Lamy
 * @since 2.0
 */
public class Tomcat8Runner
{
    // true/false to use the server.xml located in the jar /conf/server.xml
    public static final String USE_SERVER_XML_KEY = "useServerXml";

    // contains war name wars=foo.war,bar.war
    public static final String WARS_KEY = "wars";

    public static final String ARCHIVE_GENERATION_TIMESTAMP_KEY = "generationTimestamp";

    public static final String ENABLE_NAMING_KEY = "enableNaming";

    public static final String ACCESS_LOG_VALVE_FORMAT_KEY = "accessLogValveFormat";

    public static final String CODE_SOURCE_CONTEXT_PATH = "codeSourceContextPath";

    /**
     * key of the property which contains http protocol : HTTP/1.1 or org.apache.coyote.http11.Http11NioProtocol
     */
    public static final String HTTP_PROTOCOL_KEY = "connectorhttpProtocol";

    /**
     * key for default http port defined in the plugin
     */
    public static final String HTTP_PORT_KEY = "httpPort";


    public int httpPort;

    public int httpsPort;

    public int maxPostSize = 2097152;

    public int ajpPort;

    public String serverXmlPath;

    public Properties runtimeProperties;

    public boolean resetExtract;

    public boolean debug = false;

    public String clientAuth = "false";

    public String keyAlias = null;

    public String httpProtocol;

    public String extractDirectory = ".extract";

    public File extractDirectoryFile;

    public String codeSourceContextPath = null;

    public File codeSourceWar = null;

    public String loggerName;

    Catalina container;

    Tomcat tomcat;

    String uriEncoding = "ISO-8859-1";

    /**
     * key = context of the webapp, value = war path on file system
     */
    Map<String, String> webappWarPerContext = new HashMap<String, String>();

    public Tomcat8Runner()
    {
        // no op
    }

    public void run()
        throws Exception
    {

        PasswordUtil.deobfuscateSystemProps();

        if ( loggerName != null && loggerName.length() > 0 )
        {
            installLogger( loggerName );
        }

        this.extractDirectoryFile = new File( this.extractDirectory );

        debugMessage( "use extractDirectory:" + extractDirectoryFile.getPath() );

        boolean archiveTimestampChanged = false;

        // compare timestamp stored during previous run if exists
        File timestampFile = new File( extractDirectoryFile, ".tomcat_executable_archive.timestamp" );

        Properties timestampProps = loadProperties( timestampFile );

        if ( timestampFile.exists() )
        {
            String timestampValue = timestampProps.getProperty( Tomcat8Runner.ARCHIVE_GENERATION_TIMESTAMP_KEY );
            if ( timestampValue != null )
            {
                long timestamp = Long.parseLong( timestampValue );
                archiveTimestampChanged =
                    Long.parseLong( runtimeProperties.getProperty( Tomcat8Runner.ARCHIVE_GENERATION_TIMESTAMP_KEY ) )
                        > timestamp;

                debugMessage( "read timestamp from file " + timestampValue + ", archiveTimestampChanged: "
                                  + archiveTimestampChanged );
            }

        }

        codeSourceContextPath = runtimeProperties.getProperty( CODE_SOURCE_CONTEXT_PATH );
        if ( codeSourceContextPath != null && !codeSourceContextPath.isEmpty() )
        {
            codeSourceWar = AccessController.doPrivileged( new PrivilegedAction<File>()
            {
                public File run()
                {
                    try
                    {
                        File src =
                            new File( Tomcat8Runner.class.getProtectionDomain().getCodeSource().getLocation().toURI() );
                        if ( src.getName().endsWith( ".war" ) )
                        {
                            return src;
                        }
                        else
                        {
                            debugMessage( "ERROR: Code source is not a war file, ignoring." );
                        }
                    }
                    catch ( URISyntaxException e )
                    {
                        debugMessage( "ERROR: Could not find code source. " + e.getMessage() );

                    }
                    return null;
                }
            } );
        }

        // do we have to extract content
        {
            if ( !extractDirectoryFile.exists() || resetExtract || archiveTimestampChanged )
            {
                extract();
                //if archiveTimestampChanged or timestamp file not exists store the last timestamp from the archive
                if ( archiveTimestampChanged || !timestampFile.exists() )
                {
                    timestampProps.put( Tomcat8Runner.ARCHIVE_GENERATION_TIMESTAMP_KEY, runtimeProperties.getProperty(
                        Tomcat8Runner.ARCHIVE_GENERATION_TIMESTAMP_KEY ) );
                    saveProperties( timestampProps, timestampFile );
                }
            }
            else
            {
                String wars = runtimeProperties.getProperty( WARS_KEY );
                populateWebAppWarPerContext( wars );
            }
        }

        // create tomcat various paths
        new File( extractDirectory, "conf" ).mkdirs();
        new File( extractDirectory, "logs" ).mkdirs();
        new File( extractDirectory, "webapps" ).mkdirs();
        new File( extractDirectory, "work" ).mkdirs();
        File tmpDir = new File( extractDirectory, "temp" );
        tmpDir.mkdirs();

        System.setProperty( "java.io.tmpdir", tmpDir.getAbsolutePath() );

        System.setProperty( "catalina.base", extractDirectoryFile.getAbsolutePath() );
        System.setProperty( "catalina.home", extractDirectoryFile.getAbsolutePath() );

        // start with a server.xml
        if ( serverXmlPath != null || useServerXml() )
        {
            container = new Catalina();
            container.setUseNaming( this.enableNaming() );
            if ( serverXmlPath != null && new File( serverXmlPath ).exists() )
            {
                container.setConfigFile( serverXmlPath );
            }
            else
            {
                container.setConfigFile( new File( extractDirectory, "conf/server.xml" ).getAbsolutePath() );
            }
            container.start();
        }
        else
        {
            tomcat = new Tomcat()
            {
                public Context addWebapp( Host host, String url, String name, String path )
                {

                    Context ctx = new StandardContext();
                    ctx.setName( name );
                    ctx.setPath( url );
                    ctx.setDocBase( path );

                    ContextConfig ctxCfg = new ContextConfig();
                    ctx.addLifecycleListener( ctxCfg );

                    ctxCfg.setDefaultWebXml( new File( extractDirectory, "conf/web.xml" ).getAbsolutePath() );

                    if ( host == null )
                    {
                        getHost().addChild( ctx );
                    }
                    else
                    {
                        host.addChild( ctx );
                    }

                    return ctx;
                }
            };

            if ( this.enableNaming() )
            {
                System.setProperty( "catalina.useNaming", "true" );
                tomcat.enableNaming();
            }

            tomcat.getHost().setAppBase( new File( extractDirectory, "webapps" ).getAbsolutePath() );

            String connectorHttpProtocol = runtimeProperties.getProperty( HTTP_PROTOCOL_KEY );

            if ( httpProtocol != null && httpProtocol.trim().length() > 0 )
            {
                connectorHttpProtocol = httpProtocol;
            }

            debugMessage( "use connectorHttpProtocol:" + connectorHttpProtocol );

            if ( httpPort > 0 )
            {
                Connector connector = new Connector( connectorHttpProtocol );
                connector.setPort( httpPort );
                connector.setMaxPostSize( maxPostSize );

                if ( httpsPort > 0 )
                {
                    connector.setRedirectPort( httpsPort );
                }
                connector.setURIEncoding( uriEncoding );

                tomcat.getService().addConnector( connector );

                tomcat.setConnector( connector );
            }

            // add a default acces log valve
            AccessLogValve alv = new AccessLogValve();
            alv.setDirectory( new File( extractDirectory, "logs" ).getAbsolutePath() );
            alv.setPattern( runtimeProperties.getProperty( Tomcat8Runner.ACCESS_LOG_VALVE_FORMAT_KEY ) );
            tomcat.getHost().getPipeline().addValve( alv );

            // create https connector
            if ( httpsPort > 0 )
            {
                Connector httpsConnector = new Connector( connectorHttpProtocol );
                httpsConnector.setPort( httpsPort );
                httpsConnector.setMaxPostSize( maxPostSize );
                httpsConnector.setSecure( true );
                httpsConnector.setProperty( "SSLEnabled", "true" );
                httpsConnector.setProperty( "sslProtocol", "TLS" );
                httpsConnector.setURIEncoding( uriEncoding );

                String keystoreFile = System.getProperty( "javax.net.ssl.keyStore" );
                String keystorePass = System.getProperty( "javax.net.ssl.keyStorePassword" );
                String keystoreType = System.getProperty( "javax.net.ssl.keyStoreType", "jks" );

                if ( keystoreFile != null )
                {
                    httpsConnector.setAttribute( "keystoreFile", keystoreFile );
                }
                if ( keystorePass != null )
                {
                    httpsConnector.setAttribute( "keystorePass", keystorePass );
                }
                httpsConnector.setAttribute( "keystoreType", keystoreType );

                String truststoreFile = System.getProperty( "javax.net.ssl.trustStore" );
                String truststorePass = System.getProperty( "javax.net.ssl.trustStorePassword" );
                String truststoreType = System.getProperty( "javax.net.ssl.trustStoreType", "jks" );
                if ( truststoreFile != null )
                {
                    httpsConnector.setAttribute( "truststoreFile", truststoreFile );
                }
                if ( truststorePass != null )
                {
                    httpsConnector.setAttribute( "truststorePass", truststorePass );
                }
                httpsConnector.setAttribute( "truststoreType", truststoreType );

                httpsConnector.setAttribute( "clientAuth", clientAuth );
                httpsConnector.setAttribute( "keyAlias", keyAlias );

                tomcat.getService().addConnector( httpsConnector );

                if ( httpPort <= 0 )
                {
                    tomcat.setConnector( httpsConnector );
                }
            }

            // create ajp connector
            if ( ajpPort > 0 )
            {
                Connector ajpConnector = new Connector( "org.apache.coyote.ajp.AjpProtocol" );
                ajpConnector.setPort( ajpPort );
                ajpConnector.setURIEncoding( uriEncoding );
                tomcat.getService().addConnector( ajpConnector );
            }

            // add webapps
            for ( Map.Entry<String, String> entry : this.webappWarPerContext.entrySet() )
            {
                String baseDir = null;
                Context context = null;
                if ( entry.getKey().equals( "/" ) )
                {
                    baseDir = new File( extractDirectory, "webapps/ROOT.war" ).getAbsolutePath();
                    context = tomcat.addWebapp( "", baseDir );
                }
                else
                {
                    baseDir = new File( extractDirectory, "webapps/" + entry.getValue() ).getAbsolutePath();
                    context = tomcat.addWebapp( entry.getKey(), baseDir );
                }

                URL contextFileUrl = getContextXml( baseDir );
                if ( contextFileUrl != null )
                {
                    context.setConfigFile( contextFileUrl );
                }
            }

            if ( codeSourceWar != null )
            {
                String baseDir = new File( extractDirectory, "webapps/" + codeSourceWar.getName() ).getAbsolutePath();
                Context context = tomcat.addWebapp( codeSourceContextPath, baseDir );
                URL contextFileUrl = getContextXml( baseDir );
                if ( contextFileUrl != null )
                {
                    context.setConfigFile( contextFileUrl );
                }
            }

            tomcat.start();

            Runtime.getRuntime().addShutdownHook( new TomcatShutdownHook() );

        }

        waitIndefinitely();

    }

    protected class TomcatShutdownHook
        extends Thread
    {

        protected TomcatShutdownHook()
        {
            // no op
        }

        @Override
        public void run()
        {
            try
            {
                Tomcat8Runner.this.stop();
            }
            catch ( Throwable ex )
            {
                ExceptionUtils.handleThrowable( ex );
                System.out.println( "fail to properly shutdown Tomcat:" + ex.getMessage() );
            }
            finally
            {
                // If JULI is used, shut JULI down *after* the server shuts down
                // so log messages aren't lost
                LogManager logManager = LogManager.getLogManager();
                if ( logManager instanceof ClassLoaderLogManager )
                {
                    ( (ClassLoaderLogManager) logManager ).shutdown();
                }
            }
        }
    }

    private URL getContextXml( String warPath )
        throws IOException
    {
        InputStream inputStream = null;
        try
        {
            String urlStr = "jar:file:" + warPath + "!/META-INF/context.xml";
            debugMessage( "search context.xml in url:'" + urlStr + "'" );
            URL url = new URL( urlStr );
            inputStream = url.openConnection().getInputStream();
            if ( inputStream != null )
            {
                return url;
            }
        }
        catch ( FileNotFoundException e )
        {
            return null;
        }
        finally
        {
            closeQuietly( inputStream );
        }
        return null;
    }

    private static void closeQuietly( InputStream inputStream )
    {
        if ( inputStream == null )
        {
            return;
        }
        try
        {
            inputStream.close();
        }
        catch ( IOException e )
        {
            // ignore exception here
        }
    }

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
                throw new Error( "InterruptedException on wait Indefinitely lock:" + exception.getMessage(),
                                 exception );
            }
        }
    }

    public void stop()
        throws Exception
    {
        if ( container != null )
        {
            container.stop();
        }
        if ( tomcat != null )
        {
            tomcat.stop();
        }
    }

    protected void extract()
        throws Exception
    {

        if ( extractDirectoryFile.exists() )
        {
            debugMessage( "delete extractDirectory:" + extractDirectoryFile.getAbsolutePath() );
            FileUtils.deleteDirectory( extractDirectoryFile );
        }

        if ( !this.extractDirectoryFile.exists() )
        {
            boolean created = this.extractDirectoryFile.mkdirs();
            if ( !created )
            {
                throw new Exception( "FATAL: impossible to create directory:" + this.extractDirectoryFile.getPath() );
            }
        }

        // ensure webapp dir is here
        boolean created = new File( extractDirectory, "webapps" ).mkdirs();
        if ( !created )
        {
            throw new Exception(
                "FATAL: impossible to create directory:" + this.extractDirectoryFile.getPath() + "/webapps" );

        }

        String wars = runtimeProperties.getProperty( WARS_KEY );
        populateWebAppWarPerContext( wars );

        for ( Map.Entry<String, String> entry : webappWarPerContext.entrySet() )
        {
            debugMessage( "webappWarPerContext entry key/value: " + entry.getKey() + "/" + entry.getValue() );
            InputStream inputStream = null;
            try
            {
                File expandFile = null;
                inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream( entry.getValue() );
                if ( !useServerXml() )
                {
                    if ( entry.getKey().equals( "/" ) )
                    {
                        expandFile = new File( extractDirectory, "webapps/ROOT.war" );
                    }
                    else
                    {
                        expandFile = new File( extractDirectory, "webapps/" + entry.getValue() );
                    }
                }
                else
                {
                    expandFile = new File( extractDirectory, "webapps/" + entry.getValue() );
                }

                debugMessage( "expand to file:" + expandFile.getPath() );

                // MTOMCAT-211 ensure parent directories created
                File parentFile = expandFile.getParentFile();
                if ( !parentFile.mkdirs() && !parentFile.isDirectory() )
                {
                    throw new Exception( "FATAL: impossible to create directories:" + parentFile );
                }

                expand( inputStream, expandFile );

            }
            finally
            {
                if ( inputStream != null )
                {
                    inputStream.close();
                }
            }
        }

        //Copy code source to webapps folder
        if ( codeSourceWar != null )
        {
            FileInputStream inputStream = null;
            try
            {
                File expandFile = new File( extractDirectory, "webapps/" + codeSourceContextPath + ".war" );
                inputStream = new FileInputStream( codeSourceWar );
                debugMessage( "move code source to file:" + expandFile.getPath() );
                expand( inputStream, expandFile );
            }
            finally
            {
                if ( inputStream != null )
                {
                    inputStream.close();
                }
            }
        }

        // expand tomcat configuration files if there
        expandConfigurationFile( "catalina.properties", extractDirectoryFile );
        expandConfigurationFile( "logging.properties", extractDirectoryFile );
        expandConfigurationFile( "tomcat-users.xml", extractDirectoryFile );
        expandConfigurationFile( "catalina.policy", extractDirectoryFile );
        expandConfigurationFile( "context.xml", extractDirectoryFile );
        expandConfigurationFile( "server.xml", extractDirectoryFile );
        expandConfigurationFile( "web.xml", extractDirectoryFile );

    }

    private static void expandConfigurationFile( String fileName, File extractDirectory )
        throws Exception
    {
        InputStream inputStream = null;
        try
        {
            inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream( "conf/" + fileName );
            if ( inputStream != null )
            {
                File confDirectory = new File( extractDirectory, "conf" );
                if ( !confDirectory.exists() )
                {
                    confDirectory.mkdirs();
                }
                expand( inputStream, new File( confDirectory, fileName ) );
            }
        }
        finally
        {
            if ( inputStream != null )
            {
                inputStream.close();
            }
        }

    }

    /**
     * @param warsValue we can value in format: wars=foo.war|contextpath;bar.war  ( |contextpath is optionnal if empty use the war name)
     *                  so here we return war file name and populate webappWarPerContext
     */
    private void populateWebAppWarPerContext( String warsValue )
    {
        if ( warsValue == null )
        {
            return;
        }

        StringTokenizer st = new StringTokenizer( warsValue, ";" );
        while ( st.hasMoreTokens() )
        {
            String warValue = st.nextToken();
            debugMessage( "populateWebAppWarPerContext warValue:" + warValue );
            String warFileName = "";
            String contextValue = "";
            int separatorIndex = warValue.indexOf( "|" );
            if ( separatorIndex >= 0 )
            {
                warFileName = warValue.substring( 0, separatorIndex );
                contextValue = warValue.substring( separatorIndex + 1, warValue.length() );

            }
            else
            {
                warFileName = contextValue;
            }
            debugMessage( "populateWebAppWarPerContext contextValue/warFileName:" + contextValue + "/" + warFileName );
            this.webappWarPerContext.put( contextValue, warFileName );
        }
    }


    /**
     * Expand the specified input stream into the specified file.
     *
     * @param input InputStream to be copied
     * @param file  The file to be created
     * @throws java.io.IOException if an input/output error occurs
     */
    private static void expand( InputStream input, File file )
        throws IOException
    {
        BufferedOutputStream output = null;
        try
        {
            output = new BufferedOutputStream( new FileOutputStream( file ) );
            byte buffer[] = new byte[2048];
            while ( true )
            {
                int n = input.read( buffer );
                if ( n <= 0 )
                {
                    break;
                }
                output.write( buffer, 0, n );
            }
        }
        finally
        {
            if ( output != null )
            {
                try
                {
                    output.close();
                }
                catch ( IOException e )
                {
                    // Ignore
                }
            }
        }
    }

    public boolean useServerXml()
    {
        return Boolean.parseBoolean( runtimeProperties.getProperty( USE_SERVER_XML_KEY, Boolean.FALSE.toString() ) );
    }


    public void debugMessage( String message )
    {
        if ( debug )
        {
            System.out.println( message );
        }
    }


    public boolean enableNaming()
    {
        return Boolean.parseBoolean( runtimeProperties.getProperty( ENABLE_NAMING_KEY, Boolean.FALSE.toString() ) );
    }

    private void installLogger( String loggerName )
        throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
        InvocationTargetException
    {
        if ( "slf4j".equals( loggerName ) )
        {

            try
            {
                // Check class is available

                //final Class<?> clazz = Class.forName( "org.slf4j.bridge.SLF4JBridgeHandler" );
                final Class<?> clazz =
                    Thread.currentThread().getContextClassLoader().loadClass( "org.slf4j.bridge.SLF4JBridgeHandler" );

                // Remove all JUL handlers
                java.util.logging.LogManager.getLogManager().reset();

                // Install slf4j bridge handler
                final Method method = clazz.getMethod( "install", null );
                method.invoke( null );
            }
            catch ( ClassNotFoundException e )
            {
                System.out.println( "WARNING: issue configuring slf4j jul bridge, skip it" );
            }
        }
        else
        {
            System.out.println( "WARNING: loggerName " + loggerName + " not supported, skip it" );
        }
    }

    private Properties loadProperties( File file )
        throws FileNotFoundException, IOException
    {
        Properties properties = new Properties();
        if ( file.exists() )
        {

            FileInputStream fileInputStream = new FileInputStream( file );
            try
            {
                properties.load( fileInputStream );
            }
            finally
            {
                fileInputStream.close();
            }

        }
        return properties;
    }

    private void saveProperties( Properties properties, File file )
        throws FileNotFoundException, IOException
    {
        FileOutputStream fileOutputStream = new FileOutputStream( file );
        try
        {
            properties.store( fileOutputStream, "Timestamp file for executable war/jar" );
        }
        finally
        {
            fileOutputStream.close();
        }
    }
}
