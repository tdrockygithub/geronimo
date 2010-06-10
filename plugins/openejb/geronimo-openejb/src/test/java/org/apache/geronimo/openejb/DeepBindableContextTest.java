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


package org.apache.geronimo.openejb;

import javax.naming.Context;
import javax.naming.NamingException;

import junit.framework.TestCase;
import org.apache.geronimo.gjndi.GlobalContextGBean;
import org.apache.xbean.naming.global.GlobalContextManager;

/**
 * @version $Rev$ $Date$
 */
public class DeepBindableContextTest extends TestCase {
    
    public void testBindUnbind() throws Exception {
        System.setProperty("java.naming.factory.initial", "org.apache.xbean.naming.global.GlobalContextManager");
        GlobalContextGBean globalContext = new GlobalContextGBean(null);
        GlobalContextManager.setGlobalContext(globalContext);
        DeepBindableContext context = new DeepBindableContext("openejb", false, true, false, false);
        globalContext.bind("openejb", context);

        Context contextWrapper = context.newContextWrapper();
        testBindUnbind(context, contextWrapper, "openejb/foo/bar", "");
        testBindUnbind(context, contextWrapper, "java:openejb/foo/bar", "java:");

    }

    private void testBindUnbind(DeepBindableContext context, Context contextWrapper, String s, String prefix) throws NamingException {
        contextWrapper.bind(s, "bar");
        assertEquals("bar", context.lookup(s.substring(prefix.length())));
        contextWrapper.unbind(s);
        try {
            context.lookup(s);
            fail(s + "should not be bound");
        } catch (NamingException ne) {
            //expected
        }
    }
}