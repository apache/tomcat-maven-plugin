package org.apache.tomcat.maven.runner;

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

import java.util.Properties;


/**
 * Password obfuscate utility class. Lifted from Jetty org.mortbay.jetty.security.Password
 * <p/>
 * <p/>
 * Passwords that begin with OBF: are de obfuscated.
 * <p/>
 * Passwords can be obfuscated by running Obfuscate as a main class. Obfuscated password are required if a system needs
 * to recover the full password (eg. so that it may be passed to another system).
 * <p/>
 * They are not secure, but prevent casual observation.
 *
 * @see <a
 *      href="http://grepcode.com/file_/repo1.maven.org/maven2/org.mortbay.jetty/jetty/6.1.11/org/mortbay/jetty/security/Password.java/?v=source"
 *      >Jetty Source org.mortbay.jetty.security.Password</a>
 * @since 2.0
 */
public class PasswordUtil
{
    public static final String __OBFUSCATE = "OBF:";

    /* ------------------------------------------------------------ */
    public static String obfuscate( String s )
    {
        StringBuilder buf = new StringBuilder();
        byte[] b = s.getBytes();

        buf.append( __OBFUSCATE );
        for ( int i = 0; i < b.length; i++ )
        {
            byte b1 = b[i];
            byte b2 = b[s.length() - ( i + 1 )];
            int i1 = 127 + b1 + b2;
            int i2 = 127 + b1 - b2;
            int i0 = i1 * 256 + i2;
            String x = Integer.toString( i0, 36 );

            switch ( x.length() )
            {
                case 1:
                    buf.append( '0' );
                case 2:
                    buf.append( '0' );
                case 3:
                    buf.append( '0' );
                default:
                    buf.append( x );
            }
        }
        return buf.toString();

    }

    /* ------------------------------------------------------------ */
    public static String deobfuscate( String s )
    {
        if ( s.startsWith( __OBFUSCATE ) )
        {
            s = s.substring( __OBFUSCATE.length() );

            byte[] b = new byte[s.length() / 2];
            int l = 0;
            for ( int i = 0; i < s.length(); i += 4 )
            {
                String x = s.substring( i, i + 4 );
                int i0 = Integer.parseInt( x, 36 );
                int i1 = ( i0 / 256 );
                int i2 = ( i0 % 256 );
                b[l++] = (byte) ( ( i1 + i2 - 254 ) / 2 );
            }
            return new String( b, 0, l );
        }
        else
        {
            return s;
        }

    }

    public static void deobfuscateSystemProps()
    {
        Properties props = System.getProperties();
        for ( Object obj : props.keySet() )
        {
            if ( obj instanceof String )
            {
                String key = (String) obj;
                String value = (String) props.getProperty( key );
                if ( value != null && value.startsWith( __OBFUSCATE ) )
                {
                    System.setProperty( key, deobfuscate( value ) );
                }
            }
        }
    }

    public static void main( String[] args )
    {
        if ( args[0].startsWith( __OBFUSCATE ) )
        {
            System.out.println( PasswordUtil.deobfuscate( args[1] ) );
        }
        else
        {
            System.out.println( PasswordUtil.obfuscate( args[1] ) );
        }
    }
}
