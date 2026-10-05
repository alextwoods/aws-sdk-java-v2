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
 * Root package of the {@code software.amazon.awssdk:eventstream} V3 artifact: V3-owned AWS event stream binary
 * framing.
 *
 * <p>Owns: the message model ({@link software.amazon.awssdk.eventstream.Message},
 * {@link software.amazon.awssdk.eventstream.HeaderValue}), the encoder
 * ({@link software.amazon.awssdk.eventstream.MessageEncoder}), the incremental, bounded decoder
 * ({@link software.amazon.awssdk.eventstream.MessageDecoder}), precise malformed-frame failures
 * ({@link software.amazon.awssdk.eventstream.EventStreamFormatException}), the per-frame transform hook
 * ({@link software.amazon.awssdk.eventstream.FrameTransformer}) and signature chaining over it.
 *
 * <p>Signing: {@link software.amazon.awssdk.eventstream.EventStreamSigner} owns the chain (prior-signature state,
 * {@code :date}/{@code :chunk-signature} envelopes, the signed closing frame, fail-stop on error). Two narrow seams
 * keep the auth scheme and the identity outside this artifact:
 * {@link software.amazon.awssdk.eventstream.FrameSignatureAlgorithm} (the scheme's per-frame signature function;
 * {@link software.amazon.awssdk.eventstream.SigV4FrameSignatureAlgorithm} implements SigV4
 * {@code AWS4-HMAC-SHA256-PAYLOAD} with JDK crypto only) and
 * {@link software.amazon.awssdk.eventstream.SigningKeyProvider} (the SigV4 signing key per scope date, supplied by
 * {@code auth-aws} from the identity that signed the initial request). Mapping modeled events to messages (event
 * stream SERDE) and attaching the signer to request bodies are protocol-core's.
 *
 * <p>Allowed V3 module dependencies: {@code sdk-core}. Forbidden dependencies: generated service artifacts.
 */
package software.amazon.awssdk.eventstream;
