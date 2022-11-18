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
import org.apache.tomcat.maven.common.deployer.TomcatManagerResponse;

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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class TomcatManagerTest
    extends TestCase
{

    Tomcat tomcat;

    Tomcat redirectTomcat;

    UploadServlet uploadServlet;

    RedirectServlet redirectServlet;

    int port;

    int redirectPort;

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

        redirectTomcat = new Tomcat();
        redirectTomcat.setBaseDir( System.getProperty( "java.io.tmpdir" ) );
        redirectTomcat.setPort( 0 );
        context = redirectTomcat.addContext( "", System.getProperty( "java.io.tmpdir" ) );
        redirectServlet = new RedirectServlet();
        redirectTomcat.addServlet( context, "foo", redirectServlet );
        context.addServletMapping( "/*", "foo" );
        redirectTomcat.start();
        redirectPort = redirectTomcat.getConnector().getLocalPort();

        System.out.println( "redirect Tomcat started on port:" + redirectPort );
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
        uploadServlet.uploadedResources.clear();
        TomcatManager tomcatManager = new TomcatManager( new URL( "http://localhost:" + this.port + "/foo/bar" ) );
        TomcatManagerResponse response =
            tomcatManager.deploy( "foo", new File( getBasedir(), "src/test/resources/test.txt" ) );

        assertEquals( 200, response.getStatusCode() );

        assertEquals( 1, uploadServlet.uploadedResources.size() );
        assertEquals( "/foo/bar/deploy", uploadServlet.uploadedResources.get( 0 ).requestUri );
        FileInputStream fileInputStream = new FileInputStream( uploadServlet.uploadedResources.get( 0 ).uploadedFile );
        try
        {
            StringWriter sw = new StringWriter();
            IOUtils.copy( fileInputStream, sw );
            assertTrue( sw.toString().contains( "Apache Tomcat rocks!!" ) );
        }
        finally
        {
            fileInputStream.close();
        }
    }

    public void testDeployWarWithRedirect()
        throws Exception
    {
        uploadServlet.uploadedResources.clear();
        TomcatManager tomcatManager =
            new TomcatManager( new URL( "http://localhost:" + this.redirectPort + "/foo/bar" ) );
        redirectServlet.redirectPath = "http://localhost:" + this.port + "/foo/bar/redirected";
        TomcatManagerResponse response =
            tomcatManager.deploy( "foo", new File( getBasedir(), "src/test/resources/test.txt" ) );

        assertEquals( 200, response.getStatusCode() );

        assertEquals( "no request to redirect servlet", 1, redirectServlet.uploadedResources.size() );
        assertEquals( "/foo/bar/deploy", redirectServlet.uploadedResources.get( 0 ).requestUri );
        assertEquals( "no  redirected request to upload servlet", 1, uploadServlet.uploadedResources.size() );

        assertEquals( "/foo/bar/deploy", redirectServlet.uploadedResources.get( 0 ).requestUri );

        FileInputStream fileInputStream = new FileInputStream( uploadServlet.uploadedResources.get( 0 ).uploadedFile );
        try
        {
            StringWriter sw = new StringWriter();
            IOUtils.copy( fileInputStream, sw );
            assertTrue( sw.toString().contains( "Apache Tomcat rocks!!" ) );
        }
        finally
        {
            fileInputStream.close();
        }
    }

    public void testDeployWarWithRedirectRelative()
        throws Exception
    {
        uploadServlet.uploadedResources.clear();
        TomcatManager tomcatManager =
            new TomcatManager( new URL( "http://localhost:" + this.redirectPort + "/foo/bar" ) );
        redirectServlet.redirectPath = "redirectrelative/foo";
        TomcatManagerResponse response =
            tomcatManager.deploy( "foo", new File( getBasedir(), "src/test/resources/test.txt" ) );

        assertEquals( 200, response.getStatusCode() );

        assertEquals( "no request to redirect servlet", 2, redirectServlet.uploadedResources.size() );
        assertEquals( "/foo/bar/deploy", redirectServlet.uploadedResources.get( 0 ).requestUri );
        assertEquals( "found redirected request to upload servlet", 0, uploadServlet.uploadedResources.size() );

        assertEquals( "/foo/bar/deploy", redirectServlet.uploadedResources.get( 0 ).requestUri );

        FileInputStream fileInputStream =
            new FileInputStream( redirectServlet.uploadedResources.get( 1 ).uploadedFile );
        try
        {
            StringWriter sw = new StringWriter();
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
            File file = Files.createTempFile( "tomcat-unit-test", "tmp" ).toFile();
            uploadedResources.add( new UploadedResource( req.getRequestURI(), file ) );
            IOUtils.copy( req.getInputStream(), new FileOutputStream( file ) );
        }
    }

    public class RedirectServlet
        extends HttpServlet
    {
        int redirectPort = 0;

        String redirectPath;

        public List<UploadedResource> uploadedResources = new ArrayList<UploadedResource>();

        @Override
        protected void doPut( HttpServletRequest req, HttpServletResponse resp )
            throws ServletException, IOException
        {
            System.out.println( "RedirectServlet put ok:" + req.getRequestURI() );
            if ( req.getRequestURI().contains( "redirectrelative" ) )
            {
                File file = Files.createTempFile( "tomcat-unit-test", "tmp" ).toFile();
                uploadedResources.add( new UploadedResource( req.getRequestURI(), file ) );
                IOUtils.copy( req.getInputStream(), new FileOutputStream( file ) );
                return;
            }
            uploadedResources.add( new UploadedResource( req.getRequestURI(), null ) );
            String redirectUri =
                redirectPort > 0 ? "http://localhost:" + redirectPort + "/" + redirectPath : redirectPath;
            resp.sendRedirect( redirectUri );
        }
    }
}
