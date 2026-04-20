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
package org.apache.tomcat.maven.common.deployer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.maven.settings.Proxy;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyUtils;

/**
 * A Tomcat manager webapp invocation wrapper.
 *
 * @author Mark Hobson (markhobson@gmail.com)
 */
public class TomcatManager
{
    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------

    private static final int BUFFER_SIZE = 2048;

    private static final int SC_OK = 200;
    private static final int SC_CREATED = 201;
    private static final int SC_ACCEPTED = 202;
    private static final int SC_MOVED_PERMANENTLY = 301;
    private static final int SC_MOVED_TEMPORARILY = 302;
    private static final int SC_SEE_OTHER = 303;

    // ----------------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------------

    /**
     * The full URL of the Tomcat manager instance to use.
     */
    private URL url;

    /**
     * The username to use when authenticating with Tomcat manager.
     */
    private final String username;

    /**
     * The password to use when authenticating with Tomcat manager.
     */
    private final String password;

    /**
     * The URL encoding charset to use when communicating with Tomcat manager.
     */
    private final String charset;

    /**
     * The user agent name to use when communicating with Tomcat manager.
     */
    private String userAgent;

    /**
     * @since 2.0
     */
    private final boolean usernameProvided;

    /**
     * @since 2.2
     */
    private final boolean verbose;

    private Proxy proxySettings;

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    /**
     * Creates a Tomcat manager wrapper for the specified URL that uses a username of <code>admin</code>, an empty
     * password and ISO-8859-1 URL encoding.
     *
     * @param url the full URL of the Tomcat manager instance to use
     */
    public TomcatManager( URL url )
    {
        this( url, "admin" );
    }

    /**
     * Creates a Tomcat manager wrapper for the specified URL and username that uses an empty password and ISO-8859-1
     * URL encoding.
     *
     * @param url      the full URL of the Tomcat manager instance to use
     * @param username the username to use when authenticating with Tomcat manager
     */
    public TomcatManager( URL url, String username )
    {
        this( url, username, "" );
    }

    /**
     * Creates a Tomcat manager wrapper for the specified URL, username and password that uses ISO-8859-1 URL encoding.
     *
     * @param url      the full URL of the Tomcat manager instance to use
     * @param username the username to use when authenticating with Tomcat manager
     * @param password the password to use when authenticating with Tomcat manager
     */
    public TomcatManager( URL url, String username, String password )
    {
        this( url, username, password, "ISO-8859-1" );
    }


    /**
     * Creates a Tomcat manager wrapper for the specified URL, username, password and URL encoding.
     *
     * @param url      the full URL of the Tomcat manager instance to use
     * @param username the username to use when authenticating with Tomcat manager
     * @param password the password to use when authenticating with Tomcat manager
     * @param charset  the URL encoding charset to use when communicating with Tomcat manager
     */
    public TomcatManager( URL url, String username, String password, String charset )
    {
        this( url, username, password, charset, true );
    }

    /**
     * Creates a Tomcat manager wrapper for the specified URL, username, password and URL encoding.
     *
     * @param url      the full URL of the Tomcat manager instance to use
     * @param username the username to use when authenticating with Tomcat manager
     * @param password the password to use when authenticating with Tomcat manager
     * @param charset  the URL encoding charset to use when communicating with Tomcat manager
     * @param verbose  if the build is in verbose mode (quiet mode otherwise)
     * @since 2.2
     */
    public TomcatManager( URL url, String username, String password, String charset, boolean verbose )
    {
        this.url = url;
        this.username = username;
        this.password = password;
        this.charset = charset;
        this.verbose = verbose;
        this.usernameProvided = username != null && !username.isEmpty();
    }

    // ----------------------------------------------------------------------
    // Public Methods
    // ----------------------------------------------------------------------

    /**
     * Gets the full URL of the Tomcat manager instance.
     *
     * @return the full URL of the Tomcat manager instance
     */
    public URL getURL()
    {
        return url;
    }

    /**
     * Gets the username to use when authenticating with Tomcat manager.
     *
     * @return the username to use when authenticating with Tomcat manager
     */
    public String getUserName()
    {
        return username;
    }

    /**
     * Gets the password to use when authenticating with Tomcat manager.
     *
     * @return the password to use when authenticating with Tomcat manager
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * Gets the URL encoding charset to use when communicating with Tomcat manager.
     *
     * @return the URL encoding charset to use when communicating with Tomcat manager
     */
    public String getCharset()
    {
        return charset;
    }

