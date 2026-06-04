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

import org.codehaus.plexus.component.annotations.Component;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Default implementation of {@link MessagesProvider} that loads messages from a ResourceBundle.
 *
 * @author Olivier Lamy
 * @since 2.0
 */
@Component(role = MessagesProvider.class)
public class DefaultMessagesProvider implements MessagesProvider {

    /**
     * Plugin messages
     */
    private final ResourceBundle messages;


    /**
     * Creates a new instance and loads the message bundle.
     */
    public DefaultMessagesProvider() {
        try {
            messages = ResourceBundle.getBundle(getClass().getPackage().getName() + ".messages");
        } catch (MissingResourceException e) {
            throw new IllegalStateException("Required message bundle 'messages' not found on classpath", e);
        }
    }

    /**
     * Returns the underlying resource bundle.
     * @return the resource bundle
     */
    public ResourceBundle getResourceBundle() {
        return this.messages;
    }

    @Override
    public String getMessage(String key) {
        try {
            return getResourceBundle().getString(key);
        } catch (NullPointerException | MissingResourceException | ClassCastException exception) {
            return "???" + key + "???";
        }
    }

    @Override
    public String getMessage(String key, Object... params) {
        String template = getMessage(key);
        if (params == null || params.length == 0) {
            return template;
        }
        try {
            return MessageFormat.format(template, params);
        } catch (IllegalArgumentException e) {
            return template + " [formatting error: " + e.getMessage() + "]";
        }
    }
}
