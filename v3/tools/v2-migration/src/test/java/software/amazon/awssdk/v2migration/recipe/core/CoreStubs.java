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

package software.amazon.awssdk.v2migration.recipe.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * A small V2-shaped API for the source recipe tests (a service client, its builder and a model type; a credentials
 * type; client configuration options and the client override configuration with a Modern Retry setter; the request
 * override builder; an interceptor type), and the V3-shaped API the transformed sources compile against.
 */
final class CoreStubs {
    static final String MODEL = "software.amazon.awssdk.services.demo.model";
    static final String OLD_SHAPE = MODEL + ".OldShape";
    static final String NEW_SHAPE = MODEL + ".NewShape";
    static final String CLIENT = "software.amazon.awssdk.services.demo.DemoClient";
    static final String CLIENT_BUILDER = "software.amazon.awssdk.services.demo.DemoClientBuilder";
    static final String OPTION = "software.amazon.awssdk.core.client.config.DemoOption";
    static final String CLIENT_OVERRIDE = "software.amazon.awssdk.core.client.config.ClientOverrideConfiguration";
    static final String CLIENT_OVERRIDE_BUILDER = CLIENT_OVERRIDE + "$Builder";
    static final String OVERRIDE = "software.amazon.awssdk.core.RequestOverrideConfiguration";
    static final String OVERRIDE_BUILDER = OVERRIDE + "$Builder";
    static final String OLD_HOOK = "software.amazon.awssdk.core.interceptor.OldHook";
    static final String NEW_HOOK = "software.amazon.awssdk.core.interceptor.NewHook";
    static final String LEGACY_CREDENTIALS = "software.amazon.awssdk.auth.credentials.LegacyCredentials";
    static final String IDENTITY = "software.amazon.awssdk.identity.spi.AwsCredentialsIdentity";
    static final String RETRY_STRATEGY = "software.amazon.awssdk.retries.api.RetryStrategy";
    static final String V3_RETRY_STRATEGY = "software.amazon.awssdk.retries.RetryStrategy";

    private static final String STRING = "Ljava/lang/String;";
    private static final String CONSUMER = "Ljava/util/function/Consumer;";

