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

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;


import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of {@link ClassLoaderEntriesCalculator} that calculates classpath entries
 * from Maven project dependencies.
 *
 * @author Olivier Lamy
 * @since 2.0
 */
@Component(role = ClassLoaderEntriesCalculator.class)
public class DefaultClassLoaderEntriesCalculator implements ClassLoaderEntriesCalculator {
    /**
     * Creates an instance of DefaultClassLoaderEntriesCalculator.
     */
    public DefaultClassLoaderEntriesCalculator() {
        // default constructor
    }

    @Requirement
    private ArchiverManager archiverManager;


    @Override
    public ClassLoaderEntriesCalculatorResult calculateClassPathEntries(ClassLoaderEntriesCalculatorRequest request)
            throws TomcatRunException {
        Set<String> classLoaderEntries = new LinkedHashSet<>();

        List<String> fileInClassLoaderEntries = new ArrayList<>();

        List<File> tmpDirectories = new ArrayList<>();

        List<String> buildDirectories = new ArrayList<>();

        // add classes directories to loader
        try {
            List<String> classPathElements = request.isUseTestClassPath()
                    ? request.getMavenProject().getTestClasspathElements()
                    : request.getMavenProject().getRuntimeClasspathElements();
            if (classPathElements != null) {
                for (String classPathElement : classPathElements) {
                    File classPathElementFile = new File(classPathElement);
                    if (classPathElementFile.isDirectory()) {
                        request.getLog()
                                .debug("adding classPathElementFile " + classPathElementFile.toURI().toString());
                        classLoaderEntries.add(classPathElementFile.toURI().toString());
                        buildDirectories.add(classPathElement);
                    }
                }
            }
        } catch (DependencyResolutionRequiredException e) {
            throw new TomcatRunException(e.getMessage(), e);
        }

        File tmpExtractDatas = new File(request.getMavenProject().getBuild().getDirectory(),
                "apache-tomcat-maven-plugin");

        tmpExtractDatas.mkdirs();

        // add artifacts to loader
        if (request.getDependencies() != null) {
            for (Artifact artifact : request.getDependencies()) {
                String scope = artifact.getScope();

                // skip provided and test scoped artifacts
                if (!Artifact.SCOPE_PROVIDED.equals(scope) //
                        && (!Artifact.SCOPE_TEST.equals(scope) || request.isUseTestClassPath())) {
                    request.getLog().debug("add dependency to webapploader " + artifact.getGroupId() + ":" +
                            artifact.getArtifactId() + ":" + artifact.getVersion() + ":" + artifact.getScope());
                    // we add artifact dependencies and projects from reactor if file (ie jar) as users can go to
                    // install/package phase
                    // so artifact.getFile is a file not a directory and not added when iterate on
                    // project.classPathElements
                    if (!isInProjectReferences(artifact, request.getMavenProject()) || artifact.getFile().isFile()) {
                        String fileName = artifact.getGroupId() + "-" + artifact.getFile().getName();
                        if (!fileInClassLoaderEntries.contains(fileName)) {
                            classLoaderEntries.add(artifact.getFile().toURI().toString());
                            fileInClassLoaderEntries.add(fileName);
                        }
                    } else {
                        request.getLog()
                                .debug("skip adding artifact " + artifact.getArtifactId() + " as it's in reactors");

                    }
                }

                // in case of war dependency we must add /WEB-INF/lib/*.jar in entries and WEB-INF/classes
                if ("war".equals(artifact.getType()) && request.isAddWarDependenciesInClassloader()) {

                    File tmpDir = new File(tmpExtractDatas, artifact.getArtifactId());

                    boolean existed = !tmpDir.mkdirs();
                    // does a directory for this artifact already exist?
                    if (existed) {
                        // check timestamp to see if artifact is newer than extracted directory
                        long dirLastMod = tmpDir.lastModified();
                        long warLastMod = artifact.getFile().lastModified();

                        if (warLastMod == 0L || warLastMod > dirLastMod) {
                            request.getLog()
                                    .debug("re-exploding artifact " + artifact.getArtifactId() + " due to newer WAR");

                            deleteDirectory(tmpDir, request.getLog());
                            tmpDir = new File(tmpExtractDatas, artifact.getArtifactId());
                            tmpDir.mkdirs();
                            existed = false;
                        } else {
                            request.getLog()
                                    .debug("using existing exploded war for artifact " + artifact.getArtifactId());
                        }
                    }

                    // Add all directories for cleanup on shutdown
                    tmpDirectories.add(tmpDir);

                    try {
                        // explode the archive if it is not already exploded
                        if (!existed) {
                            File warFile = artifact.getFile();
                            UnArchiver unArchiver = archiverManager.getUnArchiver("jar");
                            unArchiver.setSourceFile(warFile);
                            unArchiver.setDestDirectory(tmpDir);
                            unArchiver.extract();
                        }

                        File libsDirectory = new File(tmpDir, "WEB-INF/lib");
                        if (libsDirectory.exists()) {
                            String[] jars = libsDirectory.list(new FilenameFilter() {
                                @Override
                                public boolean accept(File file, String s) {
                                    return s.endsWith(".jar");
                                }
                            });
                            if (jars != null) {
                                for (String jar : jars) {
                                    File jarFile = new File(libsDirectory, jar);
                                    if (!fileInClassLoaderEntries.contains(jarFile.getName())) {
                                        classLoaderEntries.add(jarFile.toURI().toString());
                                        fileInClassLoaderEntries.add(jarFile.getName());
                                    } else {
                                        request.getLog().debug("skip adding file " + jarFile.getPath() +
                                                " as it's already in classloader entries");
                                    }
                                }
                            }
                        }
                        File classesDirectory = new File(tmpDir, "WEB-INF/classes");
                        if (classesDirectory.exists()) {
                            classLoaderEntries.add(classesDirectory.toURI().toString());
                        }
                    } catch (NoSuchArchiverException e) {
                        throw new TomcatRunException(e.getMessage(), e);
                    } catch (ArchiverException e) {
                        request.getLog().error(
                                "fail to extract war file " + artifact.getFile() + ", reason:" + e.getMessage(), e);
                        throw new TomcatRunException(e.getMessage(), e);
                    }
                }
            }
        }

        return new ClassLoaderEntriesCalculatorResult(new ArrayList<>(classLoaderEntries), //
                tmpDirectories, //
                buildDirectories);

    }

    private void deleteDirectory(File directory, Log log) throws TomcatRunException {
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            log.error("fail to delete directory file " + directory + ", reason:" + e.getMessage(), e);
            throw new TomcatRunException(e.getMessage(), e);
        }
    }

    /**
     * Checks if the given artifact is referenced by the project's reactor projects.
     * @param artifact the artifact to check
     * @param project the Maven project
     * @return true if the artifact is in the project references
     */
    protected boolean isInProjectReferences(Artifact artifact, MavenProject project) {
        if (project.getProjectReferences() == null || project.getProjectReferences().isEmpty()) {
            return false;
        }
        Collection<MavenProject> mavenProjects = project.getProjectReferences().values();
        for (MavenProject mavenProject : mavenProjects) {
            if (mavenProject.getId() != null && mavenProject.getId().equals(artifact.getId())) {
                return true;
            }
        }
        return false;
    }
}
