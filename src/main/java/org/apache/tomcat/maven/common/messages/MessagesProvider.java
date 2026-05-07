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
package org.apache.tomcat.maven.common.messages;

/**
 * messages provider
 *
 * @author Olivier Lamy
 * 
 * @since 2.0
 */
public interface MessagesProvider {
    /**
     * Returns the message for the given key.
     * @param key the message key
     * @return the message string
     */
    String getMessage(String key);

    /**
     * Returns the formatted message for the given key with the specified parameters.
     * @param key the message key
     * @param param1 the formatting parameters
     * @return the formatted message string
     */
    String getMessage(String key, Object... param1);

}
