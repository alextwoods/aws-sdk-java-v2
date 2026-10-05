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

package software.amazon.awssdk.http.urlconnection;

import javax.net.ssl.TrustManager;

/**
 * Supplies the trust managers of TLS connections; the V2 {@code TlsTrustManagersProvider} role for
 * {@link software.amazon.awssdk.http.HttpClientOption#TLS_TRUST_MANAGERS}. Called once, when the client is built.
 */
@FunctionalInterface
public interface TlsTrustManagersProvider {
    /** The trust managers, or {@code null} for the JDK defaults. */
    TrustManager[] trustManagers();
}
