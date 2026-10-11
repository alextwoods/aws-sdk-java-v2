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

package software.amazon.awssdk.codegen.poet.protocoltest;

import com.squareup.javapoet.ClassName;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Types of {@code smithy-protocol-tests-core} that generated protocol tests use. Codegen references them by name, so it
 * does not depend on that test module.
 */
@SdkInternalApi
final class ProtocolTestRuntimeTypes {
    static final String PACKAGE = "software.amazon.awssdk.protocol.smithytests";

    static final ClassName PROTOCOL_TEST_HARNESS = ClassName.get(PACKAGE, "ProtocolTestHarness");
    static final ClassName PROTOCOL_TEST_HARNESSES = ClassName.get(PACKAGE, "ProtocolTestHarnesses");
    static final ClassName PROTOCOL_ASSERTS = ClassName.get(PACKAGE, "ProtocolAsserts");
    static final ClassName PROTOCOL_TEST_VALUES = ClassName.get(PACKAGE, "ProtocolTestValues");
    static final ClassName CAPTURED_REQUEST = ClassName.get(PACKAGE, "CapturedRequest");
    static final ClassName STUBBED_RESPONSE = ClassName.get(PACKAGE, "StubbedResponse");

    /** The tag prefix {@code ProtocolTestSummaryExtension} counts by. */
    static final String PROTOCOL_TAG_PREFIX = "protocol:";

    /** The idempotency token Smithy protocol test cases expect. */
    static final String IDEMPOTENCY_TOKEN = "00000000-0000-4000-8000-000000000000";

    private ProtocolTestRuntimeTypes() {
    }
}
