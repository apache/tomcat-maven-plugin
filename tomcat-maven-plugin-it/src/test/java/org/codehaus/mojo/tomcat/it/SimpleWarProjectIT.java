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

import static junitx.framework.StringAssert.assertContains;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Mark Michaelis
 */
public class SimpleWarProjectIT
    extends AbstractWarProjectIT
{
    private static final Logger LOG = LoggerFactory.getLogger( SimpleWarProjectIT.class );

    @Override
    protected String getWebappUrl()
    {
        return "http://localhost:8080/";
    }

    @Override
    protected String getWarArtifactId()
    {
        return "simple-war-project";
    }

    @Test
    public void testIt()
        throws Exception
    {
        final String responseBody = executeVerifyWithGet();
        assertNotNull( "Received message body must not be null.", responseBody );
        assertContains( "Response must match expected content.", "It works !!", responseBody );

        assertTrue( "Tomcat folder should exist in target folder of project at " + webappHome,
                    new File( webappHome, "target/tomcat" ).exists() );
        LOG.info( "Error Free Log check" );
        verifier.verifyErrorFreeLog();
    }

}
