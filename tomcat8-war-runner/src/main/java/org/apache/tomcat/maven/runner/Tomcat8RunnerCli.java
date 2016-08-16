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
import java.util.Map;
import java.util.Properties;

/**
 * @author Olivier Lamy
 * @since 2.0
 */
@SuppressWarnings( "static-access" )
public class Tomcat8RunnerCli
{

    public static final String STAND_ALONE_PROPERTIES_FILENAME = "tomcat.standalone.properties";

    static Option httpPort =
        OptionBuilder.withArgName( "httpPort" ).hasArg().withDescription( "http port to use" ).create( "httpPort" );

    static Option httpsPort =
        OptionBuilder.withArgName( "httpsPort" ).hasArg().withDescription( "https port to use" ).create( "httpsPort" );

    static Option maxPostSize =
        OptionBuilder.withArgName( "maxPostSize" ).hasArg().withDescription( "max post size to use" ).create(
            "maxPostSize" );

    static Option ajpPort =
        OptionBuilder.withArgName( "ajpPort" ).hasArg().withDescription( "ajp port to use" ).create( "ajpPort" );

    static Option serverXmlPath =
        OptionBuilder.withArgName( "serverXmlPath" ).hasArg().withDescription( "server.xml to use, optional" ).create(
            "serverXmlPath" );

    static Option resetExtract =
        OptionBuilder.withArgName( "resetExtract" ).withDescription( "clean previous extract directory" ).create(
            "resetExtract" );

    static Option help = OptionBuilder.withLongOpt( "help" ).withDescription( "help" ).create( 'h' );

    static Option debug = OptionBuilder.withLongOpt( "debug" ).withDescription( "debug" ).create( 'X' );

    static Option sysProps = OptionBuilder.withDescription( "use value for given property" ).hasArgs().withDescription(
        "key=value" ).withValueSeparator().create( 'D' );

    static Option clientAuth =
        OptionBuilder.withArgName( "clientAuth" ).withDescription( "enable client authentication for https" ).create(
            "clientAuth" );

    static Option keyAlias =
        OptionBuilder.withArgName( "keyAlias" ).hasArgs().withDescription( "alias from keystore for ssl" ).create(
            "keyAlias" );

    static Option obfuscate =
        OptionBuilder.withArgName( "password" ).hasArgs().withDescription( "obfuscate the password and exit" ).create(
            "obfuscate" );

    static Option httpProtocol = OptionBuilder.withArgName( "httpProtocol" ).hasArg().withDescription(
        "http protocol to use: HTTP/1.1 or org.apache.coyote.http11.Http11NioProtocol" ).create( "httpProtocol" );

    static Option extractDirectory = OptionBuilder.withArgName( "extractDirectory" ).hasArg().withDescription(
        "path to extract war content, default value: .extract" ).create( "extractDirectory" );

    static Option loggerName = OptionBuilder.withArgName( "loggerName" ).hasArg().withDescription(
        "logger to use: slf4j to use slf4j bridge on top of jul" ).create( "loggerName" );

    static Option uriEncoding = OptionBuilder.withArgName( "uriEncoding" ).hasArg().withDescription(
        "connector uriEncoding default ISO-8859-1" ).create( "uriEncoding" );

    static Options options = new Options();

    static
    {
        options.addOption( httpPort ) //
            .addOption( httpsPort ) //
            .addOption( ajpPort ) //
            .addOption( serverXmlPath ) //
            .addOption( resetExtract ) //
            .addOption( help ) //
            .addOption( debug ) //
            .addOption( sysProps ) //
            .addOption( httpProtocol ) //
            .addOption( clientAuth ) //
            .addOption( keyAlias ) //
            .addOption( obfuscate ) //
            .addOption( extractDirectory ) //
            .addOption( loggerName ) //
            .addOption( uriEncoding ) //
            .addOption( maxPostSize );
    }