    /**
     * Gets the user agent name to use when communicating with Tomcat manager.
     *
     * @return the user agent name to use when communicating with Tomcat manager
     */
    public String getUserAgent()
    {
        return userAgent;
    }

    /**
     * Sets the user agent name to use when communicating with Tomcat manager.
     *
     * @param userAgent the user agent name to use when communicating with Tomcat manager
     */
    public void setUserAgent( String userAgent )
    {
        this.userAgent = userAgent;
    }

    /**
     * Sets the proxy to use when communicating with Tomcat manager.
     *
     * @param proxy the proxy to use when communicating with Tomcat manager
     */
    public void setProxy( Proxy proxy )
    {
        this.proxySettings = proxy;
    }

    // ----------------------------------------------------------------------
    // Deploy Methods
    // ----------------------------------------------------------------------

    /**
     * Deploys the specified WAR as a URL to the specified context path.
     *
     * @param path the webapp context path to deploy to
     * @param war  the URL of the WAR to deploy
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse deploy( String path, URL war )
        throws TomcatManagerException, IOException
    {
        return deploy( path, war, false );
    }

    /**
     * Deploys the specified WAR as a URL to the specified context path, optionally undeploying the webapp if it already
     * exists.
     *
     * @param path   the webapp context path to deploy to
     * @param war    the URL of the WAR to deploy
     * @param update whether to first undeploy the webapp if it already exists
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse deploy( String path, URL war, boolean update )
        throws TomcatManagerException, IOException
    {
        return deploy( path, war, update, null );
    }

    /**
     * Deploys the specified WAR as a URL to the specified context path, optionally undeploying the webapp if it already
     * exists and using the specified tag name.
     *
     * @param path   the webapp context path to deploy to
     * @param war    the URL of the WAR to deploy
     * @param update whether to first undeploy the webapp if it already exists
     * @param tag    the tag name to use
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse deploy( String path, URL war, boolean update, String tag )
        throws TomcatManagerException, IOException
    {
        return deployImpl( path, null, war, null, update, tag );
    }

    /**
     * Deploys the specified WAR as a HTTP PUT to the specified context path.
     *
     * @param path the webapp context path to deploy to
     * @param war  an input stream to the WAR to deploy
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse deploy( String path, File war )
        throws TomcatManagerException, IOException
    {
        return deploy( path, war, false );
    }

    /**
     * Deploys the specified WAR as a HTTP PUT to the specified context path, optionally undeploying the webapp if it
     * already exists.
     *
     * @param path   the webapp context path to deploy to
     * @param war    an input stream to the WAR to deploy
     * @param update whether to first undeploy the webapp if it already exists
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse deploy( String path, File war, boolean update )
        throws TomcatManagerException, IOException
    {
        return deploy( path, war, update, null );
    }

    /**
     * Deploys the specified WAR as a HTTP PUT to the specified context path, optionally undeploying the webapp if it
     * already exists and using the specified tag name.
     *
     * @param path   the webapp context path to deploy to
     * @param war    an input stream to the WAR to deploy
     * @param update whether to first undeploy the webapp if it already exists
     * @param tag    the tag name to use
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse deploy( String path, File war, boolean update, String tag )
        throws TomcatManagerException, IOException
    {
        return deployImpl( path, null, null, war, update, tag );
    }

    /**
     * Deploys the specified WAR as a HTTP PUT to the specified context path, optionally undeploying the webapp if it
     * already exists and using the specified tag name.
     *
     * @param path   the webapp context path to deploy to
     * @param war    an input stream to the WAR to deploy
     * @param update whether to first undeploy the webapp if it already exists
     * @param tag    the tag name to use
     * @param length the size of the war deployed
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     * @since 2.0
     */
    public TomcatManagerResponse deploy( String path, File war, boolean update, String tag, long length )
        throws TomcatManagerException, IOException
    {
        return deployImpl( path, null, null, war, update, tag, length );
    }

