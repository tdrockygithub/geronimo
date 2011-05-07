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
package org.apache.geronimo.persistence.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.gbean.annotation.GBean;
import org.apache.geronimo.gbean.annotation.ParamAttribute;
import org.apache.geronimo.j2ee.deployment.EARContext;
import org.apache.geronimo.j2ee.deployment.JndiPlan;
import org.apache.geronimo.j2ee.deployment.Module;
import org.apache.geronimo.j2ee.deployment.annotation.PersistenceContextAnnotationHelper;
import org.apache.geronimo.j2ee.deployment.annotation.PersistenceUnitAnnotationHelper;
import org.apache.geronimo.j2ee.deployment.model.naming.PatternType;
import org.apache.geronimo.j2ee.deployment.model.naming.PersistenceContextRefType;
import org.apache.geronimo.j2ee.deployment.model.naming.PersistenceContextTypeType;
import org.apache.geronimo.j2ee.deployment.model.naming.PersistenceUnitRefType;
import org.apache.geronimo.j2ee.deployment.model.naming.PropertyType;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.kernel.GBeanNotFoundException;
import org.apache.geronimo.kernel.config.Configuration;
import org.apache.geronimo.naming.deployment.AbstractNamingBuilder;
import org.apache.geronimo.naming.reference.PersistenceContextReference;
import org.apache.geronimo.naming.reference.PersistenceUnitReference;
import org.apache.openejb.jee.InjectionTarget;
import org.apache.openejb.jee.JndiConsumer;
import org.apache.openejb.jee.PersistenceContextRef;
import org.apache.openejb.jee.PersistenceContextType;
import org.apache.openejb.jee.PersistenceRef;
import org.apache.openejb.jee.PersistenceUnitRef;
import org.apache.openejb.jee.Property;

/**
 * @version $Rev$ $Date$
 */
@GBean(j2eeType = NameFactory.MODULE_BUILDER)
public class PersistenceRefBuilder extends AbstractNamingBuilder {
    private static final Set PERSISTENCE_UNIT_INTERFACE_TYPES = Collections.singleton("org.apache.geronimo.persistence.PersistenceUnitGBean");
    private final AbstractNameQuery defaultPersistenceUnitAbstractNameQuery;
    private final boolean strictMatching;


    public PersistenceRefBuilder(@ParamAttribute(name = "defaultPersistenceUnitAbstractNameQuery") AbstractNameQuery defaultPersistenceUnitAbstractNameQuery,
                                 @ParamAttribute(name = "strictMatching") boolean strictMatching) {
        this.defaultPersistenceUnitAbstractNameQuery = defaultPersistenceUnitAbstractNameQuery;
        this.strictMatching = strictMatching;
    }

