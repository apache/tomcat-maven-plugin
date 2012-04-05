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

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

/**
 * @author Olivier Lamy
 * @since 2.0
 */
public class ExtendedTomcat
    extends Tomcat
{

    private File configurationDir;

    public ExtendedTomcat( File configurationDir )
    {
        super();
        this.configurationDir = configurationDir;
    }

    public Context addWebapp( Host host, String url, String name, String path )
    {

        Context ctx = new StandardContext();
        ctx.setName( name );
        ctx.setPath( url );
        ctx.setDocBase( path );

        ContextConfig ctxCfg = new ContextConfig();
        ctx.addLifecycleListener( ctxCfg );

        ctxCfg.setDefaultWebXml( new File( configurationDir, "conf/web.xml" ).getAbsolutePath() );

        if ( host == null )
        {
            getHost().addChild( ctx );
        }
        else
        {
            host.addChild( ctx );
        }

        return ctx;
    }
}
