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

package org.apache.geronimo.security.jaas;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.management.MalformedObjectNameException;

import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.security.AbstractTest;
import org.apache.geronimo.security.ContextManager;
import org.apache.geronimo.security.DomainPrincipal;
import org.apache.geronimo.security.IdentificationPrincipal;
import org.apache.geronimo.security.RealmPrincipal;
import org.apache.geronimo.security.realm.GenericSecurityRealm;


/**
 * @version $Rev$ $Date$
 */
public class LoginPropertiesFileTest extends AbstractLoginModuleTest {

    protected GBeanData setupTestLoginModule() throws MalformedObjectNameException {
        GBeanData gbean;
        gbean = buildGBeanData("name", "PropertiesLoginModule", LoginModuleGBean.getGBeanInfo());
        gbean.setAttribute("loginModuleClass", "org.apache.geronimo.security.realm.providers.PropertiesFileLoginModule");
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("usersURI", "src/test/data/data/users.properties");
        props.put("groupsURI", "src/test/data/data/groups.properties");
        gbean.setAttribute("options", props);
        gbean.setAttribute("loginDomainName", "TestProperties");
        gbean.setAttribute("wrapPrincipals", Boolean.TRUE);
        return gbean;
    }

    public void testLogin() throws Exception {

        LoginContext context = new LoginContext(SIMPLE_REALM, new AbstractTest.UsernamePasswordCallback("alan", "starcraft"));

        context.login();
        Subject subject = context.getSubject();

        assertTrue("expected non-null subject", subject != null);
        assertTrue("subject should have no remote principal", subject.getPrincipals(IdentificationPrincipal.class).size() == 0);
        assertEquals("subject should have 6 principals (" + subject.getPrincipals().size() + ")", 6, subject.getPrincipals().size());
        assertEquals("subject should have 2 realm principals (" + subject.getPrincipals(RealmPrincipal.class).size() + ")", 2, subject.getPrincipals(RealmPrincipal.class).size());
        assertEquals("subject should have 2 domain principals (" + subject.getPrincipals(DomainPrincipal.class).size() + ")", 2, subject.getPrincipals(DomainPrincipal.class).size());

        context.logout();
        assertEquals("subject should have no principals (" + subject.getPrincipals().size() + ")", 0, subject.getPrincipals().size());
 
        assertTrue("id of server subject should be null", ContextManager.getSubjectId(subject) == null);
    }

    public void testNullUserLogin() throws Exception {
        LoginContext context = new LoginContext(SIMPLE_REALM, new UsernamePasswordCallback(null, "starcraft"));

        try {
            context.login();
            fail("Should not allow this login with null username");
        } catch (LoginException e) {
        }
    }

    public void testBadUserLogin() throws Exception {
        LoginContext context = new LoginContext(SIMPLE_REALM, new UsernamePasswordCallback("bad", "starcraft"));

        try {
            context.login();
            fail("Should not allow this login with bad username");
        } catch (LoginException e) {
        }
    }

    public void testNullPasswordLogin() throws Exception {
        LoginContext context = new LoginContext(SIMPLE_REALM, new UsernamePasswordCallback("alan", null));

        try {
            context.login();
            fail("Should not allow this login with null password");
        } catch (LoginException e) {
        }
    }

    public void testBadPasswordLogin() throws Exception {
        LoginContext context = new LoginContext(SIMPLE_REALM, new UsernamePasswordCallback("alan", "bad"));

        try {
            context.login();
            fail("Should not allow this login with bad password");
        } catch (LoginException e) {
        }
    }

    public void testNoPrincipalsAddedOnFailure() throws Exception {
        LoginContext context = new LoginContext(COMPLEX_REALM, new UsernamePasswordCallback("alan", "bad"));

        context.login();
        Subject subject = context.getSubject();
        assertTrue("expected non-null subject", subject != null);
        assertTrue("expected zero principals", subject.getPrincipals().size() == 0);
        context.logout();
    }

}
