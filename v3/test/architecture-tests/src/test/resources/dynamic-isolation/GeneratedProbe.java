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

import java.util.function.Function;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.test.codegen.lifecycle.CodegenLifecycleFixtureClient;
import software.amazon.awssdk.test.codegen.lifecycle.model.PingRequest;

/**
 * Builds the generated codegen-lifecycle fixture's sync client over the in-memory transport and invokes Ping once.
 * Returns a transcript of the response, the transmitted request and every hook the call ran.
 */
public final class GeneratedProbe implements Function<String, String> {
    @Override
    public String apply(String message) {
        ProbeSupport.Transport transport = new ProbeSupport.Transport(HttpTransportMode.SYNC);
        ProbeSupport.Recorder recorder = new ProbeSupport.Recorder();
        try (CodegenLifecycleFixtureClient client = CodegenLifecycleFixtureClient.builder()
                .region(Region.US_WEST_2)
                .credentialsProvider(ProbeSupport.CREDENTIALS)
                .endpointOverride(ProbeSupport.ENDPOINT)
                .httpClient(transport)
                .addExecutionInterceptor(recorder)
                .build()) {
            String reply = client.ping(PingRequest.builder().message(message).build()).message();
            return "reply=" + reply + "\nrequests=" + transport.targets + "\n" + String.join("\n", recorder.events);
        }
    }
}
