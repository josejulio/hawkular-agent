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
import org.restexpress.Flags;
import org.restexpress.Request;
import org.restexpress.Response;
import org.restexpress.route.RouteBuilder;

import io.netty.handler.codec.http.HttpMethod;

/**
 * Emits JSON data of all cached metrics and inventory.
 * This controller is required to be public (no auth needed).
 * Used by HOSA as our proxy to storage.
 */

public class CachedMetricsAndInventoryController extends AgentRestController {
    protected static final String ENDPOINT = "cache";

    private final CacheMetricStorage metricStorage;
    private final CacheInventoryStorage inventoryStorage;

    public CachedMetricsAndInventoryController(
            CacheMetricStorage metricStorage,
            CacheInventoryStorage inventoryStorage) {
        this(ENDPOINT, metricStorage, inventoryStorage);
    }

    public CachedMetricsAndInventoryController(
            String endpoint,
            CacheMetricStorage metricStorage,
            CacheInventoryStorage inventoryStorage) {
        super(endpoint);
        this.metricStorage = metricStorage;
        this.inventoryStorage = inventoryStorage;
    }

    @Override
    public void configureRouteBuilder(RouteBuilder builder) {
        builder.action(ENDPOINT, HttpMethod.GET)
                .flag(Flags.Auth.PUBLIC_ROUTE)
                .noSerialization();
    }

    public String cache(Request request, Response response) {
        StringBuilder json = new StringBuilder();
        json.append("{\"metrics\":");
        json.append(this.metricStorage.getCacheAsJsonString());
        json.append(",\"inventory\":");
        json.append(this.inventoryStorage.getCacheAsJsonString());
        json.append("}");
        return json.toString();
    }
}
