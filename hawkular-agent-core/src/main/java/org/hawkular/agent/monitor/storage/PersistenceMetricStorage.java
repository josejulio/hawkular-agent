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
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hawkular.agent.monitor.api.AvailDataPayloadBuilder;
import org.hawkular.agent.monitor.api.MetricDataPayloadBuilder;
import org.hawkular.agent.monitor.api.MetricTagPayloadBuilder;
import org.hawkular.agent.monitor.config.AgentCoreEngineConfiguration.StorageAdapterConfiguration;
import org.hawkular.agent.monitor.diagnostics.Diagnostics;
import org.hawkular.agent.monitor.log.AgentLoggers;
import org.hawkular.agent.monitor.log.MsgLogger;
import org.hawkular.agent.monitor.util.Util;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Stores the metrics to Hawkular Metrics.
 */
public class PersistenceMetricStorage extends BaseMetricStorage {
    private static final MsgLogger log = AgentLoggers.getLogger(PersistenceMetricStorage.class);

    private Diagnostics diagnostics;
    private HttpClientBuilder httpClientBuilder;

    public PersistenceMetricStorage(HawkularStorageAdapter storageAdapter, Diagnostics diagnostics,
            HttpClientBuilder httpClientBuilder) {
        super(storageAdapter);
        this.diagnostics = diagnostics;
        this.httpClientBuilder = httpClientBuilder;
    }

    @Override
    public void store(MetricDataPayloadBuilder payloadBuilder, long waitMillis) {
        String jsonPayload = "?";

        try {
            // Determine what tenant header to use.
            // If no tenant override is specified in the payload, use the agent's tenant ID.
            Map<String, String> tenantIdHeader;
            String metricTenantId = payloadBuilder.getTenantId();
            if (metricTenantId == null) {
                tenantIdHeader = getAgentTenantIdHeader();
            } else {
                tenantIdHeader = getStorageAdapter().getTenantHeader(metricTenantId);
            }

            // get the payload in JSON format
            jsonPayload = payloadBuilder.toPayload().toString();

            // build the REST URL...
            StorageAdapterConfiguration config = getStorageAdapter().getStorageAdapterConfiguration();
            StringBuilder url = Util.getContextUrlString(config.getUrl(), config.getMetricsContext());
            url.append("metrics/data");

            // now send the REST request
            Request request = this.httpClientBuilder.buildJsonPostRequest(url.toString(), tenantIdHeader, jsonPayload);

            final CountDownLatch latch = (waitMillis <= 0) ? null : new CountDownLatch(1);
            final String jsonPayloadFinal = jsonPayload;
            this.httpClientBuilder.getHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    try {
                        log.errorFailedToStoreMetricData(e, jsonPayloadFinal);
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
                            log.errorFailedToStoreMetricData(e, jsonPayloadFinal);
                            diagnostics.getStorageErrorRate().mark(1);
                        } else {
                            // looks like everything stored successfully
                            diagnostics.getMetricRate().mark(payloadBuilder.getNumberDataPoints());
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
            log.errorFailedToStoreMetricData(t, jsonPayload);
            diagnostics.getStorageErrorRate().mark(1);
        }
    }

    @Override
    public void store(AvailDataPayloadBuilder payloadBuilder, long waitMillis) {
        String jsonPayload = "?";

        try {
            // Determine what tenant header to use.
            // If no tenant override is specified in the payload, use the agent's tenant ID.
            Map<String, String> tenantIdHeader;
            String metricTenantId = payloadBuilder.getTenantId();
            if (metricTenantId == null) {
                tenantIdHeader = getAgentTenantIdHeader();
            } else {
                tenantIdHeader = getStorageAdapter().getTenantHeader(metricTenantId);
            }

            // get the payload in JSON format
            jsonPayload = payloadBuilder.toPayload().toString();

            // build the REST URL...
            StorageAdapterConfiguration config = getStorageAdapter().getStorageAdapterConfiguration();
            StringBuilder url = Util.getContextUrlString(config.getUrl(), config.getMetricsContext());
            url.append("availability/data");

            // now send the REST request
            Request request = this.httpClientBuilder.buildJsonPostRequest(url.toString(), tenantIdHeader, jsonPayload);

            final CountDownLatch latch = (waitMillis <= 0) ? null : new CountDownLatch(1);
            final String jsonPayloadFinal = jsonPayload;
            this.httpClientBuilder.getHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    try {
                        log.errorFailedToStoreAvailData(e, jsonPayloadFinal);
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
                            log.errorFailedToStoreAvailData(e, jsonPayloadFinal);
                            diagnostics.getStorageErrorRate().mark(1);
                        } else {
                            // looks like everything stored successfully
                            diagnostics.getAvailRate().mark(payloadBuilder.getNumberDataPoints());
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
            log.errorFailedToStoreAvailData(t, jsonPayload);
            diagnostics.getStorageErrorRate().mark(1);
        }
    }

    @Override
    public void store(MetricTagPayloadBuilder payloadBuilder, long waitMillis) {
        Map<String, String> jsonPayloads = null;

        try {
            // Determine what tenant header to use.
            // If no tenant override is specified in the payload, use the agent's tenant ID.
            Map<String, String> tenantIdHeader;
            String metricTenantId = payloadBuilder.getTenantId();
            if (metricTenantId == null) {
                tenantIdHeader = getAgentTenantIdHeader();
            } else {
                tenantIdHeader = getStorageAdapter().getTenantHeader(metricTenantId);
            }

            // get the payload(s)
            jsonPayloads = payloadBuilder.toPayload();

            // build the REST URL...
            StorageAdapterConfiguration config = getStorageAdapter().getStorageAdapterConfiguration();
            String url = Util.getContextUrlString(config.getUrl(), config.getMetricsContext()).toString();

            // The way the metrics REST API works is you can only add tags for one metric at a time
            // so loop through each metric ID and send one REST request for each one.
            for (Map.Entry<String, String> jsonPayload : jsonPayloads.entrySet()) {
                String relativePath = jsonPayload.getKey(); // this identifies the metric (e.g. "gauges/<id>")
                String tagsJson = jsonPayload.getValue();
                String currentUrl = url + relativePath + "/tags";

                // now send the REST request
                Request request = this.httpClientBuilder.buildJsonPutRequest(currentUrl, tenantIdHeader, tagsJson);
                final CountDownLatch latch = (waitMillis <= 0) ? null : new CountDownLatch(1);

                this.httpClientBuilder.getHttpClient().newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        try {
                            log.errorFailedToStoreMetricTags(e, tagsJson);
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
                                log.errorFailedToStoreMetricTags(e, tagsJson);
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
            }

        } catch (Throwable t) {
            log.errorFailedToStoreMetricTags(t, (jsonPayloads == null) ? "?" : jsonPayloads.toString());
            diagnostics.getStorageErrorRate().mark(1);
        }
    }
}
