package org.apache.tomcat.maven.common.deployer;

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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;

/**
 * FIXME http connection tru a proxy
 * A Tomcat manager webapp invocation wrapper.
 *
 * @author Mark Hobson <markhobson@gmail.com>
 * @version $Id: TomcatManager.java 12852 2010-10-12 22:04:32Z thragor $
 */
public class TomcatManager
{
    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------

    /**
     * The charset to use when decoding Tomcat manager responses.
     */
    private static final String MANAGER_CHARSET = "UTF-8";

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
    private String username;

    /**
     * The password to use when authenticating with Tomcat manager.
     */
    private String password;

    /**
     * The URL encoding charset to use when communicating with Tomcat manager.
     */
    private String charset;

    /**
     * The user agent name to use when communicating with Tomcat manager.
     */
    private String userAgent;

    private DefaultHttpClient httpClient;

    private BasicHttpContext localContext;

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
        this.url = url;
        this.username = username;
        this.password = password;
        this.charset = charset;

        this.httpClient = new DefaultHttpClient( new BasicClientConnectionManager() );
        if ( StringUtils.isNotEmpty( username ) && StringUtils.isNotEmpty( password ) )
        {
            Credentials creds = new UsernamePasswordCredentials( username, password );

            String host = url.getHost();
            int port = url.getPort() > -1 ? url.getPort() : AuthScope.ANY_PORT;

            httpClient.getCredentialsProvider().setCredentials( new AuthScope( host, port ), creds );

            AuthCache authCache = new BasicAuthCache();
            BasicScheme basicAuth = new BasicScheme();
            HttpHost targetHost = new HttpHost( url.getHost(), url.getPort(), url.getProtocol() );
            authCache.put( targetHost, basicAuth );

            localContext = new BasicHttpContext();
            localContext.setAttribute( ClientContext.AUTH_CACHE, authCache );
        }
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
    public TomcatManagerResponse deploy( String path, InputStream war )
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
    public TomcatManagerResponse deploy( String path, InputStream war, boolean update )
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
    public TomcatManagerResponse deploy( String path, InputStream war, boolean update, String tag )
        throws TomcatManagerException, IOException
    {
        return deployImpl( path, null, null, war, update, tag );
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
        StringBuffer buffer = new StringBuffer();
        buffer.append( "/resources" );

        if ( type != null )
        {
            buffer.append( "?type=" + URLEncoder.encode( type, charset ) );
        }
        return invoke( buffer.toString() );
    }

    /**
     * Lists the security role names and corresponding descriptions that are available.
     *
     * @return the list of security role names and corresponding descriptions
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    public TomcatManagerResponse getRoles()
        throws TomcatManagerException, IOException
    {
        return invoke( "/roles" );
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
        return invoke( path, null );
    }

    // ----------------------------------------------------------------------
    // Private Methods
    // ----------------------------------------------------------------------

    /**
     * Deploys the specified WAR.
     *
     * @param path   the webapp context path to deploy to
     * @param config the URL of the context XML configuration to deploy, or null for none
     * @param war    the URL of the WAR to deploy, or null to use <code>data</code>
     * @param data   an input stream to the WAR to deploy, or null to use <code>war</code>
     * @param update whether to first undeploy the webapp if it already exists
     * @param tag    the tag name to use
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    private TomcatManagerResponse deployImpl( String path, URL config, URL war, InputStream data, boolean update, String tag )
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

        return invoke( buffer.toString(), data );
    }


    /**
     * Invokes Tomcat manager with the specified command and content data.
     *
     * @param path the Tomcat manager command to invoke
     * @param data an input stream to the content data
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException            if an i/o error occurs
     */
    protected TomcatManagerResponse invoke( String path, InputStream data )
        throws TomcatManagerException, IOException
    {

        HttpRequestBase httpRequestBase = null;
        if ( data == null )
        {
            httpRequestBase = new HttpGet( url + path );
        }
        else
        {
            HttpPut httpPut = new HttpPut( url + path );

            httpPut.setEntity( new RequestEntityImplementation( data, -1 ) );

            httpRequestBase = httpPut;

        }

        if ( userAgent != null )
        {
            httpRequestBase.setHeader( "User-Agent", userAgent );
        }

        HttpResponse response = httpClient.execute( httpRequestBase, localContext );

        return new TomcatManagerResponse().setStatusCode( response.getStatusLine().getStatusCode() ).setReasonPhrase(
            response.getStatusLine().getReasonPhrase() ).setHttpResponseBody(
            IOUtils.toString( response.getEntity().getContent() ) );

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
        StringBuffer buffer = new StringBuffer();
        buffer.append( username ).append( ':' );
        if ( password != null )
        {
            buffer.append( password );
        }
        return "Basic " + new String( Base64.encodeBase64( buffer.toString().getBytes() ) );
    }

    private final class RequestEntityImplementation
        extends AbstractHttpEntity
    {

        private final static int BUFFER_SIZE = 2048;

        private InputStream stream;

        private long length = -1;

        private RequestEntityImplementation( final InputStream stream, long length )
        {
            this.stream = stream;
            this.length = length;
        }

        public long getContentLength()
        {
            return length >= 0 ? length : -1;
        }


        public InputStream getContent()
            throws IOException, IllegalStateException
        {
            return this.stream;
        }

        public boolean isRepeatable()
        {
            return false;
        }


        public void writeTo( final OutputStream outstream )
            throws IOException
        {
            if ( outstream == null )
            {
                throw new IllegalArgumentException( "Output stream may not be null" );
            }

            try
            {
                byte[] buffer = new byte[BUFFER_SIZE];
                int l;
                if ( this.length < 0 )
                {
                    // until EOF
                    while ( ( l = stream.read( buffer ) ) != -1 )
                    {
                        //fireTransferProgress( transferEvent, buffer, -1 );
                        outstream.write( buffer, 0, l );
                    }
                }
                else
                {
                    // no need to consume more than length
                    long remaining = this.length;
                    while ( remaining > 0 )
                    {
                        l = stream.read( buffer, 0, (int) Math.min( BUFFER_SIZE, remaining ) );
                        if ( l == -1 )
                        {
                            break;
                        }
                        //fireTransferProgress( transferEvent, buffer, (int) Math.min( BUFFER_SIZE, remaining ) );
                        outstream.write( buffer, 0, l );
                        remaining -= l;
                    }
                }
            }
            finally
            {
                stream.close();
            }
        }

        public boolean isStreaming()
        {
            return true;
        }


    }
}
