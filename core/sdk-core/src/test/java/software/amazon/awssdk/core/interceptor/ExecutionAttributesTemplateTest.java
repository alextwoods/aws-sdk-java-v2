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

package software.amazon.awssdk.core.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExecutionAttributesTemplateTest {
    private static final ExecutionAttribute<String> A = new ExecutionAttribute<>("TemplateTestA");
    private static final ExecutionAttribute<String> B = new ExecutionAttribute<>("TemplateTestB");
    private static final ExecutionAttribute<String> C = new ExecutionAttribute<>("TemplateTestC");

    @Test
    void of_capturesOnlyTheAttributesThatAreSet() {
        ExecutionAttributes source = new ExecutionAttributes().putAttribute(A, "a").putAttribute(B, "b");
        ExecutionAttributesTemplate template = ExecutionAttributesTemplate.of(source);
        assertThat(template.size()).isEqualTo(2);
    }

    @Test
    void applyTo_overwritesTemplateAttributesAndLeavesOthersAlone() {
        ExecutionAttributesTemplate template =
            ExecutionAttributesTemplate.of(new ExecutionAttributes().putAttribute(A, "a").putAttribute(B, "b"));

        ExecutionAttributes target = new ExecutionAttributes().putAttribute(A, "request-supplied").putAttribute(C, "c");
        template.applyTo(target);

        assertThat(target.getAttribute(A)).as("template wins, as the per-attribute puts did").isEqualTo("a");
        assertThat(target.getAttribute(B)).isEqualTo("b");
        assertThat(target.getAttribute(C)).as("not in the template, untouched").isEqualTo("c");
    }

    @Test
    void applyTo_producesTheSameAttributesAsTheEquivalentPuts() {
        ExecutionAttributes viaPuts = new ExecutionAttributes().putAttribute(C, "c")
                                                               .putAttribute(A, "a")
                                                               .putAttribute(B, "b");

        ExecutionAttributesTemplate template =
            ExecutionAttributesTemplate.of(new ExecutionAttributes().putAttribute(A, "a").putAttribute(B, "b"));
        ExecutionAttributes viaTemplate = new ExecutionAttributes().putAttribute(C, "c");
        template.applyTo(viaTemplate);

        assertThat(viaTemplate).isEqualTo(viaPuts);
    }

    @Test
    void of_isASnapshot_laterChangesToTheSourceDoNotLeak() {
        ExecutionAttributes source = new ExecutionAttributes().putAttribute(A, "a");
        ExecutionAttributesTemplate template = ExecutionAttributesTemplate.of(source);
        source.putAttribute(A, "changed").putAttribute(B, "added");

        ExecutionAttributes target = new ExecutionAttributes();
        template.applyTo(target);
        assertThat(target.getAttribute(A)).isEqualTo("a");
        assertThat(target.getAttribute(B)).isNull();
    }

    @Test
    void applyTo_targetCreatedBeforeNewAttributesWereRegistered_grows() {
        ExecutionAttributes target = new ExecutionAttributes();
        ExecutionAttribute<String> late = new ExecutionAttribute<>("TemplateTestLate-" + System.nanoTime());
        ExecutionAttributesTemplate template =
            ExecutionAttributesTemplate.of(new ExecutionAttributes().putAttribute(late, "late"));

        template.applyTo(target);
        assertThat(target.getAttribute(late)).isEqualTo("late");
    }
}
