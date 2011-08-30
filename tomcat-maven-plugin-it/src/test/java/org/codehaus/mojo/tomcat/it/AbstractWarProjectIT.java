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



import java.io.File;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all tests which have a war-project using the tomcat-maven-plugin below project-resources.
 *
 * @author Mark Michaelis
 */
public abstract class AbstractWarProjectIT
{
    private static final Logger LOG = LoggerFactory.getLogger( AbstractWarProjectIT.class );

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
     * HttpClient to use to connect to the deployed web-application.
     */
    private DefaultHttpClient httpClient;

    /**
     * Helper for Maven-Integration-Tests.
     */
    protected Verifier verifier;

    /**
     * Where the war project got placed to.
     */
    protected File webappHome;

    @Before
    public void setUp()
        throws Exception
    {
        httpClient = new DefaultHttpClient();

        final HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout( params, 15000 );
        HttpConnectionParams.setSoTimeout( params, 15000 );

        webappHome = ResourceExtractor.simpleExtractResources( getClass(), "/" + getWarArtifactId() );
        verifier = new Verifier( webappHome.getAbsolutePath() );
        verifier.deleteArtifact( "org.codehaus.mojo.tomcat.it", getWarArtifactId(), "1.0-SNAPSHOT", "war" );
    }

    @After
    public void tearDown()
        throws Exception
    {
        httpClient.getConnectionManager().shutdown();
        verifier.resetStreams();
        verifier.deleteArtifact( "org.codehaus.mojo.tomcat.it", getWarArtifactId(), "1.0-SNAPSHOT", "war" );
    }

    /**
     * Executes mvn verify and retrieves the response from the web application.
     * @return the response given
     * @throws VerificationException if the verifier failed to execute the goal
     * @throws InterruptedException if the execution got interrupted in some way
     */
    protected final String executeVerifyWithGet()
        throws VerificationException, InterruptedException
    {
        final String[] responseBodies = new String[]{ null };

        final Thread thread = new Thread( "webapp-response-retriever" )
        {
            @Override
            public void run()
            {
                responseBodies[0] = getResponseBody( 15000 );
            }
        };

        thread.start();

        LOG.info( "Executing verify on " + webappHome.getAbsolutePath() );
        verifier.executeGoal( "verify" );

        thread.join();

        return responseBodies[0];
    }

    private String getResponseBody( int timeout )
    {
        String responseBody = null;
        final long startTime = System.currentTimeMillis();
        final long endTime = startTime + timeout;
        long currentTime = System.currentTimeMillis();
        try
        {
            while ( pingUrl() != 200 && currentTime < endTime )
            {
                LOG.debug( "Ping..." );
                Thread.sleep( 500 );
                currentTime = System.currentTimeMillis();
            }
            if ( currentTime < endTime )
            {
                responseBody = getResponseBody();
                LOG.debug( "Received: " + responseBody );
            }
            else
            {
                LOG.error( "Timeout met while trying to access web application." );
            }
        }
        catch ( IOException e )
        {
            LOG.error( "Exception while trying to access web application.", e );
        }
        catch ( InterruptedException e )
        {
            LOG.error( "Exception while trying to access web application.", e );
        }
        return responseBody;
    }

    private String getResponseBody()
        throws IOException
    {
        HttpGet httpGet = new HttpGet( getWebappUrl() );
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        return httpClient.execute( httpGet, responseHandler );
    }

    private int pingUrl()
    {
        final HttpHead httpHead = new HttpHead( getWebappUrl() );
        try
        {
            final HttpResponse response = httpClient.execute( httpHead );
            return response.getStatusLine().getStatusCode();
        }
        catch ( IOException e )
        {
            LOG.debug( "Ignoring exception while pinging URL " + httpHead.getURI(), e );
            return -1;
        }
    }

}
