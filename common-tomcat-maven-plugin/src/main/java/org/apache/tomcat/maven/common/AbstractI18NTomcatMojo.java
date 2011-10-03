package org.apache.tomcat.maven.common;

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

import org.apache.maven.plugin.AbstractMojo;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Abstract goal that provides i18n support.
 *
 * @author Mark Hobson <markhobson@gmail.com>
 * @version $Id: AbstractI18NMojo.java 12852 2010-10-12 22:04:32Z thragor $
 */
public abstract class AbstractI18NTomcatMojo
    extends AbstractMojo
{

    // ----------------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------------

    /**
     * The plugin messages.
     */
    private ResourceBundle messages;

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    /**
     * Creates a new <code>AbstractI18NMojo</code>.
     */
    public AbstractI18NTomcatMojo( )
    {
        String packageName = getClass( ).getPackage( ).getName( );

        messages = ResourceBundle.getBundle( packageName + ".messages" );
    }

    // ----------------------------------------------------------------------
    // Protected Methods
    // ----------------------------------------------------------------------

    /**
     * Gets the message for the given key from this packages resource bundle.
     *
     * @param key the key for the required message
     * @return the message
     */
    protected String getMessage( String key )
    {
        try
        {
            return messages.getString( key );
        }
        catch ( NullPointerException exception )
        {
            return "???" + key + "???";
        }
        catch ( MissingResourceException exception )
        {
            return "???" + key + "???";
        }
        catch ( ClassCastException exception )
        {
            return "???" + key + "???";
        }
    }

    /**
     * Gets the message for the given key from this packages resource bundle and formats it with the given parameter.
     *
     * @param key   the key for the required message
     * @param param the parameter to be used to format the message with
     * @return the formatted message
     */
    protected String getMessage( String key, Object param )
    {
        return MessageFormat.format( getMessage( key ), new Object[]{ param } );
    }

    /**
     * Gets the message for the given key from this packages resource bundle and formats it with the given parameters.
     *
     * @param key    the key for the required message
     * @param param1 the first parameter to be used to format the message with
     * @param param2 the second parameter to be used to format the message with
     * @return the formatted message
     */
    protected String getMessage( String key, Object param1, Object param2 )
    {
        return MessageFormat.format( getMessage( key ), new Object[]{ param1, param2 } );
    }

    /**
     * Gets the message for the given key from this packages resource bundle and formats it with the given parameters.
     *
     * @param key    the key for the required message
     * @param params the parameters to be used to format the message with
     * @return the formatted message
     */
    protected String getMessage( String key, Object[] params )
    {
        return MessageFormat.format( getMessage( key ), params );
    }

    protected abstract String getPath();
}
