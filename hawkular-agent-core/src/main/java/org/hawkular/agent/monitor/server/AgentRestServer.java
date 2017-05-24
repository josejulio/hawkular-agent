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

import org.hawkular.agent.monitor.server.controller.AgentRestController;
import org.hawkular.agent.monitor.util.BaseRestServerGenerator;

import org.restexpress.RestExpress;

public class AgentRestServer {

    protected RestExpress server = null;

    public AgentRestServer(BaseRestServerGenerator restServerGenerator) {
        this.server = restServerGenerator.getRestServer();
    }

    public AgentRestServer addController(AgentRestController controller) {
        controller.configureRouteBuilder(this.server.uri(controller.endpoint(), controller));
        return this;
    }

    public void startServer() {
        server.bind();
    }

    public void shutdown() {
        this.shutdown(false);
    }

    public void shutdown(boolean forceStop) {
        server.shutdown(!forceStop);
    }
}
