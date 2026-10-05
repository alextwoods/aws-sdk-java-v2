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

package software.amazon.awssdk.awscodegen;

import static software.amazon.awssdk.awscodegen.RuntimeSymbols.MODEL_PROVENANCE;
import static software.amazon.awssdk.awscodegen.RuntimeSymbols.STRING;

import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Emits the package-private {@value AwsCodegen#SERVICE_INFO_TYPE} type that records which service shape, model inputs
 * and model revision a generated tree was produced from, and reads the model provenance published with it.
 */
final class ServiceInfoGenerator {

    private ServiceInfoGenerator() {
    }

    static void generate(AwsCodegenContext context) {
        ServiceShape service = context.service();
        String typeName = context.symbolProvider().toSymbol(service).getName();
        context.writerDelegator().useShapeWriter(service, writer -> {
            writer.write("/**");
            writer.write(" * Generation identity of the {@code $L} service.", service.getId());
            writer.write(" */");
            writer.openBlock("final class $L {", "}", typeName, () -> {
                writer.write("/** Absolute Smithy shape ID of the generated service. */");
                writer.write("static final $T SERVICE_SHAPE_ID = $S;", STRING, service.getId().toString());
                writer.write("");
                writer.write("/** Location-independent SHA-256 digest of the canonical model inputs. */");
                writer.write("static final $T MODEL_DIGEST = $S;", STRING, context.modelDigest());
                writer.write("");
                writer.write("/** Exact, untruncated immutable revision of the canonical model source. */");
                writer.write("static final $T MODEL_REVISION = $S;", STRING, context.modelRevision());
                writer.write("");
                writer.openBlock("private $L() {", "}", typeName, () -> {
                });
                writer.write("");
                writer.write("/** The model provenance published in this service's artifact. */");
                writer.openBlock("static $T provenance() {", "}", MODEL_PROVENANCE, () ->
                    writer.write("return $T.load($L.class, SERVICE_SHAPE_ID, MODEL_REVISION);", MODEL_PROVENANCE,
                                 typeName));
            });
        });
    }
}
