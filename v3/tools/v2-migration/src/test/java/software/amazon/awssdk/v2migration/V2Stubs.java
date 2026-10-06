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

package software.amazon.awssdk.v2migration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * Compiles a small V2-shaped API (packages, nesting, generics and signatures that mirror the real V2 surface) into a
 * class directory that test projects are attributed against, so tests do not depend on V2 jars.
 */
final class V2Stubs {

    static final String REGION = "software.amazon.awssdk.regions.Region";
    static final String S3_CLIENT = "software.amazon.awssdk.services.s3.S3Client";
    static final String S3_CLIENT_BUILDER = "software.amazon.awssdk.services.s3.S3ClientBuilder";
    static final String AWS_CLIENT_BUILDER = "software.amazon.awssdk.awscore.client.builder.AwsClientBuilder";
    static final String INTERCEPTOR = "software.amazon.awssdk.core.interceptor.ExecutionInterceptor";
    static final String RETRY_POLICY = "software.amazon.awssdk.core.retry.RetryPolicy";
    static final String OUTER = "software.amazon.awssdk.core.Outer";

    static final String REGION_OF = "api:" + REGION + "#of(Ljava/lang/String;)Lsoftware/amazon/awssdk/regions/Region;";
    static final String REGION_US_EAST_1 = "api:" + REGION + "#US_EAST_1:Lsoftware/amazon/awssdk/regions/Region;";
    static final String S3_BUILDER = "api:" + S3_CLIENT + "#builder()Lsoftware/amazon/awssdk/services/s3/S3ClientBuilder;";
    static final String BUILDER_REGION = "api:" + AWS_CLIENT_BUILDER
                                         + "#region(Lsoftware/amazon/awssdk/regions/Region;)"
                                         + "Lsoftware/amazon/awssdk/awscore/client/builder/AwsClientBuilder;";
    static final String BUILDER_BUILD = "api:" + AWS_CLIENT_BUILDER + "#build()Ljava/lang/Object;";
    static final String BEFORE_EXECUTION = "api:" + INTERCEPTOR
                                           + "#beforeExecution(Lsoftware/amazon/awssdk/core/interceptor/Context$BeforeExecution;"
                                           + "Lsoftware/amazon/awssdk/core/interceptor/ExecutionAttributes;)V";
    static final String DEFAULT_RETRY_POLICY = "api:" + RETRY_POLICY
                                               + "#defaultRetryPolicy()Lsoftware/amazon/awssdk/core/retry/RetryPolicy;";
    static final String INNER_INIT = "api:" + OUTER + "$Inner#<init>(Lsoftware/amazon/awssdk/core/Outer;"
                                     + "Ljava/lang/String;)V";

    private static final Map<String, String> SOURCES = Map.of(
        REGION, "package software.amazon.awssdk.regions;\n"
                + "public final class Region {\n"
                + "    public static final Region US_EAST_1 = new Region();\n"
                + "    private Region() {}\n"
                + "    public static Region of(String id) { return US_EAST_1; }\n"
                + "}\n",
        AWS_CLIENT_BUILDER, "package software.amazon.awssdk.awscore.client.builder;\n"
                            + "import software.amazon.awssdk.regions.Region;\n"
                            + "public interface AwsClientBuilder<B extends AwsClientBuilder<B, C>, C> {\n"
                            + "    B region(Region region);\n"
                            + "    C build();\n"
                            + "}\n",
        S3_CLIENT_BUILDER, "package software.amazon.awssdk.services.s3;\n"
                           + "import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;\n"
                           + "public interface S3ClientBuilder extends AwsClientBuilder<S3ClientBuilder, S3Client> {\n"
                           + "}\n",
        S3_CLIENT, "package software.amazon.awssdk.services.s3;\n"
                   + "public interface S3Client extends AutoCloseable {\n"
                   + "    static S3ClientBuilder builder() { return null; }\n"
                   + "}\n",
        INTERCEPTOR, "package software.amazon.awssdk.core.interceptor;\n"
                     + "public interface ExecutionInterceptor {\n"
                     + "    default void beforeExecution(Context.BeforeExecution context, ExecutionAttributes attributes) {}\n"
                     + "}\n",
        "software.amazon.awssdk.core.interceptor.Context", "package software.amazon.awssdk.core.interceptor;\n"
                                                           + "public final class Context {\n"
                                                           + "    public interface BeforeExecution {}\n"
                                                           + "}\n",
        "software.amazon.awssdk.core.interceptor.ExecutionAttributes", "package software.amazon.awssdk.core.interceptor;\n"
                                                                       + "public final class ExecutionAttributes {}\n",
        RETRY_POLICY, "package software.amazon.awssdk.core.retry;\n"
                      + "public final class RetryPolicy {\n"
                      + "    public static RetryPolicy defaultRetryPolicy() { return null; }\n"
                      + "}\n",
        OUTER, "package software.amazon.awssdk.core;\n"
               + "public class Outer {\n"
               + "    public class Inner { public Inner(String name) {} }\n"
               + "}\n");

    private V2Stubs() {
    }

    /** Compiles the stub API into {@code directory} and returns it. */
    static Path compile(Path directory) throws IOException {
        Path sources = directory.resolve("src");
        Path classes = directory.resolve("classes");
        Files.createDirectories(classes);
        List<String> args = new ArrayList<>(List.of("-d", classes.toString(), "-proc:none", "--release", "21"));
        for (Map.Entry<String, String> source : SOURCES.entrySet()) {
            Path file = sources.resolve(source.getKey().replace('.', '/') + ".java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, source.getValue(), StandardCharsets.UTF_8);
            args.add(file.toString());
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int status = compiler.run(null, null, null, args.toArray(new String[0]));
        if (status != 0) {
            throw new IllegalStateException("Stub compilation failed; see the compiler output");
        }
        return classes;
    }
}
