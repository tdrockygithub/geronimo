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

package org.apache.geronimo.console.jmxmanager;

import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.management.ObjectName;
import javax.management.j2ee.statistics.BoundaryStatistic;
import javax.management.j2ee.statistics.BoundedRangeStatistic;
import javax.management.j2ee.statistics.CountStatistic;
import javax.management.j2ee.statistics.RangeStatistic;
import javax.management.j2ee.statistics.Statistic;
import javax.management.j2ee.statistics.Stats;
import javax.management.j2ee.statistics.TimeStatistic;

import org.apache.geronimo.console.util.TimeUtils;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.gbean.GAttributeInfo;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GOperationInfo;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.kernel.GBeanNotFoundException;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.KernelRegistry;

/**
 * The JMX manager helper
 */
public class JMXManagerHelper {
    /** Used to return all MBeans */
    private static final String ALL_MBEANS = "AllMBeans";
    private static final String SERVICEMODULE_KEY = "ServiceModule";
    private static final String GBEANINFO_ATTRIB = "GBeanInfo";
    private static final String STATSPROVIDER_ATTRIB = "statisticsProvider";
    private static final String STATS_ATTRIB = "stats";
    

    private final Kernel kernel;

    /**
     * Construct an JMX manager helper (default)
     */
    public JMXManagerHelper() {
        kernel = KernelRegistry.getSingleKernel();
    }

    /**
     * List MBeans using a domain
     */
    public Collection listByDomain(String domain) {
        Collection result = new ArrayList();
        if ((domain == null) || (domain.trim().length() == 0)) {
            return result;
        }

        return listByPattern(domain + ":*");
    }

    /**
     * List MBeans containing a substring in its object name
     */
    public Collection listBySubstring(String substring) {
        Collection result = new ArrayList();
        if ((substring == null) || (substring.trim().length() == 0)) {
            return result;
        }
        
        Collection abstractNames = getAbstractNames(substring);
        for (Iterator it = abstractNames.iterator(); it.hasNext();) {
            AbstractName aname = (AbstractName) it.next();
            ObjectName oname = aname.getObjectName();
            String[] pair = { aname.toString(), oname.toString() };
            result.add(pair);
        }
        
        return result;
    }
    
    /**
     * List MBeans using a pattern (ObjectName)
     */
    public Collection listByPattern(String pattern) {
        Collection result = new ArrayList();
        if ((pattern == null) || (pattern.trim().length() == 0)) {
            return result;
        }

        try {
            // TODO: Use AbstractNameQuery
            // Uses Object names for query pattern to support
            // domain searches. Can't find a way to do it using
            // AbstractNameQuery.
            Map abstractNames = getAbstractNames();
            ObjectName onamePattern = new ObjectName(pattern);
            Set beans = kernel.listGBeans(onamePattern);
            for (Iterator it = beans.iterator(); it.hasNext();) {
                ObjectName oname = (ObjectName) it.next();
                AbstractName aname = (AbstractName) abstractNames.get(oname);
                String[] pair = { aname.toString(), oname.toString() };
                result.add(pair);
            }
        } catch (Exception e) {
            // Malformed object name, just return what you have
        }

        return result;
    }

    /**
     * List MBeans using J2EE type
     */
    public Collection listByJ2EEType(String type) {
        Collection result = new ArrayList();
        Map m = null;

        if ((type == null) || (type.trim().length() == 0)) {
            return result;
        } else {
            if (ALL_MBEANS.equalsIgnoreCase(type)) {
                m = Collections.EMPTY_MAP;
            } else {
                m = Collections.singletonMap(NameFactory.J2EE_TYPE, type);
            }
        }

        AbstractNameQuery query = new AbstractNameQuery(null, m,
                Collections.EMPTY_SET);
        Set beans = kernel.listGBeans(query);
        for (Iterator it = beans.iterator(); it.hasNext();) {
            AbstractName abstractName = (AbstractName) it.next();
            ObjectName objectName = abstractName.getObjectName();
            String[] pair = { abstractName.toString(), objectName.toString() };
            result.add(pair);
        }

        return result;
    }

