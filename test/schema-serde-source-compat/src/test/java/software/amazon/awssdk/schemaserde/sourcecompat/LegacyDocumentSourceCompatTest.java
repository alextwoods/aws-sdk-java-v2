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

package software.amazon.awssdk.schemaserde.sourcecompat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.schemaserde.sourcecompat.fixtures.LegacyDocumentFixture;

/**
 * Runs {@link LegacyDocumentFixture} (Requirement 16.5).
 */
class LegacyDocumentSourceCompatTest {
    @Test
    void describe_builtDocument_readsEveryKind() {
        assertThat(LegacyDocumentFixture.describe(LegacyDocumentFixture.build()))
            .isEqualTo("true 10 fixture 42 9000000000 1.25 3.5 false true string,number,boolean,null v [1, 2.5] "
                       + "map of 10");
    }

    @Test
    void factories_sameInput_equalDocuments() {
        List<Document> first = LegacyDocumentFixture.factories();
        List<Document> second = LegacyDocumentFixture.factories();

        assertThat(first).isEqualTo(second);
        assertThat(first.get(0).asString()).isEqualTo("s");
        assertThat(first.get(10).isNull()).isTrue();
        assertThat(first.get(12).toString()).isEqualTo("{\"k\": \"v\"}");
    }
}
