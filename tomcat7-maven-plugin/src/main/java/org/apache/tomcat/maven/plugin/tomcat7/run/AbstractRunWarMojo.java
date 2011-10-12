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

import java.io.File;

/**
 * Runs the current project as a packaged web application using an embedded Tomcat server.
 * 
 * @requiresDependencyResolution runtime
 * @author Mark Hobson <markhobson@gmail.com>
 * @version $Id: AbstractRunWarMojo.java 12852 2010-10-12 22:04:32Z thragor $
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
     * 
     * @parameter expression = "${project.build.directory}/${project.build.finalName}"
     * @required
     */
    private File warDirectory;

    /**
     * The path of the Tomcat context XML file.
     * 
     * @parameter expression =
     *            "${project.build.directory}/${project.build.finalName}/META-INF/context.xml"
     */
    private File contextFile;

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
