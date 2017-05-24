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

import org.restexpress.Flags;
import org.restexpress.Request;
import org.restexpress.Response;
import org.restexpress.route.RouteBuilder;

import io.netty.handler.codec.http.HttpMethod;


/**
 * Basic controller that requires to be public (no auth needed)
 * ToDo: Might be a good idea to implement some annotations to make it easy to configure this.
 */
// @controller(endpoint="echo")

public class EchoController extends AgentRestController {

    public EchoController() {
        super("echo");
    }

    public EchoController(String endpoint) {
        super(endpoint);
    }

    @Override
    public void configureRouteBuilder(RouteBuilder builder) {
        // It would be easier with annotations
        builder.action("echo", HttpMethod.GET)
                .flag(Flags.Auth.PUBLIC_ROUTE)
                .noSerialization();
    }

    //  @action(method=HttpMethod.GET)
    public String echo(Request request, Response response) {
        return request.getQueryStringMap().get("message");
    }
}
