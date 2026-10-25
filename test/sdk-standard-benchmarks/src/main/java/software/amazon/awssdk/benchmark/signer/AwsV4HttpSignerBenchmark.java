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

package software.amazon.awssdk.benchmark.signer;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;

/**
 * Signs a DynamoDB-shaped request through the public {@link AwsV4HttpSigner}, the entry point the request pipeline calls.
 * {@code Sigv4SignerBenchmark} in {@code test/sdk-benchmarks} measures the internal {@code V4RequestSigner} only, so it does
 * not see changes above it in {@code DefaultAwsV4HttpSigner}, such as the header-signing fast path (racecar F, task 9.20).
 * This class lives in {@code sdk-standard-benchmarks} because {@code sdk-benchmarks} does not build on this branch: it needs
 * {@code ec2} and {@code cloudfront}, which are in the off-by-default {@code fleet-services} profile, and
 * {@code protocol-tests}, which is no longer a module.
 */
@State(Scope.Thread)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class AwsV4HttpSignerBenchmark {
    private static final byte[] GET_ITEM_BODY =
        "{\"TableName\":\"benchmark-table\",\"Key\":{\"id\":{\"S\":\"item-0001\"}},\"ConsistentRead\":true}"
            .getBytes(StandardCharsets.UTF_8);

    /**
     * {@code session} signs with session credentials (an extra signed header), {@code basic} without.
     */
    @Param({"session", "basic"})
    public String credentials;

    private AwsV4HttpSigner signer;
    private SignRequest<? extends AwsCredentialsIdentity> request;

    @Setup(Level.Trial)
    public void setup() {
        signer = AwsV4HttpSigner.create();
        AwsCredentialsIdentity identity =
            "session".equals(credentials)
            ? AwsSessionCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", "token")
            : AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
        SdkHttpRequest httpRequest =
            SdkHttpRequest.builder()
                          .method(SdkHttpMethod.POST)
                          .protocol("https")
                          .host("dynamodb.us-east-1.amazonaws.com")
                          .encodedPath("/")
                          .putHeader("Content-Type", "application/x-amz-json-1.0")
                          .putHeader("X-Amz-Target", "DynamoDB_20120810.GetItem")
                          .putHeader("Content-Length", Integer.toString(GET_ITEM_BODY.length))
                          .putHeader("amz-sdk-invocation-id", "8f2c0b1e-6a5d-4b1f-9a77-3f0f2d6c1e55")
                          .putHeader("amz-sdk-request", "attempt=1; max=3")
                          .putHeader("User-Agent", "aws-sdk-java/2.x benchmark")
                          .build();
        request = SignRequest.builder(identity)
                             .request(httpRequest)
                             .payload(ContentStreamProvider.fromByteArray(GET_ITEM_BODY))
                             .putProperty(AwsV4HttpSigner.REGION_NAME, "us-east-1")
                             .putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "dynamodb")
                             .putProperty(AwsV4HttpSigner.SIGNING_CLOCK, Clock.systemUTC())
                             .build();
    }

    @Benchmark
    public SignedRequest sign() {
        return signer.sign(request);
    }
}
