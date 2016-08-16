package org.apache.tomcat.maven.plugin.tomcat8.run;

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

import org.apache.catalina.loader.ResourceEntry;
import org.apache.catalina.loader.WebappClassLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * @author Olivier Lamy
 */
public class MavenWebappClassLoader
    extends WebappClassLoader
{

    private ClassLoader classLoader;

    public MavenWebappClassLoader( ClassLoader parent )
    {
        super( parent );
        this.classLoader = parent;
    }

    @Override
    public Class<?> findClass( String name )
        throws ClassNotFoundException
    {
        return super.findClass( name );
    }

    @Override
    public URL findResource( String name )
    {
        return super.findResource( name );
    }

    @Override
    public Enumeration<URL> findResources( String name )
        throws IOException
    {
        return super.findResources( name );
    }

    @Override
    public URL getResource( String name )
    {
        return super.getResource( name );
    }

    @Override
    public InputStream getResourceAsStream( String name )
    {
        return super.getResourceAsStream( name );
    }

    @Override
    public Class<?> loadClass( String name )
        throws ClassNotFoundException
    {
        try
        {
            return classLoader.loadClass( name );
        }
        catch ( ClassNotFoundException e )
        {
            // go to top
        }
        return super.loadClass( name );
    }

    @Override
    public synchronized Class<?> loadClass( String name, boolean resolve )
        throws ClassNotFoundException
    {
        try
        {
            return classLoader.loadClass( name );
        }
        catch ( ClassNotFoundException e )
        {
            // go to top
        }
        return super.loadClass( name, resolve );
    }

    @Override
    protected ResourceEntry findResourceInternal( String name, String path )
    {
        return super.findResourceInternal( name, path );
    }

    @Override
    protected Class<?> findClassInternal( String name )
        throws ClassNotFoundException
    {
        return super.findClassInternal( name );
    }
}
