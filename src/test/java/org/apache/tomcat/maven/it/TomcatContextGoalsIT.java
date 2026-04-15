package org.apache.tomcat.maven.it;

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

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class TomcatContextGoalsIT extends AbstractWarProjectIT {
    @Override
    protected String getWebappUrl() {
        return "http://localhost:" + getHttpItPort() + "/foo/";
    }

    @Override
    protected String getWarArtifactId() {
        return "context-goals-test";
    }

    @Test
    public void testContextGoals() throws Exception {
        final String responseBody = executeVerifyWithGet();

        assertTrue("Tomcat folder should exist in target folder of project at " + webappHome,
                new File(webappHome, "target/tomcat").exists());

        logger.info("Verifying context goals output");
        verifier.verifyTextInLog("OK - Deployed application at context path [/foo]");
        verifier.verifyTextInLog("OK - Undeployed application at context path [/foo]");
        verifier.verifyTextInLog("OK - Session information for application at context path [/foo]");
        verifier.verifyTextInLog("OK - Reloaded application at context path [/foo]");
        verifier.verifyTextInLog("OK - Stopped application at context path [/foo]");
        verifier.verifyTextInLog("OK - Started application at context path [/foo]");

        logger.info("Error Free Log check");
        verifier.verifyErrorFreeLog();
    }

    @Override
    protected int getTimeout() {
        return 40000;
    }
}