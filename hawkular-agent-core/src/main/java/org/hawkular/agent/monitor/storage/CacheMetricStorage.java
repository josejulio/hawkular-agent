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

import java.util.Map;

import org.hawkular.agent.monitor.api.AvailDataPayloadBuilder;
import org.hawkular.agent.monitor.api.MetricDataPayloadBuilder;
import org.hawkular.agent.monitor.api.MetricTagPayloadBuilder;
import org.hawkular.agent.monitor.log.AgentLoggers;
import org.hawkular.agent.monitor.log.MsgLogger;

/**
 * Caches the metrics in memory but does not write them to Hawkular Metrics.
 */
public class CacheMetricStorage extends BaseMetricStorage {
    private static final MsgLogger log = AgentLoggers.getLogger(CacheMetricStorage.class);

    public CacheMetricStorage(HawkularStorageAdapter storageAdapter) {
        super(storageAdapter);
    }

    @Override
    public void store(MetricDataPayloadBuilder payloadBuilder, long waitMillis) {
        String jsonPayload = "?";

        try {
            // Determine what tenant header to use.
            // If no tenant override is specified in the payload, use the agent's tenant ID.
            String metricTenantId = payloadBuilder.getTenantId();
            if (metricTenantId == null) {
                metricTenantId = getStorageAdapter().getStorageAdapterConfiguration().getTenantId();
            }

            // get the payload in JSON format
            jsonPayload = payloadBuilder.toPayload().toString();

            // TODO cache it somewhere

        } catch (Throwable t) {
            log.errorFailedToStoreMetricData(t, jsonPayload);
        }
    }

    @Override
    public void store(AvailDataPayloadBuilder payloadBuilder, long waitMillis) {
        String jsonPayload = "?";

        try {
            // Determine what tenant header to use.
            // If no tenant override is specified in the payload, use the agent's tenant ID.
            String metricTenantId = payloadBuilder.getTenantId();
            if (metricTenantId == null) {
                metricTenantId = getStorageAdapter().getStorageAdapterConfiguration().getTenantId();
            }

            // get the payload in JSON format
            jsonPayload = payloadBuilder.toPayload().toString();

            // TODO cache it somewhere

        } catch (Throwable t) {
            log.errorFailedToStoreAvailData(t, jsonPayload);
        }
    }

    @Override
    public void store(MetricTagPayloadBuilder payloadBuilder, long waitMillis) {
        Map<String, String> jsonPayloads = null;

        try {
            // Determine what tenant header to use.
            // If no tenant override is specified in the payload, use the agent's tenant ID.
            String metricTenantId = payloadBuilder.getTenantId();
            if (metricTenantId == null) {
                metricTenantId = getStorageAdapter().getStorageAdapterConfiguration().getTenantId();
            }

            // get the payload(s)
            jsonPayloads = payloadBuilder.toPayload();

            // TODO cache it somewhere

        } catch (Throwable t) {
            log.errorFailedToStoreMetricTags(t, (jsonPayloads == null) ? "?" : jsonPayloads.toString());
        }
    }
}
