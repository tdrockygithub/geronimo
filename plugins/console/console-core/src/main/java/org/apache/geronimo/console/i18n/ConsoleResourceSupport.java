/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.geronimo.console.i18n;

import java.util.Enumeration;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.console.util.PortletManager;

/**
 * This class provide the common functions for its subclasses.
 */
public class ConsoleResourceSupport extends ResourceBundle {
    private static final Log log = LogFactory.getLog(ConsoleResourceSupport.class);
    private static final String BASENAME = "portletinfo";

    @Override
    public Enumeration<String> getKeys() {
        //TODO should we return the keys?
        return null;
    }

    @Override
    protected Object handleGetObject(String key) {
        try {
            ConsoleResourceRegistry resourceRegistry = (ConsoleResourceRegistry) PortletManager.getKernel().getGBean(ConsoleResourceRegistry.class);
            String value = resourceRegistry.handleGetObject(BASENAME, getLocale(), key);
            return value == null ? key : value;
        } catch (Exception e) {
            log.error("Cannot get the console resource registery service", e);
        }
        return null;
    }

}
