#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.webapp.test;
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

import com.thoughtworks.selenium.DefaultSelenium;
import junit.framework.TestCase;

/**
 * @author Olivier Lamy
 */
public class SimpleTest
    extends TestCase
{
    
    public void testSimple() throws Exception
    {
        
        int seleniumPort = Integer.parseInt( System.getProperty( "selenium.port", "4444" ) );
        String browser = System.getProperty( "seleniumBrowser", "*firefox" );
        String serverUrl = System.getProperty( "serverUrl", "http://localhost:9090/" );
        
        DefaultSelenium s = new DefaultSelenium( "localhost", seleniumPort, browser, serverUrl );
        s.start(  );
        s.open( "index.html" );
        s.type( "who", "foo" );
        s.click( "send-btn" );
        // wait a bit ajax response
        Thread.sleep( 1000 );
        String text = s.getText( "response" );
        assertEquals( "Hello foo", text );

    }
    
}
