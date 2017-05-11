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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hawkular.agent.monitor.api.AvailDataPayloadBuilder;
import org.hawkular.agent.monitor.api.MetricDataPayloadBuilder;
import org.hawkular.agent.monitor.api.MetricTagPayloadBuilder;
import org.hawkular.agent.monitor.util.Util;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Caches the metrics in memory but does not write them to Hawkular Metrics.
 */
public class CacheMetricStorage extends BaseMetricStorage {
    //private static final MsgLogger log = AgentLoggers.getLogger(CacheMetricStorage.class);

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

    private final LinkedList<CachedTenantData> cachedMetricData;
    private final LinkedList<CachedTenantData> cachedAvailData;
    private final HashMap<String, CachedTenantData> cachedTagData; // keyed on metric ID
    private final Object lock = new Object();

    public CacheMetricStorage(HawkularStorageAdapter storageAdapter) {
        super(storageAdapter);
        this.cachedMetricData = new LinkedList<>();
        this.cachedAvailData = new LinkedList<>();
        this.cachedTagData = new HashMap<>();
    }

    public String getCacheAsJsonString() {
        List<CachedTenantData> metricsCopy;
        List<CachedTenantData> availsCopy;
        Map<String, CachedTenantData> tagsCopy;

        synchronized (lock) {
            // transfer the caches data to temporary objects, clearing the cache objects themselves as we go
            metricsCopy = new ArrayList<>(cachedMetricData.size());
            while (!cachedMetricData.isEmpty()) {
                metricsCopy.add(cachedMetricData.remove());
            }
            availsCopy = new ArrayList<>(cachedAvailData.size());
            while (!cachedAvailData.isEmpty()) {
                availsCopy.add(cachedAvailData.remove());
            }
            tagsCopy = new HashMap<>(cachedTagData);
            cachedTagData.clear();
        }

        // build the JSON data
        HashMap<String, Object> allData = new HashMap<>(3);
        allData.put("metrics", metricsCopy);
        allData.put("avails", availsCopy);
        allData.put("tags", tagsCopy);
        return Util.toJson(allData);
    }

    public void shutdown() {
        synchronized (lock) {
            cachedMetricData.clear();
            cachedAvailData.clear();
            cachedTagData.clear();
        }
        super.shutdown();
    }

    @Override
    public void store(MetricDataPayloadBuilder payloadBuilder, long waitMillis) {
        // Determine what tenant header to use.
        // If no tenant override is specified in the payload, use the agent's tenant ID.
        String metricTenantId = payloadBuilder.getTenantId();
        if (metricTenantId == null) {
            metricTenantId = getStorageAdapter().getStorageAdapterConfiguration().getTenantId();
        }

        // get the payload in JSON format
        String jsonPayload = payloadBuilder.toPayload().toString();

        // cache it
        synchronized (lock) {
            cachedMetricData.add(new CachedTenantData(metricTenantId, jsonPayload));
        }
    }

    @Override
    public void store(AvailDataPayloadBuilder payloadBuilder, long waitMillis) {
        // Determine what tenant header to use.
        // If no tenant override is specified in the payload, use the agent's tenant ID.
        String metricTenantId = payloadBuilder.getTenantId();
        if (metricTenantId == null) {
            metricTenantId = getStorageAdapter().getStorageAdapterConfiguration().getTenantId();
        }

        // get the payload in JSON format
        String jsonPayload = payloadBuilder.toPayload().toString();

        // cache it
        synchronized (lock) {
            cachedAvailData.add(new CachedTenantData(metricTenantId, jsonPayload));
        }
    }

    @Override
    public void store(MetricTagPayloadBuilder payloadBuilder, long waitMillis) {
        // Determine what tenant header to use.
        // If no tenant override is specified in the payload, use the agent's tenant ID.
        String metricTenantId = payloadBuilder.getTenantId();
        if (metricTenantId == null) {
            metricTenantId = getStorageAdapter().getStorageAdapterConfiguration().getTenantId();
        }

        // get the payload(s)
        Map<String, String> jsonPayloads = payloadBuilder.toPayload();

        // for each metric ID, cache their data - note jsonPayload key identifies the metric (e.g. "gauges/<id>") 
        synchronized (lock) {
            for (Map.Entry<String, String> jsonPayload : jsonPayloads.entrySet()) {
                cachedTagData.put(jsonPayload.getKey(), new CachedTenantData(metricTenantId, jsonPayload.getValue()));
            }
        }
    }
}
