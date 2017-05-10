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
import org.hawkular.agent.monitor.api.AvailStorage;
import org.hawkular.agent.monitor.api.MetricDataPayloadBuilder;
import org.hawkular.agent.monitor.api.MetricStorage;
import org.hawkular.agent.monitor.api.MetricTagPayloadBuilder;

public abstract class BaseMetricStorage implements MetricStorage, AvailStorage {
    private final HawkularStorageAdapter storageAdapter;
    private final Map<String, String> agentTenantIdHeader;

    public BaseMetricStorage(HawkularStorageAdapter storageAdapter) {
        this.storageAdapter = storageAdapter;
        this.agentTenantIdHeader = storageAdapter
                .getTenantHeader(storageAdapter.getStorageAdapterConfiguration().getTenantId());
    }

    @Override
    public MetricDataPayloadBuilder createMetricDataPayloadBuilder() {
        return new MetricDataPayloadBuilderImpl();
    }

    @Override
    public AvailDataPayloadBuilder createAvailDataPayloadBuilder() {
        return new AvailDataPayloadBuilderImpl();
    }

    @Override
    public MetricTagPayloadBuilder createMetricTagPayloadBuilder() {
        return new MetricTagPayloadBuilderImpl();
    }

    protected HawkularStorageAdapter getStorageAdapter() {
        return storageAdapter;
    }

    protected Map<String, String> getAgentTenantIdHeader() {
        return agentTenantIdHeader;
    }
}
