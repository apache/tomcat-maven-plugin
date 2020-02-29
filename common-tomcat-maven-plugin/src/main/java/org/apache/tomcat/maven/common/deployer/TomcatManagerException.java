package org.apache.tomcat.maven.common.deployer;

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
 * Indicates an error received from Tomcat manager.
 *
 * @author Mark Hobson (markhobson@gmail.com)
 */
public class TomcatManagerException
    extends Exception
{
    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------

    /**
     * The Java serialization UID for this class.
     */
    private static final long serialVersionUID = 4167819069046408371L;

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    /**
     * Creates a new <code>TomcatManagerException</code> with no message or cause.
     */
    public TomcatManagerException()
    {
        super();
    }

    /**
     * Creates a new <code>TomcatManagerException</code> with the specified message and no cause.
     *
     * @param message the message for this exception
     */
    public TomcatManagerException( String message )
    {
        super( message );
    }

    /**
     * Creates a new <code>TomcatManagerException</code> with the specified message and cause.
     *
     * @param message the message for this exception
     * @param cause   the cause of this exception
     */
    public TomcatManagerException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