    static final String SHAPE_CREATE = "api:" + OLD_SHAPE + "#create()" + descriptor(OLD_SHAPE);
    static final String NEW_SHAPE_CREATE = "api:" + NEW_SHAPE + "#create()" + descriptor(NEW_SHAPE);
    static final String SHAPE_NAME = "api:" + OLD_SHAPE + "#name()" + STRING;
    static final String NEW_SHAPE_NAME = "api:" + NEW_SHAPE + "#name()" + STRING;
    static final String CLIENT_BUILDER_FACTORY = "api:" + CLIENT + "#builder()" + descriptor(CLIENT_BUILDER);
    static final String BUILD = "api:" + CLIENT_BUILDER + "#build()" + descriptor(CLIENT);
    static final String LEGACY_ENDPOINT = setter(CLIENT_BUILDER, "legacyEndpoint", STRING);
    static final String ENDPOINT_URL = setter(CLIENT_BUILDER, "endpointUrl", STRING);
    static final String BUILDER_REGION = setter(CLIENT_BUILDER, "region", STRING);
    static final String DUALSTACK = setter(CLIENT_BUILDER, "dualstack", "Z");
    static final String BUILDER_CREDENTIALS = setter(CLIENT_BUILDER, "credentials", descriptor(LEGACY_CREDENTIALS));
    static final String V3_BUILDER_CREDENTIALS = setter(CLIENT_BUILDER, "credentials", descriptor(IDENTITY));
    static final String BUILDER_OVERRIDE = setter(CLIENT_BUILDER, "overrideConfiguration", CONSUMER);
    static final String CREDENTIALS_CREATE = "api:" + LEGACY_CREDENTIALS + "#create()" + descriptor(LEGACY_CREDENTIALS);
    static final String IDENTITY_CREATE = "api:" + IDENTITY + "#create()" + descriptor(IDENTITY);
    static final String OLD_DESCRIBE = "api:" + CLIENT + "#oldDescribe(" + STRING + ")" + STRING;
    static final String DESCRIBE = "api:" + CLIENT + "#describe(" + STRING + ")" + STRING;
    static final String OLD_TIMEOUT = "api:" + OPTION + "#OLD_TIMEOUT:" + descriptor(OPTION);
    static final String TIMEOUT = "api:" + OPTION + "#TIMEOUT:" + descriptor(OPTION);
    static final String LEGACY_FLAG = "api:" + OPTION + "#LEGACY_FLAG:" + descriptor(OPTION);
    static final String RETRY_STRATEGY_SETTER = setter(CLIENT_OVERRIDE_BUILDER, "retryStrategy",
                                                       descriptor(RETRY_STRATEGY));
    static final String V3_RETRY_STRATEGY_SETTER = setter(CLIENT_OVERRIDE_BUILDER, "retryStrategy",
                                                          descriptor(V3_RETRY_STRATEGY));
    static final String RETRY_STRATEGY_CONSUMER = setter(CLIENT_OVERRIDE_BUILDER, "retryStrategy", CONSUMER);
    static final String MAX_ATTEMPTS = "api:" + RETRY_STRATEGY + "$Builder#maxAttempts(I)"
                                       + descriptor(RETRY_STRATEGY + "$Builder");
    static final String OVERRIDE_FACTORY = "api:" + OVERRIDE + "#builder()" + descriptor(OVERRIDE_BUILDER);
    static final String OVERRIDE_BUILD = "api:" + OVERRIDE_BUILDER + "#build()" + descriptor(OVERRIDE);
    static final String PUT_LEGACY_HEADER = setter(OVERRIDE_BUILDER, "putLegacyHeader", STRING + STRING);
    static final String PUT_HEADER = setter(OVERRIDE_BUILDER, "putHeader", STRING + STRING);

    private static final Map<String, String> V2 = Map.of(
        OLD_SHAPE, "package " + MODEL + ";\n"
                   + "public class OldShape {\n"
                   + "    public static OldShape create() { return new OldShape(); }\n"
                   + "    public String name() { return \"shape\"; }\n"
                   + "}\n",
        CLIENT_BUILDER, "package software.amazon.awssdk.services.demo;\n"
                        + "import java.util.function.Consumer;\n"
                        + "import software.amazon.awssdk.auth.credentials.LegacyCredentials;\n"
                        + "import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;\n"
                        + "public interface DemoClientBuilder {\n"
                        + "    DemoClientBuilder legacyEndpoint(String endpoint);\n"
                        + "    DemoClientBuilder region(String region);\n"
                        + "    DemoClientBuilder dualstack(boolean enabled);\n"
                        + "    DemoClientBuilder credentials(LegacyCredentials credentials);\n"
                        + "    DemoClientBuilder overrideConfiguration(Consumer<ClientOverrideConfiguration.Builder> c);\n"
                        + "    DemoClient build();\n"
                        + "}\n",
        CLIENT, "package software.amazon.awssdk.services.demo;\n"
                + "public interface DemoClient {\n"
                + "    static DemoClientBuilder builder() { return null; }\n"
                + "    String oldDescribe(String id);\n"
                + "}\n",
        OPTION, "package software.amazon.awssdk.core.client.config;\n"
                + "public final class DemoOption {\n"
                + "    public static final DemoOption OLD_TIMEOUT = new DemoOption();\n"
                + "    public static final DemoOption LEGACY_FLAG = new DemoOption();\n"
                + "}\n",
        CLIENT_OVERRIDE, "package software.amazon.awssdk.core.client.config;\n"
                         + "import java.util.function.Consumer;\n"
                         + "import software.amazon.awssdk.retries.api.RetryStrategy;\n"
                         + "public final class ClientOverrideConfiguration {\n"
                         + "    public interface Builder {\n"
                         + "        Builder retryStrategy(RetryStrategy strategy);\n"
                         + "        Builder retryStrategy(Consumer<RetryStrategy.Builder> strategy);\n"
                         + "    }\n"
                         + "}\n",
        RETRY_STRATEGY, "package software.amazon.awssdk.retries.api;\n"
                        + "public interface RetryStrategy {\n"
                        + "    interface Builder {\n"
                        + "        Builder maxAttempts(int maxAttempts);\n"
                        + "    }\n"
                        + "}\n",
        LEGACY_CREDENTIALS, "package software.amazon.awssdk.auth.credentials;\n"
                            + "public class LegacyCredentials {\n"
                            + "    public static LegacyCredentials create() { return new LegacyCredentials(); }\n"
                            + "}\n",
        OVERRIDE, "package software.amazon.awssdk.core;\n"
                  + "public final class RequestOverrideConfiguration {\n"
                  + "    public static Builder builder() { return null; }\n"
                  + "    public interface Builder {\n"
                  + "        Builder putLegacyHeader(String name, String value);\n"
                  + "        RequestOverrideConfiguration build();\n"
                  + "    }\n"
                  + "}\n",
        OLD_HOOK, "package software.amazon.awssdk.core.interceptor;\n"
                  + "public class OldHook {}\n");

