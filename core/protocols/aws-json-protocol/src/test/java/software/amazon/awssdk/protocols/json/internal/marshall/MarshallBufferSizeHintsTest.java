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

package software.amazon.awssdk.protocols.json.internal.marshall;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MarshallBufferSizeHintsTest {

    @Test
    void hintFor_unknownOperation_returnsDefault() {
        MarshallBufferSizeHints hints = new MarshallBufferSizeHints();

        assertThat(hints.hintFor("Unknown")).isEqualTo(1024);
    }

    @Test
    void hintFor_nullOperation_returnsDefault() {
        MarshallBufferSizeHints hints = new MarshallBufferSizeHints();
        hints.record(null, 50_000);

        assertThat(hints.hintFor(null)).isEqualTo(1024);
    }

    @Test
    void record_largerBody_raisesHintImmediately() {
        MarshallBufferSizeHints hints = new MarshallBufferSizeHints();

        hints.record("PutItem", 50_000);

        assertThat(hints.hintFor("PutItem")).isEqualTo(50_000);
    }

    @Test
    void record_smallerBody_decaysGradually() {
        MarshallBufferSizeHints hints = new MarshallBufferSizeHints();
        hints.record("PutItem", 50_000);

        hints.record("PutItem", 1_000);

        int afterOne = hints.hintFor("PutItem");
        assertThat(afterOne).isLessThan(50_000)
                            .isGreaterThan(40_000);

        for (int i = 0; i < 100; i++) {
            hints.record("PutItem", 1_000);
        }
        assertThat(hints.hintFor("PutItem")).isEqualTo(1024);
    }

    @Test
    void record_belowMinimum_clampsToMinimum() {
        MarshallBufferSizeHints hints = new MarshallBufferSizeHints();

        hints.record("GetItem", 66);

        assertThat(hints.hintFor("GetItem")).isEqualTo(1024);
    }

    @Test
    void record_aboveMaximum_clampsToMaximum() {
        MarshallBufferSizeHints hints = new MarshallBufferSizeHints();

        hints.record("BatchWrite", 10 * 1024 * 1024);

        assertThat(hints.hintFor("BatchWrite")).isEqualTo(128 * 1024);
    }

    @Test
    void record_operationsAreIndependent() {
        MarshallBufferSizeHints hints = new MarshallBufferSizeHints();

        hints.record("PutItem", 50_000);
        hints.record("GetItem", 2_000);

        assertThat(hints.hintFor("PutItem")).isEqualTo(50_000);
        assertThat(hints.hintFor("GetItem")).isEqualTo(2_000);
    }

    @Test
    void record_steadyStateWorkload_stabilizesAtBodySize() {
        MarshallBufferSizeHints hints = new MarshallBufferSizeHints();

        for (int i = 0; i < 10; i++) {
            hints.record("BatchWrite", 50_000);
        }

        assertThat(hints.hintFor("BatchWrite")).isEqualTo(50_000);
    }
}
