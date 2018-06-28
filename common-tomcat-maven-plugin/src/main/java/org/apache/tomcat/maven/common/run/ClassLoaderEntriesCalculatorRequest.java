package org.apache.tomcat.maven.common.run;
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.Set;

/**
 * @author Olivier Lamy
 * @since 2.0
 */
public class ClassLoaderEntriesCalculatorRequest
{
    private MavenProject mavenProject;

    private Set<Artifact> dependencies;

    private List<String> additionalClasspathDirs;

    private Log log;

    private boolean addWarDependenciesInClassloader;

    private boolean useTestClassPath;

    public MavenProject getMavenProject()
    {
        return mavenProject;
    }

    public ClassLoaderEntriesCalculatorRequest setMavenProject( MavenProject mavenProject )
    {
        this.mavenProject = mavenProject;
        return this;
    }

    public Set<Artifact> getDependencies()
    {
        return dependencies;
    }

    public ClassLoaderEntriesCalculatorRequest setDependencies( Set<Artifact> dependencies )
    {
        this.dependencies = dependencies;
        return this;
    }

    public List<String> getAdditionalClasspathDirs() {
        return additionalClasspathDirs;
    }

    public ClassLoaderEntriesCalculatorRequest setAdditionalClasspathDirs(List<String> additionalClasspathDirs) {
        this.additionalClasspathDirs = additionalClasspathDirs;
        return this;
    }

    public Log getLog()
    {
        return log;
    }

    public ClassLoaderEntriesCalculatorRequest setLog( Log log )
    {
        this.log = log;
        return this;
    }

    public boolean isAddWarDependenciesInClassloader()
    {
        return addWarDependenciesInClassloader;
    }

    public ClassLoaderEntriesCalculatorRequest setAddWarDependenciesInClassloader(
        boolean addWarDependenciesInClassloader )
    {
        this.addWarDependenciesInClassloader = addWarDependenciesInClassloader;
        return this;
    }

    public boolean isUseTestClassPath()
    {
        return useTestClassPath;
    }

    public ClassLoaderEntriesCalculatorRequest setUseTestClassPath( boolean useTestClassPath )
    {
        this.useTestClassPath = useTestClassPath;
        return this;
    }

}
