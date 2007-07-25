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
package org.apache.geronimo.tomcat.connector;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.management.geronimo.WebManager;
import org.apache.geronimo.system.serverinfo.ServerInfo;
import org.apache.geronimo.tomcat.BaseGBean;
import org.apache.geronimo.tomcat.ObjectRetriever;
import org.apache.geronimo.tomcat.TomcatContainer;

public class ConnectorGBean extends BaseGBean implements GBeanLifecycle, ObjectRetriever, CommonProtocol {

    private static final Log log = LogFactory.getLog(ConnectorGBean.class);

    public final static String CONNECTOR_CONTAINER_REFERENCE = "TomcatContainer";

    protected final ServerInfo serverInfo;
    
    protected final Connector connector;

    private final TomcatContainer container;

    private String name;

    public ConnectorGBean(String name, String protocol, TomcatContainer container, ServerInfo serverInfo) throws Exception {
        super();

        // Do we really need this?? For Tomcat I don't think so...
        // validateProtocol(protocol);

        if (name == null) {
            throw new IllegalArgumentException("name cannot be null.");
        }

        if (container == null) {
            throw new IllegalArgumentException("container cannot be null.");
        }

        if (serverInfo == null){
            throw new IllegalArgumentException("serverInfo cannot be null.");
        }
        
        initProtocol();

        this.name = name;
        this.container = container;
        this.serverInfo = serverInfo;

        // Create the Connector object
        connector = new Connector(protocol);

    }

    public void doFail() {
        log.warn(name + " connector failed");
        doStop();
    }

    public void doStart() throws LifecycleException {
        container.addConnector(connector);
        connector.start();
        if (log.isDebugEnabled())
            log.debug(name + " connector started");
    }

    public void doStop() {
        try {
            connector.stop();
        } catch (LifecycleException e) {
            log.error(e);
        }

        container.removeConnector(connector);

        if (log.isDebugEnabled())
            log.debug(name + " connector stopped");
    }
    
    /**
     * Ensures that this implementation can handle the requested protocol.
     * @param protocol
     */
    protected void initProtocol() {}
    
    public Object getInternalObject() {
        return connector;
    }

    public String getName() {
        return name;
    }

    public void setAllowTrace(boolean allow) {
        connector.setAllowTrace(allow);
    }

    public boolean getAllowTrace() {
        return connector.getAllowTrace();
    }

    public void setEmptySessionPath(boolean emptySessionPath) {
        connector.setEmptySessionPath(emptySessionPath);
    }

    public void setEnableLookups(boolean enabled) {
        connector.setEnableLookups(enabled);
    }

    public int getMaxPostSize() {
        int value = connector.getMaxPostSize();
        return value == 0 ? 2097152 : value;
    }

    public void setMaxPostSize(int bytes) {
        connector.setMaxPostSize(bytes);
    }

    public String getProtocol() {
        return connector.getProtocol();
    }

    public String getProxyName() {
        return connector.getProxyName();
    }

    public int getProxyPort() {
        return connector.getProxyPort();
    }

    public int getRedirectPort() {
        return connector.getRedirectPort();
    }

    public String getScheme() {
        return connector.getScheme();
    }

    public boolean getSecure() {
        return connector.getSecure();
    }

    public String getUriEncoding() {
        return connector.getURIEncoding();
    }

    public boolean getUseBodyEncodingForURI() {
        return connector.getUseBodyEncodingForURI();
    }

    public boolean getUseIPVHosts() {
        return connector.getUseIPVHosts();
    }

    public void setMaxSavePostSize(int maxSavePostSize) {
        connector.setMaxSavePostSize(maxSavePostSize);
    }

    public void setProxyName(String proxyName) {
        connector.setProxyName(proxyName);
    }

    public void setProxyPort(int port) {
        connector.setProxyPort(port);
    }

    public void setRedirectPort(int port) {
        connector.setRedirectPort(port);
    }

    public void setScheme(String scheme) {
        connector.setScheme(scheme);
    }

    public void setSecure(boolean secure) {
        connector.setSecure(secure);
    }
    
    public boolean getSslEnabled() {
        Object value = connector.getAttribute("SSLEnabled");
        return value == null ? false : new Boolean(value.toString()).booleanValue();
    }

    public void setSslEnabled(boolean sslEnabled) {
        connector.setAttribute("SSLEnabled", sslEnabled);
    }

    public void setUriEncoding(String uriEncoding) {
        connector.setURIEncoding(uriEncoding);
    }

    public void setUseBodyEncodingForURI(boolean useBodyEncodingForURI) {
        connector.setUseBodyEncodingForURI(useBodyEncodingForURI);
    }

    public void setUseIPVHosts(boolean useIPVHosts) {
        connector.setUseIPVHosts(useIPVHosts);
    }

    public void setXpoweredBy(boolean xpoweredBy) {
        connector.setXpoweredBy(xpoweredBy);
    }

    public boolean getEnableLookups() {
        return connector.getEnableLookups();
    }

    public int getMaxSavePostSize() {
        int value = connector.getMaxSavePostSize();
        return value == 0 ? 4096 : value;
    }

    public boolean getEmptySessionPath() {
        return connector.getEmptySessionPath();
    }

    public boolean getXpoweredBy() {
        return connector.getXpoweredBy();
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic("Tomcat Connector", ConnectorGBean.class);

        infoFactory.addAttribute("name", String.class, true);
        infoFactory.addAttribute("protocol", String.class, true);
        infoFactory.addReference(CONNECTOR_CONTAINER_REFERENCE, TomcatContainer.class, NameFactory.GERONIMO_SERVICE);
        infoFactory.addReference("ServerInfo", ServerInfo.class, "GBean");
        infoFactory.addInterface(ObjectRetriever.class);
        infoFactory.addInterface(CommonProtocol.class,
                
                new String[]{
                        "allowTrace",
                        "emptySessionPath",
                        "enableLookups",
                        "maxPostSize",
                        "maxSavePostSize",
                        "protocol",
                        "proxyName",
                        "proxyPort",
                        "redirectPort",
                        "scheme",
                        "secure",
                        "sslEnabled",
                        "uriEncoding",
                        "useBodyEncodingForURI",
                        "useIPVHosts",
                        "xpoweredBy"
                },

                new String[]{
                        "allowTrace",
                        "emptySessionPath",
                        "enableLookups",
                        "maxPostSize",
                        "maxSavePostSize",
                        "protocol",
                        "proxyName",
                        "proxyPort",
                        "redirectPort",
                        "scheme",
                        "secure",
                        "sslEnabled",
                        "uriEncoding",
                        "useBodyEncodingForURI",
                        "useIPVHosts",
                        "xpoweredBy"
                }
        );
        infoFactory.setConstructor(new String[] { "name", "protocol", "TomcatContainer", "ServerInfo" });
        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }

}
