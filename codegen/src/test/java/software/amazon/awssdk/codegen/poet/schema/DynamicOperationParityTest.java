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

package software.amazon.awssdk.codegen.poet.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.service.PaginatorDefinition;
import software.amazon.awssdk.codegen.poet.model.ModelPackageCompiler;
import software.amazon.awssdk.codegen.schema.RuntimeCompiler;
import software.amazon.awssdk.codegen.utils.AuthUtils;
import software.amazon.awssdk.dynamicclient.DynamicClient;
import software.amazon.awssdk.dynamicclient.DynamicOperation;
import software.amazon.awssdk.dynamicclient.internal.DynamicOperationMetadata;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.traits.PaginatedTrait;

/**
 * Requirement 30.1 for prototype services, without the optional Property 40 (task 13.11): for every operation, the
 * dynamic client's {@code getOperation} reports the same metadata as the generated {@code <Svc>Operations} field, and
 * the per-call settings the dynamic client derives from the schemas match what the generated operation method passes.
 *
 * <p>The generated operations come from the model package compiled in process, as in
 * {@link SchemaConverterEquivalenceTest}: a {@code dynamic-client} test dependency on a service module would make the
 * reactor cyclic.
 */
class DynamicOperationParityTest {
    private static final ShapeId UNIT = ShapeId.fromParts("smithy.api", "Unit");

    @TempDir
    Path tmp;

    /**
     * Compares, per operation: the operation, input and output ShapeIds, the error ShapeIds in order, the ordered
     * effective auth scheme IDs, every trait of the operation schema, the idempotency token and stream members; and the
     * settings {@link DynamicOperationMetadata} derives (request compression, {@code httpChecksumRequired}, host
     * prefix, bearer credential type, paginator tokens) against the intermediate model the generated client method is
     * generated from, and against the same derivation from the generated operation.
     */
    @ParameterizedTest
    @ValueSource(strings = {"cloudwatch", "secretsmanager", "sqs", "sns", "route53"})
    void getOperation_matchesTheGeneratedOperations(String service) throws Exception {
        IntermediateModel model = PrototypeServices.prepare(service, tmp);
        SchemaCodegen codegen = SchemaCodegen.create(model, PrototypeServices.initializers());
        Path classes = ModelPackageCompiler.compile(model, codegen, tmp.resolve("compile"));

        try (URLClassLoader loader = RuntimeCompiler.classLoader(classes);
             DynamicClient client = DynamicClient.builder()
                                                 .model(model.smithyModel())
                                                 .serviceId(model.serviceId())
                                                 .region(Region.US_EAST_1)
                                                 .credentialsProvider(AnonymousCredentialsProvider.create())
                                                 .httpClient(new UnusedSyncClient())
                                                 .httpClient(new UnusedAsyncClient())
                                                 .build()) {
            Map<String, ApiOperation<?, ?>> generated =
                generatedOperations(Class.forName(codegen.operations().className().reflectionName(), true, loader));
            assertThat(generated).hasSameSizeAs(model.getOperations());

            Function<software.amazon.smithy.model.shapes.ShapeId, software.amazon.smithy.model.shapes.ShapeId> owners =
                SchemaCodegen.errorShapeOwners(model);
            Set<ShapeId> skippedErrors = new TreeSet<>(Comparator.comparing(ShapeId::toString));
            Set<String> multiTokenPaginators = new TreeSet<>();
            int compressed = 0;
            int idempotent = 0;
            int paginated = 0;
            for (OperationModel opModel : model.getOperations().values()) {
                String name = opModel.getOperationName();
                ApiOperation<?, ?> expected = generated.get(name);
                assertThat(expected).as("generated operation %s", name).isNotNull();
                DynamicOperation actual = client.getOperation(name);
                assertSameMetadata(name, expected, actual, owners, skippedErrors);

                DynamicOperationMetadata features = DynamicOperationMetadata.of(actual);
                DynamicOperationMetadata fromGenerated = DynamicOperationMetadata.of(expected);
                assertSameFeatures(name, fromGenerated, features);
                assertMatchesTheIntermediateModel(model, opModel, actual, features, multiTokenPaginators);

                compressed += features.requestCompression() != null ? 1 : 0;
                idempotent += actual.idempotencyTokenMember() != null ? 1 : 0;
                paginated += actual.paginated().isPresent() ? 1 : 0;
            }
            // Guards against a vacuous comparison of the features the prototype services use.
            if ("cloudwatch".equals(service)) {
                assertThat(compressed).as("request compression operations").isPositive();
            }
            if ("secretsmanager".equals(service)) {
                assertThat(idempotent).as("idempotency token operations").isPositive();
            }
            assertThat(paginated).as("paginated operations").isPositive();
            // The known differences, which the comparisons above allow: Route 53's HealthCheckInUse has no generated
            // exception class (the intermediate model drops it), and its ListResourceRecordSets paginator has three
            // tokens, which @paginated cannot express.
            boolean route53 = "route53".equals(service);
            assertThat(skippedErrors).as("errors without a generated exception class")
                                     .isEqualTo(route53 ? Collections.singleton(ShapeId.from("com.amazonaws.route53#"
                                                                                             + "HealthCheckInUse"))
                                                        : Collections.emptySet());
            assertThat(multiTokenPaginators).as("multi-token paginators")
                                            .isEqualTo(route53 ? Collections.singleton("ListResourceRecordSets")
                                                               : Collections.emptySet());
        }
    }

