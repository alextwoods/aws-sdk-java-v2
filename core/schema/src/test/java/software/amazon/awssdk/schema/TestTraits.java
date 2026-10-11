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

package software.amazon.awssdk.schema;

import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.Trait;

/**
 * Third-party-style trait classes for {@link TraitKey} and {@link TraitMap} tests. Each class gets its key the first
 * time a test asks for it, so each test that checks key creation uses classes no other test touches.
 */
final class TestTraits {
    private TestTraits() {
    }

    abstract static class BaseTestTrait implements Trait {
        @Override
        public ShapeId traitId() {
            return ShapeId.fromParts("com.example.test", getClass().getSimpleName());
        }

        @Override
        public Document toDocument() {
            return Document.EMPTY_MAP;
        }
    }

    // Used by TraitKeyTest.get_newClasses_getConsecutiveIds.
    static final class SequentialA extends BaseTestTrait {
    }

    static final class SequentialB extends BaseTestTrait {
    }

    // Used by TraitKeyTest.get_concurrentFirstCalls_returnOneKeyPerClassWithDenseIds.
    static final class RacedA extends BaseTestTrait {
    }

    static final class RacedB extends BaseTestTrait {
    }

    static final class RacedC extends BaseTestTrait {
    }

    static final class RacedD extends BaseTestTrait {
    }

    static final class AfterRace extends BaseTestTrait {
    }

    // Used by TraitMapTest.get_keyIdBeyondStorage_returnsNull.
    static final class NeverStored extends BaseTestTrait {
    }

    // Used by TraitMapTest for third-party typed traits.
    static final class ThirdParty extends BaseTestTrait {
    }
}
