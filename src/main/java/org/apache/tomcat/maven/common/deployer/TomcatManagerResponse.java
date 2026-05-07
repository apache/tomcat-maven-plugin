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
package org.apache.tomcat.maven.common.deployer;

/**
 * Response object for Tomcat manager operations.
 *
 * @author Olivier Lamy
 * @since 2.0
 */
public class TomcatManagerResponse {
    private int statusCode;

    private String reasonPhrase;

    private String httpResponseBody;

    /**
     * Creates an instance of TomcatManagerResponse.
     */
    public TomcatManagerResponse() {
        // no op
    }

    /**
     * Returns the HTTP status code.
     * @return the status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Sets the HTTP status code.
     * @param statusCode the status code
     * @return this response for chaining
     */
    public TomcatManagerResponse setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    /**
     * Returns the HTTP reason phrase.
     * @return the reason phrase
     */
    public String getReasonPhrase() {
        return reasonPhrase;
    }

    /**
     * Sets the HTTP reason phrase.
     * @param reasonPhrase the reason phrase
     * @return this response for chaining
     */
    public TomcatManagerResponse setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
        return this;
    }

    /**
     * Returns the HTTP response body.
     * @return the response body
     */
    public String getHttpResponseBody() {
        return httpResponseBody;
    }

    /**
     * Sets the HTTP response body.
     * @param httpResponseBody the response body
     * @return this response for chaining
     */
    public TomcatManagerResponse setHttpResponseBody(String httpResponseBody) {
        this.httpResponseBody = httpResponseBody;
        return this;
    }
}
