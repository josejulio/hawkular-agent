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
package org.hawkular.agent.monitor.server.controller;

import org.hawkular.agent.monitor.storage.CacheInventoryStorage;
import org.hawkular.agent.monitor.storage.CacheMetricStorage;
import org.restexpress.route.RouteBuilder;

import io.netty.handler.codec.http.HttpMethod;

/**
 * Controller that isn't required to be public.
 * @see CachedMetricsAndInventoryController
 */
public class SecuredCachedMetricsAndInventoryController extends CachedMetricsAndInventoryController {

    public SecuredCachedMetricsAndInventoryController(
            CacheMetricStorage metricStorage,
            CacheInventoryStorage inventoryStorage) {
        super("secure-" + ENDPOINT, metricStorage, inventoryStorage);
    }

    @Override
    public void configureRouteBuilder(RouteBuilder builder) {
        builder.action(ENDPOINT, HttpMethod.GET)
                .noSerialization();
    }
}
