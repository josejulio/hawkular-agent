/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;

/**
 * PathAddress methods found on wildfly-core
 * https://github.com/wildfly/wildfly-core/blob/588145a00ca5ed3beb0027f8f44be87db6689db3/controller/src/main/java/org/jboss/as/controller/PathAddress.java
 */
public class PathAddressExtension {

    public static PathAddress parseCLIStyleAddress(String address) throws IllegalArgumentException {
        // TODO: We could check if PathAddress class has parseCLIStyleAddress method and redirect to it instead.
        PathAddress parsedAddress = PathAddress.EMPTY_ADDRESS;
        if (address == null || address.trim().isEmpty()) {
            return parsedAddress;
        }
        String trimmedAddress = address.trim();
        if (trimmedAddress.charAt(0) != '/' || !Character.isAlphabetic(trimmedAddress.charAt(1))) {
            // https://github.com/wildfly/wildfly-core/blob/588145a00ca5ed3beb0027f8f44be87db6689db3/controller/src/main/java/org/jboss/as/controller/logging/ControllerLogger.java#L3296-L3297
            throw new IllegalArgumentException(
                    "Illegal path address '" + address + "' , it is not in a correct CLI format");
        }
        char[] characters = address.toCharArray();
        boolean escaped = false;
        StringBuilder keyBuffer = new StringBuilder();
        StringBuilder valueBuffer = new StringBuilder();
        StringBuilder currentBuffer = keyBuffer;
        for (int i = 1; i < characters.length; i++) {
            switch (characters[i]) {
                case '/':
                    if (escaped) {
                        escaped = false;
                        currentBuffer.append(characters[i]);
                    } else {
                        parsedAddress = addpathAddressElement(parsedAddress, address, keyBuffer, valueBuffer);
                        keyBuffer = new StringBuilder();
                        valueBuffer = new StringBuilder();
                        currentBuffer = keyBuffer;
                    }
                    break;
                case '\\':
                    if (escaped) {
                        escaped = false;
                        currentBuffer.append(characters[i]);
                    } else {
                        escaped = true;
                    }
                    break;
                case '=':
                    if (escaped) {
                        escaped = false;
                        currentBuffer.append(characters[i]);
                    } else {
                        currentBuffer = valueBuffer;
                    }
                    break;
                default:
                    currentBuffer.append(characters[i]);
                    break;
            }
        }
        parsedAddress = addpathAddressElement(parsedAddress, address, keyBuffer, valueBuffer);
        return parsedAddress;
    }

    private static PathAddress addpathAddressElement(PathAddress parsedAddress, String address, StringBuilder keyBuffer, StringBuilder valueBuffer) {
        if (keyBuffer.length() > 0) {
            if (valueBuffer.length() > 0) {
                return parsedAddress.append(PathElement.pathElement(keyBuffer.toString(), valueBuffer.toString()));
            }
            throw new IllegalArgumentException(
                    "Illegal path address '" + address + "' , it is not in a correct CLI format");
        }
        return parsedAddress;
    }

}