    /**
     * Deploys the specified context XML configuration to the specified context path.
     *
     * @param path   the webapp context path to deploy to
     * @param config the URL of the context XML configuration to deploy
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse deployContext( String path, URL config )
        throws TomcatManagerException, IOException
    {
        return deployContext( path, config, false );
    }

    /**
     * Deploys the specified context XML configuration to the specified context path, optionally undeploying the webapp
     * if it already exists.
     *
     * @param path   the webapp context path to deploy to
     * @param config the URL of the context XML configuration to deploy
     * @param update whether to first undeploy the webapp if it already exists
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse deployContext( String path, URL config, boolean update )
        throws TomcatManagerException, IOException
    {
        return deployContext( path, config, update, null );
    }

    /**
     * Deploys the specified context XML configuration to the specified context path, optionally undeploying the webapp
     * if it already exists and using the specified tag name.
     *
     * @param path   the webapp context path to deploy to
     * @param config the URL of the context XML configuration to deploy
     * @param update whether to first undeploy the webapp if it already exists
     * @param tag    the tag name to use
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse deployContext( String path, URL config, boolean update, String tag )
        throws TomcatManagerException, IOException
    {
        return deployContext( path, config, null, update, tag );
    }

    /**
     * Deploys the specified context XML configuration and WAR as a URL to the specified context path.
     *
     * @param path   the webapp context path to deploy to
     * @param config the URL of the context XML configuration to deploy
     * @param war    the URL of the WAR to deploy
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse deployContext( String path, URL config, URL war )
        throws TomcatManagerException, IOException
    {
        return deployContext( path, config, war, false );
    }

    /**
     * Deploys the specified context XML configuration and WAR as a URL to the specified context path, optionally
     * undeploying the webapp if it already exists.
     *
     * @param path   the webapp context path to deploy to
     * @param config the URL of the context XML configuration to deploy
     * @param war    the URL of the WAR to deploy
     * @param update whether to first undeploy the webapp if it already exists
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse deployContext( String path, URL config, URL war, boolean update )
        throws TomcatManagerException, IOException
    {
        return deployContext( path, config, war, update, null );
    }

    /**
     * Deploys the specified context XML configuration and WAR as a URL to the specified context path, optionally
     * undeploying the webapp if it already exists and using the specified tag name.
     *
     * @param path   the webapp context path to deploy to
     * @param config the URL of the context XML configuration to deploy
     * @param war    the URL of the WAR to deploy
     * @param update whether to first undeploy the webapp if it already exists
     * @param tag    the tag name to use
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse deployContext( String path, URL config, URL war, boolean update, String tag )
        throws TomcatManagerException, IOException
    {
        return deployImpl( path, config, war, null, update, tag );
    }

    // ----------------------------------------------------------------------
    // Other Manager Methods
    // ----------------------------------------------------------------------

    /**
     * Undeploys the webapp at the specified context path.
     *
     * @param path the webapp context path to undeploy
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse undeploy( String path )
        throws TomcatManagerException, IOException
    {
        return invoke( "/undeploy?path=" + URLEncoder.encode( path, charset ) );
    }

    /**
     * Reloads the webapp at the specified context path.
     *
     * @param path the webapp context path to reload
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse reload( String path )
        throws TomcatManagerException, IOException
    {
        return invoke( "/reload?path=" + URLEncoder.encode( path, charset ) );
    }

    /**
     * Starts the webapp at the specified context path.
     *
     * @param path the webapp context path to start
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse start( String path )
        throws TomcatManagerException, IOException
    {
        return invoke( "/start?path=" + URLEncoder.encode( path, charset ) );
    }

    /**
     * Stops the webapp at the specified context path.
     *
     * @param path the webapp context path to stop
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse stop( String path )
        throws TomcatManagerException, IOException
    {
        return invoke( "/stop?path=" + URLEncoder.encode( path, charset ) );
    }

    /**
     * Lists all the currently deployed web applications.
     *
     * @return the list of currently deployed applications
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse list()
        throws TomcatManagerException, IOException
    {
        return invoke( "/list" );
    }

    /**
     * Lists information about the Tomcat version, OS, and JVM properties.
     *
     * @return the server information
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse getServerInfo()
        throws TomcatManagerException, IOException
    {
        return invoke( "/serverinfo" );
    }

    /**
     * Lists all of the global JNDI resources.
     *
     * @return the list of all global JNDI resources
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse getResources()
        throws TomcatManagerException, IOException
    {
        return getResources( null );
    }

    /**
     * Lists the global JNDI resources of the given type.
     *
     * @param type the class name of the resources to list, or <code>null</code> for all
     * @return the list of global JNDI resources of the given type
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse getResources( String type )
        throws TomcatManagerException, IOException
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append( "/resources" );

        if ( type != null )
        {
            buffer.append("?type=").append(URLEncoder.encode(type, charset));
        }
        return invoke( buffer.toString() );
    }

    /**
     * Lists the default session timeout and the number of currently active sessions for the given context path.
     *
     * @param path the context path to list session information for
     * @return the default session timeout and the number of currently active sessions
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse getSessions( String path )
        throws TomcatManagerException, IOException
    {
        return invoke( "/sessions?path=" + URLEncoder.encode( path, charset ) );
    }

    // ----------------------------------------------------------------------
    // Protected Methods
    // ----------------------------------------------------------------------

    /**
     * Invokes Tomcat manager with the specified command.
     *
     * @param path the Tomcat manager command to invoke
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    protected TomcatManagerResponse invoke( String path )
        throws TomcatManagerException, IOException
    {
        return invoke( path, null, -1 );
    }

    // ----------------------------------------------------------------------
    // Private Methods
    // ----------------------------------------------------------------------

    private TomcatManagerResponse deployImpl( String path, URL config, URL war, File data, boolean update, String tag )
        throws TomcatManagerException, IOException
    {
        return deployImpl( path, config, war, data, update, tag, -1 );
    }

    /**
     * Deploys the specified WAR.
     *
     * @param path   the webapp context path to deploy to
     * @param config the URL of the context XML configuration to deploy, or null for none
     * @param war    the URL of the WAR to deploy, or null to use <code>data</code>
     * @param data   WAR file to deploy, or null to use <code>war</code>
     * @param update whether to first undeploy the webapp if it already exists
     * @param tag    the tag name to use
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    private TomcatManagerResponse deployImpl( String path, URL config, URL war, File data, boolean update, String tag,
                                              long length )
        throws TomcatManagerException, IOException
    {
        StringBuilder buffer = new StringBuilder( "/deploy" );
        buffer.append( "?path=" ).append( URLEncoder.encode( path, charset ) );

        if ( config != null )
        {
            buffer.append( "&config=" ).append( URLEncoder.encode( config.toString(), charset ) );
        }

        if ( war != null )
        {
            buffer.append( "&war=" ).append( URLEncoder.encode( war.toString(), charset ) );
        }

        if ( update )
        {
            buffer.append( "&update=true" );
        }

        if ( tag != null )
        {
            buffer.append( "&tag=" ).append( URLEncoder.encode( tag, charset ) );
        }

        return invoke( buffer.toString(), data, length );
    }


    /**
     * Invokes Tomcat manager with the specified command and content data.
     *
     * @param path the Tomcat manager command to invoke
     * @param data file to deploy
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    protected TomcatManagerResponse invoke( String path, File data, long length )
        throws TomcatManagerException, IOException
    {
        HttpURLConnection connection = openConnection( url + path );

        if ( data == null )
        {
            connection.setRequestMethod( "GET" );
        }
        else
        {
            connection.setRequestMethod( "PUT" );
            connection.setDoOutput( true );
            connection.setFixedLengthStreamingMode( length >= 0 ? length : data.length() );

            if ( userAgent != null )
            {
                connection.setRequestProperty( "User-Agent", userAgent );
            }

            long completed = 0;
            try ( OutputStream os = connection.getOutputStream();
                  FileInputStream stream = new FileInputStream( data ) )
            {
                transferInitiated( url + path );
                long startTime = System.currentTimeMillis();
                byte[] buffer = new byte[BUFFER_SIZE];

                if ( length < 0 )
                {
                    // until EOF
                    int l;
                    while ( ( l = stream.read( buffer ) ) != -1 )
                    {
                        transferProgressed( completed += l, -1 );
                        os.write( buffer, 0, l );
                    }
                }
                else
                {
                    // no need to consume more than length
                    long remaining = length;
                    while ( remaining > 0 )
                    {
                        int transferSize = (int) Math.min( BUFFER_SIZE, remaining );
                        completed += transferSize;
                        int l = stream.read( buffer, 0, transferSize );
                        if ( l == -1 )
                        {
                            break;
                        }

                        os.write( buffer, 0, l );
                        remaining -= l;
                        transferProgressed( completed, length );
                    }
                }
                transferSucceeded( completed, startTime );
            } finally {
                System.out.println();
            }
        }

        int statusCode = connection.getResponseCode();

        String relocateUrl = null;
        switch ( statusCode )
        {
            // Success Codes
            case SC_OK: // 200
            case SC_CREATED: // 201
            case SC_ACCEPTED: // 202
                break;
            // handle all redirect even if http specs says " the user agent MUST NOT automatically redirect the request unless it can be confirmed by the user"
            case SC_MOVED_PERMANENTLY: // 301
            case SC_MOVED_TEMPORARILY: // 302
            case SC_SEE_OTHER: // 303
                relocateUrl = calculateRelocatedUrl( connection );
                this.url = new URL( relocateUrl );
                return invoke( path, data, length );
        }

        String responseBody;
        try ( InputStream is = connection.getInputStream() != null ? connection.getInputStream() : connection.getErrorStream() )
        {
            responseBody = IOUtils.toString( is, StandardCharsets.UTF_8 );
        }

        return new TomcatManagerResponse()
            .setStatusCode( statusCode )
            .setReasonPhrase( connection.getResponseMessage() )
            .setHttpResponseBody( responseBody );
    }

    protected String calculateRelocatedUrl( HttpURLConnection connection )
    {
        String locationField = connection.getHeaderField( "Location" );
        // is it a relative Location or a full ?
        return locationField.startsWith( "http" ) ? locationField : url.toString() + '/' + locationField;
    }

    private HttpURLConnection openConnection( String urlString ) throws IOException
    {
        URL url = new URL( urlString );
        java.net.Proxy netProxy = java.net.Proxy.NO_PROXY;

        if ( proxySettings != null )
        {
            ProxyInfo proxyInfo = new ProxyInfo();
            proxyInfo.setNonProxyHosts( proxySettings.getNonProxyHosts() );

            if ( !ProxyUtils.validateNonProxyHosts( proxyInfo, url.getHost() ) )
            {
                netProxy = new java.net.Proxy( java.net.Proxy.Type.HTTP,
                    new InetSocketAddress( proxySettings.getHost(), proxySettings.getPort() ) );
            }
        }

        HttpURLConnection connection = (HttpURLConnection) url.openConnection( netProxy );

        connection.setInstanceFollowRedirects( false );
        connection.setUseCaches( false );
        connection.setDefaultUseCaches( false );
        connection.setRequestProperty( "Accept", "text/plain" );

        if ( userAgent != null )
        {
            connection.setRequestProperty( "User-Agent", userAgent );
        }

        if ( usernameProvided )
        {
            String authorization = toAuthorization( username, password );
            connection.setRequestProperty( "Authorization", authorization );
        }

        if ( netProxy != java.net.Proxy.NO_PROXY )
        {
            String proxyAuth = toAuthorization( proxySettings.getUsername(), proxySettings.getPassword() );
            connection.setRequestProperty( "Proxy-Authorization", proxyAuth );
        }

        return connection;
    }

    /**
     * Gets the HTTP Basic Authorization header value for the supplied username and password.
     *
     * @param username the username to use for authentication
     * @param password the password to use for authentication
     * @return the HTTP Basic Authorization header value
     */
    private String toAuthorization( String username, String password )
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append( username ).append( ':' );
        if ( password != null )
        {
            buffer.append( password );
        }
        return "Basic " + new String( Base64.encodeBase64( buffer.toString().getBytes() ) );
    }

    private void transferInitiated( String targetUrl )
    {
        String message = "Uploading";

        System.out.println( message + ": " + targetUrl );
    }

    private void transferProgressed( long completedSize, long totalSize )
    {
        if ( !verbose )
        {
            return;
        }

        StringBuilder buffer = new StringBuilder( 64 );

        buffer.append( getStatus( completedSize, totalSize ) ).append( "  " );
        buffer.append( '\r' );

        System.out.print( buffer );
    }

    private void transferSucceeded( long contentLength, long startTime )
    {
        if ( contentLength >= 0 )
        {
            String type = "Uploaded";
            String len = contentLength >= 1024 ? toKB( contentLength ) + " KB" : contentLength + " B";

            String throughput = "";
            long duration = System.currentTimeMillis() - startTime;
            if ( duration > 0 )
            {
                DecimalFormat format = new DecimalFormat( "0.0", new DecimalFormatSymbols( Locale.ENGLISH ) );
                double kbPerSec = ( contentLength / 1024.0 ) / ( duration / 1000.0 );
                throughput = " at " + format.format( kbPerSec ) + " KB/sec";
            }

            System.out.println( type + ": " + url + " (" + len + throughput + ")" );
        }
    }

    private String getStatus( long complete, long total )
    {
        if ( total >= 1024 )
        {
            return toKB( complete ) + "/" + toKB( total ) + " KB ";
        }
        else if ( total >= 0 )
        {
            return complete + "/" + total + " B ";
        }
        else if ( complete >= 1024 )
        {
            return toKB( complete ) + " KB ";
        }
        else
        {
            return complete + " B ";
        }
    }

    private long toKB( long bytes )
    {
        return ( bytes + 1023 ) / 1024;
    }

}