    /**
     * Return all service modules
     */
    public Collection getServiceModules() {
        Map svcModules = new TreeMap();
        Collection svcModuleMBeans = getAbstractNames(SERVICEMODULE_KEY + '=');
        for (Iterator it = svcModuleMBeans.iterator(); it.hasNext();) {
            AbstractName aname = (AbstractName) it.next();
            String svcModule = aname.getNameProperty(SERVICEMODULE_KEY);
            if (!svcModules.containsKey(svcModule)) {
                svcModules.put(svcModule, null);
            }
        }

        return svcModules.keySet();
    }

    /**
     * Return abstract names containing a substring
     */
    private Collection getAbstractNames(String substring) {
        Collection result = new ArrayList();
        if ((substring == null) || (substring.trim().length() == 0)) {
            return result;
        }

        Map abstractNames = getAbstractNames();
        for (Iterator it = abstractNames.keySet().iterator(); it.hasNext();) {
            ObjectName oname = (ObjectName) it.next();
            if (oname.toString().indexOf(substring) > 0) {
                AbstractName aname = (AbstractName) abstractNames.get(oname);
                result.add(aname);
            }
        }
        
        return result;
    }

    /**
     * Return all abstract names as a map
     */
    private Map getAbstractNames() {
        Map abstractNames = new HashMap();
        // Create Map (Key = ObjectName, Value = AbstractName)
        AbstractNameQuery query = new AbstractNameQuery(null,
                Collections.EMPTY_MAP, Collections.EMPTY_SET);
        Set allBeans = kernel.listGBeans(query);
        for (Iterator it = allBeans.iterator(); it.hasNext();) {
            AbstractName abstractName = (AbstractName) it.next();
            ObjectName objectName = abstractName.getObjectName();
            abstractNames.put(objectName, abstractName);
        }

        return abstractNames;
    }

    /**
     * Return MBean attributes
     */
    public Collection getAttributes(String abstractName) {
        Map attributes = new TreeMap();
        try {
            AbstractName aname = new AbstractName(URI.create(abstractName));
            GBeanInfo info = kernel.getGBeanInfo(aname);
            Set attribs = info.getAttributes();
            for (Iterator i = attribs.iterator(); i.hasNext();) {
                GAttributeInfo attribInfo = (GAttributeInfo) i.next();
                // Don't include 'GBeanInfo' attributes
                String attribName = attribInfo.getName();
                if (!GBEANINFO_ATTRIB.equals(attribName)) {
                    Map attribInfoMap = getAttribInfoAsMap(aname, attribInfo);
                    attributes.put(attribName, attribInfoMap);
                }
            }
        } catch (GBeanNotFoundException e) {
            // GBean not found, just ignore
        }

        return attributes.values();
    }

    /**
     * Return attribute info as map
     */
    private Map getAttribInfoAsMap(AbstractName abstractName,
            GAttributeInfo attribInfo) {
        Map map = new TreeMap();
        String attribName = attribInfo.getName();
        map.put("name", attribName);
        map.put("getterName", attribInfo.getGetterName());
        map.put("setterName", attribInfo.getSetterName());
        map.put("type", attribInfo.getType());
        map.put("manageable", String.valueOf(attribInfo.isManageable()));
        map.put("persistent", String.valueOf(attribInfo.isPersistent()));
        map.put("readable", String.valueOf(attribInfo.isReadable()));
        map.put("writable", String.valueOf(attribInfo.isWritable()));
        if (attribInfo.isReadable()) {
            String attribValue = "";
            try {
                Object value = kernel.getAttribute(abstractName, attribName);
                if (value != null) {
                    if (value instanceof String[]) {
                        attribValue = Arrays.asList((String[]) value)
                                .toString();
                    } else {
                        attribValue = value.toString();
                    }
                }
            } catch (Exception e) {
                // GBean or attribute not found, just ignore
                attribValue = "** EXCEPTION: " + e;
            }
            map.put("value", attribValue);
        }
        return map;
    }

