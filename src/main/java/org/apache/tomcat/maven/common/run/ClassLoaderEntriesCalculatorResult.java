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

import java.io.File;
import java.util.List;

/**
 * Result object for classloader entries calculation.
 *
 * @author Olivier Lamy
 * @since 2.0
 */
public class ClassLoaderEntriesCalculatorResult {
    /**
     * classpath entries File .toURI().toString()
     */
    private List<String> classPathEntries;

    /**
     * List of files to cleanup after execution
     */
    private List<File> tmpDirectories;


    /**
     * directory part of webapp classpath (project.build.directory and reactor projects)
     */
    private final List<String> buildDirectories;

    /**
     * Creates a new result with the given classpath entries, temporary directories, and build directories.
     * @param classPathEntries classpath entries as File.toURI().toString() values
     * @param tmpDirectories list of files to cleanup after execution
     * @param buildDirectories directory part of webapp classpath (project.build.directory and reactor projects)
     */
    public ClassLoaderEntriesCalculatorResult(List<String> classPathEntries, List<File> tmpDirectories,
            List<String> buildDirectories) {
        this.classPathEntries = classPathEntries;
        this.tmpDirectories = tmpDirectories;
        this.buildDirectories = buildDirectories;
    }

    /**
     * Returns the classpath entries.
     * @return the classpath entries
     */
    public List<String> getClassPathEntries() {
        return classPathEntries;
    }

    /**
     * Sets the classpath entries.
     * @param classPathEntries the classpath entries
     */
    public void setClassPathEntries(List<String> classPathEntries) {
        this.classPathEntries = classPathEntries;
    }

    /**
     * Returns the temporary directories to clean up.
     * @return the temporary directories
     */
    public List<File> getTmpDirectories() {
        return tmpDirectories;
    }

    /**
     * Sets the temporary directories.
     * @param tmpDirectories the temporary directories
     */
    public void setTmpDirectories(List<File> tmpDirectories) {
        this.tmpDirectories = tmpDirectories;
    }

    /**
     * Returns the build directories.
     * @return the build directories
     */
    public List<String> getBuildDirectories() {
        return buildDirectories;
    }
}
