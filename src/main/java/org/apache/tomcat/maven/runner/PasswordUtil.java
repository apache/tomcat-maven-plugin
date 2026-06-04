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
package org.apache.tomcat.maven.runner;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


/**
 * Password obfuscate utility class. Lifted from Jetty org.mortbay.jetty.security.Password
 * <p>
 * Passwords that begin with OBF: are de obfuscated.
 * </p>
 * <p>
 * Passwords can be obfuscated by running Obfuscate as a main class. Obfuscated password are required if a system needs
 * to recover the full password (eg. so that it may be passed to another system).
 * </p>
 * <p>
 * They are not secure, but prevent casual observation.
 * </p>
 *
 * @since 2.0
 */
public class PasswordUtil {
    /**
     * Prefix for obfuscated passwords.
     */
    public static final String __OBFUSCATE = "OBF:";

    /**
     * Creates an instance of PasswordUtil.
     */
    public PasswordUtil() {
        // default constructor
    }

    /**
     * Obfuscates the given password string.
     * @param s the password to obfuscate
     * @return the obfuscated password string
     */
    public static String obfuscate(String s) {
        StringBuilder buf = new StringBuilder();
        byte[] b = s.getBytes(StandardCharsets.UTF_8);

        buf.append(__OBFUSCATE);
        for (int i = 0; i < b.length; i++) {
            byte b1 = b[i];
            byte b2 = b[s.length() - (i + 1)];
            int i1 = 127 + b1 + b2;
            int i2 = 127 + b1 - b2;
            int i0 = i1 * 256 + i2;
            String x = Integer.toString(i0, 36);

            switch (x.length()) {
                case 1:
                    buf.append('0');
                case 2:
                    buf.append('0');
                case 3:
                    buf.append('0');
                default:
                    buf.append(x);
            }
        }
        return buf.toString();

    }

    /**
     * Deobfuscates the given obfuscated password string.
     * @param s the obfuscated password string
     * @return the deobfuscated password string, or the original if not obfuscated
     */
    public static String deobfuscate(String s) {
        if (s.startsWith(__OBFUSCATE)) {
            s = s.substring(__OBFUSCATE.length());
            if (s.length() % 4 != 0) {
                throw new IllegalArgumentException("Invalid obfuscated password: length must be a multiple of 4");
            }
            byte[] b = new byte[s.length() / 2];
            int l = 0;
            for (int i = 0; i < s.length(); i += 4) {
                String x = s.substring(i, i + 4);
                int i0 = Integer.parseInt(x, 36);
                int i1 = (i0 / 256);
                int i2 = (i0 % 256);
                b[l++] = (byte) ((i1 + i2 - 254) / 2);
            }
            return new String(b, 0, l, StandardCharsets.UTF_8);
        } else {
            return s;
        }

    }

    /**
     * Deobfuscates all system properties that start with the obfuscation prefix.
     */
    public static void deobfuscateSystemProps() {
        Properties props = System.getProperties();
        List<String> keysToModify = new ArrayList<>();
        for (Object obj : props.keySet()) {
            if (obj instanceof String) {
                String key = (String) obj;
                String value = props.getProperty(key);
                if (value != null && value.startsWith(__OBFUSCATE)) {
                    keysToModify.add(key);
                }
            }
        }
        for (String key : keysToModify) {
            System.setProperty(key, deobfuscate(System.getProperty(key)));
        }
    }

    /**
     * Main entry point for command-line password obfuscation/deobfuscation.
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: PasswordUtil <password>");
            System.exit(1);
        }
        String input = args[0];
        if (input.startsWith(__OBFUSCATE)) {
            System.out.println(deobfuscate(input));
        } else {
            System.out.println(obfuscate(input));
        }
    }}