    /**
     * Return MBean operations
     */
    public Collection getOperations(String abstractName) {
        Map operations = new TreeMap();
        try {
            AbstractName aname = new AbstractName(URI.create(abstractName));
            GBeanInfo info = kernel.getGBeanInfo(aname);
            Set opers = info.getOperations();
            for (Iterator i = opers.iterator(); i.hasNext();) {
                GOperationInfo operInfo = (GOperationInfo) i.next();
                Map operInfoMap = getOperInfoAsMap(operInfo);
                String operName = (String) operInfoMap.get("name");
                operations.put(operName, operInfoMap);
            }
        } catch (Exception e) {
            // GBean not found, just ignore
        }

        return operations.values();
    }

    /**
     * Return operation info as map
     */
    private Map getOperInfoAsMap(GOperationInfo operInfo) {
        Map map = new TreeMap();
        map.put("methodName", operInfo.getMethodName());
        map.put("name", operInfo.getName());
        map.put("parameterList", operInfo.getParameterList());
        return map;
    }

    /**
     * Return MBean basic info
     */
    public Collection getMBeanInfo(String abstractName) {
        Collection info = new ArrayList();
        try {
            AbstractName aname = new AbstractName(URI.create(abstractName));
            info.add(new String[] { "abstractName", aname.toString() });
            ObjectName oname = aname.getObjectName();
            info.add(new String[] { "objectName", oname.toString() });
            GBeanInfo beanInfo = kernel.getGBeanInfo(aname);
            String className = beanInfo.getClassName();
            info.add(new String[] { "className", className });
            String domain = oname.getDomain();
            info.add(new String[] { "domain", domain });
            String j2eeType = beanInfo.getJ2eeType();
            info.add(new String[] { "j2eeType", j2eeType });
            // String sourceClass = beanInfo.getSourceClass();
            // info.add(new String[] { "sourceClass", sourceClass });
        } catch (Exception e) {
            // GBean not found, just ignore
        }

        return info;
    }

    /**
     * Return all MBeans that provide stats
     */
    public Collection getStatsProvidersMBeans() {
        Collection result = new ArrayList();

        Object[] allMBeans = listByPattern("*:*").toArray();
        for (int i = 0; i < allMBeans.length; i++) {
            try {
                String[] mbean = (String[]) allMBeans[i];
                AbstractName abstractName = new AbstractName(URI
                        .create(mbean[0]));
                Boolean statisticsProvider = (Boolean) kernel.getAttribute(
                        abstractName, "statisticsProvider");
                if (Boolean.TRUE.equals(statisticsProvider)) {
                    result.add(mbean);
                }
            } catch (Exception e) {
                // ignore
            }
        }

        return result;
    }

