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

package software.amazon.awssdk.internal.http;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Internal capability interface for HTTP messages whose headers are stored in flat name/value-pair form
 * ({@link StridedHeaders}). Hot-path consumers (the SigV4 signer, transport adapters) test for this and iterate
 * entries directly, skipping the per-name {@code List<String>} materialization that
 * {@code SdkHttpHeaders#forEachHeader} requires.
 *
 * <p>Iteration order is identical to {@code forEachHeader}: names sorted case-insensitively, values of one name
 * adjacent and in insertion order. Names present with an empty value list are not visited (matching a
 * {@code forEachHeader} callback receiving an empty list and contributing nothing).
 */
@SdkInternalApi
public interface FlatHeaderAccess {
    void forEachHeaderEntry(HeaderEntryConsumer consumer);
}
