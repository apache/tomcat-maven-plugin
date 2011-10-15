package org.apache.tomcat.maven.runner;
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

import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Catalina;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.tomcat.util.http.fileupload.FileUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * FIXME add junit for that but when https://issues.apache.org/bugzilla/show_bug.cgi?id=52028 fixed
 * Main class used to run the standalone wars in a Apache Tomcat instance.
 * @author Olivier Lamy
 * @since 2.0
 */
public class Tomcat7Runner
{
    // true/false to use the server.xml located in the jar /conf/server.xml
    public static final String USE_SERVER_XML_KEY = "useServerXml";

    // contains war name wars=foo.war,bar.war
    public static final String WARS_KEY = "wars";
    
    public int httpPort;

    public int httpsPort;

    public int ajpPort;

    public String serverXmlPath;
    
    public Properties runtimeProperties;

    public boolean resetExtract;

    public File extractDirectory = new File( ".extract" );

    Catalina container;

    Tomcat tomcat;

    public Tomcat7Runner()
    {
        // no op
    }
    
    public void run() 
        throws Exception {
        
        // do we have to extract content
        if ( !new File(".extract").exists() || resetExtract )
        {
            extract();
        }

        System.setProperty("catalina.base", extractDirectory.getAbsolutePath());
        System.setProperty("catalina.home", extractDirectory.getAbsolutePath());


        // start with a server.xml
        if ( serverXmlPath != null || Boolean.parseBoolean( runtimeProperties.getProperty( USE_SERVER_XML_KEY )) )
        {
            container = new Catalina();
            // FIXME get this from runtimeProperties ?
            //container.setUseNaming(this.useNaming);
            if ( serverXmlPath != null && new File( serverXmlPath ).exists() )
            {
                container.setConfig( serverXmlPath );
            } else
            {
                container.setConfig( new File( extractDirectory, "conf/server.xml" ).getAbsolutePath() );
            }
            container.start();                        
        } else {
            tomcat = new Tomcat();
            tomcat.getHost().setAppBase(new File(extractDirectory, "webapps").getAbsolutePath());

            Connector connector = new Connector( "HTTP/1.1" );
            connector.setPort(httpPort);

            if ( httpsPort > 0 )
            {
                connector.setRedirectPort(httpsPort);
            }
            // FIXME parameter for that def ? ISO-8859-1
            //connector.setURIEncoding(uriEncoding);

            tomcat.getService().addConnector( connector );

            tomcat.setConnector(connector);

            // add a default acces log valve
            AccessLogValve alv = new AccessLogValve();
            alv.setDirectory(new File(extractDirectory, "logs").getAbsolutePath());
            alv.setPattern("%h %l %u %t \"%r\" %s %b %I %D");
            tomcat.getHost().getPipeline().addValve(alv);


             // create https connector
             if ( httpsPort > 0 )
             {
                Connector httpsConnector = new Connector( "HTTP/1.1" );
                httpsConnector.setPort(httpsPort);
                // FIXME parameters for that !!
                /*
                if ( keystoreFile != null )
                {
                    httpsConnector.setAttribute("keystoreFile", keystoreFile);
                }
                if ( keystorePass != null )
                {
                    httpsConnector.setAttribute("keystorePass", keystorePass);
                }*/
                tomcat.getService().addConnector( httpsConnector );

             }

             // create ajp connector
             if ( ajpPort > 0 )
             {
                Connector ajpConnector = new Connector( "org.apache.coyote.ajp.AjpProtocol" );
                ajpConnector.setPort(ajpPort);
                // FIXME parameter for that def ? ISO-8859-1
                //ajpConnector.setURIEncoding(uriEncoding);
                tomcat.getService().addConnector( ajpConnector );
             }
             tomcat.start();
        }


    }
    
    public void stop()
        throws Exception
    {
        if ( container != null )
        {
            container.stop();
        }
        if ( tomcat != null )
        {
            tomcat.stop();
        }
    }
    
    protected void extract() 
        throws Exception
    {

        if (extractDirectory.exists())
        {
            FileUtils.deleteDirectory(extractDirectory);
        }
        extractDirectory.mkdirs();

        // create tomcat various paths
        new File(extractDirectory, "conf").mkdirs();
        new File(extractDirectory, "logs").mkdirs();
        new File(extractDirectory, "webapps").mkdirs();
        new File(extractDirectory, "work").mkdirs();

        String wars = runtimeProperties.getProperty( WARS_KEY );
        StringTokenizer st = new StringTokenizer( wars,";" );
        while (st.hasMoreTokens())
        {
            InputStream inputStream = null;
            try
            {
                String war = st.nextToken();
                inputStream =
                    Thread.currentThread().getContextClassLoader().getResourceAsStream( war );
                expand( inputStream, new File( extractDirectory, "webapps/" + war ) );
            } finally {
                if ( inputStream != null )
                {
                    inputStream.close();
                }
            }
        }

        // expand tomcat configuration files if there
        expandConfigurationFile( "catalina.properties", extractDirectory  );
        expandConfigurationFile( "logging.properties", extractDirectory  );
        expandConfigurationFile( "tomcat-users.xml", extractDirectory  );
        expandConfigurationFile( "catalina.policy", extractDirectory  );
        expandConfigurationFile( "context.xml", extractDirectory  );
        expandConfigurationFile( "server.xml", extractDirectory  );
        expandConfigurationFile( "web.xml", extractDirectory  );
        
    }
    
    private static void expandConfigurationFile(String fileName, File extractDirectory)
        throws Exception
    {
        InputStream inputStream = null;
        try
        {
            inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream( "conf/" + fileName );
            if (inputStream != null)
            {
                expand( inputStream, new File( extractDirectory, "conf/" + fileName ) );
            }
        } finally {
            if (inputStream != null)
            {
                inputStream.close();
            }
        }

    }

    /**
     * Expand the specified input stream into the specified file.
     *
     * @param input InputStream to be copied
     * @param file The file to be created
     *
     * @exception java.io.IOException if an input/output error occurs
     */
    private static void expand(InputStream input, File file)
        throws IOException
    {
        BufferedOutputStream output = null;
        try {
            output =
                new BufferedOutputStream(new FileOutputStream(file));
            byte buffer[] = new byte[2048];
            while (true) {
                int n = input.read(buffer);
                if (n <= 0)
                    break;
                output.write(buffer, 0, n);
            }
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

}
