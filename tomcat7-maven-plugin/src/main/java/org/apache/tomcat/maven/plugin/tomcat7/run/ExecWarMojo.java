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

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Create a self executable jar file containing all necessary Apache Tomcat classes. 
 * This allows for using just <code>java -jar mywebapp.jar</code> to run your webapp without 
 * needing to install a Tomcat instance.
 * More details <a href="http://tomcat.apache.org/maven-plugin-2.0/executable-war-jar.html">here</a>.
 *
 * @author Olivier Lamy
 * @since 2.0
 */
@Mojo( name = "exec-war", threadSafe = true )
@Execute( phase = LifecyclePhase.PACKAGE )
public class ExecWarMojo
    extends AbstractExecWarMojo
{
    // no op only mojo metadatas
}
