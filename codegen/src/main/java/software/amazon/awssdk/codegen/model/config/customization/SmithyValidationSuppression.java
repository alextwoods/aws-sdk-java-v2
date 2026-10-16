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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.utils.ToString;

/**
 * One entry of the {@code smithyValidationSuppressions} list of {@code customization.config}: Smithy validation events
 * of severity {@code ERROR} or {@code DANGER} that the Codegen_Pipeline does not fail a C2J test model on. For example:
 *
 * <pre>
 * "smithyValidationSuppressions": [
 *     {
 *         "id": "ExclusiveStructureMemberTrait",
 *         "shapes": ["com.amazonaws.amazonprotocolrestjson#IdempotentOperationStructure"],
 *         "reason": "Tests an idempotency token bound to a header, a label and a query parameter at once."
 *     }
 * ]
 * </pre>
 *
 * <p>{@code id} matches an event ID as Smithy suppressions do: the ID itself or any ID that starts with it followed by a
 * dot ({@code RuleSetParameter} matches {@code RuleSetParameter.TestCase.Unused}). {@code shapes}, when not empty,
 * limits the entry to events on those absolute shape IDs. {@code reason} is required. The rules are in
 * {@code software.amazon.awssdk.codegen.pipeline.SmithyValidationSuppressions}: suppressions are allowed only in
 * modules that opt in (the {@code allowSmithyValidationSuppressions} parameter of the code generator Maven plugin, set
 * only by test modules), and an entry that matches no event fails generation.
 */
public final class SmithyValidationSuppression {
    private String id;
    private List<String> shapes = new ArrayList<>();
    private String reason;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getShapes() {
        return shapes;
    }

    public void setShapes(List<String> shapes) {
        this.shapes = shapes;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SmithyValidationSuppression that = (SmithyValidationSuppression) o;
        return Objects.equals(id, that.id) && Objects.equals(shapes, that.shapes) && Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(shapes);
        result = 31 * result + Objects.hashCode(reason);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("SmithyValidationSuppression")
                       .add("id", id)
                       .add("shapes", shapes)
                       .add("reason", reason)
                       .build();
    }
}
