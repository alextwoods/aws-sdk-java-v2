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

package software.amazon.awssdk.test.fault;

/**
 * The fault catalogue of Requirement 19.7: every kind a release fault matrix must inject at least once. A case
 * declares the kinds it injects, so a suite can check that its matrix covers the whole catalogue.
 */
public enum FaultKind {
    /** The transport cannot connect ({@link TransportFault.ConnectFailure}). */
    CONNECTION_FAILURE,
    /** The transport fails after reading part of the request body ({@link TransportFault.PartialRequest}). */
    PARTIAL_REQUEST_BODY,
    /** The transport fails after delivering part of the response body ({@link TransportFault.PartialResponse}). */
    PARTIAL_RESPONSE_BODY,
    /** The transport times out ({@link TransportFault.Timeout}). */
    TIMEOUT,
    /** The service throttles the request (HTTP 429 or a throttling error code). */
    THROTTLING,
    /** The service sends an {@code x-amz-retry-after} delay hint with a retryable error. */
    RETRY_DELAY_HINT,
    /** The service answers 2xx with a body the protocol cannot read. */
    MALFORMED_SUCCESS,
    /** The service answers with an error the model declares. */
    MODELED_ERROR,
    /** The service answers with an error the model does not declare. */
    UNMODELED_ERROR,
    /** The identity provider fails ({@link IdentityStep#FAIL}). */
    IDENTITY_FAILURE,
    /** Endpoint resolution fails ({@link EndpointMode#FAIL}). */
    ENDPOINT_FAILURE,
    /** The caller cancels the call ({@link CancelPoint}). */
    CANCELLATION,
    /** The signer fails ({@link SignerStep#FAIL}); beyond the catalogue of Requirement 19.7. */
    SIGNER_FAILURE,
    /** The request body fails while it is read ({@link BodyKind#FAILING_ONCE}); beyond Requirement 19.7. */
    REQUEST_BODY_FAILURE
}
