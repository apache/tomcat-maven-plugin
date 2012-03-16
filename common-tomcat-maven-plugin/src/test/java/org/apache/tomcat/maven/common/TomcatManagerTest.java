package org.apache.tomcat.maven.common;
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

import junit.framework.TestCase;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.IOUtils;
import org.apache.tomcat.maven.common.deployer.TomcatManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class TomcatManagerTest
    extends TestCase
{

    Tomcat tomcat;

    UploadServlet uploadServlet;

    int port;

    public static String getBasedir()
    {
        return System.getProperty( "basedir" );
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        tomcat = new Tomcat();
        tomcat.setBaseDir( System.getProperty( "java.io.tmpdir" ) );
        tomcat.setPort( 0 );

        Context context = tomcat.addContext( "", System.getProperty( "java.io.tmpdir" ) );
        uploadServlet = new UploadServlet();
        tomcat.addServlet( context, "foo", uploadServlet );
        context.addServletMapping( "/*", "foo" );

        tomcat.start();

        port = tomcat.getConnector().getLocalPort();

        System.out.println( "Tomcat started on port:" + port );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
        tomcat.stop();
    }


    public void testDeployWar()
        throws Exception
    {
        TomcatManager tomcatManager = new TomcatManager( new URL( "http://localhost:" + this.port + "/foo/bar" ) );
        tomcatManager.deploy( "foo", new FileInputStream( new File( getBasedir(), "src/test/resources/test.txt" ) ) );
        StringWriter sw = new StringWriter();
        assertEquals( 1, uploadServlet.uploadedResources.size() );
        FileInputStream fileInputStream = new FileInputStream( uploadServlet.uploadedResources.get( 0 ).uploadedFile );
        try
        {
            IOUtils.copy( fileInputStream, sw );
            assertTrue( sw.toString().contains( "Apache Tomcat rocks!!" ) );
        }
        finally
        {
            fileInputStream.close();
        }
    }

    //-----------------------------
    // internal for tests
    //-----------------------------

    public class UploadedResource
    {
        public String requestUri;

        public File uploadedFile;

        public UploadedResource( String requestUri, File uploadedFile )
        {
            this.requestUri = requestUri;
            this.uploadedFile = uploadedFile;
        }
    }

    public class UploadServlet
        extends HttpServlet
    {

        public List<UploadedResource> uploadedResources = new ArrayList<UploadedResource>();

        @Override
        protected void doPut( HttpServletRequest req, HttpServletResponse resp )
            throws ServletException, IOException
        {
            System.out.println( "put ok:" + req.getRequestURI() );
            super.doPut( req, resp );
            File file = File.createTempFile( "tomcat-unit-test", "tmp" );
            uploadedResources.add( new UploadedResource( req.getRequestURI(), file ) );
            IOUtils.copy( req.getInputStream(), new FileOutputStream( file ) );
        }
    }
}
