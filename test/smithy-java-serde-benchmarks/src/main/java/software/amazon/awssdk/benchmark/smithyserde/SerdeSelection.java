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

package software.amazon.awssdk.benchmark.smithyserde;

import java.lang.reflect.Field;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.protocols.smithyjava.SmithyJavaSerde;

/**
 * Maps a benchmark's {@code serde} parameter onto the switches a client reads at construction.
 *
 * <ul>
 *   <li>{@code v2} — v2's own marshallers and response handlers (the stock SDK).</li>
 *   <li>{@code smithy} — smithy-java's codecs, with smithy-java's default JSON provider (its shaded
 *       Jackson 3).</li>
 *   <li>{@code smithy-native} — smithy-java's codecs with its own hand-written JSON reader/writer
 *       ({@code smithy-java.json-provider=smithy}). That property is read once per JVM, which is fine here
 *       because JMH gives every parameter value its own forks.</li>
 * </ul>
 */
final class SerdeSelection {

    private SerdeSelection() {
    }

    static void select(String serde) {
        switch (serde) {
            case "v2":
                System.setProperty(SmithyJavaSerde.ENABLED_PROPERTY, "false");
                break;
            case "smithy":
                System.setProperty(SmithyJavaSerde.ENABLED_PROPERTY, "true");
                break;
            case "smithy-native":
                System.setProperty(SmithyJavaSerde.ENABLED_PROPERTY, "true");
                System.setProperty("smithy-java.json-provider", "smithy");
                break;
            default:
                throw new IllegalArgumentException("Unknown serde: " + serde);
        }
    }

    /**
     * Fails the run if the client is not on the serde its arm claims: the generated client keeps a
     * {@code smithySerde} field that is null exactly when it is using v2's serde.
     */
    static void verify(String serde, Object client, AwsResponse sample) {
        boolean expectSmithy = !"v2".equals(serde);
        boolean onSmithy = smithySerdeField(client) != null;
        if (onSmithy != expectSmithy) {
            throw new IllegalStateException("serde=" + serde + " but the client is " + (onSmithy ? "on" : "off")
                                            + " smithy-java serde");
        }
        if (sample.responseMetadata().requestId() == null) {
            throw new IllegalStateException("serde=" + serde + " produced a response without metadata: " + sample);
        }
    }

    private static Object smithySerdeField(Object client) {
        try {
            Field field = client.getClass().getDeclaredField("smithySerde");
            field.setAccessible(true);
            return field.get(client);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Client " + client.getClass() + " was not generated with smithy-java serde", e);
        }
    }
}
