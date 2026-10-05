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

package software.amazon.awssdk.http.apache.internal;

import java.io.IOException;

/**
 * Tags a failure of the TLS handshake or hostname verification, so that it is reported in
 * {@link software.amazon.awssdk.http.HttpPhase#TLS_HANDSHAKE} even when Apache's connection operator surfaces it
 * from its connect step. Deliberately not a {@link java.net.SocketTimeoutException}, which the operator would rewrap
 * as a connect timeout; a handshake timeout is the cause.
 */
public final class TlsHandshakeException extends IOException {
    private static final long serialVersionUID = 1L;

    public TlsHandshakeException(IOException cause) {
        super("The TLS handshake failed", cause);
    }
}
