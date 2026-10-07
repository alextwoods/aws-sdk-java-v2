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

package software.amazon.awssdk.protocols.smithyjava;

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * The switch between smithy-java serde and v2's own marshallers.
 *
 * <p>A generated client for a service with {@code generateSmithyJavaSerde} reads this once, in its
 * constructor, and keeps v2's marshallers and response handlers when it is off. Everything else in the
 * pipeline is the same either way, which makes a paired comparison between the two a comparison of
 * serde alone: one jar, one classpath, one JVM configuration, with only this property changed.
 *
 * <p>On by default. Set the system property {@value #ENABLED_PROPERTY} to {@code false} to turn it off
 * for clients built after that point.
 */
@SdkProtectedApi
public final class SmithyJavaSerde {

    public static final String ENABLED_PROPERTY = "aws.smithyJavaSerde";

    private SmithyJavaSerde() {
    }

    public static boolean isEnabled() {
        return !"false".equalsIgnoreCase(System.getProperty(ENABLED_PROPERTY));
    }
}
