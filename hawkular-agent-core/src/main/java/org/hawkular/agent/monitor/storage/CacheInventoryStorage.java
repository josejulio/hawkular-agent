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
package org.hawkular.agent.monitor.storage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.hawkular.agent.monitor.api.InventoryStorage;
import org.hawkular.agent.monitor.config.AgentCoreEngineConfiguration.StorageAdapterConfiguration;
import org.hawkular.agent.monitor.diagnostics.Diagnostics;
import org.hawkular.agent.monitor.log.AgentLoggers;
import org.hawkular.agent.monitor.log.MsgLogger;
import org.hawkular.agent.monitor.util.Util;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An {@link InventoryStorage} that caches discovered inventory in memory.
 */
public class CacheInventoryStorage extends BaseInventoryStorage {

    private static final MsgLogger log = AgentLoggers.getLogger(CacheInventoryStorage.class);

    private class CachedTenantData {
        @JsonProperty("tenant")
        private final String tenant;
        @JsonProperty("data")
        private final String data;

        private CachedTenantData(String tenant, String data) {
            this.tenant = tenant;
            this.data = data;
        }
    }

    // all have keys of encoded metric name
    private final HashSet<String> deletedMetrics;
    private final HashMap<String, CachedTenantData> cachedMetrics;
    private final HashMap<String, CachedTenantData> cachedMetricDefinitions;
    private final Object lock = new Object();

    public CacheInventoryStorage(
            String feedId,
            StorageAdapterConfiguration config,
            int autoDiscoveryScanPeriodSeconds,
            Diagnostics diagnostics) {
        super(feedId, config, autoDiscoveryScanPeriodSeconds, diagnostics);
        this.deletedMetrics = new HashSet<>();
        this.cachedMetrics = new HashMap<>();
        this.cachedMetricDefinitions = new HashMap<>();
    }

    public String getCacheAsJsonString() {
        synchronized (lock) {
            HashMap<String, Object> allData = new HashMap<>(3);
            allData.put("deleted-metrics", deletedMetrics);
            allData.put("metrics", cachedMetrics);
            allData.put("metric-definitions", cachedMetricDefinitions);

            try {
                return Util.toJson(allData);
            } finally {
                deletedMetrics.clear();
                cachedMetrics.clear();
                cachedMetricDefinitions.clear();
            }
        }
    }

    public void shutdown() {
        // DELETEME
        log.fatal("METRICS START");
        log.fatal(getCacheAsJsonString());
        log.fatal("METRICS END");

        synchronized (lock) {
            deletedMetrics.clear();
            cachedMetrics.clear();
            cachedMetricDefinitions.clear();
        }
        super.shutdown();
    }

    protected void deleteMetric(InventoryMetric metric, Map<String, String> headers) {
        String doomedMetricName = metric.encodedName();
        synchronized (lock) {
            deletedMetrics.add(doomedMetricName);
            cachedMetrics.remove(doomedMetricName);
            cachedMetricDefinitions.remove(doomedMetricName);
        }
    }

    protected void storeMetric(InventoryMetric.WithData metric, Map<String, String> headers) throws Exception {
        String tenant = headers.get(HEADER_HAWKULAR_TENANT);
        String metricName = metric.encodedName();
        String metricData = metric.getPayload();
        synchronized (lock) {
            cachedMetrics.put(metricName, new CachedTenantData(tenant, metricData));
        }
    }

    protected void tagMetric(InventoryMetric.WithData metric, Map<String, String> headers) throws Exception {
        String tenant = headers.get(HEADER_HAWKULAR_TENANT);
        String metricDefinitionName = metric.encodedName();
        String metricDefinitionData = Util.toJson(metric.toMetricDefinition());
        synchronized (lock) {
            cachedMetricDefinitions.put(metricDefinitionName, new CachedTenantData(tenant, metricDefinitionData));
        }
    }
}
