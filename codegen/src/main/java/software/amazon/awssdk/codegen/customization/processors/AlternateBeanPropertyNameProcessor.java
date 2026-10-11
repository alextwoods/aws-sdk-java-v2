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

package software.amazon.awssdk.codegen.customization.processors;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.ModifyModelShapeModifier;
import software.amazon.awssdk.codegen.model.config.customization.ShapeModifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;

/**
 * Applies {@code shapeModifiers.<shape>.modify.<member>.alternateBeanPropertyName} to the intermediate model: the
 * member gets an additional bean-style setter, {@code set<alternateBeanPropertyName>} (DynamoDB
 * {@code AttributeValue.BuilderImpl.setNull}).
 *
 * <p>The C2J route applies this customization through the C2J member ({@link ShapeModifiersProcessor} preprocess and
 * {@code AddShapes}). The Smithy route builds members from the Smithy model, where the C2J_Converter can only carry it
 * on a {@code com.amazonaws.c2j#alternateBeanPropertyName} marker, which the intermediate model does not read
 * (Requirement 17.9). So the Smithy route applies it here, from {@code customization.config}. A member renamed by
 * {@code emitPropertyName} is looked up by its new name. A shape or member that does not exist is skipped: stage 2 of
 * the Codegen_Pipeline already reports a shape modifier for a missing shape.
 */
@SdkInternalApi
final class AlternateBeanPropertyNameProcessor implements CodegenCustomizationProcessor {
    private final Map<String, ShapeModifier> shapeModifiers;

    AlternateBeanPropertyNameProcessor(Map<String, ShapeModifier> shapeModifiers) {
        this.shapeModifiers = shapeModifiers;
    }

    @Override
    public void preprocess(ServiceModel serviceModel) {
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        if (shapeModifiers == null) {
            return;
        }
        shapeModifiers.forEach((shapeName, modifier) -> {
            if (modifier.getModify() == null) {
                return;
            }
            List<ShapeModel> shapes = Utils.findShapesByC2jName(intermediateModel, shapeName);
            for (Map<String, ModifyModelShapeModifier> modifies : modifier.getModify()) {
                modifies.forEach((memberName, modify) -> {
                    if (modify.getAlternateBeanPropertyName() == null) {
                        return;
                    }
                    String name = modify.getEmitPropertyName() != null ? modify.getEmitPropertyName() : memberName;
                    for (ShapeModel shape : shapes) {
                        MemberModel member = shape.tryFindMemberModelByC2jName(name, false);
                        if (member != null) {
                            member.setAdditionalBeanStyleSetterName("set" + modify.getAlternateBeanPropertyName());
                        }
                    }
                });
            }
        });
    }
}
