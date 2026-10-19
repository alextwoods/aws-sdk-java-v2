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

package software.amazon.awssdk.archtests.serde;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import software.amazon.awssdk.archtests.ClassFiles;
import software.amazon.awssdk.archtests.serde.MarshallerDependencyScanner.Rule;

/**
 * Negative and positive fixtures for {@link MarshallerDependencyScanner}: ASM-generated classes that reference a
 * legacy serde type in each position the scanner reads, real legacy classes (the DynamoDB Streams client and its
 * generated marshallers, the legacy protocol modules), and classes that reference only the types the new path legitimately
 * shares with the legacy path.
 */
class MarshallerDependencyScannerTest {

    private static final String PKG = "fixture/serde/";
    private static final String HTTP_RESPONSE_HANDLER = "software/amazon/awssdk/core/http/HttpResponseHandler";

    private final Map<String, byte[]> classes = new TreeMap<>();

    @Test
    void scan_fieldOfGeneratedMarshallerType_isReported() {
        define("UsesMarshallerField", cw -> cw.visitField(Opcodes.ACC_PRIVATE, "m",
                                                          "L" + PKG + "PutMetricDataRequestMarshaller;", null, null)
                                              .visitEnd());

        assertThat(scan()).containsExactlyInAnyOrder(
            violation("UsesMarshallerField", PKG + "PutMetricDataRequestMarshaller", Rule.MARSHALLER_NAME));
    }

    @Test
    void scan_methodCallOnUnmarshaller_isReported() {
        define("CallsResponseParser", cw -> method(cw, mv -> {
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, PKG + "GetMetricDataResponseUnmarshaller", "unmarshall",
                               "(Ljava/lang/Object;)Ljava/lang/Object;", false);
            mv.visitInsn(Opcodes.POP);
        }));

