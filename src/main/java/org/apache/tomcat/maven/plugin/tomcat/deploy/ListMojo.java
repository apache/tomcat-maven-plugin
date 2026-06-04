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
package org.apache.tomcat.maven.plugin.tomcat.deploy;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.tomcat.maven.common.deployer.TomcatManagerException;
import org.apache.tomcat.maven.common.deployer.TomcatManagerResponse;
import org.apache.tomcat.maven.plugin.tomcat.AbstractCatalinaMojo;

import java.io.IOException;

/**
 * List all web applications currently running in Tomcat.
 * Connects to the Tomcat manager and lists all deployed applications with their status.
 *
 * @since 3.0
 */
@Mojo(name = "list", threadSafe = true)
public class ListMojo extends AbstractCatalinaMojo {
    /**
     * Creates an instance of ListMojo.
     */
    public ListMojo() {
        // default constructor
    }
    // ----------------------------------------------------------------------
    // Protected Methods
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected void invokeManager() throws MojoExecutionException, TomcatManagerException, IOException {
        getLog().info(messagesProvider.getMessage("ListMojo.listApps", getURL()));

        TomcatManagerResponse tomcatResponse = getManager().list();
        checkTomcatResponse(tomcatResponse);
        log(tomcatResponse.getHttpResponseBody());
    }
}