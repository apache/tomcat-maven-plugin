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
package org.apache.tomcat.maven.plugin.tomcat.run;

import java.io.File;

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

/**
 * Webapp represents information specified in the plugin configuration section for each webapp.
 *
 * @since 2.0
 */
public class Webapp {

    /**
     *
     */
    private String groupId;
    /**
     *
     */
    private String artifactId;
    /**
     *
     */
    private String version = null;
    /**
     *
     */
    private String type = "war";
    /**
     *
     */
    private String classifier;
    /**
     * @parameter
     */
    private String contextPath;
    private Artifact artifact;
    private File contextFile;
    private boolean asWebapp = false;

    /**
     * Creates an instance of Webapp.
     */
    public Webapp() {
        // default constructor
    }

    /**
     * Creates a Webapp from the given Maven artifact.
     * @param artifact the Maven artifact
     */
    public Webapp(Artifact artifact) {
        this.setArtifact(artifact);
        this.setGroupId(artifact.getGroupId());
        this.setArtifactId(artifact.getArtifactId());
        this.setVersion(artifact.getVersion());
        this.setClassifier(artifact.getClassifier());
        this.setType(artifact.getType());
    }

    /**
     * Returns the Maven group ID.
     * @return the group ID
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Sets the Maven group ID.
     * @param groupId the group ID
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * Returns the Maven artifact ID.
     * @return the artifact ID
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Sets the Maven artifact ID.
     * @param artifactId the artifact ID
     */
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * Returns the artifact version.
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the artifact version.
     * @param version the version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the artifact type.
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the artifact type.
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the artifact classifier.
     * @return the classifier
     */
    public String getClassifier() {
        return classifier;
    }

    /**
     * Sets the artifact classifier.
     * @param classifier the classifier
     */
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    /**
     * Returns the context path for the webapp.
     * @return the context path
     */
    public String getContextPath() {
        if (contextPath == null || contextPath.isEmpty()) {
            return this.artifactId;
        }
        return contextPath;
    }

    /**
     * Sets the context path for the webapp.
     * @param contextPath the context path
     */
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    /**
     * Returns the resolved Maven artifact.
     * @return the artifact
     */
    public Artifact getArtifact() {
        return artifact;
    }

    /**
     * Sets the resolved Maven artifact.
     * @param artifact the Maven artifact
     */
    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    /**
     * Sets the context XML file.
     * @param contextFile the context file
     */
    public void setContextFile(File contextFile) {
        this.contextFile = contextFile;
    }

    /**
     * Returns the context XML file.
     * @return the context file
     */
    public File getContextFile() {
        return contextFile;
    }

    /**
     * Returns whether the webapp should be added as a webapp context.
     * @return true if added as a webapp
     */
    public boolean isAsWebapp() {
        return asWebapp;
    }

    /**
     * Sets whether the webapp should be added as a webapp context.
     * @param asWebapp true if added as a webapp
     */
    public void setAsWebapp(boolean asWebapp) {
        this.asWebapp = asWebapp;
    }
}
