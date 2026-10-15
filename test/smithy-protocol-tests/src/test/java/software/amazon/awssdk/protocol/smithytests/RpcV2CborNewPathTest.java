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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.protocols.client.rpcv2.RpcV2CborProtocol;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.services.nonquerycompatiblerpcv2protocol.NonQueryCompatibleRpcV2ProtocolClient;
import software.amazon.awssdk.services.querycompatiblerpcv2protocol.QueryCompatibleRpcV2ProtocolClient;
import software.amazon.awssdk.services.rpcv2protocol.RpcV2ProtocolClient;
import software.amazon.awssdk.services.sdkrpcv2protocol.SdkRpcV2ProtocolClient;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Every operation of every rpcv2Cbor Protocol_Test_Service client runs on the schema-based pipeline path (task 3.34),
 * so the generated rpcv2Cbor protocol tests test that path: the call carries its {@code <Svc>Operations} constant as
 * {@code API_OPERATION}, {@code CLIENT_PROTOCOL} is {@link RpcV2CborProtocol}, and the request is sent from
 * {@code SyncApiCallPipeline}, never from the legacy {@code RequestPipelineBuilder} stages.
 */
class RpcV2CborNewPathTest {
    private static final String NEW_PATH_PIPELINE = "software.amazon.awssdk.core.internal.http.SyncApiCallPipeline";
    private static final String LEGACY_PIPELINE_PACKAGE = "software.amazon.awssdk.core.internal.http.pipeline.stages.";

    static Stream<Arguments> clients() {
        return Stream.of(
            Arguments.of(RpcV2ProtocolClient.class, (Supplier<?>) RpcV2ProtocolClient::builder, 13),
            Arguments.of(QueryCompatibleRpcV2ProtocolClient.class,
                         (Supplier<?>) QueryCompatibleRpcV2ProtocolClient::builder, 1),
            Arguments.of(NonQueryCompatibleRpcV2ProtocolClient.class,
                         (Supplier<?>) NonQueryCompatibleRpcV2ProtocolClient::builder, 1),
            Arguments.of(SdkRpcV2ProtocolClient.class, (Supplier<?>) SdkRpcV2ProtocolClient::builder, 1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("clients")
    <C extends SdkClient, B extends AwsClientBuilder<B, C>> void everyOperation_runsOnTheNewPipelinePath(
        Class<C> clientType, Supplier<B> builder, int operationCount) throws Exception {
        List<Method> operations = operations(clientType);
        assertThat(operations).as("operations of %s", clientType.getSimpleName()).hasSize(operationCount);

        for (Method operation : operations) {
            Recorder recorder = new Recorder();
            ProtocolTestHarness<C> harness = ProtocolTestHarnesses.forClient(
                "smithy.protocols#rpcv2Cbor",
                builder,
                b -> b.region(Region.US_EAST_1)
                      .credentialsProvider(AnonymousCredentialsProvider.create())
                      .overrideConfiguration(o -> o.retryStrategy(AwsRetryStrategy.doNotRetry())
                                                   .addExecutionInterceptor(recorder)));
            Object request = newEmptyRequest(operation.getParameterTypes()[0]);
            harness.captureRequest(c -> invoke(operation, c, request), "https://example.com");

            String name = clientType.getSimpleName() + "." + operation.getName();
            assertThat(recorder.operation).as("API_OPERATION of %s", name).isNotNull();
            assertThat(recorder.operation.name()).as("API_OPERATION of %s", name)
                                                 .isEqualToIgnoringCase(operation.getName());
            assertThat(recorder.protocol).as("CLIENT_PROTOCOL of %s", name).isInstanceOf(RpcV2CborProtocol.class);
            assertThat(recorder.frames).as("pipeline of %s", name).contains(NEW_PATH_PIPELINE);
            assertThat(recorder.frames).as("pipeline of %s", name)
                                       .noneMatch(f -> (f.startsWith(LEGACY_PIPELINE_PACKAGE)
                                                        && !f.startsWith(LEGACY_PIPELINE_PACKAGE + "utils."))
                                                       || f.endsWith(".RequestPipelineBuilder"));
        }
    }

    /**
     * The client interface's operation methods that take the request object (not the {@code Consumer} overloads).
     */
    private static List<Method> operations(Class<?> clientType) {
        return Arrays.stream(clientType.getDeclaredMethods())
                     .filter(m -> !Modifier.isStatic(m.getModifiers()))
                     .filter(m -> m.getParameterCount() == 1 && SdkRequest.class.isAssignableFrom(m.getParameterTypes()[0]))
                     .sorted(Comparator.comparing(Method::getName))
                     .collect(Collectors.toList());
    }

    private static Object newEmptyRequest(Class<?> requestType) throws Exception {
        return ((SdkBuilder<?, ?>) requestType.getMethod("builder").invoke(null)).build();
    }

    private static void invoke(Method operation, Object client, Object request) {
        try {
            operation.invoke(client, request);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new IllegalStateException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class Recorder implements ExecutionInterceptor {
        private ApiOperation<?, ?> operation;
        private Object protocol;
        private List<String> frames = new ArrayList<>();

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            operation = executionAttributes.getAttribute(SdkExecutionAttribute.API_OPERATION);
            protocol = executionAttributes.getAttribute(SdkExecutionAttribute.CLIENT_PROTOCOL);
            frames = Arrays.stream(Thread.currentThread().getStackTrace())
                           .map(StackTraceElement::getClassName)
                           .collect(Collectors.toList());
        }
    }
}
