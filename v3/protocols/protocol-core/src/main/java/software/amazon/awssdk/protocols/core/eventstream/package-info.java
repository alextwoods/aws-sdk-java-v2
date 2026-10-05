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

/**
 * Event stream SERDE of the protocol runtimes: {@link software.amazon.awssdk.protocols.core.eventstream.EventStreamCodec}
 * maps modeled events to event stream messages with the protocol's
 * {@link software.amazon.awssdk.protocols.core.eventstream.EventPayloadCodec}, and
 * {@link software.amazon.awssdk.protocols.core.eventstream.EventStreams} builds the demand-driven request bodies and
 * the sync/async response readers over the V3 framing of {@code software.amazon.awssdk.eventstream}. Per-frame
 * signing ({@link software.amazon.awssdk.eventstream.EventStreamSigner}) attaches through
 * {@link software.amazon.awssdk.protocols.core.eventstream.EventStreamRequestBody#withFrameTransformer}, which also
 * defines the request body's cancellation and signing-failure behavior for sync and async transports.
 */
package software.amazon.awssdk.protocols.core.eventstream;
