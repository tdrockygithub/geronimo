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
 * Web access log portlet tests
 *
 * @version $Rev$ $Date$
 */
public class WebAccessLogViewerPortletTest
    extends BasicConsoleTestSupport
{
    @Test
    public void testWebAccessLogViewerLink() throws Exception {
    	
        selenium.click("link=Server Logs");
        waitForPageLoad();
//        assertEquals("Geronimo Console", selenium.getTitle());
        assertEquals("Web Access Log Viewer", 
                     selenium.getText(getPortletTitleLocation(3))); 
        // Test help link
        selenium.click(getPortletHelpLocation(3));
        waitForPageLoad();
        selenium.isTextPresent("This portlet displays and filters the Jetty log file.");
    }
}
