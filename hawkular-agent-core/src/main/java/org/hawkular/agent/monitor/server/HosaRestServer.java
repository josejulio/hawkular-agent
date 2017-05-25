/*
 * Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.agent.monitor.server;

import org.hawkular.agent.monitor.server.controller.CachedMetricsAndInventoryController;
import org.hawkular.agent.monitor.server.controller.SecuredCachedMetricsAndInventoryController;
import org.hawkular.agent.monitor.storage.CacheInventoryStorage;
import org.hawkular.agent.monitor.storage.CacheMetricStorage;
import org.hawkular.agent.monitor.util.BaseRestServerGenerator;

/**
 * HOSA rest server with unsecured and secured controllers.
 */
public class HosaRestServer extends AgentRestServer {

    public HosaRestServer(BaseRestServerGenerator restServerGenerator,
            CacheMetricStorage metricStorage,
            CacheInventoryStorage inventoryStorage) {
        super(restServerGenerator);

        addController(new CachedMetricsAndInventoryController(metricStorage, inventoryStorage));
        addController(new SecuredCachedMetricsAndInventoryController(metricStorage, inventoryStorage));

        // addController(new EchoController());
        // addController(new SecuredEchoController());
    }
}
