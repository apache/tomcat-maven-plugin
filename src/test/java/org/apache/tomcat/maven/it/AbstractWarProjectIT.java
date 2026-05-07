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
package org.apache.tomcat.maven.it;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.After;
import org.junit.Before;

/**
 * Base class for all tests which have a war-project using the tomcat-maven-plugin below project-resources.
 *
 * @author Mark Michaelis
 */
public abstract class AbstractWarProjectIT {
    protected Logger logger = Logger.getLogger(getClass().getName());

    /**
     * This URL will be queried for content. It will also be used to wait for the startup of the webapp.
     *
     * @return the URL to ping
     */
    protected abstract String getWebappUrl();

    /**
     * Artifact ID of the war project. Needed to uninstall any artifacts.
     *
     * @return artifact ID of the war project under test
     */
    protected abstract String getWarArtifactId();

    /**
     * Helper for Maven-Integration-Tests.
     */
    protected Verifier verifier;

    /**
     * Where the war project got placed to.
     */
    protected File webappHome;

    @Before
    public void setUp() throws Exception {

        webappHome = ResourceExtractor.simpleExtractResources(getClass(), "/" + getWarArtifactId());
        verifier = new Verifier(webappHome.getAbsolutePath());

        boolean debugVerifier = Boolean.getBoolean("verifier.maven.debug");

        verifier.setMavenDebug(debugVerifier);
        verifier.setDebugJvm(Boolean.getBoolean("verifier.debugJvm"));
        verifier.displayStreamBuffers();

        verifier.deleteArtifact("org.apache.tomcat.maven.it", getWarArtifactId(), "1.0-SNAPSHOT", "war");
    }

    @After
    public void tearDown() throws Exception {
        verifier.resetStreams();
        verifier.deleteArtifact("org.apache.tomcat.maven.it", getWarArtifactId(), "1.0-SNAPSHOT", "war");
    }

    /**
     * Executes mvn verify and retrieves the response from the web application.
     *
     * @return the response given
     * 
     * @throws VerificationException if the verifier failed to execute the goal
     * @throws InterruptedException  if the execution got interrupted in some way
     */
    protected final String executeVerifyWithGet() throws VerificationException, InterruptedException, IOException {
        final String[] responseBodies = new String[] { null };

        final Thread thread = new Thread("webapp-response-retriever") {
            @Override
            public void run() {
                responseBodies[0] = getResponseBody(getTimeout());
            }
        };

        thread.start();

        logger.info("Executing verify on " + webappHome.getAbsolutePath());

        verifier.setCliOptions(getCliOptions());

        verifier.executeGoal(getGoal());

        verifier.displayStreamBuffers();

        thread.join();

        return responseBodies[0];
    }


    protected String getGoal() {
        return "verify";
    }

    protected List<String> getCliOptions() {
        return Collections.emptyList();
    }

    private String getResponseBody(int timeout) {
        String responseBody = null;
        final long startTime = System.currentTimeMillis();
        final long endTime = startTime + timeout;
        long currentTime = System.currentTimeMillis();
        try {
            while (pingUrl() != 200 && currentTime < endTime) {
                logger.fine("Ping...");
                Thread.sleep(500);
                currentTime = System.currentTimeMillis();
            }
            if (currentTime < endTime) {
                responseBody = getResponseBody();
                logger.fine("Received: " + responseBody);
            } else {
                logger.severe("Timeout met while trying to access web application.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception while trying to access web application.", e);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Exception while trying to access web application.", e);
        }
        return responseBody;
    }

    private String getResponseBody() throws IOException {
        URL url;
        try {
            url = new URI(getWebappUrl()).toURL();
        } catch (URISyntaxException e) {
            throw new MalformedURLException(e.getMessage());
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(getTimeout());
        connection.setReadTimeout(getTimeout());
        try (InputStream is = connection.getInputStream()) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } finally {
            connection.disconnect();
        }
    }

    private int pingUrl() {
        final URL url;
        try {
            url = new URI(getWebappUrl()).toURL();
        } catch (IOException | URISyntaxException e) {
            logger.log(Level.FINE, "Ignoring exception while pinging URL " + getWebappUrl(), e);
            return -1;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(getTimeout());
            connection.setReadTimeout(getTimeout());
            return connection.getResponseCode();
        } catch (IOException e) {
            logger.log(Level.FINE, "Ignoring exception while pinging URL " + getWebappUrl(), e);
            return -1;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    protected int getTimeout() {
        return 15000;
    }

    protected static String getHttpItPort() {
        return System.getProperty("its.http.port");
    }

    protected static String getHttpsItPort() {
        return System.getProperty("its.https.port");
    }

    protected static String getAjpItPort() {
        return System.getProperty("its.ajp.port");
    }

}