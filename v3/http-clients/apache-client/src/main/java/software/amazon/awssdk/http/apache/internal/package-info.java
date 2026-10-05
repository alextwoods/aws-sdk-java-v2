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
 * The Apache HttpClient 4 engine behind {@link software.amazon.awssdk.http.apache.ApacheHttpClient}. Internal: use the
 * public builder and service provider, and expect the package to change without notice.
 *
 * <p>The engine drives Apache's {@code PoolingHttpClientConnectionManager} and {@code ManagedHttpClientConnection}
 * directly rather than Apache's {@code HttpClient} execution chain, so that every step of an exchange has one
 * {@link software.amazon.awssdk.http.HttpPhase}, the request line is the request's raw target, and header order,
 * repeats and case reach the wire unchanged. {@link software.amazon.awssdk.http.apache.internal.ApacheFailureMapper}
 * is the single place engine failures become {@link software.amazon.awssdk.http.HttpTransportException}s.
 */
package software.amazon.awssdk.http.apache.internal;
