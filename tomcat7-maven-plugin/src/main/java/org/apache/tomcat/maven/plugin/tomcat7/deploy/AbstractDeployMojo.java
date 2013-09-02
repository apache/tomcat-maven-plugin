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
import org.apache.tomcat.maven.plugin.tomcat7.AbstractWarCatalinaMojo;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Deploy a WAR to Tomcat.
 *
 * @author Mark Hobson <markhobson@gmail.com>
 */
public abstract class AbstractDeployMojo
    extends AbstractWarCatalinaMojo
{
    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The deployment mode to use. This must be either <code>war</code> to deploy the war, <code>context</code> to
     * deploy the context XML file, or <code>both</code> to deploy the war with the context XML file.
     */
    @Parameter( property = "maven.tomcat.mode", defaultValue = "war", required = true )
    private String mode;

    /**
     * The path of the Tomcat context XML file. This is not used for war deployment mode.
     */
    @Parameter( defaultValue = "${project.build.directory}/${project.build.finalName}/META-INF/context.xml" )
    private File contextFile;

    /**
     * Whether Tomcat should automatically undeploy webapps that already exist when deploying.
     */
    @Parameter( property = "maven.tomcat.update", defaultValue = "false", required = true )
    private boolean update;

    /**
     * The Tomcat webapp tag name to use.
     */
    @Parameter( property = "maven.tomcat.tag" )
    private String tag;

    // ----------------------------------------------------------------------
    // Protected Methods
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void invokeManager()
        throws MojoExecutionException, TomcatManagerException, IOException
    {
        if ( "war".equals( mode ) )
        {
            deployWar();
        }
        else if ( "context".equals( mode ) )
        {
            deployContext();
        }
        else if ( "both".equals( mode ) )
        {
            deployWarAndContext();
        }
        else
        {
            throw new MojoExecutionException( messagesProvider.getMessage( "AbstractDeployMojo.unknownMode", mode ) );
        }
    }

    /**
     * Gets the Tomcat WAR file. This may be a file or a directory depending on the deployment mode.
     *
     * @return the Tomcat WAR file.
     */
    protected abstract File getWarFile();

    /**
     * Ensures that the Tomcat WAR file exists and is the correct type for the deployment mode.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     *          if the WAR file does not exist or is not the correct type for the deployment mode
     */
    protected abstract void validateWarFile()
        throws MojoExecutionException;

    /**
     * Gets the Tomcat context XML file.
     *
     * @return the Tomcat context XML file.
     */
    protected File getContextFile()
    {
        return contextFile;
    }

    /**
     * Ensures that the Tomcat context XML file exists and is indeed a file.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     *          if the context file does not exist or is not a file
     */
    protected void validateContextFile()
        throws MojoExecutionException
    {
        if ( !contextFile.exists() || !contextFile.isFile() )
        {
            throw new MojoExecutionException(
                messagesProvider.getMessage( "AbstractDeployMojo.missingContext", contextFile.getPath() ) );
        }
    }

    /**
     * Gets whether Tomcat should automatically undeploy webapps that already exist when deploying.
     *
     * @return whether Tomcat should automatically undeploy webapps that already exist when deploying
     */
    protected boolean isUpdate()
    {
        return update;
    }

    /**
     * Gets the Tomcat webapp tag name to use.
     *
     * @return the Tomcat webapp tag name to use
     */
    protected String getTag()
    {
        return tag;
    }

    /**
     * Deploys the WAR to Tomcat.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     *                             if there was a problem locating the WAR
     * @throws org.apache.tomcat.maven.common.deployer.TomcatManagerException
     *                             if the Tomcat manager request fails
     * @throws java.io.IOException if an i/o error occurs
     */
    protected void deployWar()
        throws MojoExecutionException, TomcatManagerException, IOException
    {
        validateWarFile();

        getLog().info( messagesProvider.getMessage( "AbstractDeployMojo.deployingWar", getDeployedURL() ) );

        URL warURL = getWarFile().toURL();
        log( getManager().deploy( getPath(), warURL, isUpdate(), getTag() ).getHttpResponseBody() );
    }

    /**
     * Deploys the context XML file to Tomcat.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     *                             if there was a problem locating the context XML file
     * @throws org.apache.tomcat.maven.common.deployer.TomcatManagerException
     *                             if the Tomcat manager request fails
     * @throws java.io.IOException if an i/o error occurs
     */
    protected void deployContext()
        throws MojoExecutionException, TomcatManagerException, IOException
    {
        validateContextFile();

        getLog().info( messagesProvider.getMessage( "AbstractDeployMojo.deployingContext", getDeployedURL() ) );

        URL contextURL = getContextFile().toURL();
        log( getManager().deployContext( getPath(), contextURL, isUpdate(), getTag() ).getHttpResponseBody() );
    }

    /**
     * Deploys the WAR and context XML file to Tomcat.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     *                             if there was a problem locating either the WAR or the context XML file
     * @throws org.apache.tomcat.maven.common.deployer.TomcatManagerException
     *                             if the Tomcat manager request fails
     * @throws java.io.IOException if an i/o error occurs
     */
    protected void deployWarAndContext()
        throws MojoExecutionException, TomcatManagerException, IOException
    {
        validateWarFile();
        validateContextFile();

        getLog().info( messagesProvider.getMessage( "AbstractDeployMojo.deployingWarContext", getDeployedURL() ) );

        URL warURL = getWarFile().toURL();
        URL contextURL = getContextFile().toURL();

        TomcatManagerResponse tomcatResponse = getManager().deployContext( getPath(), contextURL, warURL, isUpdate(), getTag() );

        checkTomcatResponse( tomcatResponse );

        log( tomcatResponse.getHttpResponseBody() );
    }
}
