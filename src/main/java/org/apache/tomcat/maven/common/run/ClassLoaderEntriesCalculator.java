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
package org.apache.tomcat.maven.common.run;

/**
 * Calculator for determining classpath entries for the Tomcat webapp classloader.
 *
 * @author Olivier Lamy
 * @since 2.0
 */
public interface ClassLoaderEntriesCalculator {
    /**
     * Calculates the classpath entries for the classloader.
     * @param classLoaderEntriesCalculatorRequest the request containing project and dependency information
     * @return the result with classpath entries, temporary directories, and build directories
     * @throws TomcatRunException if calculation fails
     */
    ClassLoaderEntriesCalculatorResult calculateClassPathEntries(
            ClassLoaderEntriesCalculatorRequest classLoaderEntriesCalculatorRequest) throws TomcatRunException;
}
