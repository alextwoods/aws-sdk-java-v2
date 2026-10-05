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

package software.amazon.awssdk.test.codegen.lifecycle;

import software.amazon.awssdk.awscore.metadata.ModelProvenance;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.smithy.runtime.schema.ServiceSchemaIndex;
import software.amazon.awssdk.test.codegen.lifecycle.internal.operation.CodegenLifecycleFixtureOperations;
import software.amazon.awssdk.test.codegen.lifecycle.model.internal.CodegenLifecycleFixtureSchemas;
import software.amazon.smithy.java.core.schema.Schema;

/**
 * Hand-written main source that references the generated {@code GeneratedServiceInfo} type, static operation table and
 * schema holder. This class compiles only if {@code aws-codegen:generate} produced the service sources and registered
 * them before the {@code compile} phase, and the generated descriptors and schemas compile against {@code aws-core}
 * and {@code smithy-runtime}.
 */
public final class CodegenLifecycleFixture {
    private CodegenLifecycleFixture() {
    }

    /** Service shape ID taken from the generated sources. */
    public static String serviceShapeId() {
        return GeneratedServiceInfo.SERVICE_SHAPE_ID;
    }

    /** Model digest taken from the generated sources. */
    public static String modelDigest() {
        return GeneratedServiceInfo.MODEL_DIGEST;
    }

    /** Model revision taken from the generated sources. */
    public static String modelRevision() {
        return GeneratedServiceInfo.MODEL_REVISION;
    }

    /** The model provenance published in this module's jar, read through the generated identity type. */
    public static ModelProvenance provenance() {
        return GeneratedServiceInfo.provenance();
    }

    /** The descriptor of the {@code Ping} operation, taken through the generated static operation table. */
    public static AwsOperationDescriptor pingOperation() {
        return CodegenLifecycleFixtureOperations.PING;
    }

    /** The exported schema of the {@code WidgetTree} structure, taken from the generated schema holder. */
    public static Schema widgetTreeSchema() {
        return CodegenLifecycleFixtureSchemas.WIDGET_TREE;
    }

    /** The exact-ID view of the fixture's generated schemas. */
    public static ServiceSchemaIndex schemaIndex() {
        return CodegenLifecycleFixtureSchemas.index();
    }
}
