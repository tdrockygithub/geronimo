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
package org.apache.geronimo.cxf;

import java.util.Map;

import javax.wsdl.Definition;
import javax.wsdl.extensions.schema.SchemaReference;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.WSDLQueryHandler;
import org.apache.geronimo.jaxws.WSDLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeronimoQueryHandler extends WSDLQueryHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GeronimoQueryHandler.class);

    public GeronimoQueryHandler(Bus bus) {
        super(bus);
    }

    protected void updateDefinition(Definition def,
                                    Map<String, Definition> done,
                                    Map<String, SchemaReference> doneSchemas,
                                    String base,
                                    EndpointInfo ei) {
        if (done.get("") == def) {
            String serviceName = ei.getService().getName().getLocalPart();
            String portName = ei.getName().getLocalPart();
            // remove other services and ports from wsdl
            WSDLUtils.trimDefinition(def, serviceName, portName);
        }
        super.updateDefinition(def, done, doneSchemas, base, ei);
    }

}
