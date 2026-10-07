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

package software.amazon.awssdk.pipeline;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.override.CallOptions;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.RetryMode;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;

/**
 * The client-constant user-agent dimensions of one client (Requirements 21.6, 21.7): the SDK version, the service,
 * the HTTP Implementation and the retry mode. A {@link ClientRuntime} built with one gives each of its
 * {@link PipelinePlan}s an {@link Operation} user agent, resolved once per plan, that adds the operation and the
 * feature identifiers the plan's plugins contribute; each call then adds the feature identifiers and API names its
 * override configuration contributes ({@link ApiName}), and nothing else. A component that does not contribute to a
 * call is absent from the call's user agent.
 *
 * <p>The header value has the form
 * <pre>
 * aws-sdk-java/&lt;sdk&gt; ua/2.1 os/&lt;os&gt;#&lt;version&gt; lang/java#&lt;version&gt;
 *     api/&lt;service&gt;#&lt;sdk&gt; md/op#&lt;operation&gt; md/http#&lt;implementation&gt; cfg/retry-mode#&lt;mode&gt; [m/&lt;id&gt;,&lt;id&gt;...]
 *     [&lt;name&gt;/&lt;version&gt; ...]
 * </pre>
 * where {@code m/} lists, once each and in contribution order, the plan's feature identifiers and the versions of the
 * call's {@value ApiName#SDK_METRICS} API names, and the other API names follow as {@code name/version}. Characters a
 * user-agent token cannot carry are replaced by {@code _}. Immutable and thread safe.
 */
public final class UserAgent {
    /** The user-agent header name. */
    public static final String HEADER = "User-Agent";

    private static final String MODULE_RESOURCE = "META-INF/aws-sdk-java-v3/modules/aws-pipeline.properties";
    private static final String SDK_VERSION = readSdkVersion();
    private static final String SYSTEM = "os/" + token(System.getProperty("os.name", "unknown")) + "#"
                                         + token(System.getProperty("os.version", "unknown")) + " lang/java#"
                                         + token(System.getProperty("java.version", "unknown"));

    private final String serviceName;
    private final String sdkVersion;
    private final String httpImplementation;
    private final String retryMode;