    public void buildNaming(JndiConsumer specDD, JndiPlan plan, Module module, Map<EARContext.Key, Object> sharedContext) throws DeploymentException {
        Configuration localConfiguration = module.getEarContext().getConfiguration();
        List<DeploymentException> problems = new ArrayList<DeploymentException>();

        // Discover and process any @PersistenceUnitRef and @PersistenceContextRef annotations (if !metadata-complete)
        if (module.getClassFinder() != null) {
            processAnnotations(specDD, module);
        }

        //persistenceUnit refs
        Collection<PersistenceUnitRef> specPersistenceUnitRefsUntyped = specDD.getPersistenceUnitRef();
        Map<String, PersistenceUnitRefType> PersistenceUnitRefsUntyped = getPersistenceUnitRefs(plan);
        for (Map.Entry<String, PersistenceUnitRef> entry : specDD.getPersistenceUnitRefMap().entrySet()) {
            try {
                String persistenceUnitRefName = entry.getKey();
                PersistenceUnitRef persistenceUnitRef = entry.getValue();
                AbstractNameQuery persistenceUnitNameQuery;
                PersistenceUnitRefType PersistenceUnitRef = PersistenceUnitRefsUntyped.remove(persistenceUnitRefName);
                if (PersistenceUnitRef != null) {
                    persistenceUnitNameQuery = findPersistenceUnit(PersistenceUnitRef, localConfiguration);
                } else {
                    persistenceUnitNameQuery = findPersistenceUnitQuery(module, localConfiguration, persistenceUnitRef);
                }

                PersistenceUnitReference reference = new PersistenceUnitReference(module.getConfigId(), persistenceUnitNameQuery);

                put(persistenceUnitRefName, reference, module.getJndiContext(), persistenceUnitRef.getInjectionTarget(), sharedContext);
            } catch (DeploymentException e) {
                problems.add(e);
            }

        }
        //geronimo-only persistence unit refs have no injections
        for (PersistenceUnitRefType PersistenceUnitRef : PersistenceUnitRefsUntyped.values()) {
            try {
                String persistenceUnitRefName = PersistenceUnitRef.getPersistenceUnitRefName();
                AbstractNameQuery persistenceUnitNameQuery = findPersistenceUnit(PersistenceUnitRef, localConfiguration);
                PersistenceUnitReference reference = new PersistenceUnitReference(module.getConfigId(), persistenceUnitNameQuery);
                put(persistenceUnitRefName, reference, module.getJndiContext(), Collections.<InjectionTarget>emptyList(), sharedContext);
            } catch (DeploymentException e) {
                problems.add(e);
            }
        }


        //persistence context refs
        Collection<PersistenceContextRef> specPersistenceContextRefsUntyped = specDD.getPersistenceContextRef();
        Map<String, PersistenceContextRefType> PersistenceContextRefsUntyped = getPersistenceContextRefs(plan);
        for (Map.Entry<String, PersistenceContextRef> entry : specDD.getPersistenceContextRefMap().entrySet()) {
            try {
                String persistenceContextRefName = entry.getKey();
                PersistenceContextRef persistenceContextRef = entry.getValue();
                PersistenceContextType persistenceContextType = persistenceContextRef.getPersistenceContextType();
                boolean transactionScoped = persistenceContextType == null || persistenceContextType.equals(PersistenceContextType.TRANSACTION);

                List<Property> propertyTypes = persistenceContextRef.getPersistenceProperty();
                Map<String, String> properties = new HashMap<String, String>();
                for (Property propertyType : propertyTypes) {
                    String key = propertyType.getName();
                    String value = propertyType.getValue();
                    properties.put(key, value);
                }

                AbstractNameQuery persistenceUnitNameQuery;
                PersistenceContextRefType PersistenceContextRef = PersistenceContextRefsUntyped.remove(persistenceContextRefName);
                if (PersistenceContextRef != null) {
                    persistenceUnitNameQuery = findPersistenceUnit(PersistenceContextRef, localConfiguration);
                    addProperties(PersistenceContextRef, properties);
                } else {
                    persistenceUnitNameQuery = findPersistenceUnitQuery(module, localConfiguration, persistenceContextRef);
                }
                PersistenceContextReference reference = new PersistenceContextReference(module.getConfigId(), persistenceUnitNameQuery, transactionScoped, properties);
                put(persistenceContextRefName, reference, module.getJndiContext(), persistenceContextRef.getInjectionTarget(), sharedContext);
            } catch (DeploymentException e) {
                problems.add(e);
            }
        }

        // Support persistence context refs that are mentioned only in the geronimo plan
        for (PersistenceContextRefType PersistenceContextRef : PersistenceContextRefsUntyped.values()) {
            try {
                String persistenceContextRefName = PersistenceContextRef.getPersistenceContextRefName();
                PersistenceContextTypeType persistenceContextType = PersistenceContextRef.getPersistenceContextType();
                boolean transactionScoped = persistenceContextType == null || !persistenceContextType.equals(PersistenceContextTypeType.EXTENDED);
                Map<String, String> properties = new HashMap<String, String>();
                addProperties(PersistenceContextRef, properties);
                AbstractNameQuery persistenceUnitNameQuery = findPersistenceUnit(PersistenceContextRef, localConfiguration);
                PersistenceContextReference reference = new PersistenceContextReference(module.getConfigId(), persistenceUnitNameQuery, transactionScoped, properties);
                put(persistenceContextRefName, reference, module.getJndiContext(), Collections.<InjectionTarget>emptyList(), sharedContext);
            } catch (DeploymentException e) {
                problems.add(e);
            }

        }

        if (!problems.isEmpty()) {
            throw new DeploymentException("At least one deployment problem:", problems);
        }
    }

