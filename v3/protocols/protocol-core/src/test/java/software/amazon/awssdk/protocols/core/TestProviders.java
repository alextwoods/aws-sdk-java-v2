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

package software.amazon.awssdk.protocols.core;

import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Protocol providers for tests. {@link RestXml} and {@link AwsJson10} are registered in this module's test
 * {@code META-INF/services}; {@link DuplicateRestXml} is registered only by the duplicate-discovery test.
 */
public final class TestProviders {
    private TestProviders() {
    }

    /** A provider of {@code id} with the given binding behavior. */
    public static ProtocolProvider of(ShapeId id, boolean bindings) {
        return new Fixed(id, bindings);
    }

    /** A test provider with a fixed protocol ID. */
    public static class Fixed implements ProtocolProvider {
        private final ShapeId id;
        private final boolean bindings;

        Fixed(ShapeId id, boolean bindings) {
            this.id = id;
            this.bindings = bindings;
        }

        @Override
        public ShapeId protocolId() {
            return id;
        }

        @Override
        public boolean usesHttpBindingTraits() {
            return bindings;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + id + "]";
        }
    }

    /** Discoverable restXml provider. */
    public static final class RestXml extends Fixed {
        public RestXml() {
            super(ProtocolIds.REST_XML, true);
        }
    }

    /** Discoverable awsJson1_0 provider. */
    public static final class AwsJson10 extends Fixed {
        public AwsJson10() {
            super(ProtocolIds.AWS_JSON_1_0, false);
        }
    }

    /** A second restXml provider, registered only by the duplicate-discovery test. */
    public static final class DuplicateRestXml extends Fixed {
        public DuplicateRestXml() {
            super(ProtocolIds.REST_XML, true);
        }
    }
}
