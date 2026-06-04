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

    private final int statusCode;

    private final String reasonPhrase;

    private final String httpResponseBody;

    /**
     * Creates an instance of TomcatManagerResponse.
     *
     * @param statusCode the HTTP status code
     * @param reasonPhrase the HTTP reason phrase
     * @param httpResponseBody the HTTP response body
     */
    public TomcatManagerResponse(int statusCode, String reasonPhrase, String httpResponseBody) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
        this.httpResponseBody = httpResponseBody;
    }

    /**
     * Returns the HTTP status code.
     * @return the status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the HTTP reason phrase.
     * @return the reason phrase
     */
    public String getReasonPhrase() {
        return reasonPhrase;
    }

    /**
     * Returns the HTTP response body.
     * @return the response body
     */
    public String getHttpResponseBody() {
        return httpResponseBody;
    }

}
