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
package org.hawkular.agent.monitor.util;

import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.xml.bind.DatatypeConverter;

import org.hawkular.agent.monitor.log.AgentLoggers;
import org.hawkular.agent.monitor.log.MsgLogger;

import org.restexpress.ContentType;
import org.restexpress.Flags;
import org.restexpress.Request;
import org.restexpress.RestExpress;
import org.restexpress.exception.UnauthorizedException;
import org.restexpress.pipeline.Preprocessor;
import org.restexpress.route.Route;

import io.netty.handler.codec.http.HttpHeaders;


/**
 * Can be used to generate RestServers including those that require SSL.
 */
public class BaseRestServerGenerator {
    private static final MsgLogger log = AgentLoggers.getLogger(BaseRestServerGenerator.class);

    public static class Configuration {
        public static class Builder {
            private String address;
            private int port;
            private String username;
            private String password;
            private boolean useSSL;
            private String keystorePath;
            private String keystorePassword;
            private SSLContext sslContext;

            public Builder() {

            }

            public Configuration build() {
                return new Configuration(address, port, username, password, useSSL, keystorePath,
                keystorePassword, sslContext);
            }

            public Builder address(String address) {
                this.address = address;
                return this;
            }

            public Builder port(int port) {
                this.port = port;
                return this;
            }

            public Builder username(String username) {
                this.username = username;
                return this;
            }

            public Builder password(String password) {
                this.password = password;
                return this;
            }


            public Builder useSSL(boolean useSSL) {
                this.useSSL = useSSL;
                return this;
            }

            public Builder keystorePath(String keystorePath) {
                this.keystorePath = keystorePath;
                return this;
            }

            public Builder keystorePassword(String keystorePassword) {
                this.keystorePassword = keystorePassword;
                return this;
            }

            public Builder sslContext(SSLContext sslContext) {
                this.sslContext = sslContext;
                return this;
            }
        }

        private final String address;
        private final int port;
        private final String username;
        private final String password;
        private final boolean useSSL;
        private final String keystorePath;
        private final String keystorePassword;
        private final SSLContext sslContext;

        private Configuration(String address, int port, String username, String password, boolean useSSL,
                              String keystorePath, String keystorePassword, SSLContext sslContext) {
            this.address = address;
            this.port = port;
            this.username = username;
            this.password = password;
            this.useSSL = useSSL;
            this.keystorePath = keystorePath;
            this.keystorePassword = keystorePassword;
            this.sslContext = sslContext;
        }

        public String getAddress() {
            return address;
        }

        public int getPort() {
            return port;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public boolean isUseSSL() {
            return useSSL;
        }

        public String getKeystorePath() {
            return keystorePath;
        }

        public String getKeystorePassword() {
            return keystorePassword;
        }

        public SSLContext getSslContext() {
            return sslContext;
        }
    }

    private final Configuration configuration;
    private final RestExpress restServer;

    public BaseRestServerGenerator(Configuration configuration) {
        this.configuration = configuration;

        RestExpress restServer = new RestExpress();

        restServer.setHostname(configuration.getAddress());
        restServer.setPort(configuration.getPort());

        if (this.configuration.isUseSSL()) {
            SSLContext theSslContextToUse;
            if (this.configuration.getSslContext() == null) {
                if (this.configuration.getKeystorePath() != null) {
                    KeyStore keyStore = SSLUtil.loadKeystore(this.configuration.getKeystorePath(),
                        this.configuration.getKeystorePassword());
                    theSslContextToUse = SSLUtil.buildSSLContext(keyStore, this.configuration.getKeystorePassword(),
                    null); // No need for a truststore here, as, this is going to act as a server
                } else {
                    theSslContextToUse = null; // Rely on the JVM default
                }
            } else {
                theSslContextToUse = this.configuration.getSslContext();
            }

            if (theSslContextToUse != null) {
                restServer.setSSLContext(configuration.getSslContext());
            }
        }

        if (this.configuration.getUsername() != null && !this.configuration.getUsername().isEmpty()) {
            restServer.addPreprocessor(new HttpBasicAuthenticationRestServerPreprocessor(
                    this.configuration.getUsername(),
                    this.configuration.getPassword()));
        }

        this.restServer = restServer;

    }

    public RestExpress getRestServer() {
        return this.restServer;
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    // Based on https://github.com/RestExpress/RestExpress/blob/master/core/src/main/java/org/restexpress/preprocessor/HttpBasicAuthenticationPreprocessor.java
    private static class HttpBasicAuthenticationRestServerPreprocessor implements Preprocessor {

        private String username;
        private String password;

        public HttpBasicAuthenticationRestServerPreprocessor(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public void process(Request request) {
            Route route = request.getResolvedRoute();
            if(route == null || !route.isFlagged(Flags.Auth.PUBLIC_ROUTE) && !route.isFlagged(Flags.Auth.NO_AUTHENTICATION)) {
                String authorization = request.getHeader("Authorization");
                if(authorization == null || !authorization.startsWith("Basic ")) {
                    this.throwUnauthorizedException();
                }

                String[] pieces = authorization.split(" ");
                byte[] bytes = DatatypeConverter.parseBase64Binary(pieces[1]);
                String credentials = new String(bytes, ContentType.CHARSET);
                String[] parts = credentials.split(":");
                if(parts.length < 2) {
                    this.throwUnauthorizedException();
                }

                if (!username.equals(parts[0]) || !password.equals(parts[1])) {
                    throwUnauthorizedException();
                }
            }
        }

        private void throwUnauthorizedException() {
            UnauthorizedException e = new UnauthorizedException("Authentication required");
            e.setHeader(HttpHeaders.Names.WWW_AUTHENTICATE, "Basic realm=\"Hawkular-Agent\"");
            throw e;
        }
    }
}
