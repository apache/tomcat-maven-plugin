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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tomcat.maven.common.deployer.TomcatManagerException;

import java.io.IOException;

/**
 * Undeploy a WAR from Tomcat.
 *
 * @author Mark Hobson <markhobson@gmail.com>
 * @version $Id: UndeployMojo.java 12852 2010-10-12 22:04:32Z thragor $
 * @goal undeploy
 */
public class UndeployMojo
    extends AbstractWarCatalinaMojo
{
    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * Whether to fail the build if the web application cannot be undeployed.
     *
     * @parameter expression = "${maven.tomcat.failOnError}" default-value = "true"
     */
    private boolean failOnError;

    // ----------------------------------------------------------------------
    // Protected Methods
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected void invokeManager()
        throws MojoExecutionException, TomcatManagerException, IOException
    {
        getLog().info( messagesProvider.getMessage( "UndeployMojo.undeployingApp", getDeployedURL() ) );

        try
        {
            log( getManager().undeploy( getPath() ).getHttpResponseBody() );
        }
        catch ( TomcatManagerException exception )
        {
            if ( failOnError )
            {
                throw exception;
            }

            getLog().warn( messagesProvider.getMessage( "UndeployMojo.undeployError", exception.getMessage() ) );
        }
    }
}
