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

/**
 * Abstract goal that provides common configuration for Catalina-based goals.
 * 
 * @author Mark Hobson <markhobson@gmail.com>
 * @version $Id: AbstractWarCatalinaMojo.java 12852 2010-10-12 22:04:32Z thragor $
 */
public abstract class AbstractWarCatalinaMojo
    extends AbstractCatalinaMojo
{
    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The packaging of the Maven project that this goal operates upon.
     * 
     * @parameter expression = "${project.packaging}"
     * @required
     * @readonly
     */
    private String packaging;

    /**
     * If set to true ignore if packaging of project is not 'war'.
     * @since 1.1
     * @parameter expression="${tomcat.ignorePackaging}" default-value="false"
     */
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