    private static final Map<String, String> V3 = Map.of(
        NEW_SHAPE, "package " + MODEL + ";\n"
                   + "public class NewShape {\n"
                   + "    public static NewShape create() { return new NewShape(); }\n"
                   + "    public String name() { return \"shape\"; }\n"
                   + "}\n",
        CLIENT_BUILDER, "package software.amazon.awssdk.services.demo;\n"
                        + "import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;\n"
                        + "public interface DemoClientBuilder {\n"
                        + "    DemoClientBuilder endpointUrl(String endpoint);\n"
                        + "    DemoClientBuilder region(String region);\n"
                        + "    DemoClientBuilder credentials(AwsCredentialsIdentity credentials);\n"
                        + "    DemoClient build();\n"
                        + "}\n",
        CLIENT, "package software.amazon.awssdk.services.demo;\n"
                + "public interface DemoClient {\n"
                + "    static DemoClientBuilder builder() { return null; }\n"
                + "    String describe(String id);\n"
                + "}\n",
        OPTION, "package software.amazon.awssdk.core.client.config;\n"
                + "public final class DemoOption {\n"
                + "    public static final DemoOption TIMEOUT = new DemoOption();\n"
                + "}\n",
        IDENTITY, "package software.amazon.awssdk.identity.spi;\n"
                  + "public class AwsCredentialsIdentity {\n"
                  + "    public static AwsCredentialsIdentity create() { return new AwsCredentialsIdentity(); }\n"
                  + "}\n",
        OVERRIDE, "package software.amazon.awssdk.core;\n"
                  + "public final class RequestOverrideConfiguration {\n"
                  + "    public static Builder builder() { return null; }\n"
                  + "    public interface Builder {\n"
                  + "        Builder putHeader(String name, String value);\n"
                  + "        RequestOverrideConfiguration build();\n"
                  + "    }\n"
                  + "}\n");

    private CoreStubs() {
    }

    /** Compiles the V2 stub API into {@code directory} and returns its class directory. */
    static Path compileV2(Path directory) throws IOException {
        return CoreFixtures.compile(directory, V2, List.of());
    }

    /** Compiles the V3 stub API into {@code directory} and returns its class directory. */
    static Path compileV3(Path directory) throws IOException {
        return CoreFixtures.compile(directory, V3, List.of());
    }

    static String descriptor(String binaryName) {
        return "L" + binaryName.replace('.', '/') + ";";
    }

    /** The ID of a fluent setter of {@code owner} that returns {@code owner}. */
    private static String setter(String owner, String name, String parameters) {
        return "api:" + owner + "#" + name + "(" + parameters + ")" + descriptor(owner);
    }
}
