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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.util.Set;

/**
 * Request object for classloader entries calculation.
 *
 * @author Olivier Lamy
 * @since 2.0
 */
public class ClassLoaderEntriesCalculatorRequest {
    /**
     * Creates an instance of ClassLoaderEntriesCalculatorRequest.
     */
    public ClassLoaderEntriesCalculatorRequest() {
        // default constructor
    }

    private MavenProject mavenProject;

    private Set<Artifact> dependencies;

    private Log log;

    private boolean addWarDependenciesInClassloader;

    private boolean useTestClassPath;

    /**
     * Returns the Maven project.
     * @return the Maven project
     */
    public MavenProject getMavenProject() {
        return mavenProject;
    }

    /**
     * Sets the Maven project.
     * @param mavenProject the Maven project
     * @return this request for chaining
     */
    public ClassLoaderEntriesCalculatorRequest setMavenProject(MavenProject mavenProject) {
        this.mavenProject = mavenProject;
        return this;
    }

    /**
     * Returns the set of dependencies.
     * @return the dependencies
     */
    public Set<Artifact> getDependencies() {
        return dependencies;
    }

    /**
     * Sets the dependencies.
     * @param dependencies the dependencies
     * @return this request for chaining
     */
    public ClassLoaderEntriesCalculatorRequest setDependencies(Set<Artifact> dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    /**
     * Returns the log instance.
     * @return the log
     */
    public Log getLog() {
        return log;
    }

    /**
     * Sets the log instance.
     * @param log the log
     * @return this request for chaining
     */
    public ClassLoaderEntriesCalculatorRequest setLog(Log log) {
        this.log = log;
        return this;
    }

    /**
     * Returns whether WAR dependencies should be added to the classloader.
     * @return true if WAR dependencies should be added
     */
    public boolean isAddWarDependenciesInClassloader() {
        return addWarDependenciesInClassloader;
    }

    /**
     * Sets whether WAR dependencies should be added to the classloader.
     * @param addWarDependenciesInClassloader the flag value
     * @return this request for chaining
     */
    public ClassLoaderEntriesCalculatorRequest setAddWarDependenciesInClassloader(
            boolean addWarDependenciesInClassloader) {
        this.addWarDependenciesInClassloader = addWarDependenciesInClassloader;
        return this;
    }

    /**
     * Returns whether the test classpath should be used.
     * @return true if test classpath should be used
     */
    public boolean isUseTestClassPath() {
        return useTestClassPath;
    }

    /**
     * Sets whether the test classpath should be used.
     * @param useTestClassPath the flag value
     * @return this request for chaining
     */
    public ClassLoaderEntriesCalculatorRequest setUseTestClassPath(boolean useTestClassPath) {
        this.useTestClassPath = useTestClassPath;
        return this;
    }

}
