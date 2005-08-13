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
package org.apache.geronimo.j2ee.management.geronimo;

import java.net.UnknownHostException;
import java.net.InetSocketAddress;
import org.apache.geronimo.kernel.management.StateManageable;

/**
 * Base management interface for a network connector used to handle some
 * protocol in order to talk to some part of the Geronimo server.
 *
 * @version $Rev: 46228 $ $Date: 2004-09-16 21:21:04 -0400 (Thu, 16 Sep 2004) $
 */
public interface NetworkConnector {
    /**
     * Gets the network protocol that this connector handles.
     */
    String getProtocol();

    /**
     * Gets the network port that this connector listens on.
     */
    int getPort();

    /**
     * Sets the network port that this connector listens on.
     */
    void setPort(int port);

    /**
     * Gets the hostname/IP that this connector listens on.
     */
    String getHost();

    /**
     * Sets the hostname/IP that this connector listens on.  This is typically
     * most useful for machines with multiple network cards, but can be used
     * to limit a connector to only listen for connections from the local
     * machine (127.0.0.1).  To listen on all available network interfaces,
     * specify an address of 0.0.0.0.
     */
    void setHost(String host) throws UnknownHostException;

    /**
     * Every connector must specify a property of type InetSocketAddress
     * because we use that to identify the network services to print a list
     * during startup.  However, this can be read-only since the host and port
     * are set separately using setHost and setPort.
     */
    InetSocketAddress getListenAddress();
}
