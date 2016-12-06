/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.agent.monitor.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.inject.Injector;

public class WildflyCompabilityUtils {

    public static void subsystemSetHostCapable(SubsystemRegistration subsystem) {
        try {
            Method setHostCapableMethod = SubsystemRegistration.class.getMethod("setHostCapable");
            try {
                setHostCapableMethod.invoke(subsystem);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } catch (NoSuchMethodException _nsme) {
            // This method doesn't exist on EAP6.
        }
    }

    public static ManagedReferenceFactory getImmediateManagedReferenceFactory(Object jndiObject) {
        try {
            Class<?> immediateManagedReferenceFactory =
                    Class.forName("org.jboss.as.naming.ImmediateManagedReferenceFactory");
            Constructor<?> constructor = immediateManagedReferenceFactory.getConstructor(Object.class);
            return (ManagedReferenceFactory) constructor.newInstance(jndiObject);
        } catch (ClassNotFoundException _cnfe) {
            // This class does not exist on EAP6.
            return new EAP6ImmediateManagedReferenceFactory(jndiObject);
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Injector<ManagedReferenceFactory> getObjectInjectorFromBinderService(BinderService binderService) {
        try {
            Method getManagedObjectInjectorMethod = BinderService.class.getMethod("getManagedObjectInjector");
            return (Injector<ManagedReferenceFactory>) getManagedObjectInjectorMethod.invoke(binderService);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Injector<ServiceBasedNamingStore> getNamingStoreInjectorFromBinderService(BinderService binderService) {
        try {
            Method getManagedObjectInjectorMethod = BinderService.class.getMethod("getNamingStoreInjector");
            return (Injector<ServiceBasedNamingStore>) getManagedObjectInjectorMethod.invoke(binderService);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ManagedReferenceFactory based on:
     * https://github.com/wildfly/wildfly/blob/c6f48cac0710457d9374eb24cc82093899bdd7bf/naming/src/main/java/org/jboss/as/naming/ImmediateManagedReferenceFactory.java
     */
    private static class EAP6ImmediateManagedReferenceFactory implements ManagedReferenceFactory {
        private final ManagedReference reference;

        public EAP6ImmediateManagedReferenceFactory(final Object instance) {
            this.reference = new EAP6ImmediateManagedReference(instance);
        }

        @Override
        public ManagedReference getReference() {
            return this.reference;
        }
    }

    /**
     * ManagedReference based on:
     * https://github.com/wildfly/wildfly/blob/2b6526250242810b9075c3dc79c5f06e6ea8cfe4/naming/src/main/java/org/jboss/as/naming/ImmediateManagedReference.java
     */
    private static class EAP6ImmediateManagedReference implements ManagedReference {
        private final Object instance;

        public EAP6ImmediateManagedReference(final Object instance) {
            this.instance = instance;
        }

        @Override
        public void release() {

        }

        @Override
        public Object getInstance() {
            return this.instance;
        }
    }

}