    private AbstractNameQuery findPersistenceUnitQuery(Module module, Configuration localConfiguration, PersistenceRef persistenceRef) throws DeploymentException {
        AbstractNameQuery persistenceUnitNameQuery;
        if (persistenceRef.getPersistenceUnitName() != null && !persistenceRef.getPersistenceUnitName().trim().isEmpty()) {
            String persistenceUnitName = persistenceRef.getPersistenceUnitName().trim();
            persistenceUnitNameQuery = findPersistenceUnit(module, localConfiguration, persistenceUnitName);
        } else {
            AbstractName childName = module.getEarContext().getNaming().createChildName(module.getModuleName(), "", NameFactory.PERSISTENCE_UNIT);
            Map<String, String> name = new HashMap<String, String>(childName.getName());
            name.remove(NameFactory.J2EE_NAME);
            persistenceUnitNameQuery = new AbstractNameQuery(null, name, PERSISTENCE_UNIT_INTERFACE_TYPES);
            Set<AbstractNameQuery> patterns = Collections.singleton(persistenceUnitNameQuery);
            LinkedHashSet<GBeanData> gbeans = localConfiguration.findGBeanDatas(localConfiguration, patterns);
            persistenceUnitNameQuery = checkForDefaultPersistenceUnit(gbeans);
            if (gbeans.isEmpty()) {
                gbeans = localConfiguration.findGBeanDatas(patterns);
                persistenceUnitNameQuery = checkForDefaultPersistenceUnit(gbeans);

                if (gbeans.isEmpty()) {
                    if (defaultPersistenceUnitAbstractNameQuery == null) {
                        throw new DeploymentException("No default PersistenceUnit specified, and none located");
                    }
                    persistenceUnitNameQuery = defaultPersistenceUnitAbstractNameQuery;
                }
            }
            checkForGBean(localConfiguration, persistenceUnitNameQuery, false, false);
        }
        return persistenceUnitNameQuery;
    }

    private AbstractNameQuery findPersistenceUnit(Module module, Configuration localConfiguration, String persistenceUnitName) throws DeploymentException {
        AbstractNameQuery persistenceUnitNameQuery;
        //First try unrestricted search using provided name
        Map<String, String> nameMap = new HashMap<String, String>();
        nameMap.put("j2eeType", NameFactory.PERSISTENCE_UNIT);
        nameMap.put("name", persistenceUnitName);
        persistenceUnitNameQuery = new AbstractNameQuery(localConfiguration.getId(), nameMap, PERSISTENCE_UNIT_INTERFACE_TYPES);
        switch (checkForGBean(localConfiguration, persistenceUnitNameQuery, strictMatching, true)) {
            case 1:
                return persistenceUnitNameQuery;
            case 0:
                //try unrestricted search on name persistence/<name>
                persistenceUnitName = "persistence/" + persistenceUnitName;
                nameMap.put("name", persistenceUnitName);
                persistenceUnitNameQuery = new AbstractNameQuery(localConfiguration.getId(), nameMap, PERSISTENCE_UNIT_INTERFACE_TYPES);
                if (1 == checkForGBean(localConfiguration, persistenceUnitNameQuery, false, true)) {
                    return persistenceUnitNameQuery;
                }

            case 2:
        }
        //there was more than one match, and if necessary persistence/ was prepended to the name.
        AbstractName childName = module.getEarContext().getNaming().createChildName(module.getModuleName(), persistenceUnitName, NameFactory.PERSISTENCE_UNIT);
        persistenceUnitNameQuery = new AbstractNameQuery(localConfiguration.getId(), childName.getName(), PERSISTENCE_UNIT_INTERFACE_TYPES);
        checkForGBean(localConfiguration, persistenceUnitNameQuery, false, false);
        return persistenceUnitNameQuery;
    }

    private static int checkForGBean(Configuration localConfiguration, AbstractNameQuery persistenceQuery, boolean allowNone, boolean allowMultiple) throws DeploymentException {
        try {
            localConfiguration.findGBeanData(persistenceQuery);
            return 1;
        } catch (GBeanNotFoundException e) {
            if (e.hasMatches()) {
                if (allowMultiple) {
                    return 2;
                }
                throw new DeploymentException("More than one reference at deploy time for query " + persistenceQuery + ". " + e.getMatches(), e);
            } else if (allowNone) {
                return 0;
            }
            throw new DeploymentException("No references found at deploy time for query " + persistenceQuery, e);
        }
    }

