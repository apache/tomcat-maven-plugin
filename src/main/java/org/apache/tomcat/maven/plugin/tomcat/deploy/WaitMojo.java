package org.apache.tomcat.maven.plugin.tomcat.deploy;

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
import org.apache.tomcat.maven.common.deployer.TomcatManagerException;
import org.apache.tomcat.maven.plugin.tomcat.AbstractCatalinaMojo;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Wait for the Tomcat server to become responsive.
 *
 * @since 3.0
 */
@Mojo(name = "wait", threadSafe = true)
public class WaitMojo extends AbstractCatalinaMojo {
    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The maximum time to wait for the server to become responsive, in milliseconds.
     */
    @Parameter(property = "maven.tomcat.wait.timeout", defaultValue = "60000")
    private long timeout = 60000;

    /**
     * The interval between checks, in milliseconds.
     */
    @Parameter(property = "maven.tomcat.wait.interval", defaultValue = "1000")
    private long interval = 1000;

    /**
     * The HTTP status code to wait for.
     */
    @Parameter(property = "maven.tomcat.wait.status", defaultValue = "200")
    private int status = 200;

    // ----------------------------------------------------------------------
    // Protected Methods
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected void invokeManager() throws MojoExecutionException, TomcatManagerException, IOException {
        URL deployedURL = getDeployedURL();

        getLog().info(messagesProvider.getMessage("WaitMojo.waitingForApp", deployedURL, timeout));

        long startTime = System.currentTimeMillis();

        while (true) {
            try {
                if (isServerResponsive(deployedURL)) {
                    getLog().info(messagesProvider.getMessage("WaitMojo.appReady", deployedURL));
                    return;
                }
            } catch (IOException e) {
                getLog().debug("Server not yet responsive: " + e.getMessage());
            }

            if (System.currentTimeMillis() - startTime > timeout) {
                throw new MojoExecutionException(messagesProvider.getMessage("WaitMojo.timeout", deployedURL, timeout));
            }

            try {
                TimeUnit.MILLISECONDS.sleep(interval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MojoExecutionException("Wait interrupted", e);
            }
        }
    }

    /**
     * Checks if the server is responsive by making an HTTP request.
     *
     * @param url the URL to check
     * 
     * @return true if the server responds with the expected status code
     * 
     * @throws IOException if an I/O error occurs
     */
    private boolean isServerResponsive(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout((int) interval);
        connection.setReadTimeout((int) interval);
        connection.setRequestMethod("HEAD");

        try {
            int responseCode = connection.getResponseCode();
            return responseCode == status;
        } finally {
            connection.disconnect();
        }
    }
}