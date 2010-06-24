/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.geronimo.bval.deployment;

import java.util.Map;

import org.apache.geronimo.bval.DefaultValidatorFactoryReference;
import org.apache.geronimo.bval.DefaultValidatorReference;
import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.gbean.annotation.GBean;
import org.apache.geronimo.gbean.annotation.ParamAttribute;
import org.apache.geronimo.j2ee.deployment.EARContext;
import org.apache.geronimo.j2ee.deployment.Module;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.j2ee.jndi.JndiKey;
import org.apache.geronimo.kernel.repository.Environment;
import org.apache.geronimo.naming.deployment.AbstractNamingBuilder;
import org.apache.openejb.jee.JndiConsumer;
import org.apache.xmlbeans.QNameSet;
import org.apache.xmlbeans.XmlObject;

/**
 * @version $Rev$ $Date$
 */
@GBean(j2eeType = NameFactory.MODULE_BUILDER)
public class BValNamingBuilder extends AbstractNamingBuilder {

    public BValNamingBuilder(@ParamAttribute(name = "defaultEnvironment")Environment defaultEnvironment) {
        super(defaultEnvironment);
    }

    @Override
    public void buildNaming(JndiConsumer specDD, XmlObject xmlObject1, Module module, Map<EARContext.Key, Object> keyObjectMap) throws DeploymentException {
        put("java:comp/Validator", new DefaultValidatorReference(), module.getJndiContext());
        put("java:comp/ValidatorFactory", new DefaultValidatorFactoryReference(), module.getJndiContext());
    }

    @Override
    public QNameSet getSpecQNameSet() {
        return QNameSet.EMPTY;
    }

    @Override
    public QNameSet getPlanQNameSet() {
        return QNameSet.EMPTY;
    }
}
