/**
 * Copyright 2006 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.console.threads;

import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import org.apache.geronimo.console.MultiPagePortlet;
import org.apache.geronimo.console.MultiPageModel;

/**
 * A portlet that lets you configure and deploy thread pools.
 *
 * @version $Rev: 368994 $ $Date: 2006-01-14 02:07:18 -0500 (Sat, 14 Jan 2006) $
 */
public class ThreadPoolPortlet extends MultiPagePortlet {
    public void init(PortletConfig config) throws PortletException {
        super.init(config);
        addHelper(new ListScreenHandler(), config);
        addHelper(new MonitorScreenHandler(), config);
    }

    protected MultiPageModel getModel(PortletRequest request) {
        return new AbstractThreadHandler.ThreadPoolData(request);
    }
}
