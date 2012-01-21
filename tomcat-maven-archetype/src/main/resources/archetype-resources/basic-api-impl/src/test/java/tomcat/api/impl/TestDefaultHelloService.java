#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.tomcat.api.impl;

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

import ${package}.tomcat.api.HelloService;

import junit.framework.TestCase;
import org.apache.catalina.Context;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.startup.Tomcat;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.web.context.ContextLoaderListener;

/**
 * @author Olivier Lamy
 */
@RunWith( JUnit4.class )
public class TestDefaultHelloService
    extends TestCase
{
    int port;

    private Tomcat tomcat;

    @Before
    public void startTomcat()
        throws Exception
    {
        tomcat = new Tomcat();
        tomcat.setBaseDir( System.getProperty( "java.io.tmpdir" ) );
        tomcat.setPort( 0 );

        Context context = tomcat.addContext( "", System.getProperty( "java.io.tmpdir" ) );

        ApplicationParameter applicationParameter = new ApplicationParameter();
        applicationParameter.setName( "contextConfigLocation" );
        applicationParameter.setValue( getSpringConfigLocation() );
        context.addApplicationParameter( applicationParameter );

        context.addApplicationListener( ContextLoaderListener.class.getName() );

        Tomcat.addServlet( context, "cxf", new CXFServlet() );
        context.addServletMapping( "/" + getRestServicesPath() + "/*", "cxf" );

        tomcat.start();

        port = tomcat.getConnector().getLocalPort();

        System.out.println("Tomcat started on port:"+port);
    }

    @After
    public void stopTomcat()
        throws Exception
    {
        tomcat.stop();
    }

    protected String getRestServicesPath()
    {
        return "foo";
    }

    protected String getSpringConfigLocation()
    {
        return "classpath*:META-INF/spring-context.xml";
    }

    @Test
    public void testSayHello()
    {
        HelloService service =
            JAXRSClientFactory.create( "http://localhost:" + port + "/" + getRestServicesPath() + "/testServices/",
                                       HelloService.class );
        String who = "foo";
        assertEquals( "Hello "+who, service.sayHello( who ) );
    }
}
