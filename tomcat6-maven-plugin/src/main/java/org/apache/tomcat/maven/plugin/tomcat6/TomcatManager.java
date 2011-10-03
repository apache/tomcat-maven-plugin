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

import org.apache.commons.codec.binary.Base64;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
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
     * @param url the full URL of the Tomcat manager instance to use
     * @param username the username to use when authenticating with Tomcat manager
     */
    public TomcatManager( URL url, String username )
    {
        this( url, username, "" );
    }

    /**
     * Creates a Tomcat manager wrapper for the specified URL, username and password that uses ISO-8859-1 URL encoding.
     * 
     * @param url the full URL of the Tomcat manager instance to use
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
     * @param url the full URL of the Tomcat manager instance to use
     * @param username the username to use when authenticating with Tomcat manager
     * @param password the password to use when authenticating with Tomcat manager
     * @param charset the URL encoding charset to use when communicating with Tomcat manager
     */
    public TomcatManager( URL url, String username, String password, String charset )
    {
        this.url = url;
        this.username = username;
        this.password = password;
        this.charset = charset;
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
     * @param war the URL of the WAR to deploy
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException if an i/o error occurs
     */
    public String deploy( String path, URL war )
        throws TomcatManagerException, IOException
    {
        return deploy( path, war, false );
    }

    /**
     * Deploys the specified WAR as a URL to the specified context path, optionally undeploying the webapp if it already
     * exists.
     * 
     * @param path the webapp context path to deploy to
     * @param war the URL of the WAR to deploy
     * @param update whether to first undeploy the webapp if it already exists
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException if an i/o error occurs
     */
    public String deploy( String path, URL war, boolean update )
        throws TomcatManagerException, IOException
    {
        return deploy( path, war, update, null );
    }

    /**
     * Deploys the specified WAR as a URL to the specified context path, optionally undeploying the webapp if it already
     * exists and using the specified tag name.
     * 
     * @param path the webapp context path to deploy to
     * @param war the URL of the WAR to deploy
     * @param update whether to first undeploy the webapp if it already exists
     * @param tag the tag name to use
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException if an i/o error occurs
     */
    public String deploy( String path, URL war, boolean update, String tag )
        throws TomcatManagerException, IOException
    {
        return deployImpl( path, null, war, null, update, tag );
    }

    /**
     * Deploys the specified WAR as a HTTP PUT to the specified context path.
     * 
     * @param path the webapp context path to deploy to
     * @param war an input stream to the WAR to deploy
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException if an i/o error occurs
     */
    public String deploy( String path, InputStream war )
        throws TomcatManagerException, IOException
    {
        return deploy( path, war, false );
    }

    /**
     * Deploys the specified WAR as a HTTP PUT to the specified context path, optionally undeploying the webapp if it
     * already exists.
     * 
     * @param path the webapp context path to deploy to
     * @param war an input stream to the WAR to deploy
     * @param update whether to first undeploy the webapp if it already exists
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException if an i/o error occurs
     */
    public String deploy( String path, InputStream war, boolean update )
        throws TomcatManagerException, IOException
    {
        return deploy( path, war, update, null );
    }

    /**
     * Deploys the specified WAR as a HTTP PUT to the specified context path, optionally undeploying the webapp if it
     * already exists and using the specified tag name.
     * 
     * @param path the webapp context path to deploy to
     * @param war an input stream to the WAR to deploy
     * @param update whether to first undeploy the webapp if it already exists
     * @param tag the tag name to use
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException if an i/o error occurs
     */
    public String deploy( String path, InputStream war, boolean update, String tag )
        throws TomcatManagerException, IOException
    {
        return deployImpl( path, null, null, war, update, tag );
    }

    /**
     * Deploys the specified context XML configuration to the specified context path.
     * 
     * @param path the webapp context path to deploy to
     * @param config the URL of the context XML configuration to deploy
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException if an i/o error occurs
     */
    public String deployContext( String path, URL config )
        throws TomcatManagerException, IOException
    {
        return deployContext( path, config, false );
    }

    /**
     * Deploys the specified context XML configuration to the specified context path, optionally undeploying the webapp
     * if it already exists.
     * 
     * @param path the webapp context path to deploy to
     * @param config the URL of the context XML configuration to deploy
     * @param update whether to first undeploy the webapp if it already exists
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException if an i/o error occurs
     */
    public String deployContext( String path, URL config, boolean update )
        throws TomcatManagerException, IOException
    {
        return deployContext( path, config, update, null );
    }

    /**
     * Deploys the specified context XML configuration to the specified context path, optionally undeploying the webapp
     * if it already exists and using the specified tag name.
     * 
     * @param path the webapp context path to deploy to
     * @param config the URL of the context XML configuration to deploy
     * @param update whether to first undeploy the webapp if it already exists
     * @param tag the tag name to use
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException if an i/o error occurs
     */
    public String deployContext( String path, URL config, boolean update, String tag )
        throws TomcatManagerException, IOException
    {
        return deployContext( path, config, null, update, tag );
    }

    /**
     * Deploys the specified context XML configuration and WAR as a URL to the specified context path.
     * 
     * @param path the webapp context path to deploy to
     * @param config the URL of the context XML configuration to deploy
     * @param war the URL of the WAR to deploy
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException if an i/o error occurs
     */
    public String deployContext( String path, URL config, URL war )
        throws TomcatManagerException, IOException
    {
        return deployContext( path, config, war, false );
    }

    /**
     * Deploys the specified context XML configuration and WAR as a URL to the specified context path, optionally
     * undeploying the webapp if it already exists.
     * 
     * @param path the webapp context path to deploy to
     * @param config the URL of the context XML configuration to deploy
     * @param war the URL of the WAR to deploy
     * @param update whether to first undeploy the webapp if it already exists
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException if an i/o error occurs
     */
    public String deployContext( String path, URL config, URL war, boolean update )
        throws TomcatManagerException, IOException
    {
        return deployContext( path, config, war, update, null );
    }

    /**
     * Deploys the specified context XML configuration and WAR as a URL to the specified context path, optionally
     * undeploying the webapp if it already exists and using the specified tag name.
     * 
     * @param path the webapp context path to deploy to
     * @param config the URL of the context XML configuration to deploy
     * @param war the URL of the WAR to deploy
     * @param update whether to first undeploy the webapp if it already exists
     * @param tag the tag name to use
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException if an i/o error occurs
     */
    public String deployContext( String path, URL config, URL war, boolean update, String tag )
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
     * @throws IOException if an i/o error occurs
     */
    public String undeploy( String path )
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
     * @throws IOException if an i/o error occurs
     */
    public String reload( String path )
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
     * @throws IOException if an i/o error occurs
     */
    public String start( String path )
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
     * @throws IOException if an i/o error occurs
     */
    public String stop( String path )
        throws TomcatManagerException, IOException
    {
        return invoke( "/stop?path=" + URLEncoder.encode( path, charset ) );
    }

    /**
     * Lists all the currently deployed web applications.
     * 
     * @return the list of currently deployed applications
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException if an i/o error occurs
     */
    public String list()
        throws TomcatManagerException, IOException
    {
        return invoke( "/list" );
    }

    /**
     * Lists information about the Tomcat version, OS, and JVM properties.
     * 
     * @return the server information
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException if an i/o error occurs
     */
    public String getServerInfo()
        throws TomcatManagerException, IOException
    {
        return invoke( "/serverinfo" );
    }

    /**
     * Lists all of the global JNDI resources.
     * 
     * @return the list of all global JNDI resources
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException if an i/o error occurs
     */
    public String getResources()
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
     * @throws IOException if an i/o error occurs
     */
    public String getResources( String type )
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
     * @throws IOException if an i/o error occurs
     */
    public String getRoles()
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
     * @throws IOException if an i/o error occurs
     */
    public String getSessions( String path )
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
     * @throws IOException if an i/o error occurs
     */
    protected String invoke( String path )
        throws TomcatManagerException, IOException
    {
        return invoke( path, null );
    }

    /**
     * Invokes Tomcat manager with the specified command and content data.
     * 
     * @param path the Tomcat manager command to invoke
     * @param data an input stream to the content data
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException if an i/o error occurs
     */
    protected String invoke( String path, InputStream data )
        throws TomcatManagerException, IOException
    {
        HttpURLConnection connection = (HttpURLConnection) new URL( url + path ).openConnection();
        connection.setAllowUserInteraction( false );
        connection.setDoInput( true );
        connection.setUseCaches( false );

        if ( data == null )
        {
            connection.setDoOutput( false );
            connection.setRequestMethod( "GET" );
        }
        else
        {
            connection.setDoOutput( true );
            connection.setRequestMethod( "PUT" );
            connection.setRequestProperty( "Content-Type", "application/octet-stream" );
        }

        if ( userAgent != null )
        {
            connection.setRequestProperty( "User-Agent", userAgent );
        }
        connection.setRequestProperty( "Authorization", toAuthorization( username, password ) );

        connection.connect();

        if ( data != null )
        {
            pipe( data, connection.getOutputStream() );
        }

        String response = toString( connection.getInputStream(), MANAGER_CHARSET );

        if ( !response.startsWith( "OK -" ) )
        {
            throw new TomcatManagerException( response );
        }

        return response;
    }

    // ----------------------------------------------------------------------
    // Private Methods
    // ----------------------------------------------------------------------

    /**
     * Deploys the specified WAR.
     * 
     * @param path the webapp context path to deploy to
     * @param config the URL of the context XML configuration to deploy, or null for none
     * @param war the URL of the WAR to deploy, or null to use <code>data</code>
     * @param data an input stream to the WAR to deploy, or null to use <code>war</code>
     * @param update whether to first undeploy the webapp if it already exists
     * @param tag the tag name to use
     * @return the Tomcat manager response
     * @throws TomcatManagerException if the Tomcat manager request fails
     * @throws IOException if an i/o error occurs
     */
    private String deployImpl( String path, URL config, URL war, InputStream data, boolean update, String tag )
        throws TomcatManagerException, IOException
    {
        StringBuffer buffer = new StringBuffer( "/deploy" );
        buffer.append( "?path=" ).append( URLEncoder.encode( path, charset ) );

        if ( config != null )
        {
            buffer.append( "&config=" ).append( URLEncoder.encode( config.toString(), charset ) );
        }

        if ( war != null )
        {
            buffer.append( "&war=" ).append( URLEncoder.encode( war.toString(), charset ) );
        }
        else
        {
            // for Tomcat 5.0.27
            buffer.append( "&war=" );
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

    /**
     * Reads all the data from the specified input stream and writes it to the specified output stream. Both streams are
     * also closed.
     * 
     * @param in the input stream to read from
     * @param out the output stream to write to
     * @throws IOException if an i/o error occurs
     */
    private void pipe( InputStream in, OutputStream out )
        throws IOException
    {
        out = new BufferedOutputStream( out );
        int n;
        byte[] bytes = new byte[1024 * 4];
        while ( ( n = in.read( bytes ) ) != -1 )
        {
            out.write( bytes, 0, n );
        }
        out.flush();
        out.close();
        in.close();
    }

    /**
     * Gets the data from the specified input stream as a string using the specified charset.
     * 
     * @param in the input stream to read from
     * @param charset the charset to use when constructing the string
     * @return a string representation of the data read from the input stream
     * @throws IOException if an i/o error occurs
     */
    private String toString( InputStream in, String charset )
        throws IOException
    {
        InputStreamReader reader = new InputStreamReader( in, charset );

        StringBuffer buffer = new StringBuffer();
        char[] chars = new char[1024];
        int n;
        while ( ( n = reader.read( chars, 0, chars.length ) ) != -1 )
        {
            buffer.append( chars, 0, n );
        }

        return buffer.toString();
    }
}
