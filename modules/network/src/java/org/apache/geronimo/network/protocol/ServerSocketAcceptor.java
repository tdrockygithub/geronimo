/*
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *           
 * http://www.apache.org/licenses/LICENSE-2.0
 *          
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geronimo.network.protocol;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.geronimo.gbean.GBean;
import org.apache.geronimo.gbean.GBeanContext;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoFactory;
import org.apache.geronimo.gbean.WaitingException;
import org.apache.geronimo.network.SelectionEventListner;
import org.apache.geronimo.network.SelectorManager;
import org.apache.geronimo.network.URISupport;


/**
 * @version $Revision: 1.1 $ $Date: 2004/03/10 02:14:27 $
 */
public class ServerSocketAcceptor implements SelectionEventListner, GBean {

    final static private Log log = LogFactory.getLog(SocketProtocol.class);

    private URI uri;
    private URI connectURI;

    private ServerSocketChannel serverSocketChannel;
    private int timeOut;
    private boolean enableTcpNoDelay;
    private SelectorManager selectorManager;
    private SelectionKey selectionKey;

    private ServerSocketAcceptorListener acceptorListener;

    private final int STARTED = 0;
    private final int STOPPED = 1;
    private final int FAILED = 2;
    private int state = STOPPED;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        if (state == STARTED || state == FAILED) throw new IllegalStateException("Protocol already started");
        this.uri = uri;
    }

    public URI getConnectURI() {
        return connectURI;
    }

    public ServerSocketChannel getServerSocketChannel() {
        return serverSocketChannel;
    }

    public void setServerSocketChannel(ServerSocketChannel serverSocketChannel) {
        if (state == STARTED || state == FAILED) throw new IllegalStateException("Protocol already started");
        this.serverSocketChannel = serverSocketChannel;
    }

    public int getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(int timeOut) {
        if (state == STARTED || state == FAILED) throw new IllegalStateException("Protocol already started");
        this.timeOut = timeOut;
    }

    public boolean isEnableTcpNoDelay() {
        return enableTcpNoDelay;
    }

    public void setEnableTcpNoDelay(boolean enableTcpNoDelay) {
        if (state == STARTED || state == FAILED) throw new IllegalStateException("Protocol already started");
        this.enableTcpNoDelay = enableTcpNoDelay;
    }

    public ServerSocketAcceptorListener getAcceptorListener() {
        return acceptorListener;
    }

    public void setAcceptorListener(ServerSocketAcceptorListener acceptorListener) {
        if (state == STARTED || state == FAILED) throw new IllegalStateException("Protocol already started");
        this.acceptorListener = acceptorListener;
    }

    public SelectorManager getSelectorManager() {
        return selectorManager;
    }

    public void setSelectorManager(SelectorManager selectorManager) {
        if (state == STARTED || state == FAILED) throw new IllegalStateException("Protocol already started");
        this.selectorManager = selectorManager;
    }

    public void setGBeanContext(GBeanContext context) {
    }

    public void doStart() throws WaitingException, Exception {
        String serverBindAddress = uri.getHost();
        String clientConnectAddress = null;
        int serverBindPort = uri.getPort();
        int clientConnectPort = serverBindPort;
        int connectBackLog = 50;
        enableTcpNoDelay = true;

        Properties params = URISupport.parseQueryParameters(uri);
        enableTcpNoDelay = params.getProperty("tcp.nodelay", "true").equals("true");
        connectBackLog = Integer.parseInt(params.getProperty("tcp.backlog", "50"));
        clientConnectAddress = params.getProperty("client.host");
        clientConnectPort = Integer.parseInt(params.getProperty("client.port", "0"));

        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(InetAddress.getByName(serverBindAddress), serverBindPort), connectBackLog);
        serverSocketChannel.socket().setSoTimeout(timeOut);
        serverSocketChannel.configureBlocking(false);
        selectionKey = selectorManager.register(serverSocketChannel, SelectionKey.OP_ACCEPT, this);

        // Lookup the local host name if needed.
        clientConnectAddress = (clientConnectAddress == null || clientConnectAddress.length() == 0)
                ? InetAddress.getLocalHost().getHostName()
                : clientConnectAddress;
        clientConnectPort = (clientConnectPort <= 0) ? serverSocketChannel.socket().getLocalPort() : clientConnectPort;

        // Create the client URI:
        Properties clientParms = new Properties();
        clientParms.put("tcp.nodelay", enableTcpNoDelay ? "true" : "false");
        connectURI = new URI("async",
                             null,
                             clientConnectAddress,
                             clientConnectPort,
                             "",
                             URISupport.toQueryString(clientParms),
                             null);
        log.info("Remoting 'async' protocol available at: "
                 + serverSocketChannel.socket().getInetAddress()
                 + ":"
                 + serverSocketChannel.socket().getLocalPort());
        log.info("Remoting 'async' protocol clients will connect to: " + connectURI);

        state = STARTED;
    }

    public void doStop() throws Exception {
        selectionKey.cancel();
        serverSocketChannel.close();
        state = STOPPED;
    }

    public void doFail() {
        state = STOPPED;        
    }

    public void selectionEvent(SelectionKey selection) {
        if (selection.isAcceptable()) {
            try {
                ServerSocketChannel server = (ServerSocketChannel) selection.channel();
                SocketChannel channel = server.accept();

                if (channel == null) return;

                channel.socket().setTcpNoDelay(enableTcpNoDelay);
                acceptorListener.accept(channel);

                selectorManager.setInterestOps(selectionKey, SelectionKey.OP_ACCEPT, 0);
            } catch (IOException e) {
                // this should be ok
            }

        }
    }

    private static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoFactory infoFactory = new GBeanInfoFactory(ServerSocketAcceptor.class.getName());

        infoFactory.addAttribute("uri", true);
        infoFactory.addAttribute("serverSocketChannel", true);
        infoFactory.addAttribute("timeOut", true);
        infoFactory.addAttribute("enableTcpNoDelay", true);
        infoFactory.addAttribute("acceptorListener", true);
        infoFactory.addAttribute("selectorManager", true);
        infoFactory.addOperation("getConnectURI");

        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