    private static AbstractNameQuery checkForDefaultPersistenceUnit(LinkedHashSet<GBeanData> gbeans) throws DeploymentException {
        AbstractNameQuery persistenceUnitNameQuery = null;
        for (java.util.Iterator it = gbeans.iterator(); it.hasNext();) {
            GBeanData gbean = (GBeanData) it.next();
            AbstractName name = gbean.getAbstractName();
            Map nameMap = name.getName();
            if ("cmp".equals(nameMap.get("name"))) {
                it.remove();
            } else {
                persistenceUnitNameQuery = new AbstractNameQuery(name);
            }
        }
        if (gbeans.size() > 1) {
            throw new DeploymentException("Too many matches for no-name persistence unit: " + gbeans);
        }
        return persistenceUnitNameQuery;
    }

    private void processAnnotations(JndiConsumer specDD, Module module) throws DeploymentException {
        // Process all the annotations for this naming builder type
        PersistenceUnitAnnotationHelper.processAnnotations(specDD, module.getClassFinder());
        PersistenceContextAnnotationHelper.processAnnotations(specDD, module.getClassFinder());
    }

    private AbstractNameQuery findPersistenceUnit(PersistenceUnitRefType PersistenceRef, Configuration localConfiguration) throws DeploymentException {
        AbstractNameQuery persistenceUnitNameQuery;
        if (PersistenceRef.getPersistenceUnitName() != null) {
            String persistenceUnitName = PersistenceRef.getPersistenceUnitName();
            persistenceUnitNameQuery = new AbstractNameQuery(null, Collections.singletonMap("name", persistenceUnitName), PERSISTENCE_UNIT_INTERFACE_TYPES);
        } else {
            PatternType gbeanLocator = PersistenceRef.getPattern();

            persistenceUnitNameQuery = buildAbstractNameQuery(gbeanLocator, null, null, PERSISTENCE_UNIT_INTERFACE_TYPES);
        }
        checkForGBean(localConfiguration, persistenceUnitNameQuery, false, false);
        return persistenceUnitNameQuery;
    }

    private AbstractNameQuery findPersistenceUnit(PersistenceContextRefType persistenceContextRef, Configuration localConfiguration) throws DeploymentException {
        AbstractNameQuery persistenceUnitNameQuery;
        if (persistenceContextRef.getPersistenceUnitName() != null) {
            String persistenceUnitName = persistenceContextRef.getPersistenceUnitName();
            persistenceUnitNameQuery = new AbstractNameQuery(null, Collections.singletonMap("name", persistenceUnitName), PERSISTENCE_UNIT_INTERFACE_TYPES);
        } else {
            PatternType gbeanLocator = persistenceContextRef.getPattern();

            persistenceUnitNameQuery = buildAbstractNameQuery(gbeanLocator, null, null, PERSISTENCE_UNIT_INTERFACE_TYPES);
        }
        checkForGBean(localConfiguration, persistenceUnitNameQuery, false, false);
        return persistenceUnitNameQuery;
    }

    private Map<String, PersistenceUnitRefType> getPersistenceUnitRefs(JndiPlan plan) throws DeploymentException {
        Map<String, PersistenceUnitRefType> map = new HashMap<String, PersistenceUnitRefType>();
        if (plan != null) {
            for (PersistenceUnitRefType ref : plan.getPersistenceUnitRef()) {
                map.put(getJndiName(ref.getPersistenceUnitRefName()), ref);
            }
        }
        return map;
    }

    private void addProperties(PersistenceContextRefType persistenceContextRef, Map<String, String> properties) {
        List<PropertyType> propertyTypes = persistenceContextRef.getProperty();
        for (PropertyType propertyType : propertyTypes) {
            String key = propertyType.getKey();
            String value = propertyType.getValue();
            properties.put(key, value);
        }
    }

    private Map<String, PersistenceContextRefType> getPersistenceContextRefs(JndiPlan plan) throws DeploymentException {
        Map<String, PersistenceContextRefType> map = new HashMap<String, PersistenceContextRefType>();
        if (plan != null) {
            for (PersistenceContextRefType ref : plan.getPersistenceContextRef()) {
                map.put(getJndiName(ref.getPersistenceContextRefName()), ref);
            }
        }
        return map;
    }


}
