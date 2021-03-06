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

import java.util.HashMap;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.security.ContextManager;

/**
 * @version $Rev$ $Date$
 */
public class LoginConfiguredIdentityNamedUsernamePasswordTest extends AbstractLoginModuleTest {
    private String credname = "credname";
    private String username = "john";
    private String password = "smith";

    protected GBeanData setupTestLoginModule() throws MalformedObjectNameException {
        GBeanData gbean;
        gbean = buildGBeanData("name", "ConfiguredIdentityNamedUsernamePasswordLoginModule", LoginModuleGBean.class);
        gbean.setAttribute("loginModuleClass", ConfiguredIdentityNamedUsernamePasswordLoginModule.class.getName());
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ConfiguredIdentityNamedUsernamePasswordLoginModule.CREDENTIAL_NAME, credname);
        props.put(ConfiguredIdentityNamedUsernamePasswordLoginModule.USER_NAME, username);
        props.put(ConfiguredIdentityNamedUsernamePasswordLoginModule.PASSWORD, password);
        gbean.setAttribute("options", props);
        gbean.setAttribute("loginDomainName", "ConfiguredIdentityNamedUsernamePasswordLoginModule");
        gbean.setAttribute("wrapPrincipals", Boolean.FALSE);
        return gbean;
    }

    public void testLogin() throws Exception {
        LoginContext context = new LoginContext(COMPLEX_REALM);

        context.login();
        Subject subject = context.getSubject();

        assertTrue("expected non-null subject", subject != null);
        assertEquals("Principals", 0, subject.getPrincipals().size());
        assertEquals("Private credentials", 1, subject.getPrivateCredentials().size());
        assertEquals("NamedUsernamePasswordCredential private credentials", 1, subject.getPrivateCredentials(NamedUsernamePasswordCredential.class).size());
        assertEquals("Public credentials", 0, subject.getPublicCredentials().size());
        NamedUsernamePasswordCredential namedupc = (NamedUsernamePasswordCredential) subject.getPrivateCredentials().toArray()[0];
        assertEquals("Credential name", credname, namedupc.getName());
        assertEquals("Username", username, namedupc.getUsername());
        assertEquals("Password", password, new String(namedupc.getPassword()));

        context.logout();

        assertEquals("Private credentials upon logout", 0, subject.getPrivateCredentials().size());
        assertTrue("id of server subject should be null", ContextManager.getSubjectId(subject) == null);
    }

    public void testNullUserLogin() throws Exception {
        //not relevant
    }

    public void testBadUserLogin() throws Exception {
        //not relevant
    }

    public void testNullPasswordLogin() throws Exception {
        //not relevant
    }

    public void testBadPasswordLogin() throws Exception {
        //not relevant
    }

    public void testNoPrincipalsAddedOnFailure() throws Exception {
        //not relevant
    }

    public void testLogoutWithReadOnlySubject() throws Exception {
        LoginContext context = new LoginContext(COMPLEX_REALM);

        context.login();
        Subject subject = context.getSubject();

        assertTrue("expected non-null subject", subject != null);

        subject.setReadOnly();

        try {
            context.logout();
        } catch(Exception e) {
            fail("logout failed");
        }
        NamedUsernamePasswordCredential namedupc = (NamedUsernamePasswordCredential) subject.getPrivateCredentials().toArray()[0];
        assertTrue("credential should have been destroyed", namedupc.isDestroyed());
    }
}
