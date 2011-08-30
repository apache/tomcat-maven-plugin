package org.codehaus.mojo.tomcat;

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

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Embedded;
import org.apache.maven.plugin.logging.Log;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Registry which collects all embedded Tomcat Servers so that they will be shutdown
 * through a shutdown hook when the JVM terminates or you can ask the registry to
 * shutdown all started servers.
 * @author Mark Michaelis
 * @since 1.1
 */
public final class EmbeddedRegistry
{
    private static EmbeddedRegistry instance;

    private Set<Embedded> containers = new HashSet<Embedded>( 1 );

    /**
     * Don't instantiate - use the instance through {@link #getInstance()}.
     */
    private EmbeddedRegistry()
    {
        // no op
    }

    /**
     * Retrieve the lazily initialized instance of the registry.
     * 
     * @return singleton instance of the registry
     */
    public static EmbeddedRegistry getInstance()
    {
        if ( instance == null )
        {
            instance = new EmbeddedRegistry();
            Runtime.getRuntime().addShutdownHook( new Thread()
            {
                @Override
                public void run()
                {
                    try
                    {
                        getInstance().shutdownAll( null );
                    }
                    catch ( LifecycleException e )
                    {
                        // ignore, the exception should already have been reported
                    }
                }
            } );
        }
        return instance;
    }

    /**
     * Adds the given container to the registry which automatically registers it for the shutdown
     * hook.
     * 
     * @param container the container to register
     * @return true if it got added; false if not
     */
    public synchronized boolean register( final Embedded container )
    {
        return containers.add( container );
    }

    /**
     * Shuts down all registered embedded tomcats. All tomcats which successfully shut down will be
     * removed from the registry.
     * 
     * @param log the log to write possible shutdown exceptions to
     * @throws LifecycleException the first exception which occurred will be rethrown
     */
    public synchronized void shutdownAll( final Log log )
        throws LifecycleException
    {
        LifecycleException firstException = null;
        for ( Iterator<Embedded> iterator = containers.iterator(); iterator.hasNext(); )
        {
            Embedded embedded = iterator.next();
            try
            {
                embedded.stop();
                iterator.remove();
            }
            catch ( LifecycleException e )
            {
                if ( firstException == null )
                {
                    firstException = e;
                    error( log, e, "Error while shutting down embedded Tomcat. Will be rethrown." );
                }
                else
                {
                    error( log, e, "Error while shutting down embedded Tomcat." );
                }
            }
        }
        if ( firstException != null )
        {
            throw firstException;
        }
    }

    /**
     * Reports the exception. If a log is given (typically when called from within a Mojo) the
     * message will be printed to the log. Otherwise it will be printed to StdErr.
     * 
     * @param log the log to write the message to; null to write to stderr
     * @param e exception which shall be reported
     * @param message message which shall be reported
     */
    private void error( final Log log, final LifecycleException e, final String message )
    {
        if ( log == null )
        {
            System.err.println( "ERROR: " + message );
            e.printStackTrace();
        }
        else
        {
            log.error( message, e );
        }
    }

}
