package org.apache.tomcat.maven.plugin.tomcat7.run;
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

/**
 * <p>This mojo will create a self executable jar file containing all tomcat classes.</p>
 * <p>So you will be able to use only: java -jar createjar.jar to run your webapp without need
 * to install an Apache Tomcat instance.</p>
 * <p>More details here: <a href="http://tomcat.apache.org/maven-plugin-2.0-beta-1/executable-war-jar.html">http://tomcat.apache.org/maven-plugin-2.0-beta-1/executable-war-jar.html</a></p>
 * @author Olivier Lamy
 * @goal exec-war
 * @execute phase="package"
 * @since 2.0
 */
public class ExecWarMojo
    extends AbstractExecWarMojo
{
    // no op only mojo metadatas
}
