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

import org.hawkular.agent.monitor.api.InventoryStorage;
import org.hawkular.agent.monitor.config.AgentCoreEngineConfiguration.StorageAdapterConfiguration;
import org.hawkular.agent.monitor.diagnostics.Diagnostics;
import org.hawkular.agent.monitor.log.AgentLoggers;
import org.hawkular.agent.monitor.log.MsgLogger;
import org.hawkular.agent.monitor.util.Util;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

/**
 * An {@link InventoryStorage} that stores discovered inventory into Hawkular Server.
 */
public class AsyncInventoryStorage extends BaseInventoryStorage {

    private static final MsgLogger log = AgentLoggers.getLogger(AsyncInventoryStorage.class);

    private final HttpClientBuilder httpClientBuilder;

    public AsyncInventoryStorage(
            String feedId,
            StorageAdapterConfiguration config,
            int autoDiscoveryScanPeriodSeconds,
            HttpClientBuilder httpClientBuilder,
            Diagnostics diagnostics) {
        super(feedId, config, autoDiscoveryScanPeriodSeconds, diagnostics);
        this.httpClientBuilder = httpClientBuilder;
    }

    protected void deleteMetric(InventoryMetric metric, Map<String, String> headers) {
        try {
            StringBuilder url = Util.getContextUrlString(config.getUrl(), config.getMetricsContext())
                    .append("strings/")
                    .append(metric.encodedName());
            Request request = this.httpClientBuilder.buildJsonDeleteRequest(url.toString(), headers);
            Call call = this.httpClientBuilder.getHttpClient().newCall(request);
            try (Response response = call.execute()) {
                if (!response.isSuccessful()) {
                    throw new Exception("status-code=[" + response.code() + "], reason=["
                            + response.message() + "], url=[" + request.url().toString() + "]");
                }
            }
        } catch (InterruptedException ie) {
            log.errorFailedToStoreInventoryData(ie);
            Thread.currentThread().interrupt(); // preserve interrupt
        } catch (Exception e) {
            log.errorFailedToStoreInventoryData(e);
            diagnostics.getStorageErrorRate().mark(1);
        }
    }

    protected void storeMetric(InventoryMetric.WithData metric, Map<String, String> headers) throws Exception {
        StringBuilder url = Util.getContextUrlString(config.getUrl(), config.getMetricsContext())
                .append("strings/")
                .append(metric.encodedName())
                .append("/raw");
        Request request = httpClientBuilder.buildJsonPostRequest(url.toString(), headers, metric.getPayload());
        Call call = httpClientBuilder.getHttpClient().newCall(request);
        try (Response response = call.execute()) {
            log.tracef("Received response while uploading chunks: code [%d]", response.code());
            if (!response.isSuccessful()) {
                throw new Exception("status-code=[" + response.code() + "], reason=["
                        + response.message() + "], url=[" + request.url().toString() + "]");
            }
        }
    }

    protected void tagMetric(InventoryMetric.WithData metric, Map<String, String> headers) throws Exception {

        StringBuilder url = Util.getContextUrlString(config.getUrl(), config.getMetricsContext())
                .append("strings?overwrite=true");
        MetricDefinition def = metric.toMetricDefinition();

        Request request = httpClientBuilder.buildJsonPostRequest(url.toString(), headers, Util.toJson(def));
        Call call = httpClientBuilder.getHttpClient().newCall(request);
        try (Response response = call.execute()) {
            log.tracef("Received response while committing chunks: code [%d]", response.code());
            if (!response.isSuccessful()) {
                throw new Exception("status-code=[" + response.code() + "], reason=["
                        + response.message() + "], url=[" + request.url().toString() + "]");
            }
        }
    }
}
