package org.apache.tomcat.maven.plugin.tomcat7.deploy;

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
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tomcat.maven.common.deployer.TomcatManagerException;
import org.apache.tomcat.maven.common.deployer.TomcatManagerResponse;

import java.io.File;
import java.io.IOException;

/**
 * @author olamy
 * @since 1.0-alpha-2
 */
public class AbstractDeployWarMojo
    extends AbstractDeployMojo
{
    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The path of the WAR file to deploy.
     */
    @Parameter( defaultValue = "${project.build.directory}/${project.build.finalName}.war", required = true )
    private File warFile;

    // ----------------------------------------------------------------------
    // Protected Methods
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected File getWarFile()
    {
        return warFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateWarFile()
        throws MojoExecutionException
    {
        if ( !warFile.exists() || !warFile.isFile() )
        {
            throw new MojoExecutionException(
                messagesProvider.getMessage( "DeployMojo.missingWar", warFile.getPath() ) );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void deployWar()
        throws MojoExecutionException, TomcatManagerException, IOException
    {
        validateWarFile();

        getLog().info( messagesProvider.getMessage( "AbstractDeployMojo.deployingWar", getDeployedURL() ) );

        TomcatManagerResponse tomcatManagerResponse =
            getManager().deploy( getPath(), warFile, isUpdate(), getTag(), warFile.length() );

        checkTomcatResponse( tomcatManagerResponse );

        getLog().info( "tomcatManager status code:" + tomcatManagerResponse.getStatusCode() + ", ReasonPhrase:"
                           + tomcatManagerResponse.getReasonPhrase() );

        log( tomcatManagerResponse.getHttpResponseBody() );
    }
}
