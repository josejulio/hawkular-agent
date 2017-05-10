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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hawkular.agent.monitor.api.Avail;
import org.hawkular.agent.monitor.api.AvailDataPayloadBuilder;
import org.hawkular.agent.monitor.api.InventoryEvent;
import org.hawkular.agent.monitor.api.MetricDataPayloadBuilder;
import org.hawkular.agent.monitor.api.MetricTagPayloadBuilder;
import org.hawkular.agent.monitor.api.NotificationPayloadBuilder;
import org.hawkular.agent.monitor.api.SamplingService;
import org.hawkular.agent.monitor.config.AgentCoreEngineConfiguration;
import org.hawkular.agent.monitor.config.AgentCoreEngineConfiguration.StorageReportTo;
import org.hawkular.agent.monitor.diagnostics.Diagnostics;
import org.hawkular.agent.monitor.inventory.AvailType;
import org.hawkular.agent.monitor.inventory.MeasurementInstance;
import org.hawkular.agent.monitor.inventory.MetricType;
import org.hawkular.agent.monitor.inventory.Resource;
import org.hawkular.agent.monitor.log.AgentLoggers;
import org.hawkular.agent.monitor.log.MsgLogger;
import org.hawkular.agent.monitor.util.Util;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class HawkularStorageAdapter implements StorageAdapter {
    private static final MsgLogger log = AgentLoggers.getLogger(HawkularStorageAdapter.class);

    private AgentCoreEngineConfiguration.StorageAdapterConfiguration config;
    private Diagnostics diagnostics;
    private HttpClientBuilder httpClientBuilder;
    private BaseInventoryStorage inventoryStorage;
    private BaseMetricStorage metricStorage;
    private Map<String, String> agentTenantIdHeader;

    public HawkularStorageAdapter() {
    }

    @Override
    public void initialize(
            String feedId,
            AgentCoreEngineConfiguration.StorageAdapterConfiguration config,
            int autoDiscoveryScanPeriodSeconds,
            Diagnostics diag,
            HttpClientBuilder httpClientBuilder) {
        this.config = config;
        this.diagnostics = diag;
        this.httpClientBuilder = httpClientBuilder;
        this.agentTenantIdHeader = getTenantHeader(config.getTenantId());

        switch (config.getType()) {
            case HAWKULAR:
                this.metricStorage = new PersistenceMetricStorage(this, diagnostics, httpClientBuilder);

                // We are in a full hawkular environment - so we will store inventory directly to it
                this.inventoryStorage = new AsyncInventoryStorage(
                        feedId,
                        config,
                        autoDiscoveryScanPeriodSeconds,
                        httpClientBuilder,
                        diagnostics);
                break;

            case METRICS:
                this.metricStorage = new PersistenceMetricStorage(this, diagnostics, httpClientBuilder);

                // We are only integrating with standalone Hawkular Metrics which does not support inventory.
                this.inventoryStorage = null;
                break;

            case HOSA:
                // Allow HOSA to be our proxy - cache inventory and HOSA will periodically collect and store our cache
                this.metricStorage = new CacheMetricStorage(this);
                this.inventoryStorage = new CacheInventoryStorage(
                        feedId,
                        config,
                        autoDiscoveryScanPeriodSeconds,
                        diagnostics);
                break;

            default:
                throw new IllegalArgumentException("Invalid type. Please report this bug: " + config.getType());
        }
    }

    @Override
    public AgentCoreEngineConfiguration.StorageAdapterConfiguration getStorageAdapterConfiguration() {
        return config;
    }

    @Override
    public MetricDataPayloadBuilder createMetricDataPayloadBuilder() {
        return metricStorage.createMetricDataPayloadBuilder();
    }

    @Override
    public AvailDataPayloadBuilder createAvailDataPayloadBuilder() {
        return metricStorage.createAvailDataPayloadBuilder();
    }

    @Override
    public MetricTagPayloadBuilder createMetricTagPayloadBuilder() {
        return metricStorage.createMetricTagPayloadBuilder();
    }

    @Override
    public void storeMetrics(Set<MetricDataPoint> datapoints, long waitMillis) {
        if (datapoints == null || datapoints.isEmpty()) {
            return; // nothing to do
        }

        Map<String, Set<MetricDataPoint>> byTenantId = separateByTenantId(datapoints);
        for (Map.Entry<String, Set<MetricDataPoint>> entry : byTenantId.entrySet()) {
            String tenantId = entry.getKey();
            Set<MetricDataPoint> tenantDataPoints = entry.getValue();

            MetricDataPayloadBuilder payloadBuilder = createMetricDataPayloadBuilder();
            payloadBuilder.setTenantId(tenantId);

            for (MetricDataPoint datapoint : tenantDataPoints) {
                long timestamp = datapoint.getTimestamp();
                if (datapoint instanceof NumericMetricDataPoint) {
                    double value = ((NumericMetricDataPoint) datapoint).getMetricValue();
                    payloadBuilder.addDataPoint(datapoint.getKey(), timestamp, value, datapoint.getMetricType());
                } else if (datapoint instanceof StringMetricDataPoint) {
                    String value = ((StringMetricDataPoint) datapoint).getMetricValue();
                    payloadBuilder.addDataPoint(datapoint.getKey(), timestamp, value);
                } else {
                    log.errorf("Invalid data point type [%s] - please report this bug", datapoint.getClass());
                }
            }

            store(payloadBuilder, waitMillis);
        }

        return;
    }

    @Override
    public void store(MetricDataPayloadBuilder payloadBuilder, long waitMillis) {
        metricStorage.store(payloadBuilder, waitMillis);
    }

    @Override
    public void storeAvails(Set<AvailDataPoint> datapoints, long waitMillis) {
        if (datapoints == null || datapoints.isEmpty()) {
            return; // nothing to do
        }

        Map<String, Set<AvailDataPoint>> byTenantId = separateByTenantId(datapoints);
        for (Map.Entry<String, Set<AvailDataPoint>> entry : byTenantId.entrySet()) {
            String tenantId = entry.getKey();
            Set<AvailDataPoint> tenantDataPoints = entry.getValue();

            AvailDataPayloadBuilder payloadBuilder = createAvailDataPayloadBuilder();
            payloadBuilder.setTenantId(tenantId);

            for (AvailDataPoint datapoint : tenantDataPoints) {
                long timestamp = datapoint.getTimestamp();
                Avail value = datapoint.getValue();
                payloadBuilder.addDataPoint(datapoint.getKey(), timestamp, value);
            }

            store(payloadBuilder, waitMillis);
        }

        return;
    }

    @Override
    public void store(AvailDataPayloadBuilder payloadBuilder, long waitMillis) {
        metricStorage.store(payloadBuilder, waitMillis);
    }

    @Override
    public void store(MetricTagPayloadBuilder payloadBuilder, long waitMillis) {
        metricStorage.store(payloadBuilder, waitMillis);
    }

    @Override
    public <L> void receivedEvent(InventoryEvent<L> event) {
        if (inventoryStorage != null) {
            inventoryStorage.receivedEvent(event);
        }

        // create the metric tags for the metrics associated with the new resource
        SamplingService<L> service = event.getSamplingService();

        for (Resource<L> resource : event.getAddedOrModified()) {
            MetricTagPayloadBuilder bldr = createMetricTagPayloadBuilder();

            Collection<MeasurementInstance<L, MetricType<L>>> metrics = resource.getMetrics();
            for (MeasurementInstance<L, MetricType<L>> metric : metrics) {
                Map<String, String> tags = service.generateAssociatedMetricTags(metric);
                if (!tags.isEmpty()) {
                    for (Map.Entry<String, String> tag : tags.entrySet()) {
                        bldr.addTag(metric.getAssociatedMetricId(), tag.getKey(), tag.getValue(),
                                metric.getType().getMetricType());
                    }
                }
            }

            Collection<MeasurementInstance<L, AvailType<L>>> avails = resource.getAvails();
            for (MeasurementInstance<L, AvailType<L>> avail : avails) {
                Map<String, String> tags = service.generateAssociatedMetricTags(avail);
                if (!tags.isEmpty()) {
                    for (Map.Entry<String, String> tag : tags.entrySet()) {
                        bldr.addTag(avail.getAssociatedMetricId(), tag.getKey(), tag.getValue(),
                                org.hawkular.metrics.client.common.MetricType.AVAILABILITY);
                    }
                }
            }

            if (bldr.getNumberTags() > 0) {
                store(bldr, 0L);
            }
        }

        // TODO: should we delete the metrics from Hawkular Metrics?
    }

    @Override
    public void shutdown() {
        if (inventoryStorage != null) {
            inventoryStorage.shutdown();
        }
    }

    /**
     * Builds the header necessary for the tenant ID.
     *
     * Package-scoped so metric storage objects can access this.
     *
     * @param tenantId the tenant ID string - this is the value of the returned map
     * @return the tenant header consisting of the header key and the value
     */
    Map<String, String> getTenantHeader(String tenantId) {
        return Collections.singletonMap("Hawkular-Tenant", tenantId);
    }

    private <T extends DataPoint> Map<String, Set<T>> separateByTenantId(Set<T> dataPoints) {
        Map<String, Set<T>> byTenant = new HashMap<>();
        for (T dp : dataPoints) {
            Set<T> tenantDataPoints = byTenant.get(dp.getTenantId());
            if (tenantDataPoints == null) {
                tenantDataPoints = new HashSet<>();
                byTenant.put(dp.getTenantId(), tenantDataPoints);
            }
            tenantDataPoints.add(dp);
        }
        return byTenant;
    }

    @Override
    public NotificationPayloadBuilder createNotificationPayloadBuilder() {
        return new NotificationPayloadBuilderImpl();
    }

    @Override
    public void store(NotificationPayloadBuilder payloadBuilder, long waitMillis) {
        // if we are not in full hawkular mode, there is nothing for us to do
        if (this.config.getType() != StorageReportTo.HAWKULAR) {
            return;
        }

        try {
            // get the payload
            String payload = Util.toJson(payloadBuilder.toPayload());

            // build the REST URL...
            StringBuilder url = Util.getContextUrlString(config.getUrl(), config.getHawkularContext());
            url.append("notification");

            // now send the REST request
            Request request = this.httpClientBuilder.buildJsonPutRequest(url.toString(), agentTenantIdHeader, payload);
            final CountDownLatch latch = (waitMillis <= 0) ? null : new CountDownLatch(1);

            this.httpClientBuilder.getHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    try {
                        log.errorFailedToStoreNotification(e, payload);
                        diagnostics.getStorageErrorRate().mark(1);
                    } finally {
                        if (latch != null) {
                            latch.countDown();
                        }
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        // HTTP status of 200 means success; anything else is an error
                        if (response.code() != 200) {
                            IOException e = new IOException("status-code=[" + response.code() + "], reason=["
                                    + response.message() + "], url=[" + request.url().toString() + "]");
                            log.errorFailedToStoreNotification(e, payload);
                            diagnostics.getStorageErrorRate().mark(1);
                        }
                    } finally {
                        if (latch != null) {
                            latch.countDown();
                        }
                        response.body().close();
                    }
                }
            });

            if (latch != null) {
                latch.await(waitMillis, TimeUnit.MILLISECONDS);
            }

        } catch (Throwable t) {
            log.errorFailedToStoreNotification(t, String.valueOf(payloadBuilder.toPayload()));
            diagnostics.getStorageErrorRate().mark(1);
        }
    }
}
