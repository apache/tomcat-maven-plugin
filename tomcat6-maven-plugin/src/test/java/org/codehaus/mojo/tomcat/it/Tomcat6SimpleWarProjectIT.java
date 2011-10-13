package org.codehaus.mojo.tomcat.it;
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

import org.apache.maven.it.VerificationException;
import org.apache.tomcat.maven.it.AbstractSimpleWarProjectIT;

/**
 * @author Olivier Lamy
 */
public class Tomcat6SimpleWarProjectIT
    extends AbstractSimpleWarProjectIT
{
    @Override
    protected void verifyConnectorsStarted()
        throws VerificationException
    {
        verifier.verifyTextInLog("INFO: Starting Coyote HTTP/1.1 on http-" + getHttpItPort());
        verifier.verifyTextInLog("INFO: JK: ajp13 listening on /0.0.0.0:" + getAjpItPort());
    }
}
