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

package software.amazon.awssdk.protocol.smithytests;

/**
 * Compares an actual message body with the {@code body} string of a Smithy protocol test case, using the semantics of
 * one media type. Instances are stateless and thread-safe; obtain them from {@link BodyComparators}.
 */
@FunctionalInterface
public interface BodyComparator {

    /**
     * Assert that {@code actual} matches {@code expected}.
     *
     * <p>Both arguments are non-empty: {@link BodyComparators#assertBodyEquals} handles empty bodies before delegating.
     *
     * @param expected the test case {@code body} string (base64 for binary media types such as CBOR)
     * @param actual   the bytes sent or received
     * @throws AssertionError if the bodies do not match
     */
    void assertMatches(String expected, byte[] actual);
}
