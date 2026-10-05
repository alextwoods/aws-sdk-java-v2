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

import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.awscodegen.EndpointParamsPlan.Parameter;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.WriterDelegator;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;

/**
 * Generates V2's {@code S3Utilities} for a service the classifier gives {@link ClientSurfaceClassifier.UtilityKind#S3}
 * (Requirements 7.10, 16.5): {@code getUrl}, which resolves the object's endpoint through the service's generated
 * endpoint rules (the rule set calls resolve through) and appends the greedy-encoded key without signing or
 * transmitting, and {@code parseUri}, which ports V2's path-style, virtual-hosted-style and CLI-style URI parsing.
 * The service must have an endpoint rule set with a {@code Bucket} parameter and a region parameter.
 */
final class S3UtilitiesGenerator {
    private static final Symbol URI = jdk("java.net", "URI");
    private static final Symbol URL = jdk("java.net", "URL");
    private static final Symbol MALFORMED = jdk("java.net", "MalformedURLException");
    private static final Symbol URL_ENCODER = jdk("java.net", "URLEncoder");
    private static final Symbol URL_DECODER = jdk("java.net", "URLDecoder");
    private static final Symbol UTF_8 = jdk("java.nio.charset", "StandardCharsets");
    private static final Symbol LIST = jdk("java.util", "List");
    private static final Symbol MAP = jdk("java.util", "Map");
    private static final Symbol LINKED_MAP = jdk("java.util", "LinkedHashMap");
    private static final Symbol ARRAY_LIST = jdk("java.util", "ArrayList");
    private static final Symbol COLLECTIONS = jdk("java.util", "Collections");
    private static final Symbol OPTIONAL = jdk("java.util", "Optional");
    private static final Symbol OBJECTS = jdk("java.util", "Objects");
    private static final Symbol CONSUMER = jdk("java.util.function", "Consumer");
    private static final Symbol MATCHER = jdk("java.util.regex", "Matcher");
    private static final Symbol PATTERN = jdk("java.util.regex", "Pattern");
    private static final Symbol COMPLETION_EXCEPTION = jdk("java.util.concurrent", "CompletionException");
    private static final Symbol REGION = jdk("software.amazon.awssdk.regions", "Region");
    private static final Symbol ENDPOINT = jdk("software.amazon.awssdk.endpoints", "Endpoint");
    private static final Symbol CONFIGURATION = jdk("software.amazon.awssdk.core.client.config",
                                                    "SdkClientConfiguration");
    private static final Symbol SDK_CLIENT_OPTION = jdk("software.amazon.awssdk.core.client.config",
                                                        "SdkClientOption");
    private static final Symbol AWS_CLIENT_OPTION = jdk("software.amazon.awssdk.awscore.client.config",
                                                        "AwsClientOption");
    private static final Symbol SDK_CLIENT_EXCEPTION = jdk("software.amazon.awssdk.core.exception",
                                                           "SdkClientException");

    private S3UtilitiesGenerator() {
    }

    /** Whether {@code service}'s endpoint rule set has the parameters {@code getUrl} binds. */
    static boolean supported(ServiceShape service) {
        return service.getTrait(EndpointRuleSetTrait.class)
                      .map(t -> EndpointParamsPlan.parameters(service.getId(), t))
                      .filter(p -> param(p, "Bucket").isPresent() && p.stream().anyMatch(Parameter::region))
                      .isPresent();
    }

    static void generate(AwsCodegenContext context, Symbol type) {
        String root = context.settings().packageName();
        List<Parameter> parameters = EndpointParamsPlan.parameters(
            context.service().getId(), context.service().expectTrait(EndpointRuleSetTrait.class));
        Symbol implementation = type(root, "Default" + type.getName());
        Symbol request = type(root + ".model", "GetUrlRequest");
        Symbol uri = type(root, "S3Uri");
        Symbol params = EndpointRulesGenerator.paramsSymbol(context);
        Symbol rules = EndpointRulesGenerator.rulesSymbol(context);
        WriterDelegator<AwsJavaWriter> writers = context.writerDelegator();
        renderInterface(writers, type, implementation, request, uri);
        renderDefault(writers, type, implementation, request, uri, params, rules, parameters);
        renderRequest(writers, request);
        renderUri(writers, uri);
    }

