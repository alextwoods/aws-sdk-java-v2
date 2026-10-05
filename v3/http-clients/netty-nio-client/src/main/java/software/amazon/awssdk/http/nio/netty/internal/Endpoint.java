/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.http.nio.netty.internal;

import java.util.Locale;
import software.amazon.awssdk.http.SdkHttpRequest;

/** The pool key of a request: scheme, host and effective port. */
public record Endpoint(boolean secure, String host, int port) {
    /** The endpoint of {@code request}. */
    public static Endpoint of(SdkHttpRequest request) {
        String scheme = request.scheme().toLowerCase(Locale.ROOT);
        boolean secure = switch (scheme) {
            case "https" -> true;
            case "http" -> false;
            default -> throw new IllegalArgumentException("Unsupported scheme: " + request.scheme());
        };
        int port = request.port() >= 0 ? request.port() : secure ? 443 : 80;
        return new Endpoint(secure, request.host(), port);
    }
}
