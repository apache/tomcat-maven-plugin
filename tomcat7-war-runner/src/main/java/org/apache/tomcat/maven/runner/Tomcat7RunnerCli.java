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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Olivier Lamy
 * @since 2.0
 */
public class Tomcat7RunnerCli
{

    public static final String STAND_ALONE_PROPERTIES_FILENAME = "tomcat.standalone.properties";
    
    static Option httpPort = OptionBuilder.withArgName("httpPort")
                                    .hasArg()
                                    .withDescription("http port to use")
                                    .create("httpPort");

    static Option httpsPort = OptionBuilder.withArgName("httpsPort")
                                    .hasArg()
                                    .withDescription("https port to use")
                                    .create("httpsPort");

    static Option ajpPort = OptionBuilder.withArgName("ajpPort")
                                    .hasArg()
                                    .withDescription("ajp port to use")
                                    .create("ajpPort");

    static Option serverXmlPath = OptionBuilder.withArgName("serverXmlPath")
                                    .hasArg()
                                    .withDescription("server.xml to use, optionnal")
                                    .create("serverXmlPath");

    static Option resetExtract = OptionBuilder.withArgName("resetExtract")
                                    .withDescription("clean previous extract directory")
                                    .create("resetExtract");

    static Option help = OptionBuilder
                                    .withLongOpt( "help" )
                                    .withDescription("help")
                                    .create("h");

    static Option debug = OptionBuilder
                                    .withLongOpt( "debug" )
                                    .withDescription("debug")
                                    .create("x");

    static Options options = new Options();

    static
    {
        options.addOption( httpPort ).addOption( httpsPort ).addOption( ajpPort ).addOption( serverXmlPath )
                .addOption( resetExtract ).addOption( help ).addOption( debug );
    }


    public static void main( String[] args )
        throws Exception
    {
        CommandLineParser parser = new GnuParser();
        CommandLine line = null;
        try
        {
            line = parser.parse(Tomcat7RunnerCli.options, args);
        } catch ( ParseException e )
        {
            System.err.println( "Parsing failed.  Reason: " + e.getMessage() );
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( getCmdLineSyntax(), Tomcat7RunnerCli.options);
            System.exit( 1 );
        }

        if ( line.hasOption( help.getOpt() ))
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( getCmdLineSyntax(), Tomcat7RunnerCli.options);
            System.exit( 0 );
        }


        Tomcat7Runner tomcat7Runner = new Tomcat7Runner();

        tomcat7Runner.runtimeProperties = buildStandaloneProperties();

        if (line.hasOption( serverXmlPath.getOpt() ))
        {
            tomcat7Runner.serverXmlPath = line.getOptionValue( serverXmlPath.getOpt() ) ;
        }
        if (line.hasOption( httpPort.getOpt() ))
        {
            tomcat7Runner.httpPort = Integer.parseInt( line.getOptionValue( httpPort.getOpt() ) );
        }

        if (line.hasOption( httpsPort.getOpt() ))
        {
            tomcat7Runner.httpsPort = Integer.parseInt( line.getOptionValue( httpsPort.getOpt() ) );
        }
        if (line.hasOption( ajpPort.getOpt() ))
        {
            tomcat7Runner.ajpPort = Integer.parseInt( line.getOptionValue( ajpPort.getOpt() ) );
        }
        if ( line.hasOption( resetExtract.getOpt() ))
        {
            tomcat7Runner.resetExtract = true;
        }
        if ( line.hasOption( debug.getOpt() ) )
        {
            tomcat7Runner.debug = true;
        }
        // here we go
        tomcat7Runner.run();
    }
    
    private static Properties buildStandaloneProperties()
        throws IOException
    {
        InputStream is =
            Thread.currentThread().getContextClassLoader().getResourceAsStream( STAND_ALONE_PROPERTIES_FILENAME );
        Properties properties = new Properties( );
        properties.load( is );
        return properties;
    }
    
    public static String getCmdLineSyntax()
    {
        return "java -jar [path to your exec war jar]";
    }
}
