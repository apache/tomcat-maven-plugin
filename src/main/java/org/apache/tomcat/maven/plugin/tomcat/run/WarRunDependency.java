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
package org.apache.tomcat.maven.plugin.tomcat.run;

import org.apache.maven.model.Dependency;

import java.io.File;

/**
 * Configuration for a WAR dependency to embed in an executable JAR.
 *
 * @author Olivier Lamy
 * @since 2.0
 */
public class WarRunDependency {

    /**
     * The dependency descriptor.
     */
    public Dependency dependency;

    /**
     * The context path for the WAR.
     */
    public String contextPath;

    /**
     * The context XML file for the WAR.
     */
    public File contextXml;

    /**
     * Creates an instance of WarRunDependency.
     */
    public WarRunDependency() {
        // no op
    }

}
