package org.apache.tomcat.maven.plugin.tomcat7.run;

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
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.tomcat.maven.common.run.EmbeddedRegistry;
import org.apache.tomcat.maven.plugin.tomcat7.AbstractTomcat7Mojo;


/**
 * <p>
 * Shuts down all possibly started embedded Tomcat servers. This will be automatically done
 * through a shutdown hook or you may call this Mojo to shut them down explictly.
 * </p>
 * <p>
 * By default the <code>shutdown</code> goal is not bound to any phase. For integration tests
 * you might want to bind it to <code>post-integration-test</code>.
 * </p>
 *
 * @author Mark Michaelis
 * @since 2.0
 */
@Mojo( name = "shutdown", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true )
public class ShutdownMojo
    extends AbstractTomcat7Mojo
{

    /**
     * Ignore error when shutdown
     *
     * @since 2.0
     */
    @Parameter( property = "maven.tomcat.skipErrorOnShutdown", defaultValue = "false" )
    protected boolean skipErrorOnShutdown;

    /**
     * Skip execution
     *
     * @since 2.0
     */
    @Parameter( property = "maven.tomcat.skipShutdown", defaultValue = "false" )
    protected boolean skip;

    /**
     * Shuts down all embedded tomcats which got started up to now.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     *          if shutting down one or all servers failed
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( skip )
        {
            getLog().info( "skip execution" );
            return;
        }
        try
        {
            EmbeddedRegistry.getInstance().shutdownAll( getLog() );
        }
        catch ( Exception e )
        {
            if ( !skipErrorOnShutdown )
            {
                throw new MojoExecutionException( messagesProvider.getMessage( "ShutdownMojo.shutdownError" ), e );
            }
        }
    }
}
