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

import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * Runs the current project as a packaged web application using an embedded Tomcat server.
 *
 * @author Mark Hobson <markhobson@gmail.com>
 * @todo depend on war:exploded when MNG-1649 resolved
 */
public abstract class AbstractRunWarMojo
    extends AbstractRunMojo
{
    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The path of the exploded WAR directory to run.
     */
    @Parameter( property = "maven.tomcat.warDirectory", defaultValue = "${project.build.directory}/${project.build.finalName}", required = true )
    private File warDirectory;

    // ----------------------------------------------------------------------
    // AbstractRunMojo Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected File getDocBase()
    {
        return warDirectory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected File getContextFile()
    {
        return contextFile;
    }
}
