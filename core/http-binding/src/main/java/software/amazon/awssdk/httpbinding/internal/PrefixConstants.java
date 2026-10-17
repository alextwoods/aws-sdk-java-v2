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

package software.amazon.awssdk.httpbinding.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Header names never read into an {@code @httpPrefixHeaders} map. Port of smithy-java {@code PrefixConstants}.
 */
@SdkInternalApi
final class PrefixConstants {
    static final Set<String> OMITTED_HEADER_NAMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "authorization",
        "connection",
        "content-length",
        "expect",
        "host",
        "max-forwards",
        "proxy-authenticate",
        "server",
        "te",
        "trailer",
        "transfer-encoding",
        "upgrade",
        "user-agent",
        "www-authenticate",
        "x-forwarded-for")));

    private PrefixConstants() {
    }
}