    /**
     * The error ShapeIds are compared after mapping each of the dynamic operation's errors to the shape that owns its
     * generated exception class: when several error shapes of a model share one class (CloudWatch's
     * {@code ResourceNotFound} and {@code ResourceNotFoundException}), the generated operation lists the owner.
     */
    private static void assertSameMetadata(String name, ApiOperation<?, ?> expected, DynamicOperation actual,
                                           Function<software.amazon.smithy.model.shapes.ShapeId,
                                               software.amazon.smithy.model.shapes.ShapeId> owners,
                                           Set<ShapeId> skippedErrors) {
        assertThat(actual.name()).isEqualTo(expected.name());
        assertThat(actual.schema().id()).as("%s schema", name).isEqualTo(expected.schema().id());
        assertSameStructure(name + " input", expected.inputSchema(), actual.inputSchema());
        assertSameStructure(name + " output", expected.outputSchema(), actual.outputSchema());
        List<ShapeId> actualErrors = new ArrayList<>();
        for (ShapeId error : ids(actual.errorSchemas())) {
            software.amazon.smithy.model.shapes.ShapeId ownerId =
                owners.apply(software.amazon.smithy.model.shapes.ShapeId.from(error.toString()));
            if (ownerId == null) {
                // The intermediate model generates no exception class for it, so the generated operation omits it.
                skippedErrors.add(error);
                continue;
            }
            ShapeId owner = ShapeId.from(ownerId.toString());
            if (!actualErrors.contains(owner)) {
                actualErrors.add(owner);
            }
        }
        assertThat(actualErrors).as("%s errors", name).isEqualTo(ids(expected.errorSchemas()));
        assertThat(actual.effectiveAuthSchemes()).as("%s auth", name).isEqualTo(expected.effectiveAuthSchemes());
        assertThat(new HashSet<>(actual.schema().traits())).as("%s operation traits", name)
                                                           .isEqualTo(new HashSet<>(expected.schema().traits()));
        assertThat(memberName(actual.idempotencyTokenMember())).as("%s idempotency token", name)
                                                               .isEqualTo(memberName(expected.idempotencyTokenMember()));
        assertThat(memberName(actual.inputStreamMember())).as("%s input stream", name)
                                                          .isEqualTo(memberName(expected.inputStreamMember()));
        assertThat(memberName(actual.outputStreamMember())).as("%s output stream", name)
                                                           .isEqualTo(memberName(expected.outputStreamMember()));
    }

    /**
     * The same ShapeId, except for a {@code smithy.api#Unit} input or output: the model the dynamic client reads targets
     * {@code smithy.api#Unit}, while codegen synthesizes a structure of the operation's own (the generated request or
     * response class) that keeps its own ShapeId and carries the {@code unitType} trait. Both have no members.
     */
    private static void assertSameStructure(String what, Schema expected, Schema actual) {
        if (UNIT.equals(actual.id())) {
            assertThat(expected.hasTrait(TraitKey.UNIT_TYPE_TRAIT)).as("%s is a synthesized unit", what).isTrue();
            assertThat(expected.members()).as("%s members", what).isEmpty();
            assertThat(actual.members()).as("%s members", what).isEmpty();
            return;
        }
        assertThat(actual.id()).as(what).isEqualTo(expected.id());
    }

    private static void assertSameFeatures(String name, DynamicOperationMetadata expected,
                                           DynamicOperationMetadata actual) {
        assertThat(actual.requestCompression()).as("%s compression", name).isEqualTo(expected.requestCompression());
        assertThat(actual.httpChecksumRequired()).as("%s checksum required", name)
                                                 .isEqualTo(expected.httpChecksumRequired());
        assertThat(actual.hostPrefix()).as("%s host prefix", name).isEqualTo(expected.hostPrefix());
        assertThat(actual.bearerAuthPreferred()).as("%s bearer", name).isEqualTo(expected.bearerAuthPreferred());
        assertThat(actual.paginated()).as("%s paginated", name).isEqualTo(expected.paginated());
    }

