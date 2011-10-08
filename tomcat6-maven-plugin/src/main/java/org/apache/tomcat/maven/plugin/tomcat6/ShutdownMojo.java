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

import org.apache.catalina.LifecycleException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tomcat.maven.common.run.EmbeddedRegistry;


/**
 * <p>
 * Shuts down all possibly started embedded tomcat servers. This will be automatically down
 * through a shutdown hook or you may call this Mojo to shut them down explictly.
 * </p>
 * <p>
 * By default the <code>shutdown</code> goal is not bound to any phase. For integration tests
 * you might want to bind it to <code>post-integration-test</code>.
 * </p>
 *
 * @author Mark Michaelis
 * @goal shutdown
 * @requiresDependencyResolution runtime
 * @since 1.1
 */
public class ShutdownMojo
    extends AbstractI18NTomcat6Mojo
{
    /**
     * Shuts down all embedded tomcats which got started up to now.
     *
     * @throws MojoExecutionException if shutting down one or all servers failed
     */
    public void execute( )
        throws MojoExecutionException
    {
        try
        {
            EmbeddedRegistry.getInstance().shutdownAll( getLog( ) );
        }
        catch ( LifecycleException e )
        {
            throw new MojoExecutionException( messagesProvider.getMessage( "ShutdownMojo.shutdownError" ), e );
        }
    }
}
