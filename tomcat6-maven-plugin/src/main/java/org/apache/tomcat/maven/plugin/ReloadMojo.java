package org.apache.tomcat.maven.plugin;

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

import java.io.IOException;

/**
 * Reload a WAR in Tomcat.
 * 
 * @goal reload
 * @author olamy
 * @version $Id: ReloadMojo.java 12852 2010-10-12 22:04:32Z thragor $
 */
public class ReloadMojo
    extends AbstractWarCatalinaMojo
{
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
        getLog().info( getMessage( "RedeployMojo.redeployApp", getDeployedURL() ) );

        log( getManager().reload( getPath() ) );
    }
}
