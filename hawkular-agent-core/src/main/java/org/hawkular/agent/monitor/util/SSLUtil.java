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

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.hawkular.agent.monitor.log.AgentLoggers;
import org.hawkular.agent.monitor.log.MsgLogger;

/**
 * Just some basic ssl utilities.
 *
 */
public class SSLUtil {
    private static final MsgLogger log = AgentLoggers.getLogger(Util.class);

    public static KeyStore loadKeystore(String keystorePath, String keystorePassword) {
        try {
            return readKeyStore(keystorePath, keystorePassword);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Cannot load keystore [%s]", keystorePath), e);
        }
    }

    public static KeyStore readKeyStore(String keystorePath, String keystorePassword) throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        // get user password and file input stream
        char[] password = keystorePassword.toCharArray();
        File file = new File(keystorePath);

        log.infoUseKeystore(file.getAbsolutePath());

        try (FileInputStream fis = new FileInputStream(file)) {
            ks.load(fis, password);
        }
        return ks;
    }

    public static SSLContext buildSSLContext(KeyStore keyStore, String keystorePassword, TrustManager[] trustManagers) {
        try {

            SSLContext sslContext = SSLContext.getInstance("SSL");
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory
                    .getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keystorePassword.toCharArray());
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers,
                    new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Cannot create SSL context from keystore"), e);
        }
    }

    public static TrustManager[] buildTrustManagers(KeyStore keyStore) {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            return trustManagerFactory.getTrustManagers();
        } catch (Exception e) {
            throw new RuntimeException("Cannot build TrustManager", e);
        }
    }
}
