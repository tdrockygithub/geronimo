/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.geronimo.testsuite.console;

import org.testng.annotations.Test;

/**
 * Thread pool portlet tests
 *
 * @version $Rev$ $Date$
 */
public class ThreadPoolPortletTest
    extends BasicConsoleTestSupport
{
    @Test
    public void testThreadPoolLink() throws Exception {
    	
        selenium.click("link=Thread Pools");
        waitForPageLoad();
//        assertEquals("Geronimo Console", selenium.getTitle());
        assertEquals("Thread Pool Configuration", 
                     selenium.getText(getPortletTitleLocation())); 
    }
}
