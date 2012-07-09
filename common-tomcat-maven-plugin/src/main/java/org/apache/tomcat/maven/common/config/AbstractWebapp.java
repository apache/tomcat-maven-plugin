package org.apache.tomcat.maven.common.config;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;

import java.io.File;

/**
 * @since 2.0
 */
public class AbstractWebapp
{

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

    public AbstractWebapp()
    {
        super();
    }

    public AbstractWebapp( Artifact artifact )
    {
        this.setArtifact( artifact );
        this.setGroupId( artifact.getGroupId() );
        this.setArtifactId( artifact.getArtifactId() );
        this.setVersion( artifact.getVersion() );
        this.setClassifier( artifact.getClassifier() );
        this.setType( artifact.getType() );
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    public String getContextPath()
    {
        if ( StringUtils.isEmpty( contextPath ) )
        {
            return this.artifactId;
        }
        return contextPath;
    }

    public void setContextPath( String contextPath )
    {
        this.contextPath = contextPath;
    }

    public Artifact getArtifact()
    {
        return artifact;
    }

    public void setArtifact( Artifact artifact )
    {
        this.artifact = artifact;
    }

    public void setContextFile( File contextFile )
    {
        this.contextFile = contextFile;
    }

    public File getContextFile()
    {
        return contextFile;
    }

}