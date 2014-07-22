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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;
import org.apache.tomcat.maven.common.deployer.TomcatManagerResponse;
import org.apache.tomcat.maven.common.messages.MessagesProvider;

/**
 * @author Olivier Lamy
 * @since 2.0
 */
public abstract class AbstractTomcat7Mojo
    extends AbstractMojo
{
    @Component
    protected Settings settings;

    @Component
    protected MessagesProvider messagesProvider;

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The webapp context path to use for the web application being run. This must always start with a forward-slash
     * ('/').
     */
    @Parameter(defaultValue = "/${project.artifactId}", property = "maven.tomcat.path", required = true)
    protected String path;


    protected String getPath()
    {
        return path;
    }

    /**
     * Check response of Tomcat to know if ok or not.
     *
     * @param tomcatResponse response of tomcat return by TomcatManager class
     * @throws org.apache.maven.plugin.MojoExecutionException
     *          if HTTP status code greater than 400 (included)
     */
    protected void checkTomcatResponse( TomcatManagerResponse tomcatResponse )
        throws MojoExecutionException
    {
        int statusCode = tomcatResponse.getStatusCode();

        if ( statusCode >= 400 )
        {
            getLog().error( messagesProvider.getMessage( "tomcatHttpStatusError", statusCode,
                                                         tomcatResponse.getReasonPhrase() ) );

            throw new MojoExecutionException(
                messagesProvider.getMessage( "tomcatHttpStatusError", statusCode,
                                             tomcatResponse.getReasonPhrase() ) + ": "
                    + tomcatResponse.getHttpResponseBody() );
        }
    }
}
