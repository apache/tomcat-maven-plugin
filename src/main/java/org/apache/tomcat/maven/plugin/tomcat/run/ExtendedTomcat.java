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

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

/**
 * Extended Tomcat embedding that customizes webapp configuration.
 * Disables default web.xml and uses a custom web.xml from the configuration directory.
 *
 * @author Olivier Lamy
 * @since 2.0
 */
public class ExtendedTomcat extends Tomcat {

    private File configurationDir;

    /**
     * Creates an extended Tomcat instance with the given configuration directory.
     * @param configurationDir the Tomcat configuration directory
     */
    public ExtendedTomcat(File configurationDir) {
        super();
        if (configurationDir == null || !configurationDir.exists() || !configurationDir.isDirectory()) {
            throw new IllegalArgumentException("configurationDir must not be null and must exist");
        }
        this.configurationDir = configurationDir;
    }

    @Override
    public Context addWebapp(Host host, String contextPath, String docBase) {
        setAddDefaultWebXmlToWebapp(false);
        ContextConfig ctxCfg = new ContextConfig();
        ctxCfg.setDefaultWebXml(new File(configurationDir, "conf/web.xml").getAbsolutePath());
        return addWebapp(host, contextPath, docBase, ctxCfg);
    }
}
