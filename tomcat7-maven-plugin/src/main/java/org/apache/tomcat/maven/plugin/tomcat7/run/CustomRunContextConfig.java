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

import org.apache.catalina.startup.ContextConfig;

import javax.servlet.ServletContext;
import java.util.List;

/**
 * Created by benoitmeriaux on 29/08/2015.
 */
public class CustomRunContextConfig extends ContextConfig {

    /*In order to have the ServletContainerInitializer scanned using the classpath and not using resources path,
    we need to clear the ORDERED_LIBS attribtues of the ServletContext before the scan*/
    protected void processServletContainerInitializers(ServletContext servletContext) {
        List saveOrderedLib = (List) servletContext.getAttribute(ServletContext.ORDERED_LIBS);
        servletContext.setAttribute(ServletContext.ORDERED_LIBS, null);
        super.processServletContainerInitializers(servletContext);
        servletContext.setAttribute(ServletContext.ORDERED_LIBS, saveOrderedLib);
    }

}
