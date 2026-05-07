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
package org.apache.tomcat.maven.runner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * @author Olivier Lamy
 * 
 * @since 2.0
 */
public class TomcatRunnerCli {

    public static final String STAND_ALONE_PROPERTIES_FILENAME = "tomcat.standalone.properties";

    static final Option HTTP_PORT = Option.builder().longOpt("httpPort").hasArg().argName("httpPort")
            .desc("http port to use").get();

    static final Option HTTPS_PORT = Option.builder().longOpt("httpsPort").hasArg().argName("httpsPort")
            .desc("https port to use").get();

    static final Option MAX_POST_SIZE = Option.builder().longOpt("maxPostSize").hasArg().argName("maxPostSize")
            .desc("max post size to use").get();

    static final Option AJP_PORT = Option.builder().longOpt("ajpPort").hasArg().argName("ajpPort")
            .desc("ajp port to use").get();

    static final Option SERVER_XML_PATH = Option.builder().longOpt("serverXmlPath").hasArg().argName("serverXmlPath")
            .desc("server.xml to use, optional").get();

    static final Option RESET_EXTRACT = Option.builder().longOpt("resetExtract")
            .desc("clean previous extract directory").get();

    static final Option HELP = Option.builder().longOpt("help").desc("help").get();

    static final Option DEBUG = Option.builder().option("X").longOpt("debug").desc("debug").get();

    static final Option SYS_PROPS = Option.builder().option("D").hasArgs().valueSeparator().argName("key=value")
            .desc("use value for given property").get();

    static final Option CLIENT_AUTH = Option.builder().longOpt("clientAuth")
            .desc("enable client authentication for https").get();

    static final Option KEY_ALIAS = Option.builder().longOpt("keyAlias").hasArgs()
            .argName("alias from keystore for ssl").get();

    static final Option OBFUSCATE = Option.builder().longOpt("obfuscate").hasArgs().argName("password")
            .desc("obfuscate the password and exit").get();

    static final Option HTTP_PROTOCOL = Option.builder().longOpt("httpProtocol").hasArg().argName("httpProtocol")
            .desc("http protocol to use: HTTP/1.1 or org.apache.coyote.http11.Http11NioProtocol").get();

    static final Option EXTRACT_DIRECTORY = Option.builder().longOpt("extractDirectory").hasArg()
            .argName("extractDirectory").desc("path to extract war content, default value: .extract").get();

    static final Option URI_ENCODING = Option.builder().longOpt("uriEncoding").hasArg().argName("uriEncoding")
            .desc("connector uriEncoding default ISO-8859-1").get();

    static final Options OPTIONS = new Options();

    static {
        OPTIONS.addOption(HTTP_PORT).addOption(HTTPS_PORT).addOption(AJP_PORT).addOption(MAX_POST_SIZE)
                .addOption(SERVER_XML_PATH).addOption(RESET_EXTRACT).addOption(HELP).addOption(DEBUG)
                .addOption(SYS_PROPS).addOption(HTTP_PROTOCOL).addOption(CLIENT_AUTH).addOption(KEY_ALIAS)
                .addOption(OBFUSCATE).addOption(EXTRACT_DIRECTORY).addOption(URI_ENCODING);
    }


    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();
        CommandLine line = null;
        try {
            line = parser.parse(TomcatRunnerCli.OPTIONS, args);
        } catch (ParseException e) {
            System.err.println("Parsing failed.  Reason: " + e.getMessage());
            HelpFormatter formatter = HelpFormatter.builder().get();
            formatter.printHelp(getCmdLineSyntax(), "Apache Tomcat Maven plugin executable WAR",
                    TomcatRunnerCli.OPTIONS,
                    "NOTE: The command line options are disabled if using Tomcat server.xml configuration", true);
            System.exit(1);
        }

        if (line.hasOption(HELP)) {
            HelpFormatter formatter = HelpFormatter.builder().get();
            formatter.printHelp(getCmdLineSyntax(), "Apache Tomcat Maven plugin executable WAR",
                    TomcatRunnerCli.OPTIONS,
                    "NOTE: The command line options are disabled if using Tomcat server.xml configuration", true);
            System.exit(0);
        }

        if (line.hasOption(OBFUSCATE)) {
            System.out.println(PasswordUtil.obfuscate(line.getOptionValue(OBFUSCATE)));
            System.exit(0);
        }
        TomcatRunner tomcatRunner = new TomcatRunner();

        tomcatRunner.runtimeProperties = buildStandaloneProperties();

        if (line.hasOption(SERVER_XML_PATH)) {
            tomcatRunner.serverXmlPath = line.getOptionValue(SERVER_XML_PATH);
        }

        String port = tomcatRunner.runtimeProperties.getProperty(TomcatRunner.HTTP_PORT_KEY);
        if (port != null) {
            tomcatRunner.httpPort = Integer.parseInt(port);
        }

        // cli win for the port
        if (line.hasOption(HTTP_PORT)) {
            tomcatRunner.httpPort = Integer.parseInt(line.getOptionValue(HTTP_PORT));
        }

        if (line.hasOption(MAX_POST_SIZE)) {
            tomcatRunner.maxPostSize = Integer.parseInt(line.getOptionValue(MAX_POST_SIZE));
        }

        if (line.hasOption(HTTPS_PORT)) {
            tomcatRunner.httpsPort = Integer.parseInt(line.getOptionValue(HTTPS_PORT));
        }
        if (line.hasOption(AJP_PORT)) {
            tomcatRunner.ajpPort = Integer.parseInt(line.getOptionValue(AJP_PORT));
        }
        if (line.hasOption(RESET_EXTRACT)) {
            tomcatRunner.resetExtract = true;
        }
        if (line.hasOption(DEBUG)) {
            tomcatRunner.debug = true;
        }

        if (line.hasOption(HTTP_PROTOCOL)) {
            tomcatRunner.httpProtocol = line.getOptionValue(HTTP_PROTOCOL);
        }

        if (line.hasOption(SYS_PROPS)) {
            Properties systemProperties = line.getOptionProperties(SYS_PROPS);
            if (systemProperties != null && !systemProperties.isEmpty()) {
                for (Map.Entry<Object, Object> sysProp : systemProperties.entrySet()) {
                    System.setProperty((String) sysProp.getKey(), (String) sysProp.getValue());
                }
            }
        }
        if (line.hasOption(CLIENT_AUTH)) {
            tomcatRunner.clientAuth = "true";
        }
        if (line.hasOption(KEY_ALIAS)) {
            tomcatRunner.keyAlias = line.getOptionValue(KEY_ALIAS);
        }

        if (line.hasOption(EXTRACT_DIRECTORY)) {
            tomcatRunner.extractDirectory = line.getOptionValue(EXTRACT_DIRECTORY);
        }

        if (line.hasOption(URI_ENCODING)) {
            tomcatRunner.uriEncoding = line.getOptionValue(URI_ENCODING);
        }

        // here we go
        tomcatRunner.run();
    }

    private static Properties buildStandaloneProperties() throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(STAND_ALONE_PROPERTIES_FILENAME);
        Properties properties = new Properties();
        properties.load(is);
        return properties;
    }

    public static String getCmdLineSyntax() {
        return "java -jar [path to your exec war jar]";
    }
}