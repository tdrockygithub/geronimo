/**
 *
 * Copyright 2005 The Apache Software Foundation
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
package org.apache.geronimo.deployment.xmlbeans;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.SchemaTypeLoader;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.QNameSet;
import org.apache.xmlbeans.XmlValidationError;

/**
 * @version $Rev$ $Date$
 */
public class XmlBeansUtil {
    private static final Map NAMESPACE_UPDATES = new HashMap();
    //TODO thread safe? conncurrentReaderMap?
    private static final Map substitutionGroups = new HashMap();
    private static final XmlObject[] NO_ELEMENTS = new XmlObject[]{};

    static {
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/j2ee/application-client", "http://geronimo.apache.org/xml/ns/j2ee/application-client-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/j2ee/application", "http://geronimo.apache.org/xml/ns/j2ee/application-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/deployment", "http://geronimo.apache.org/xml/ns/deployment-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/j2ee/connector", "http://geronimo.apache.org/xml/ns/j2ee/connector-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/deployment/javabean", "http://geronimo.apache.org/xml/ns/deployment/javabean-1.0");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/loginconfig", "http://geronimo.apache.org/xml/ns/loginconfig-1.0");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/naming", "http://geronimo.apache.org/xml/ns/naming-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/security", "http://geronimo.apache.org/xml/ns/security-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/web", "http://geronimo.apache.org/xml/ns/j2ee/web-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/web/jetty", "http://geronimo.apache.org/xml/ns/j2ee/web/jetty-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/web/jetty/config", "http://geronimo.apache.org/xml/ns/j2ee/web/jetty/config-1.0");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/web/tomcat", "http://geronimo.apache.org/xml/ns/j2ee/web/tomcat-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/web/tomcat/config", "http://geronimo.apache.org/xml/ns/j2ee/web/tomcat/config-1.0");
        NAMESPACE_UPDATES.put("http://www.openejb.org/xml/ns/openejb-jar", "http://www.openejb.org/xml/ns/openejb-jar-2.1");
        NAMESPACE_UPDATES.put("http://www.openejb.org/xml/ns/pkgen", "http://www.openejb.org/xml/ns/pkgen-2.0");
        NAMESPACE_UPDATES.put("http://www.openejb.org/xml/ns/corba-css-config_1_0", "http://www.openejb.org/xml/ns/corba-css-config-2.0");
        NAMESPACE_UPDATES.put("http://www.openejb.org/xml/ns/corba-tss-config_1_0", "http://www.openejb.org/xml/ns/corba-tss-config-2.0");
    }

    private XmlBeansUtil() {
    }


    public static XmlObject parse(File file) throws IOException, XmlException {
        ArrayList errors = new ArrayList();
        XmlObject parsed = XmlObject.Factory.parse(file, createXmlOptions(errors));
        if (errors.size() != 0) {
            throw new XmlException(errors.toArray().toString());
        }
        return parsed;
    }

    public static XmlObject parse(URL url, ClassLoader cl) throws IOException, XmlException {
        ArrayList errors = new ArrayList();
        Thread currentThread = Thread.currentThread();
        ClassLoader oldcl = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(cl);
        XmlObject parsed;
        try {
            parsed = XmlObject.Factory.parse(url, createXmlOptions(errors));
        } finally {
            currentThread.setContextClassLoader(oldcl);
        }
        if (errors.size() != 0) {
            throw new XmlException(errors.toArray().toString());
        }
        return parsed;
    }

    public static XmlObject parse(InputStream is) throws IOException, XmlException {
        ArrayList errors = new ArrayList();
        XmlObject parsed = XmlObject.Factory.parse(is, createXmlOptions(errors));
        if (errors.size() != 0) {
            throw new XmlException(errors.toArray().toString());
        }
        return parsed;
    }

    public static XmlObject parse(String xml) throws XmlException {
        ArrayList errors = new ArrayList();
        XmlObject parsed = XmlObject.Factory.parse(xml, createXmlOptions(errors));
        if (errors.size() != 0) {
            throw new XmlException(errors.toArray().toString());
        }
        return parsed;
    }

    public static XmlObject parse(Element element) throws XmlException {
        ArrayList errors = new ArrayList();
        XmlObject parsed = XmlObject.Factory.parse(element, createXmlOptions(errors));
        if (errors.size() != 0) {
            throw new XmlException(errors.toArray().toString());
        }
        return parsed;
    }

    public static XmlOptions createXmlOptions(Collection errors) {
        XmlOptions options = new XmlOptions();
        options.setLoadLineNumbers();
        options.setErrorListener(errors);
        options.setLoadSubstituteNamespaces(NAMESPACE_UPDATES);
        return options;
    }

    public static void registerSubstitutionGroupElements(QName substitutionGroup, QNameSet substitutions) {
        QNameSet oldSubstitutions = (QNameSet) substitutionGroups.get(substitutionGroup);
        if (oldSubstitutions != null) {
            substitutions = oldSubstitutions.union(substitutions);
        }
        substitutionGroups.put(substitutionGroup, substitutions);
    }

    public static void unregisterSubstitutionGroupElements(QName substitutionGroup, QNameSet substitutions) {
        QNameSet oldSubstitutions = (QNameSet) substitutionGroups.get(substitutionGroup);
        QNameSet difference = oldSubstitutions.intersect(substitutions.inverse());
        substitutionGroups.put(substitutionGroup, difference);
    }

    public static QNameSet getQNameSetForSubstitutionGroup(QName substitutionGroup) {
        return (QNameSet) substitutionGroups.get(substitutionGroup);
    }

    public static XmlObject[] selectSubstitutionGroupElements(QName substitutionGroup, XmlObject container) {
        QNameSet substitutionGroupMembers = getQNameSetForSubstitutionGroup(substitutionGroup);
        if (substitutionGroupMembers == null) {
            return NO_ELEMENTS;
        }
        return container.selectChildren(substitutionGroupMembers);
    }

    public static void validateDD(XmlObject dd) throws XmlException {
        XmlOptions xmlOptions = new XmlOptions();
        xmlOptions.setLoadLineNumbers();
        Collection errors = new ArrayList();
        xmlOptions.setErrorListener(errors);
        try {
            if (!dd.validate(xmlOptions)) {

                for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
                    Object o = iterator.next();
                    if (o instanceof XmlValidationError) {
                        XmlValidationError validationError = (XmlValidationError) o;
                        List expected = validationError.getExpectedQNames();
                        QName actual = validationError.getOffendingQName();
                        if (expected != null) {
                            for (Iterator iterator1 = expected.iterator(); iterator1.hasNext();) {
                                QName expectedQName = (QName) iterator1.next();
                                QNameSet substitutions = getQNameSetForSubstitutionGroup(expectedQName);
                                if (substitutions != null && substitutions.contains(actual)) {
                                    iterator.remove();
                                    break;
                                }
                            }
                        }
                    }
                }

                if (!errors.isEmpty()) {
                    throw new XmlException("Invalid deployment descriptor: " + errors + "\nDescriptor: " + dd.toString(), null, errors);
                }
            }
        } catch (NullPointerException e) {
            //ignore
        }
//        System.out.println("descriptor: " + dd.toString());
    }
}
