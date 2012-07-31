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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Abstract goal that provides common configuration for Catalina-based goals.
 *
 * @author Mark Hobson <markhobson@gmail.com>
 */
public abstract class AbstractWarCatalinaMojo
    extends AbstractCatalinaMojo
{
    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The packaging of the Maven project that this goal operates upon.
     */
    @Parameter( defaultValue = "${project.packaging}", required = true, readonly = true )
    private String packaging;

    /**
     * If set to true ignore if packaging of project is not 'war'.
     *
     * @since 1.1
     */
    @Parameter( property = "tomcat.ignorePackaging", defaultValue = "false" )
    private boolean ignorePackaging;

    // ----------------------------------------------------------------------
    // Mojo Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute()
        throws MojoExecutionException
    {
        if ( !isWar() )
        {
            getLog().info( messagesProvider.getMessage( "AbstractWarCatalinaMojo.nonWar" ) );
            return;
        }

        super.execute();
    }

    // ----------------------------------------------------------------------
    // Protected Methods
    // ----------------------------------------------------------------------

    /**
     * Gets whether this project uses WAR packaging.
     *
     * @return whether this project uses WAR packaging
     */
    protected boolean isWar()
    {
        return "war".equals( packaging ) || ignorePackaging;
    }
}