    /**
     * Return MBean stats
     */
    public Collection getMBeanStats(String abstractName) {
        Map mbeanStats = new TreeMap();
        try {
            AbstractName aname = new AbstractName(URI.create(abstractName));
            Boolean statisticsProvider = (Boolean) kernel.getAttribute(aname,
                    STATSPROVIDER_ATTRIB);
            Stats stats = (Stats) kernel.getAttribute(aname, STATS_ATTRIB);
            if (statisticsProvider.booleanValue() == true && (stats != null)) {
                String[] statisticNames = stats.getStatisticNames();
                for (int i = 0; i < statisticNames.length; i++) {
                    Statistic statistic = stats.getStatistic(statisticNames[i]);

                    Collection mbeanStat = new ArrayList();
                    String name = statistic.getName();
                    mbeanStat.add(new String[] { "Name", name });
                    String className = statistic.getClass().getName();
                    // mbeanStat.add(new String[] { "Type", className });
                    mbeanStat.add(new String[] { "Description",
                            statistic.getDescription() });
                    mbeanStat.add(new String[] { "Unit", statistic.getUnit() });
                    Date startTime = new Date(statistic.getStartTime());
                    mbeanStat.add(new String[] { "Start Time",
                            startTime.toString() });
                    Date lastSampleTime = new Date(statistic
                            .getLastSampleTime());
                    mbeanStat.add(new String[] { "Last Sample Time",
                            lastSampleTime.toString() });

                    if (statistic instanceof CountStatistic) {
                        CountStatistic cStat = (CountStatistic) statistic;
                        long count = cStat.getCount();
                        mbeanStat.add(new String[] { "Count",
                                Long.toString(count) });
                    } else if (statistic instanceof TimeStatistic) {
                        TimeStatistic tStat = (TimeStatistic) statistic;
                        long count = tStat.getCount();
                        mbeanStat.add(new String[] { "Count",
                                Long.toString(count) });
                        String maxTime = TimeUtils.formatDuration(tStat.getMaxTime());
                        mbeanStat.add(new String[] { "Max Time",
                                maxTime });
                        String minTime = TimeUtils.formatDuration(tStat.getMinTime());
                        mbeanStat.add(new String[] { "Min Time",
                                minTime });
                        long totalTime = tStat.getTotalTime();
                        mbeanStat.add(new String[] { "Total Time",
                                Long.toString(totalTime) });
                    } else if (statistic instanceof BoundedRangeStatistic) {
                        BoundedRangeStatistic brStat = (BoundedRangeStatistic) statistic;
                        long upperBound = brStat.getUpperBound();
                        mbeanStat.add(new String[] { "Upper Bound",
                                Long.toString(upperBound) });
                        long lowerBound = brStat.getLowerBound();
                        mbeanStat.add(new String[] { "Lower Bound",
                                Long.toString(lowerBound) });
                        long highWaterMark = brStat.getHighWaterMark();
                        mbeanStat.add(new String[] { "High Water Mark",
                                Long.toString(highWaterMark) });
                        long lowWaterMark = brStat.getLowWaterMark();
                        mbeanStat.add(new String[] { "Low Water Mark",
                                Long.toString(lowWaterMark) });
                        long current = brStat.getCurrent();
                        mbeanStat.add(new String[] { "Current",
                                Long.toString(current) });
                    } else if (statistic instanceof BoundaryStatistic) {
                        BoundaryStatistic bStat = (BoundaryStatistic) statistic;
                        long upperBound = bStat.getUpperBound();
                        mbeanStat.add(new String[] { "Upper Bound",
                                Long.toString(upperBound) });
                        long lowerBound = bStat.getLowerBound();
                        mbeanStat.add(new String[] { "Lower Bound",
                                Long.toString(lowerBound) });
                    } else if (statistic instanceof RangeStatistic) {
                        RangeStatistic rStat = (RangeStatistic) statistic;
                        long highWaterMark = rStat.getHighWaterMark();
                        mbeanStat.add(new String[] { "High Water Mark",
                                Long.toString(highWaterMark) });
                        long lowWaterMark = rStat.getLowWaterMark();
                        mbeanStat.add(new String[] { "Low Water Mark",
                                Long.toString(lowWaterMark) });
                        long current = rStat.getCurrent();
                        mbeanStat.add(new String[] { "Current",
                                Long.toString(current) });
                    }

                    mbeanStats.put(name, mbeanStat);
                }
            }
        } catch (Exception e) {
            // GBean not found, just ignore
        }

        return mbeanStats.values();
    }

