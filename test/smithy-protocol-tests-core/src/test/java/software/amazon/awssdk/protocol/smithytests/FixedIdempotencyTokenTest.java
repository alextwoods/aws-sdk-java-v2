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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.util.IdempotentUtils;

class FixedIdempotencyTokenTest {

    @Test
    void install_fixesTheToken_andCloseRestoresThePreviousGenerator() {
        Supplier<String> original = IdempotentUtils.getGenerator();
        try (FixedIdempotencyToken token = FixedIdempotencyToken.install()) {
            assertThat(IdempotentUtils.getGenerator().get()).isEqualTo(FixedIdempotencyToken.TOKEN)
                                                            .isEqualTo("00000000-0000-4000-8000-000000000000");
        }
        assertThat(IdempotentUtils.getGenerator()).isSameAs(original);
    }

    @Test
    void nestedInstalls_restoreInReverseOrder_andASecondCloseDoesNothing() {
        Supplier<String> original = IdempotentUtils.getGenerator();
        Supplier<String> custom = () -> "custom";
        IdempotentUtils.setGenerator(custom);
        try {
            FixedIdempotencyToken outer = FixedIdempotencyToken.install();
            FixedIdempotencyToken inner = FixedIdempotencyToken.install();
            inner.close();
            assertThat(IdempotentUtils.getGenerator().get()).isEqualTo(FixedIdempotencyToken.TOKEN);
            outer.close();
            assertThat(IdempotentUtils.getGenerator()).isSameAs(custom);

            IdempotentUtils.setGenerator(original);
            outer.close();
            assertThat(IdempotentUtils.getGenerator()).isSameAs(original);
        } finally {
            IdempotentUtils.setGenerator(original);
        }
    }
}
