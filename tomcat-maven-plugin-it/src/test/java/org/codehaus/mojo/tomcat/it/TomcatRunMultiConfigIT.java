package org.codehaus.mojo.tomcat.it;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import static junitx.framework.StringAssert.assertContains;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests a bunch of configuration options for the tomcat-run Mojo.
 *
 * @author Mark Michaelis
 */
public final class TomcatRunMultiConfigIT
    extends AbstractWarProjectIT
{
    private static final Logger LOG = LoggerFactory.getLogger( TomcatRunMultiConfigIT.class );

    private static final String URL_QUERY = "\u3053\u3093\u306b\u3061\u306f";

    /**
     * ArtifactId of the sample WAR project.
     */
    private static final String WAR_ARTIFACT_ID = "tomcat-run-multi-config";

    @Override
    protected String getWebappUrl()
    {
        try
        {
            return new URI( "http://localhost:8001/multi-config/index.jsp?string=" + URL_QUERY ).toASCIIString();
        }
        catch ( URISyntaxException e )
        {
            LOG.error( "An exception occurred.", e );
            return "http://localhost:8001/multi-config";
        }
    }

    @Override
    protected String getWarArtifactId()
    {
        return WAR_ARTIFACT_ID;
    }

    @Test
    public void testIt()
        throws Exception
    {
        final String responseBody = executeVerifyWithGet();
        assertNotNull( "Received message body from " + getWebappUrl() + " must not be null.", responseBody );
        assertContains( "Response from " + getWebappUrl() + " must match expected content.", URL_QUERY, responseBody );

        final File tomcatFolder = new File( webappHome, "target/tc" );
        final File emptyLocation = new File( tomcatFolder, "conf/empty.txt" );

        assertTrue(
            "Tomcat folder \"" + tomcatFolder.getAbsolutePath() + "\" should exist in target folder of project at " +
                webappHome, tomcatFolder.exists() );
        assertTrue(
            "File \"" + emptyLocation.getAbsolutePath() + "\" should have been copied from tcconf to tomcat/conf",
            emptyLocation.exists() );

        LOG.info( "Error Free Log check" );
        verifier.verifyErrorFreeLog();
    }

}