    /**
     * Invoke MBean operation with arguments
     */
    public String[] invokeOperWithArgs(String abstractName, String methodName,
            String[] args, String[] types) {
        String[] result = new String[2]; // return method name & result
        result[0] = methodName + "(...)";

        try {
            Object[] newArgs = processOperArgs(args, types);
            AbstractName aname = new AbstractName(URI.create(abstractName));
            Object res = kernel.invoke(aname, methodName, newArgs, types);
            if (result != null) {
                result[1] = res.toString();
            } else {
                result[1] = "<null>";
            }
        } catch (Exception e) {
            result[1] = e.toString();
        }

        return result;
    }

    /**
     * Invoke MBean operation without arguments
     */
    public String[] invokeOperNoArgs(String abstractName, String methodName) {
        String[] result = new String[2]; // return method name & result
        result[0] = methodName + "()";

        try {
            AbstractName aname = new AbstractName(URI.create(abstractName));
            Object res = kernel.invoke(aname, methodName);
            if (result != null) {
                result[1] = res.toString();
            } else {
                result[1] = "<null>";
            }
        } catch (Exception e) {
            result[1] = e.toString();
        }

        return result;
    }

    /**
     * Process MBean operation arguments
     */
    private Object[] processOperArgs(String[] args, String[] types)
            throws Exception {
        // TODO: Modify this algorithm and add other classes
        Object[] newArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            String type = types[i];
            String arg = args[i];
            newArgs[i] = createObject(arg, type);
        }

        return newArgs;
    }

    /**
     * Create MBean operation argument
     */
    private Object createObject(String arg, String type) throws Exception {
        Object newArg = new Object();
        if ("byte".equals(type) || "java.lang.Byte".equals(type)) {
            newArg = new Byte(arg);
        } else if ("short".equals(type) || "java.lang.Short".equals(type)) {
            newArg = new Short(arg);
        } else if ("int".equals(type) || "java.lang.Integer".equals(type)) {
            newArg = new Integer(arg);
        } else if ("long".equals(type) || "java.lang.Long".equals(type)) {
            newArg = new Long(arg);
        } else if ("float".equals(type) || "java.lang.Float".equals(type)) {
            newArg = new Float(arg);
        } else if ("double".equals(type) || "java.lang.Double".equals(type)) {
            newArg = new Double(arg);
        } else if ("char".equals(type) || "java.lang.Character".equals(type)) {
            newArg = new Character(arg.charAt(0));
        } else if ("boolean".equals(type) || "java.lang.Boolean".equals(type)) {
            newArg = new Boolean(arg);
        } else if ("java.lang.String".equals(type)) {
            newArg = arg;
        } else if ("java.lang.Object".equals(type)) {
            newArg = arg;
        } else if ("java.util.Date".equals(type)) {
            newArg = DateFormat.getInstance().parse(arg);
        } else if ("java.net.URL".equals(type)) {
            newArg = new URL(arg);
        } else if ("java.net.URI".equals(type)) {
            newArg = new URI(arg);
        } else if ("javax.management.ObjectName".equals(type)) {
            newArg = new ObjectName(arg);
        } else if ("org.apache.geronimo.gbean.AbstractName".equals(type)) {
            newArg = new AbstractName(URI.create(arg));
        } else {
            // Unknown type, throw exception
            String errorMsg = "Can't create instance of '" + type + "' using '"
                    + arg + "'.";
            throw new IllegalArgumentException(errorMsg);
        }

        return newArg;
    }

    /**
     * Set MBean attribute value
     */
    public String[] setAttribute(String abstractName, String attribName,
            String attribValue, String attribType) {
        String[] result = new String[2]; // return attribute name & result
        result[0] = attribName;
        result[1] = "<SUCCESS>"; 
        
        try {
            AbstractName aname = new AbstractName(URI.create(abstractName));
            Object newAttribValue = createObject(attribValue, attribType);
            kernel.setAttribute(aname, attribName, newAttribValue);
        } catch (Exception e) {
            result[1] = e.toString();
        }

        return result;
    }

}
