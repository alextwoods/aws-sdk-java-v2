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
 * Per-call override deltas ({@code CallOption}) and the per-call configuration view ({@code CallOptions}) that layers
 * them over a client's resolved configuration (Requirements 9.4, 9.11, 9.12).
 *
 * <p>Option scope. An option applies either to every call of a client (client scope) or may be changed for one call
 * (call scope):
 * <ul>
 *   <li>Call scope: headers, raw query parameters, execution attributes, API call and attempt timeouts, the
 *       credentials identity provider, and the retry strategy, execution interceptors and telemetry options once their
 *       owning modules declare {@code CallOption}s for them (design "Request overrides").</li>
 *   <li>Client scope: every other option, including endpoint, region, protocol, HTTP client and owned resources. These
 *       are client constants resolved once at build (design "Client construction and resolved configuration";
 *       Requirement 9.5) and have no {@code CallOption}, so a request override cannot express a change to them.</li>
 * </ul>
 * A call-scoped value applies only to the call that carries it; every other call of the client reads the client's
 * value.
 */
package software.amazon.awssdk.core.override;