    private static void renderInterface(WriterDelegator<AwsJavaWriter> writers, Symbol type, Symbol implementation,
                                        Symbol request, Symbol uri) {
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/** Utilities of the service: object URLs and S3 URI parsing. Thread safe. */");
            writer.openBlock("public interface $L {", "}", type.getName(), () -> {
                writer.write("/** A builder of utilities. */");
                writer.openBlock("static Builder builder() {", "}",
                                 () -> writer.write("return new $T.DefaultBuilder();", implementation));
                writer.write("");
                writer.write("/**");
                writer.write(" * The URL of an object, resolved through the service's endpoint rules for the request's "
                             + "(else these");
                writer.write(" * utilities') region and endpoint. The URL is neither signed nor requested.");
                writer.write(" */");
                writer.write("$T getUrl($T request);", URL, request);
                writer.write("");
                writer.write("/** {@link #getUrl($T)} of the request built by {@code request}. */", request);
                writer.openBlock("default $T getUrl($T<$T.Builder> request) {", "}", URL, CONSUMER, request, () -> {
                    writer.write("$T.Builder builder = $T.builder();", request, request);
                    writer.write("request.accept(builder);");
                    writer.write("return getUrl(builder.build());");
                });
                writer.write("");
                writer.write("/**");
                writer.write(" * Parses a path-style, virtual-hosted-style or CLI-style ({@code s3://bucket/key}) S3 "
                             + "URI. Access point,");
                writer.write(" * Outposts, FIPS and dual-stack URIs are not supported. Bucket, key and query "
                             + "parameters are decoded.");
                writer.write(" *");
                writer.write(" * @throws IllegalArgumentException if {@code uri} is not a supported S3 URI");
                writer.write(" */");
                writer.write("$T parseUri($T uri);", uri, URI);
                writer.write("");
                writer.write("/** Builder of utilities; not thread safe. */");
                writer.openBlock("interface Builder {", "}", () -> {
                    writer.write("/** The default region of the URLs. */");
                    writer.write("Builder region($T region);", REGION);
                    writer.write("");
                    writer.write("/** The default endpoint the URLs are resolved against, instead of the region's. */");
                    writer.write("Builder endpointOverride($T endpointOverride);", URI);
                    writer.write("");
                    writer.write("/** Whether URLs are path-style ({@code ForcePathStyle}) instead of "
                                 + "virtual-hosted-style. */");
                    writer.write("Builder forcePathStyle(Boolean forcePathStyle);");
                    writer.write("");
                    writer.write("$L build();", type.getName());
                });
            });
        });
    }

    private static void renderDefault(WriterDelegator<AwsJavaWriter> writers, Symbol utilities, Symbol type,
                                      Symbol request, Symbol uri, Symbol params, Symbol rules,
                                      List<Parameter> parameters) {
        Parameter region = parameters.stream().filter(Parameter::region).findFirst().orElseThrow();
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/** Default {@link $T}: resolves through the service's endpoint rules. */", utilities);
            writer.openBlock("final class $L implements $T {", "}", type.getName(), utilities, () -> {
                writer.write("/** V2's S3 endpoint pattern: an optional bucket prefix, then {@code s3.} or "
                             + "{@code s3-} and a region. */");
                writer.write("private static final $T ENDPOINT_PATTERN = $T.compile(\"^(.+\\\\.)?s3[.-]"
                             + "([a-z0-9-]+)\\\\.\");", PATTERN, PATTERN);
                writer.write("private final $T region;", REGION);
                writer.write("private final $T endpointOverride;", URI);
                writer.write("private final Boolean forcePathStyle;");
                writer.write("");
                writer.openBlock("private $L($T region, $T endpointOverride, Boolean forcePathStyle) {", "}",
                                 type.getName(), REGION, URI, () -> {
                        writer.write("this.region = region;");
                        writer.write("this.endpointOverride = endpointOverride;");
                        writer.write("this.forcePathStyle = forcePathStyle;");
                    });
                writer.write("");
                writer.write("/** Utilities sharing a client's resolved region and endpoint override. */");
                writer.openBlock("static $L create($T configuration) {", "}", type.getName(), CONFIGURATION, () -> {
                    writer.write("return new $L(configuration.get($T.AWS_REGION).orElse(null),", type.getName(),
                                 AWS_CLIENT_OPTION);
                    writer.write("        configuration.get($T.CLIENT_ENDPOINT).orElse(null), null);",
                                 SDK_CLIENT_OPTION);
                });
                writer.write("");
                renderGetUrl(writer, request, params, rules, parameters, region);
                writer.write("");
                renderParseUri(writer, uri);
                writer.write("");
                renderBuilder(writer, utilities, type);
            });
        });
    }

    private static void renderGetUrl(AwsJavaWriter writer, Symbol request, Symbol params, Symbol rules,
                                     List<Parameter> parameters, Parameter region) {
        writer.write("@Override");
        writer.openBlock("public $T getUrl($T request) {", "}", URL, request, () -> {
            writer.write("$T.requireNonNull(request, \"request\");", OBJECTS);
            writer.write("$T resolvedRegion = request.region() != null ? request.region() : region;", REGION);
            writer.openBlock("if (resolvedRegion == null) {", "}", () ->
                writer.write("throw new IllegalArgumentException(\"Region should be provided either in "
                             + "GetUrlRequest object or S3Utilities object\");"));
            writer.write("$T endpoint = request.endpoint() != null ? request.endpoint() : endpointOverride;", URI);
            writer.write("$T.Builder params = $T.builder().bucket(request.bucket()).$L(resolvedRegion);", params,
                         params, region.javaName());
            param(parameters, "Key").ifPresent(p -> writer.write("params.$L(request.key());", p.javaName()));
            param(parameters, "Endpoint").ifPresent(p -> writer.openBlock("if (endpoint != null) {", "}",
                () -> writer.write("params.$L(endpoint.toString());", p.javaName())));
            param(parameters, "ForcePathStyle").ifPresent(p -> writer.openBlock("if (forcePathStyle != null) {", "}",
                () -> writer.write("params.$L(forcePathStyle);", p.javaName())));
            writer.write("$T resolved;", ENDPOINT);
            writer.write("try {");
            writer.write("    resolved = $T.defaultProvider().resolveEndpoint(params.build()).join();", rules);
            writer.write("} catch ($T e) {", COMPLETION_EXCEPTION);
            writer.write("    if (e.getCause() instanceof $T clientException) {", SDK_CLIENT_EXCEPTION);
            writer.write("        throw clientException;");
            writer.write("    }");
            writer.write("    throw $T.create(\"Endpoint resolution failed: \" + e.getCause().getMessage(), "
                         + "e.getCause());", SDK_CLIENT_EXCEPTION);
            writer.write("}");
            writer.write("$T base = resolved.url();", URI);
            writer.write("String path = base.getRawPath() == null ? \"\" : base.getRawPath();");
            writer.write("String query = request.versionId() == null ? \"\" : \"?versionId=\" + request.versionId();");
            writer.write("$T url = $T.create(base.getScheme() + \"://\" + base.getRawAuthority()", URI, URI);
            writer.write("        + (path.endsWith(\"/\") ? path : path + \"/\") + encodeKey(request.key()) "
                         + "+ query);");
            writer.write("try {");
            writer.write("    return url.toURL();");
            writer.write("} catch ($T e) {", MALFORMED);
            writer.write("    throw $T.create(\"Generated URI is malformed: \" + url, e);", SDK_CLIENT_EXCEPTION);
            writer.write("}");
        });
        writer.write("");
        writer.write("/** RFC 3986 percent-encoding of each key segment, keeping its slashes, as V2 encodes keys. */");
        writer.openBlock("private static String encodeKey(String key) {", "}", () -> {
            writer.write("StringBuilder result = new StringBuilder();");
            writer.write("String[] segments = key.split(\"/\", -1);");
            writer.openBlock("for (int i = 0; i < segments.length; i++) {", "}", () -> {
                writer.openBlock("if (i > 0) {", "}", () -> writer.write("result.append('/');"));
                writer.write("result.append($T.encode(segments[i], $T.UTF_8).replace(\"+\", \"%20\")", URL_ENCODER,
                             UTF_8);
                writer.write("        .replace(\"*\", \"%2A\").replace(\"%7E\", \"~\"));");
            });
            writer.write("return result.toString();");
        });
    }

    private static void renderParseUri(AwsJavaWriter writer, Symbol uri) {
        writer.write("@Override");
        writer.openBlock("public $T parseUri($T uri) {", "}", uri, URI, () -> {
            writer.write("$T.requireNonNull(uri, \"uri\");", OBJECTS);
            writer.openBlock("if (uri.toString().contains(\".s3-accesspoint\")) {", "}", () ->
                writer.write("throw new IllegalArgumentException(\"AccessPoints URI parsing is not supported: \" "
                             + "+ uri);"));
            writer.openBlock("if (uri.toString().contains(\".s3-outposts\")) {", "}", () ->
                writer.write("throw new IllegalArgumentException(\"Outposts URI parsing is not supported: \" "
                             + "+ uri);"));
            writer.openBlock("if (\"s3\".equalsIgnoreCase(uri.getScheme())) {", "}", () -> {
                writer.openBlock("if (uri.getAuthority() == null) {", "}", () ->
                    writer.write("throw new IllegalArgumentException(\"Invalid S3 URI: bucket not included: \" "
                                 + "+ uri);"));
                writer.write("String cliKey = uri.getPath().length() > 1 ? uri.getPath().substring(1) : null;");
                writer.write("return new $T(uri, uri.getAuthority(), cliKey, null, false, $T.of());", uri, MAP);
            });
            writer.openBlock("if (uri.getHost() == null) {", "}", () ->
                writer.write("throw new IllegalArgumentException(\"Invalid S3 URI: no hostname: \" + uri);"));
            writer.write("$T matcher = ENDPOINT_PATTERN.matcher(uri.getHost());", MATCHER);
            writer.openBlock("if (!matcher.find()) {", "}", () ->
                writer.write("throw new IllegalArgumentException(\"Invalid S3 URI: hostname does not appear to be "
                             + "a valid S3 endpoint: \" + uri);"));
            writer.write("$T parsedRegion = \"amazonaws\".equals(matcher.group(2)) ? null : $T.of(matcher.group(2));",
                         REGION, REGION);
            writer.write("$T<String, $T<String>> query = queryParameters(uri);", MAP, LIST);
            writer.write("String path = uri.getPath();");
            writer.write("boolean emptyPath = path == null || path.isEmpty() || \"/\".equals(path);");
            writer.write("String prefix = matcher.group(1);");
            writer.openBlock("if (prefix != null && !prefix.isEmpty()) {", "}", () ->
                writer.write("return new $T(uri, prefix.substring(0, prefix.length() - 1), emptyPath ? null "
                             + ": path.substring(1), parsedRegion, false, query);", uri));
            writer.write("String bucket = null;");
            writer.write("String key = null;");
            writer.openBlock("if (!emptyPath) {", "}", () -> {
                writer.write("int index = path.indexOf('/', 1);");
                writer.write("bucket = index == -1 ? path.substring(1) : path.substring(1, index);");
                writer.openBlock("if (index != -1 && index != path.length() - 1) {", "}",
                                 () -> writer.write("key = path.substring(index + 1);"));
            });
            writer.write("return new $T(uri, bucket, key, parsedRegion, true, query);", uri);
        });
        writer.write("");
        writer.write("/** The decoded query parameters, in order; a parameter without a value maps to null. */");
        writer.openBlock("private static $T<String, $T<String>> queryParameters($T uri) {", "}", MAP, LIST, URI, () -> {
            writer.write("$T<String, $T<String>> result = new $T<>();", MAP, LIST, LINKED_MAP);
            writer.openBlock("if (uri.getRawQuery() == null) {", "}", () -> writer.write("return result;"));
            writer.openBlock("for (String parameter : uri.getRawQuery().split(\"&\")) {", "}", () -> {
                writer.openBlock("if (parameter.isEmpty()) {", "}", () -> writer.write("continue;"));
                writer.write("int equals = parameter.indexOf('=');");
                writer.write("String name = decode(equals == -1 ? parameter : parameter.substring(0, equals));");
                writer.write("String value = equals == -1 ? null : decode(parameter.substring(equals + 1));");
                writer.write("result.computeIfAbsent(name, n -> new $T<>()).add(value);", ARRAY_LIST);
            });
            writer.write("return result;");
        });
        writer.write("");
        writer.openBlock("private static String decode(String value) {", "}",
                         () -> writer.write("return $T.decode(value, $T.UTF_8);", URL_DECODER, UTF_8));
    }

    private static void renderBuilder(AwsJavaWriter writer, Symbol utilities, Symbol type) {
        writer.write("/** Default {@link $T.Builder}. */", utilities);
        writer.openBlock("static final class DefaultBuilder implements $T.Builder {", "}", utilities, () -> {
            writer.write("private $T region;", REGION);
            writer.write("private $T endpointOverride;", URI);
            writer.write("private Boolean forcePathStyle;");
            builderSetter(writer, utilities, "region", writer.format("$T", REGION));
            builderSetter(writer, utilities, "endpointOverride", writer.format("$T", URI));
            builderSetter(writer, utilities, "forcePathStyle", "Boolean");
            writer.write("");
            writer.write("@Override");
            writer.openBlock("public $T build() {", "}", utilities, () ->
                writer.write("return new $L(region, endpointOverride, forcePathStyle);", type.getName()));
        });
    }

    private static void builderSetter(AwsJavaWriter writer, Symbol utilities, String name, String parameterType) {
        writer.write("");
        writer.write("@Override");
        writer.openBlock("public $T.Builder $L($L $L) {", "}", utilities, name, parameterType, name, () -> {
            writer.write("this.$L = $L;", name, name);
            writer.write("return this;");
        });
    }

    private static void renderRequest(WriterDelegator<AwsJavaWriter> writers, Symbol type) {
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/** The object, and optionally its version, region and endpoint, of a URL. */");
            writer.openBlock("public final class $L {", "}", type.getName(), () -> {
                writer.write("private final String bucket;");
                writer.write("private final String key;");
                writer.write("private final String versionId;");
                writer.write("private final $T region;", REGION);
                writer.write("private final $T endpoint;", URI);
                writer.write("");
                writer.openBlock("private $L(Builder builder) {", "}", type.getName(), () -> {
                    writer.write("this.bucket = $T.requireNonNull(builder.bucket, \"bucket\");", OBJECTS);
                    writer.write("this.key = $T.requireNonNull(builder.key, \"key\");", OBJECTS);
                    writer.write("this.versionId = builder.versionId;");
                    writer.write("this.region = builder.region;");
                    writer.write("this.endpoint = builder.endpoint;");
                });
                writer.write("");
                writer.openBlock("public static Builder builder() {", "}", () -> writer.write("return new Builder();"));
                accessor(writer, "bucket", "String");
                accessor(writer, "key", "String");
                accessor(writer, "versionId", "String");
                accessor(writer, "region", writer.format("$T", REGION));
                accessor(writer, "endpoint", writer.format("$T", URI));
                writer.write("");
                writer.write("/** Builder; bucket and key are required. Not thread safe. */");
                writer.openBlock("public static final class Builder {", "}", () -> {
                    writer.write("private String bucket;");
                    writer.write("private String key;");
                    writer.write("private String versionId;");
                    writer.write("private $T region;", REGION);
                    writer.write("private $T endpoint;", URI);
                    writer.write("");
                    writer.openBlock("private Builder() {", "}", () -> {
                    });
                    setter(writer, "bucket", "String");
                    setter(writer, "key", "String");
                    setter(writer, "versionId", "String");
                    setter(writer, "region", writer.format("$T", REGION));
                    setter(writer, "endpoint", writer.format("$T", URI));
                    writer.write("");
                    writer.openBlock("public $L build() {", "}", type.getName(),
                                     () -> writer.write("return new $L(this);", type.getName()));
                });
            });
        });
    }

    private static void renderUri(WriterDelegator<AwsJavaWriter> writers, Symbol type) {
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/** A parsed S3 URI: its bucket, key, region, style and decoded query parameters. */");
            writer.openBlock("public final class $L {", "}", type.getName(), () -> {
                writer.write("private final $T uri;", URI);
                writer.write("private final String bucket;");
                writer.write("private final String key;");
                writer.write("private final $T region;", REGION);
                writer.write("private final boolean pathStyle;");
                writer.write("private final $T<String, $T<String>> queryParameters;", MAP, LIST);
                writer.write("");
                writer.openBlock("$L($T uri, String bucket, String key, $T region, boolean pathStyle, "
                                 + "$T<String, $T<String>> queryParameters) {", "}", type.getName(), URI, REGION,
                                 MAP, LIST, () -> {
                        writer.write("this.uri = uri;");
                        writer.write("this.bucket = bucket;");
                        writer.write("this.key = key;");
                        writer.write("this.region = region;");
                        writer.write("this.pathStyle = pathStyle;");
                        writer.write("$T<String, $T<String>> copy = new $T<>();", MAP, LIST, LINKED_MAP);
                        writer.write("queryParameters.forEach((name, values) -> copy.put(name, "
                                     + "$T.unmodifiableList(new $T<>(values))));", COLLECTIONS, ARRAY_LIST);
                        writer.write("this.queryParameters = $T.unmodifiableMap(copy);", COLLECTIONS);
                    });
                writer.write("");
                writer.write("/** The parsed URI. */");
                writer.openBlock("public $T uri() {", "}", URI, () -> writer.write("return uri;"));
                optionalAccessor(writer, "bucket", "String", "bucket");
                optionalAccessor(writer, "key", "String", "key");
                optionalAccessor(writer, "region", writer.format("$T", REGION), "region");
                writer.write("");
                writer.write("/** Whether the URI is path-style. */");
                writer.openBlock("public boolean isPathStyle() {", "}", () -> writer.write("return pathStyle;"));
                writer.write("");
                writer.write("/** Every decoded query parameter by name, in order. */");
                writer.openBlock("public $T<String, $T<String>> rawQueryParameters() {", "}", MAP, LIST,
                                 () -> writer.write("return queryParameters;"));
                writer.write("");
                writer.write("/** The values of query parameter {@code name}, empty if absent. */");
                writer.openBlock("public $T<String> firstMatchingRawQueryParameters(String name) {", "}", LIST,
                                 () -> writer.write("return queryParameters.getOrDefault(name, $T.of());", LIST));
                writer.write("");
                writer.write("/** The first value of query parameter {@code name}. */");
                writer.openBlock("public $T<String> firstMatchingRawQueryParameter(String name) {", "}", OPTIONAL,
                                 () -> writer.write("return firstMatchingRawQueryParameters(name).stream()"
                                                    + ".findFirst();"));
                writer.write("");
                writer.write("@Override");
                writer.openBlock("public String toString() {", "}",
                                 () -> writer.write("return \"S3Uri(\" + uri + \")\";"));
            });
        });
    }

    private static Optional<Parameter> param(List<Parameter> parameters, String name) {
        return parameters.stream().filter(p -> p.name().equals(name)).findFirst();
    }

    private static void optionalAccessor(AwsJavaWriter writer, String name, String valueType, String field) {
        writer.write("");
        writer.write("/** The $L, if the URI has one. */", name);
        writer.openBlock("public $T<$L> $L() {", "}", OPTIONAL, valueType, name,
                         () -> writer.write("return $T.ofNullable($L);", OPTIONAL, field));
    }

    private static void accessor(AwsJavaWriter writer, String name, String valueType) {
        writer.write("");
        writer.openBlock("public $L $L() {", "}", valueType, name, () -> writer.write("return $L;", name));
    }

    private static void setter(AwsJavaWriter writer, String name, String parameterType) {
        writer.write("");
        writer.openBlock("public Builder $L($L $L) {", "}", name, parameterType, name, () -> {
            writer.write("this.$L = $L;", name, name);
            writer.write("return this;");
        });
    }

    private static Symbol type(String packageName, String name) {
        return Symbol.builder()
                     .namespace(packageName, ".")
                     .name(name)
                     .definitionFile(packageName.replace('.', '/') + "/" + name + ".java")
                     .build();
    }

    private static Symbol jdk(String packageName, String name) {
        return Symbol.builder().namespace(packageName, ".").name(name).build();
    }
}
