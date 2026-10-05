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

package probe;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Function;
import software.amazon.awssdk.core.client.config.ConfigurationSources;
import software.amazon.awssdk.dynamic.client.DynamicAsyncClient;
import software.amazon.awssdk.dynamic.client.DynamicClient;
import software.amazon.awssdk.dynamic.model.DynamicModel;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.test.codegen.lifecycle.CodegenLifecycleFixtureAsyncClient;
import software.amazon.awssdk.test.codegen.lifecycle.CodegenLifecycleFixtureClient;
import software.amazon.awssdk.test.codegen.lifecycle.model.PingRequest;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;

/**
 * Invokes the same Ping operation of the codegen-lifecycle fixture through its generated sync and async clients and
 * through dynamic sync and async clients of the fixture's own model (the argument: its IDL file, loaded as a JSON AST
 * runtime model), with the same configuration, the same in-memory transport and a direct completion executor.
 * Returns one section per client, each the reply, the transmitted requests and every hook with the aws-pipeline
 * frames that dispatched it.
 */
public final class SharedPipelineProbe implements Function<String, String> {
    private static final Executor DIRECT = Runnable::run;

    @Override
    public String apply(String modelFile) {
        DynamicModel model = DynamicModel.fromJsonAst(jsonAst(Path.of(modelFile)), "codegen-lifecycle-fixture.json");
        List<String> out = new ArrayList<>();
        PingRequest request = PingRequest.builder().message("ping").build();
        Document input = Document.of(Map.of("message", Document.of("ping")));

        ProbeSupport.Transport http = new ProbeSupport.Transport(HttpTransportMode.SYNC);
        ProbeSupport.Recorder recorder = new ProbeSupport.Recorder();
        try (CodegenLifecycleFixtureClient client = CodegenLifecycleFixtureClient.builder()
                .region(Region.US_WEST_2)
                .credentialsProvider(ProbeSupport.CREDENTIALS)
                .endpointOverride(ProbeSupport.ENDPOINT)
                .httpClient(http)
                .addExecutionInterceptor(recorder)
                .build()) {
            section(out, "generated-sync", client.ping(request).message(), http, recorder);
        }

        http = new ProbeSupport.Transport(HttpTransportMode.SYNC);
        recorder = new ProbeSupport.Recorder();
        try (DynamicClient client = DynamicClient.builder()
                .model(model)
                .region(Region.US_WEST_2)
                .credentialsProvider(ProbeSupport.CREDENTIALS)
                .endpointOverride(ProbeSupport.ENDPOINT)
                .configurationSources(ConfigurationSources.none())
                .httpClient(http)
                .addExecutionInterceptor(recorder)
                .build()) {
            section(out, "dynamic-sync", client.call("Ping", input).getMember("message").asString(), http, recorder);
        }

        http = new ProbeSupport.Transport(HttpTransportMode.ASYNC);
        recorder = new ProbeSupport.Recorder();
        try (CodegenLifecycleFixtureAsyncClient client = CodegenLifecycleFixtureAsyncClient.builder()
                .region(Region.US_WEST_2)
                .credentialsProvider(ProbeSupport.CREDENTIALS)
                .endpointOverride(ProbeSupport.ENDPOINT)
                .httpClient(http)
                .asyncCompletionExecutor(DIRECT)
                .addExecutionInterceptor(recorder)
                .build()) {
            section(out, "generated-async", client.ping(request).join().message(), http, recorder);
        }

        http = new ProbeSupport.Transport(HttpTransportMode.ASYNC);
        recorder = new ProbeSupport.Recorder();
        try (DynamicAsyncClient client = DynamicAsyncClient.builder()
                .model(model)
                .region(Region.US_WEST_2)
                .credentialsProvider(ProbeSupport.CREDENTIALS)
                .endpointOverride(ProbeSupport.ENDPOINT)
                .configurationSources(ConfigurationSources.none())
                .httpClient(http)
                .asyncCompletionExecutor(DIRECT)
                .addExecutionInterceptor(recorder)
                .build()) {
            section(out, "dynamic-async", client.callAsync("Ping", input).join().getMember("message").asString(), http,
                    recorder);
        }
        return String.join("\n", out);
    }

    private static void section(List<String> out, String name, String reply, ProbeSupport.Transport http,
                                ProbeSupport.Recorder recorder) {
        out.add("## " + name);
        out.add("reply=" + reply);
        out.add("requests=" + http.targets);
        out.addAll(recorder.events);
    }

    /** The IDL model file as a Smithy JSON AST, the dynamic client's runtime model format. */
    private static String jsonAst(Path idl) {
        ClassLoader loader = SharedPipelineProbe.class.getClassLoader();
        Model model = Model.assembler(loader).discoverModels(loader)
                           .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                           .addImport(idl)
                           .assemble().unwrap();
        return Node.prettyPrintJson(ModelSerializer.builder().build().serialize(model));
    }
}
