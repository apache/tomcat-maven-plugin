package org.apache.tomcat.maven.plugin;

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
 * Redeploy a WAR in Tomcat. Deploy with forcing update flag to true
 * 
 * @goal redeploy
 * @author Mark Hobson <markhobson@gmail.com>
 * @version $Id: RedeployMojo.java 12852 2010-10-12 22:04:32Z thragor $
 * @todo depend on war:war, war:exploded or war:inplace when MNG-1649 resolved
 */
public class RedeployMojo
    extends DeployMojo
{
    // ----------------------------------------------------------------------
    // Protected Methods
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isUpdate()
    {
        return true;
    }
}
