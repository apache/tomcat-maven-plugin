package org.apache.tomcat.maven.plugin.tomcat7;

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

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.tomcat.maven.common.deployer.TomcatManager;
import org.apache.tomcat.maven.common.deployer.TomcatManagerException;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

/**
 * Abstract goal that provides common configuration for Catalina-based goals.
 *
 * @author Mark Hobson <markhobson@gmail.com>
 */
public abstract class AbstractCatalinaMojo
    extends AbstractTomcat7Mojo
{
    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------

    /**
     * The name of this Maven plugin. Used to produce the user agent when communicating with Tomcat manager.
     */
    private String name = "Apache Tomcat Maven Plugin";

    /**
     * The default username to use when authenticating with Tomcat manager.
     */
    private static final String DEFAULT_USERNAME = "admin";

    /**
     * The default password to use when authenticating with Tomcat manager.
     */
    private static final String DEFAULT_PASSWORD = "";

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The Maven Wagon manager to use when obtaining server authentication details.
     */
    @Component
    private WagonManager wagonManager;

    /**
     * The full URL of the Tomcat manager instance to use.
     */
    @Parameter( property = "maven.tomcat.url", defaultValue = "http://localhost:8080/manager/text", required = true )
    private URL url;

    /**
     * The server id in settings.xml to use when authenticating with Tomcat manager, or <code>null</code> to use
     * defaults of username <code>admin</code> and no password.
     */
    @Parameter( property = "maven.tomcat.server" )
    private String server;

    /**
     * The URL encoding charset to use when communicating with Tomcat manager.
     */
    @Parameter( property = "maven.tomcat.charset", defaultValue = "ISO-8859-1", required = true )
    private String charset;

    /**
     * The tomcat username to use for deployment
     *
     * @since 1.0-alpha-2
     */
    @Parameter( property = "tomcat.username" )
    private String username;

    /**
     * The password to use for deployment
     *
     * @since 1.0-alpha-2
     */
    @Parameter( property = "tomcat.password" )
    private String password;

    @Parameter( defaultValue = "${plugin.version}", required = true, readonly = true )
    private String version;

    // ----------------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------------

    /**
     * The Tomcat manager wrapper object.
     */
    private TomcatManager manager;

    // ----------------------------------------------------------------------
    // Mojo Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException
    {
        try
        {
            invokeManager();
        }
        catch ( TomcatManagerException exception )
        {
            throw new MojoExecutionException(
                messagesProvider.getMessage( "AbstractCatalinaMojo.managerError", exception.getMessage() ) );
        }
        catch ( IOException exception )
        {
            throw new MojoExecutionException( messagesProvider.getMessage( "AbstractCatalinaMojo.managerIOError" ),
                                              exception );
        }
    }

    // ----------------------------------------------------------------------
    // Protected Methods
    // ----------------------------------------------------------------------

    /**
     * Invokes Tomcat manager when this Mojo is executed.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     *                             if there was a problem executing this goal
     * @throws org.apache.tomcat.maven.common.deployer.TomcatManagerException
     *                             if the Tomcat manager request fails
     * @throws java.io.IOException if an i/o error occurs
     */
    protected abstract void invokeManager()
        throws MojoExecutionException, TomcatManagerException, IOException;

    /**
     * Gets the Tomcat manager wrapper object configured for this goal.
     *
     * @return the Tomcat manager wrapper object
     * @throws org.apache.maven.plugin.MojoExecutionException
     *          if there was a problem obtaining the authentication details
     */
    protected TomcatManager getManager()
        throws MojoExecutionException
    {
        // lazily instantiate when config values have been injected
        if ( manager == null )
        {
            String userName;
            String password;

            if ( server == null )
            {
                // no server set, use defaults
                getLog().debug( messagesProvider.getMessage( "AbstractCatalinaMojo.defaultAuth" ) );
                userName = DEFAULT_USERNAME;
                password = DEFAULT_PASSWORD;
            }
            else
            {
                // obtain authenication details for specified server from wagon
                AuthenticationInfo info = wagonManager.getAuthenticationInfo( server );
                if ( info == null )
                {
                    throw new MojoExecutionException(
                        messagesProvider.getMessage( "AbstractCatalinaMojo.unknownServer", server ) );
                }

                // derive username
                userName = info.getUserName();
                if ( userName == null )
                {
                    getLog().debug( messagesProvider.getMessage( "AbstractCatalinaMojo.defaultUserName" ) );
                    userName = DEFAULT_USERNAME;
                }

                // derive password
                password = info.getPassword();
                if ( password == null )
                {
                    getLog().debug( messagesProvider.getMessage( "AbstractCatalinaMojo.defaultPassword" ) );
                    password = DEFAULT_PASSWORD;
                }
            }

            // if userName/password are defined in the mojo or the cli they override
            if ( !StringUtils.isEmpty( this.username ) )
            {
                userName = this.username;
                password = this.password == null ? "" : this.password;
            }

            manager = new TomcatManager( url, userName, password, charset, settings.isInteractiveMode() );
            manager.setUserAgent( name + "/" + version );
        }

        return manager;
    }

    /**
     * Gets the full URL of the Tomcat manager instance.
     *
     * @return the full URL of the Tomcat manager instance to use
     */
    protected URL getURL()
    {
        return url;
    }

    /**
     * Gets the webapp context path to use when communicating with Tomcat manager.
     *
     * @return the webapp context path to use
     */
    protected String getPath()
    {
        return path;
    }

    /**
     * Gets the URL of the deployed webapp.
     *
     * @return the URL of the deployed webapp
     * @throws java.net.MalformedURLException if the deployed webapp URL is invalid
     */
    protected URL getDeployedURL()
        throws MalformedURLException
    {
        return new URL( getURL(), getPath() );
    }

    /**
     * Splits the given string into lines and writes each one separately to the log at info level.
     *
     * @param string the string to write
     */
    protected void log( String string )
    {
        StringTokenizer tokenizer = new StringTokenizer( string, "\n\r" );

        while ( tokenizer.hasMoreTokens() )
        {
            getLog().info( tokenizer.nextToken() );
        }
    }
}
