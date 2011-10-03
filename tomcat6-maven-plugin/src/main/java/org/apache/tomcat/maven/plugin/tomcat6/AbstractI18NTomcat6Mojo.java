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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.tomcat.maven.common.messages.MessagesProvider;

/**
 * olamy: as long as maven plugin descriptor metadata generation doesn't support annotations outside of the same
 * project, we must have those fields here
 *
 * @author Olivier Lamy
 */
public abstract class AbstractI18NTomcat6Mojo
    extends AbstractMojo
{

    /**
     * @component
     */
    protected MessagesProvider messagesProvider;

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The webapp context path to use for the web application being run. This must always start with a forward-slash
     * ('/').
     *
     * @parameter expression="${maven.tomcat.path}" default-value="/${project.artifactId}"
     * @required
     */
    protected String path;


    protected String getPath( )
    {
        return path;
    }
}
