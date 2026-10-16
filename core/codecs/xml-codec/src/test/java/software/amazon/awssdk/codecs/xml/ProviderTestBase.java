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

package software.amazon.awssdk.codecs.xml;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.provider.Arguments;

/**
 * Runs the ported smithy-java tests against both XML implementations: StAX ({@code false}) and native ({@code true}).
 * Port of smithy-java {@code software.amazon.smithy.java.xml.ProviderTestBase}.
 */
abstract class ProviderTestBase {
    static final boolean STAX = false;
    static final boolean NATIVE = true;

    static List<Arguments> providers() {
        return Arrays.asList(Arguments.of(Named.of("stax", STAX)),
                             Arguments.of(Named.of("native", NATIVE)));
    }

    static List<Arguments> crossProviders() {
        return Arrays.asList(Arguments.of(Named.of("stax->stax", STAX), STAX),
                             Arguments.of(Named.of("native->native", NATIVE), NATIVE),
                             Arguments.of(Named.of("stax->native", STAX), NATIVE),
                             Arguments.of(Named.of("native->stax", NATIVE), STAX));
    }

    static XmlCodec codec(boolean useNative) {
        return XmlCodec.builder().useNative(useNative).build();
    }
}
