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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.tomcat.maven.common.deployer.TomcatManagerException;
import org.apache.tomcat.maven.common.deployer.TomcatManagerResponse;

import java.io.IOException;

/**
 * Lists JNDI resources in Tomcat.
 *
 * @author Mark Hobson <markhobson@gmail.com>
 */
@Mojo( name = "resources", threadSafe = true )
public class ResourcesMojo
    extends AbstractCatalinaMojo
{
    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The class name of the resources to list, or <code>null</code> for all.
     *
     * @parameter expression = "${maven.tomcat.type}"
     */
    private String type;

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
        if ( type == null )
        {
            getLog().info( messagesProvider.getMessage( "ResourcesMojo.listAllResources", getURL() ) );
        }
        else
        {
            getLog().info( messagesProvider.getMessage( "ResourcesMojo.listTypedResources", type, getURL() ) );
        }

        TomcatManagerResponse tomcatResponse = getManager().getResources( type );

        checkTomcatResponse( tomcatResponse );

        log( tomcatResponse.getHttpResponseBody() );          
    }
}