    public static void main( String[] args )
        throws Exception
    {
        CommandLineParser parser = new GnuParser();
        CommandLine line = null;
        try
        {
            line = parser.parse( Tomcat8RunnerCli.options, args );
        }
        catch ( ParseException e )
        {
            System.err.println( "Parsing failed.  Reason: " + e.getMessage() );
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( getCmdLineSyntax(), Tomcat8RunnerCli.options );
            System.exit( 1 );
        }

        if ( line.hasOption( help.getOpt() ) )
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( getCmdLineSyntax(), Tomcat8RunnerCli.options );
            System.exit( 0 );
        }

        if ( line.hasOption( obfuscate.getOpt() ) )
        {
            System.out.println( PasswordUtil.obfuscate( line.getOptionValue( obfuscate.getOpt() ) ) );
            System.exit( 0 );
        }
        Tomcat8Runner tomcat8Runner = new Tomcat8Runner();

        tomcat8Runner.runtimeProperties = buildStandaloneProperties();

        if ( line.hasOption( serverXmlPath.getOpt() ) )
        {
            tomcat8Runner.serverXmlPath = line.getOptionValue( serverXmlPath.getOpt() );
        }

        String port = tomcat8Runner.runtimeProperties.getProperty( Tomcat8Runner.HTTP_PORT_KEY );
        if ( port != null )
        {
            tomcat8Runner.httpPort = Integer.parseInt( port );
        }

        // cli win for the port
        if ( line.hasOption( httpPort.getOpt() ) )
        {
            tomcat8Runner.httpPort = Integer.parseInt( line.getOptionValue( httpPort.getOpt() ) );
        }

        if ( line.hasOption( maxPostSize.getOpt() ) )
        {
            tomcat8Runner.maxPostSize = Integer.parseInt( line.getOptionValue( maxPostSize.getOpt() ) );
        }

        if ( line.hasOption( httpsPort.getOpt() ) )
        {
            tomcat8Runner.httpsPort = Integer.parseInt( line.getOptionValue( httpsPort.getOpt() ) );
        }
        if ( line.hasOption( ajpPort.getOpt() ) )
        {
            tomcat8Runner.ajpPort = Integer.parseInt( line.getOptionValue( ajpPort.getOpt() ) );
        }
        if ( line.hasOption( resetExtract.getOpt() ) )
        {
            tomcat8Runner.resetExtract = true;
        }
        if ( line.hasOption( debug.getOpt() ) )
        {
            tomcat8Runner.debug = true;
        }

        if ( line.hasOption( httpProtocol.getOpt() ) )
        {
            tomcat8Runner.httpProtocol = line.getOptionValue( httpProtocol.getOpt() );
        }

        if ( line.hasOption( sysProps.getOpt() ) )
        {
            Properties systemProperties = line.getOptionProperties( sysProps.getOpt() );
            if ( systemProperties != null && !systemProperties.isEmpty() )
            {
                for ( Map.Entry<Object, Object> sysProp : systemProperties.entrySet() )
                {
                    System.setProperty( (String) sysProp.getKey(), (String) sysProp.getValue() );
                }
            }
        }
        if ( line.hasOption( clientAuth.getOpt() ) )
        {
            tomcat8Runner.clientAuth = clientAuth.getOpt();
        }
        if ( line.hasOption( keyAlias.getOpt() ) )
        {
            tomcat8Runner.keyAlias = line.getOptionValue( keyAlias.getOpt() );
        }

        if ( line.hasOption( extractDirectory.getOpt() ) )
        {
            tomcat8Runner.extractDirectory = line.getOptionValue( extractDirectory.getOpt() );
        }

        if ( line.hasOption( loggerName.getOpt() ) )
        {
            tomcat8Runner.loggerName = line.getOptionValue( loggerName.getOpt() );
        }

        if ( line.hasOption( uriEncoding.getOpt() ) )
        {
            tomcat8Runner.uriEncoding = line.getOptionValue( uriEncoding.getOpt() );
        }

        // here we go
        tomcat8Runner.run();
    }

    private static Properties buildStandaloneProperties()
        throws IOException
    {
        InputStream is =
            Thread.currentThread().getContextClassLoader().getResourceAsStream( STAND_ALONE_PROPERTIES_FILENAME );
        Properties properties = new Properties();
        properties.load( is );
        return properties;
    }

    public static String getCmdLineSyntax()
    {
        return "java -jar [path to your exec war jar]";
    }
}