    private UserAgent(Builder builder) {
        this.serviceName = token(Objects.requireNonNull(builder.serviceName, "serviceName"));
        this.sdkVersion = token(builder.sdkVersion == null ? SDK_VERSION : builder.sdkVersion);
        this.httpImplementation = token(Objects.requireNonNull(builder.httpImplementation, "httpImplementation"));
        this.retryMode = token(Objects.requireNonNull(builder.retryMode, "retryMode"));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The version of the V3 SDK runtime ({@code aws-pipeline}), or {@code unknown} outside a built artifact. */
    public static String sdkVersion() {
        return SDK_VERSION;
    }

    /**
     * The {@code retry_mode} name of {@code strategy}: {@code standard} or {@code adaptive} for the Modern Retry
     * strategies, {@code custom} for any other implementation.
     */
    public static String retryMode(RetryStrategy strategy) {
        if (strategy instanceof AdaptiveRetryStrategy) {
            return RetryMode.ADAPTIVE.value();
        }
        if (strategy instanceof StandardRetryStrategy) {
            return RetryMode.STANDARD.value();
        }
        return "custom";
    }

    /**
     * The {@value #HEADER} value of a call of {@code plan} with {@code options}: the plan's user agent with the API
     * names the call's overrides add; {@code null} when the client sends no SDK user agent.
     */
    static String headerValue(PipelinePlan<?, ?> plan, CallOptions options) {
        Operation operation = plan.userAgentOrNull();
        if (operation == null) {
            return null;
        }
        return options.isOverridden(SdkClientOption.API_NAMES)
               ? operation.value(options.get(SdkClientOption.API_NAMES).orElse(null))
               : operation.value();
    }

    /** The service dimension. */
    public String serviceName() {
        return serviceName;
    }

    /** The HTTP Implementation dimension. */
    public String httpImplementation() {
        return httpImplementation;
    }

    /** The retry-mode dimension. */
    public String retryMode() {
        return retryMode;
    }

    /**
     * The user agent of {@code operationName} with the plan-constant feature identifiers {@code features}; built once
     * per plan.
     */
    public Operation forOperation(String operationName, List<String> features) {
        String base = "aws-sdk-java/" + sdkVersion + " ua/2.1 " + SYSTEM + " api/" + serviceName + "#" + sdkVersion
                      + " md/op#" + token(Objects.requireNonNull(operationName, "operationName")) + " md/http#"
                      + httpImplementation + " cfg/retry-mode#" + retryMode;
        return new Operation(base, features);
    }

    @Override
    public String toString() {
        return "UserAgent(service=" + serviceName + ", http=" + httpImplementation + ", retryMode=" + retryMode + ")";
    }

    /** {@code value} with every character a user-agent token cannot carry replaced by {@code _}. */
    static String token(String value) {
        StringBuilder out = null;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!isTokenChar(c)) {
                if (out == null) {
                    out = new StringBuilder(value);
                }
                out.setCharAt(i, '_');
            }
        }
        return out == null ? value : out.toString();
    }

    private static boolean isTokenChar(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
               || "!#$%&'*+-.^_`|~".indexOf(c) >= 0;
    }

    /** A feature identifier or API name part: a token without {@code #}, {@code /} or {@code ,} separators. */
    private static String part(String value) {
        return token(value).replace('#', '_');
    }

    private static String readSdkVersion() {
        try (InputStream in = UserAgent.class.getClassLoader() == null
                              ? null : UserAgent.class.getClassLoader().getResourceAsStream(MODULE_RESOURCE)) {
            if (in == null) {
                return "unknown";
            }
            Properties properties = new Properties();
            properties.load(in);
            String version = properties.getProperty("version", "").trim();
            return version.isEmpty() ? "unknown" : version;
        } catch (IOException | RuntimeException e) {
            return "unknown";
        }
    }

    /**
     * The plan-constant user agent of one operation; {@link #value(List)} adds a call's contributions. Immutable and
     * thread safe.
     */
    public static final class Operation {
        private final String base;
        private final List<String> features;
        private final String value;

        private Operation(String base, List<String> features) {
            this.base = base;
            Set<String> unique = new LinkedHashSet<>();
            for (String feature : Objects.requireNonNull(features, "features")) {
                unique.add(part(Objects.requireNonNull(feature, "feature")));
            }
            this.features = List.copyOf(unique);
            this.value = render(this.features, List.of());
        }

        /** The feature identifiers every call of the plan carries, in contribution order. Immutable. */
        public List<String> features() {
            return features;
        }

        /** The header value of a call that contributes nothing; computed once. */
        public String value() {
            return value;
        }

        /**
         * The header value of a call whose override configuration contributes {@code apiNames}: {@link #value()}
         * when it contributes none.
         */
        public String value(List<ApiName> apiNames) {
            if (apiNames == null || apiNames.isEmpty()) {
                return value;
            }
            Set<String> callFeatures = new LinkedHashSet<>(features);
            List<String> others = new ArrayList<>();
            for (ApiName apiName : apiNames) {
                if (ApiName.SDK_METRICS.equals(apiName.name())) {
                    callFeatures.add(part(apiName.version()));
                } else {
                    String other = part(apiName.name()) + "/" + part(apiName.version());
                    if (!others.contains(other)) {
                        others.add(other);
                    }
                }
            }
            return render(List.copyOf(callFeatures), others);
        }

        private String render(List<String> metrics, List<String> others) {
            if (metrics.isEmpty() && others.isEmpty()) {
                return base;
            }
            StringBuilder out = new StringBuilder(base);
            if (!metrics.isEmpty()) {
                out.append(" m/");
                for (int i = 0; i < metrics.size(); i++) {
                    if (i > 0) {
                        out.append(',');
                    }
                    out.append(metrics.get(i));
                }
            }
            for (String other : others) {
                out.append(' ').append(other);
            }
            return out.toString();
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /** Builder of a {@link UserAgent}; the service, HTTP Implementation and retry mode are required. */
    public static final class Builder {
        private String serviceName;
        private String sdkVersion;
        private String httpImplementation;
        private String retryMode;

        private Builder() {
        }

        /** The service dimension, for example the generated client's {@code SERVICE_NAME}. */
        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        /** The SDK version dimension; {@link UserAgent#sdkVersion()} by default. */
        public Builder sdkVersion(String sdkVersion) {
            this.sdkVersion = sdkVersion;
            return this;
        }

        /** The HTTP Implementation dimension, for example the transport's {@code clientName()}. */
        public Builder httpImplementation(String httpImplementation) {
            this.httpImplementation = httpImplementation;
            return this;
        }

        /** The retry-mode dimension, for example {@link UserAgent#retryMode(RetryStrategy)}. */
        public Builder retryMode(String retryMode) {
            this.retryMode = retryMode;
            return this;
        }

        public UserAgent build() {
            return new UserAgent(this);
        }
    }
}