        assertThat(scan()).containsExactlyInAnyOrder(
            violation("CallsResponseParser", PKG + "GetMetricDataResponseUnmarshaller", Rule.MARSHALLER_NAME));
    }

    @Test
    void scan_marshallerInGenericSignatureOrAnnotationOrNestedName_isReported() {
        define("SignatureOnly", cw -> {
            cw.visitField(Opcodes.ACC_PRIVATE, "list", "Ljava/util/List;",
                          "Ljava/util/List<L" + PKG + "ListMarshaller;>;", null).visitEnd();
            cw.visitAnnotation("L" + PKG + "AnnotationMarshaller;", true).visitEnd();
            method(cw, mv -> {
                mv.visitInsn(Opcodes.ACONST_NULL);
                mv.visitTypeInsn(Opcodes.CHECKCAST, PKG + "Outer$NestedUnmarshaller");
                mv.visitInsn(Opcodes.POP);
            });
        });

        assertThat(scan()).containsExactlyInAnyOrder(
            violation("SignatureOnly", PKG + "AnnotationMarshaller", Rule.MARSHALLER_NAME),
            violation("SignatureOnly", PKG + "ListMarshaller", Rule.MARSHALLER_NAME),
            violation("SignatureOnly", PKG + "Outer$NestedUnmarshaller", Rule.MARSHALLER_NAME));
    }

    @Test
    void scan_classConstantInTransformPackage_isReported() {
        define("LoadsTransformClass", cw -> method(cw, mv -> {
            mv.visitLdcInsn(Type.getObjectType(PKG + "transform/MetricDatumSerde"));
            mv.visitInsn(Opcodes.POP);
            mv.visitLdcInsn(Type.getObjectType(PKG + "transformer/NotSerde"));
            mv.visitInsn(Opcodes.POP);
        }));

        assertThat(scan()).containsExactlyInAnyOrder(
            violation("LoadsTransformClass", PKG + "transform/MetricDatumSerde", Rule.SERDE_PACKAGE));
    }

    @Test
    void scan_legacyProtocolFactory_isReported() {
        String factory = "software/amazon/awssdk/protocols/json/AwsJsonProtocolFactory";
        define("UsesProtocolFactory", cw -> method(cw, mv -> {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, factory, "builder",
                               "()L" + factory + "$Builder;", false);
            mv.visitInsn(Opcodes.POP);
        }));

        assertThat(scan()).containsExactlyInAnyOrder(
            violation("UsesProtocolFactory", factory, Rule.LEGACY_PROTOCOL),
            violation("UsesProtocolFactory", factory + "$Builder", Rule.LEGACY_PROTOCOL));
    }

    @Test
    void scan_methodHandleToLegacyProtocolType_isReported() {
        String exceptionMetadata = "software/amazon/awssdk/protocols/core/ExceptionMetadata";
        Handle bootstrap = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
                                      "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
                                      + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;"
                                      + "Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)"
                                      + "Ljava/lang/invoke/CallSite;", false);
        Handle target = new Handle(Opcodes.H_INVOKESTATIC, exceptionMetadata, "builder",
                                   "()Ljava/lang/Object;", false);
        define("LambdaToLegacyType", cw -> method(cw, mv -> {
            mv.visitInvokeDynamicInsn("get", "()Ljava/util/function/Supplier;", bootstrap,
                                      Type.getType("()Ljava/lang/Object;"), target,
                                      Type.getType("()Ljava/lang/Object;"));
            mv.visitInsn(Opcodes.POP);
        }));

        assertThat(scan()).containsExactlyInAnyOrder(
            violation("LambdaToLegacyType", exceptionMetadata, Rule.LEGACY_PROTOCOL));
    }

    @Test
    void scan_legacyResponseHandlerDecorators_areReported() {
        String combined = "software/amazon/awssdk/core/internal/http/CombinedResponseHandler";
        String async = "software/amazon/awssdk/core/internal/http/async/AsyncResponseHandler";
        define("UsesLegacyHandlers", cw -> method(cw, mv -> {
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitTypeInsn(Opcodes.CHECKCAST, combined);
            mv.visitInsn(Opcodes.POP);
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitTypeInsn(Opcodes.INSTANCEOF, async);
            mv.visitInsn(Opcodes.POP);
        }));

        assertThat(scan()).containsExactlyInAnyOrder(
            violation("UsesLegacyHandlers", async, Rule.LEGACY_RESPONSE_HANDLER),
            violation("UsesLegacyHandlers", combined, Rule.LEGACY_RESPONSE_HANDLER));
    }

    @Test
    void scan_protocolResponseHandler_matchesEveryApplicableRule() {
        String jsonHandler = "software/amazon/awssdk/protocols/json/internal/unmarshall/JsonResponseHandler";
        define("UsesJsonResponseHandler", cw -> cw.visitField(Opcodes.ACC_PRIVATE, "h", "L" + jsonHandler + ";",
                                                              null, null).visitEnd());

        assertThat(scan()).containsExactlyInAnyOrder(
            violation("UsesJsonResponseHandler", jsonHandler, Rule.SERDE_PACKAGE),
            violation("UsesJsonResponseHandler", jsonHandler, Rule.LEGACY_PROTOCOL),
            violation("UsesJsonResponseHandler", jsonHandler, Rule.LEGACY_RESPONSE_HANDLER));
    }

    @Test
    void scan_scannedClassImplementingHttpResponseHandler_isReportedWhereverReferenced() {
        defineClass("OperationResponseHandler", "java/lang/Object", new String[] {HTTP_RESPONSE_HANDLER}, cw -> { });
        define("UsesLocalHandler", cw -> method(cw, mv -> {
            mv.visitTypeInsn(Opcodes.NEW, PKG + "OperationResponseHandler");
            mv.visitInsn(Opcodes.POP);
        }));

        assertThat(scan()).containsExactlyInAnyOrder(
            violation("OperationResponseHandler", HTTP_RESPONSE_HANDLER, Rule.LEGACY_RESPONSE_HANDLER),
            violation("OperationResponseHandler", PKG + "OperationResponseHandler", Rule.LEGACY_RESPONSE_HANDLER),
            violation("UsesLocalHandler", PKG + "OperationResponseHandler", Rule.LEGACY_RESPONSE_HANDLER));
    }

    @Test
    void scan_sdkTypeNotOnClasspath_isReportedAsUnresolved() {
        String missing = "software/amazon/awssdk/nonexistent/MissingType";
        define("UsesMissingType", cw -> cw.visitField(Opcodes.ACC_PRIVATE, "x", "L" + missing + ";", null, null)
                                          .visitEnd());

        assertThat(scan()).containsExactlyInAnyOrder(violation("UsesMissingType", missing, Rule.UNRESOLVED));
    }

    @Test
    void scan_typesSharedWithLegacyPath_areNotReported() {
        List<String> shared = Arrays.asList(
            "software/amazon/awssdk/schema/ApiOperation",
            "software/amazon/awssdk/core/client/protocol/ClientProtocol",
            "software/amazon/awssdk/core/internal/http/ProtocolMarshalling",
            "software/amazon/awssdk/core/client/handler/ClientExecutionParams",
            "software/amazon/awssdk/core/http/ExecutionContext",
            "software/amazon/awssdk/core/internal/http/pipeline/stages/utils/ExceptionReportingUtils",
            "software/amazon/awssdk/http/async/SdkAsyncHttpResponseHandler",
            "software/amazon/awssdk/awscore/eventstream/EventStreamResponseHandler",
            "software/amazon/awssdk/protocols/jsoncore/JsonNode",
            "software/amazon/awssdk/protocols/client/rpcv2/RpcV2CborProtocol");
        define("NewPathLike", cw -> method(cw, mv -> {
            for (String type : shared) {
                mv.visitInsn(Opcodes.ACONST_NULL);
                mv.visitTypeInsn(Opcodes.CHECKCAST, type);
                mv.visitInsn(Opcodes.POP);
            }
        }));

        assertThat(scan()).isEmpty();
    }

    @Test
    void scan_legacyDynamoDbStreamsClientAndMarshallers_areReported() throws IOException {
        // DynamoDB Streams shares the dynamodb module and stays on the legacy path (task 9.2); SQS no longer does.
        Map<String, byte[]> legacy = ClassFiles.read(NoMarshallerDependencyTest.locate("dynamodb"));
        String marshaller = "software/amazon/awssdk/services/dynamodb/streams/transform/GetRecordsRequestMarshaller";
        String client = "software/amazon/awssdk/services/dynamodb/streams/DefaultDynamoDbStreamsClient";
        Map<String, byte[]> subset = new TreeMap<>();
        subset.put(marshaller, legacy.get(marshaller));
        subset.put(client, legacy.get(client));

        List<String> violations = scan(subset);

        String clientName = client.replace('/', '.');
        assertThat(violations)
            .contains(violation(marshaller, marshaller, Rule.MARSHALLER_NAME),
                      violation(marshaller, marshaller, Rule.SERDE_PACKAGE),
                      violation(client, marshaller, Rule.MARSHALLER_NAME),
                      violation(client, "software/amazon/awssdk/protocols/json/AwsJsonProtocolFactory",
                                Rule.LEGACY_PROTOCOL),
                      violation(client, "software/amazon/awssdk/protocols/json/JsonOperationMetadata",
                                Rule.LEGACY_PROTOCOL),
                      violation(client, HTTP_RESPONSE_HANDLER, Rule.LEGACY_RESPONSE_HANDLER))
            .noneMatch(v -> v.endsWith("(" + Rule.UNRESOLVED.description() + ")"));
        assertThat(violations).anyMatch(v -> v.startsWith(clientName + ": references ")
                                             && v.contains("software.amazon.awssdk.protocols.core.ExceptionMetadata"));
    }

    /**
     * Every class of every legacy protocol module matches {@link Rule#LEGACY_PROTOCOL}, so
     * {@link MarshallerDependencyScanner#LEGACY_PROTOCOL_PACKAGES} covers the modules completely.
     */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"protocol-core", "aws-json-protocol", "aws-query-protocol", "aws-xml-protocol",
                            "aws-cbor-protocol", "smithy-rpcv2-protocol"})
    void rulesMatching_everyClassOfLegacyProtocolModule_matchesLegacyProtocol(String artifactId) throws IOException {
        Map<String, byte[]> moduleClasses = ClassFiles.read(NoMarshallerDependencyTest.locate(artifactId));
        assertThat(moduleClasses).isNotEmpty();
        MarshallerDependencyScanner scanner = MarshallerDependencyScanner.create();

        List<String> unmatched = moduleClasses.keySet().stream()
                                              .filter(n -> !scanner.rulesMatching(n).contains(Rule.LEGACY_PROTOCOL))
                                              .collect(Collectors.toList());

        assertThat(unmatched).as("classes of %s outside LEGACY_PROTOCOL_PACKAGES", artifactId).isEmpty();
    }

    private List<String> scan() {
        return scan(classes);
    }

    private static List<String> scan(Map<String, byte[]> toScan) {
        return MarshallerDependencyScanner.create().scan(toScan);
    }

    private static String violation(String scanned, String referenced, Rule rule) {
        String scannedName = scanned.contains("/") ? scanned : PKG + scanned;
        return scannedName.replace('/', '.') + ": references " + referenced.replace('/', '.')
               + " (" + rule.description() + ")";
    }

    private void define(String simpleName, Consumer<ClassWriter> body) {
        defineClass(simpleName, "java/lang/Object", null, body);
    }

    private void defineClass(String simpleName, String superName, String[] interfaces, Consumer<ClassWriter> body) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, PKG + simpleName, null, superName, interfaces);
        body.accept(cw);
        cw.visitEnd();
        classes.put(PKG + simpleName, cw.toByteArray());
    }

    private static void method(ClassWriter cw, Consumer<MethodVisitor> code) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "m", "()V", null, null);
        mv.visitCode();
        code.accept(mv);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
