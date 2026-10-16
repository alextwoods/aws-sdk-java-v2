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

package software.amazon.awssdk.codegen.model.config.customization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.internal.Jackson;

class SmithyValidationSuppressionTest {

    @Test
    void equalsHashCode() {
        EqualsVerifier.simple().forClass(SmithyValidationSuppression.class).verify();
    }

    @Test
    void customizationConfig_readsSuppressions() throws IOException {
        CustomizationConfig config = Jackson.load(CustomizationConfig.class,
                                                  "{\"smithyValidationSuppressions\": [{\"id\": \"TraitTarget\", "
                                                  + "\"shapes\": [\"a.b#C$d\"], \"reason\": \"Because.\"}]}");

        SmithyValidationSuppression suppression = config.getSmithyValidationSuppressions().get(0);
        assertThat(suppression.getId()).isEqualTo("TraitTarget");
        assertThat(suppression.getShapes()).containsExactly("a.b#C$d");
        assertThat(suppression.getReason()).isEqualTo("Because.");
        assertThat(suppression.toString())
            .isEqualTo("SmithyValidationSuppression(id=TraitTarget, shapes=[a.b#C$d], reason=Because.)");
    }

    @Test
    void customizationConfig_hasNoSuppressionsByDefault() {
        assertThat(CustomizationConfig.create().getSmithyValidationSuppressions()).isEmpty();
    }
}