    /**
     * The settings against what codegen reads to generate the operation method and paginators.
     */
    private static void assertMatchesTheIntermediateModel(IntermediateModel model, OperationModel opModel,
                                                          DynamicOperation operation, DynamicOperationMetadata features,
                                                          Set<String> multiTokenPaginators) {
        String name = opModel.getOperationName();
        if (opModel.getRequestcompression() == null) {
            assertThat(features.requestCompression()).as("%s compression", name).isNull();
        } else {
            assertThat(features.requestCompression().getEncodings()).as("%s compression", name)
                                                                    .isEqualTo(opModel.getRequestcompression()
                                                                                      .getEncodings());
            assertThat(features.requestCompression().isStreaming()).isEqualTo(opModel.hasStreamingInput());
        }
        assertThat(features.httpChecksumRequired()).as("%s checksum required", name)
                                                   .isEqualTo(opModel.isHttpChecksumRequired());
        assertThat(opModel.getHttpChecksum()).as("%s httpChecksum (none in the prototype services)", name).isNull();
        String hostPrefix = opModel.getEndpointTrait() == null ? null : opModel.getEndpointTrait().getHostPrefix();
        assertThat(features.hostPrefix()).as("%s host prefix", name).isEqualTo(emptyToNull(hostPrefix));
        assertThat(features.bearerAuthPreferred()).as("%s bearer", name)
                                                  .isEqualTo(AuthUtils.isOpBearerAuthPreferred(model, opModel));

        PaginatorDefinition paginator = model.getPaginators().get(name);
        if (opModel.isPaginated() && (paginator.getInputToken().size() > 1 || paginator.getOutputToken().size() > 1)) {
            // A C2J paginator with several tokens (Route 53 ListResourceRecordSets) cannot be a @paginated trait, so the
            // converted model has none and DynamicClient.paginate refuses the operation.
            assertThat(operation.paginated()).as("%s paginated", name).isEmpty();
            multiTokenPaginators.add(name);
        } else if (opModel.isPaginated()) {
            PaginatedTrait trait = operation.paginated().orElse(null);
            assertThat(trait).as("%s paginated", name).isNotNull();
            assertThat(trait.getInputToken()).as("%s input token", name).hasValue(paginator.getInputToken().get(0));
            assertThat(trait.getOutputToken()).as("%s output token", name).hasValue(paginator.getOutputToken().get(0));
            List<String> resultKeys = paginator.getResultKey();
            // A C2J paginator with several result keys (CloudWatch DescribeAlarms) has no single @paginated items path,
            // so the converted model has none; the C2J route reads the keys from paginators-1.json.
            boolean severalResultKeys = resultKeys != null && resultKeys.size() > 1;
            if (!severalResultKeys || trait.getItems().isPresent()) {
                assertThat(trait.getItems().orElse(null)).as("%s items", name)
                                                         .isEqualTo(resultKeys == null ? null : resultKeys.get(0));
            }
            if (trait.getPageSize().isPresent() || paginator.getLimitKey() == null) {
                assertThat(trait.getPageSize().orElse(null)).as("%s page size", name)
                                                            .isEqualTo(paginator.getLimitKey());
            } else {
                // @paginated's pageSize must target an integer; Route 53's MaxItems-style limit keys are strings, so the
                // converted model has no pageSize, while the C2J route reads the limit key from paginators-1.json.
                Schema limit = operation.inputSchema().member(paginator.getLimitKey());
                assertThat(limit.type()).as("%s page size member type", name).isEqualTo(ShapeType.STRING);
            }
        } else {
            // No generated paginator (no paginator, or one without both tokens, such as CloudWatch's
            // DescribeAlarmsForMetric with only a result key): DynamicClient.paginate refuses the operation too.
            PaginatedTrait trait = operation.paginated().orElse(null);
            assertThat(trait == null || !trait.getInputToken().isPresent() || !trait.getOutputToken().isPresent())
                .as("%s has no tokens: %s", name, trait)
                .isTrue();
        }
    }

    private static Map<String, ApiOperation<?, ?>> generatedOperations(Class<?> operationsClass) throws Exception {
        Map<String, ApiOperation<?, ?>> operations = new HashMap<>();
        for (Field field : operationsClass.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && ApiOperation.class.isAssignableFrom(field.getType())) {
                ApiOperation<?, ?> operation = (ApiOperation<?, ?>) field.get(null);
                operations.put(operation.name(), operation);
            }
        }
        return operations;
    }

    private static List<ShapeId> ids(List<Schema> schemas) {
        List<ShapeId> ids = new ArrayList<>();
        for (Schema schema : schemas) {
            ids.add(schema.id());
        }
        return ids;
    }

    private static String memberName(Schema member) {
        return member == null ? null : member.memberName();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static final class UnusedSyncClient implements SdkHttpClient {
        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            throw new UnsupportedOperationException("This test makes no calls");
        }

        @Override
        public void close() {
        }
    }

    private static final class UnusedAsyncClient implements SdkAsyncHttpClient {
        @Override
        public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
            throw new UnsupportedOperationException("This test makes no calls");
        }

        @Override
        public void close() {
        }
    }
}
