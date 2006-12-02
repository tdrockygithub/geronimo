/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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

package org.apache.geronimo.jetty6;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.geronimo.clustering.SessionManager;
import org.apache.geronimo.clustering.Session;
import org.apache.geronimo.clustering.SessionAlreadyExistException;
import org.apache.geronimo.clustering.SessionListener;
import org.apache.geronimo.clustering.Node;
import org.apache.geronimo.clustering.BasicNode;
import org.apache.geronimo.jetty6.cluster.ClusteredSessionHandlerFactory;

/**
 * @version $Rev$ $Date$
 */
public class ApplicationTest extends AbstractWebModuleTest {

    public void testApplication() throws Exception {
        JettyWebAppContext app = setUpAppContext(null, null, null, null, null, null, null, "war1/");

        setUpStaticContentServlet(app);

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:5678/test/hello.txt").openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        assertEquals(HttpURLConnection.HTTP_OK, connection.getResponseCode());
        assertEquals("Hello World", reader.readLine());
        connection.disconnect();
    }

    public void testApplicationWithSessionHandler() throws Exception {
        SessionManager sessionManager = new MockSessionManager();
        sessionHandlerFactory = new ClusteredSessionHandlerFactory(sessionManager);
        JettyWebAppContext app = setUpAppContext(null, null, null, null, null, null, null, "war1/");

        setUpStaticContentServlet(app);

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:5678/test/hello.txt").openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        assertEquals(HttpURLConnection.HTTP_OK, connection.getResponseCode());
        assertEquals("Hello World", reader.readLine());
        connection.disconnect();
    }

    private static class MockSessionManager implements SessionManager {

        Node node = new BasicNode("testNode");

        public Session createSession(String string) throws SessionAlreadyExistException {
            return null;
        }

        public void registerListener(SessionListener sessionListener) {
        }

        public void unregisterListener(SessionListener sessionListener) {
        }

        public Node getNode() {
            return node;
        }
    }

}
