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

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.WriterDelegator;

/**
 * Generates V2's {@code CloudFrontUtilities} for a service the classifier gives
 * {@link ClientSurfaceClassifier.UtilityKind#CLOUDFRONT} (Requirements 7.10, 16.5): signed URLs and signed cookies
 * for canned and custom policies, signed with the caller's RSA or EC private key (not Modern Auth). It ports V2's
 * semantics: the policy JSON V2's {@code SigningUtils} writes, {@code SHA1} (default) or {@code SHA256} hashing,
 * V2's URL-safe base64 ({@code +} to {@code -}, {@code =} to {@code _}, {@code /} to {@code ~}), the
 * {@code Expires}/{@code Policy}, {@code Signature}, {@code Key-Pair-Id} and {@code Hash-Algorithm} query parameters,
 * and the {@code CloudFront-*} cookies. Emits {@code <root>.CloudFrontUtilities} and its default,
 * {@code model.CannedSignerRequest}, {@code model.CustomSignerRequest}, {@code url.SignedUrl},
 * {@code cookie.CookiesForCannedPolicy}, {@code cookie.CookiesForCustomPolicy} and
 * {@code utils.SigningHashAlgorithm}.
 */
final class CloudFrontUtilitiesGenerator {
    private static final Symbol URI = jdk("java.net", "URI");
    private static final Symbol INSTANT = jdk("java.time", "Instant");
    private static final Symbol OBJECTS = jdk("java.util", "Objects");
    private static final Symbol BASE64 = jdk("java.util", "Base64");
    private static final Symbol OPTIONAL = jdk("java.util", "Optional");
    private static final Symbol CONSUMER = jdk("java.util.function", "Consumer");
    private static final Symbol UTF_8 = jdk("java.nio.charset", "StandardCharsets");
    private static final Symbol PRIVATE_KEY = jdk("java.security", "PrivateKey");
    private static final Symbol SIGNATURE = jdk("java.security", "Signature");
    private static final Symbol GENERAL_SECURITY = jdk("java.security", "GeneralSecurityException");
    private static final Symbol SECURE_RANDOM = jdk("java.security", "SecureRandom");
    private static final Symbol HTTP_REQUEST = jdk("software.amazon.awssdk.http", "SdkHttpRequest");
    private static final Symbol CONFIGURATION = jdk("software.amazon.awssdk.core.client.config",
                                                    "SdkClientConfiguration");
    private static final Symbol SDK_CLIENT_EXCEPTION = jdk("software.amazon.awssdk.core.exception",
                                                           "SdkClientException");

    private CloudFrontUtilitiesGenerator() {
    }

    static void generate(AwsCodegenContext context, Symbol type) {
        String root = context.settings().packageName();
        Symbol implementation = type(root, "Default" + type.getName());
        Symbol canned = type(root + ".model", "CannedSignerRequest");
        Symbol custom = type(root + ".model", "CustomSignerRequest");
        Symbol url = type(root + ".url", "SignedUrl");
        Symbol cannedCookies = type(root + ".cookie", "CookiesForCannedPolicy");
        Symbol customCookies = type(root + ".cookie", "CookiesForCustomPolicy");
        Symbol hash = type(root + ".utils", "SigningHashAlgorithm");
        WriterDelegator<AwsJavaWriter> writers = context.writerDelegator();
        renderInterface(writers, type, implementation, canned, custom, url, cannedCookies, customCookies);
        renderDefault(writers, type, implementation, canned, custom, url, cannedCookies, customCookies, hash);
        renderHash(writers, hash);
        renderRequest(writers, canned, hash, false);
        renderRequest(writers, custom, hash, true);
        renderSignedUrl(writers, url);
        renderCookies(writers, cannedCookies, "expiresHeaderValue", "CloudFront-Expires");
        renderCookies(writers, customCookies, "policyHeaderValue", "CloudFront-Policy");
    }

    private static void renderInterface(WriterDelegator<AwsJavaWriter> writers, Symbol type, Symbol implementation,
                                        Symbol canned, Symbol custom, Symbol url, Symbol cannedCookies,
                                        Symbol customCookies) {
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/** Utilities of the service: signed URLs and cookies of CloudFront distributions. "
                         + "Thread safe. */");
            writer.openBlock("public interface $L {", "}", type.getName(), () -> {
                writer.write("/** The utilities; they need no configuration. */");
                writer.openBlock("static $L create() {", "}", type.getName(),
                                 () -> writer.write("return $T.INSTANCE;", implementation));
                method(writer, url, "getSignedUrlWithCannedPolicy", canned,
                       "A signed URL of the resource, valid until the request's expiration date.");
                method(writer, url, "getSignedUrlWithCustomPolicy", custom,
                       "A signed URL with a custom policy: the resource pattern, active date and IP range.");
                method(writer, cannedCookies, "getCookiesForCannedPolicy", canned,
                       "Signed cookies of the resource, valid until the request's expiration date.");
                method(writer, customCookies, "getCookiesForCustomPolicy", custom,
                       "Signed cookies with a custom policy: the resource pattern, active date and IP range.");
            });
        });
    }

    private static void method(AwsJavaWriter writer, Symbol result, String name, Symbol request, String doc) {
        writer.write("");
        writer.write("/** $L */", doc);
        writer.write("$T $L($T request);", result, name, request);
        writer.write("");
        writer.write("/** {@link #$L($T)} of the request built by {@code request}. */", name, request);
        writer.openBlock("default $T $L($T<$T.Builder> request) {", "}", result, name, CONSUMER, request, () -> {
            writer.write("$T.Builder builder = $T.builder();", request, request);
            writer.write("request.accept(builder);");
            writer.write("return $L(builder.build());", name);
        });
    }

    private static void renderDefault(WriterDelegator<AwsJavaWriter> writers, Symbol utilities, Symbol type,
                                      Symbol canned, Symbol custom, Symbol url, Symbol cannedCookies,
                                      Symbol customCookies, Symbol hash) {
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/** Default {@link $T}: V2's CloudFront signing. */", utilities);
            writer.openBlock("final class $L implements $T {", "}", type.getName(), utilities, () -> {
                writer.write("static final $L INSTANCE = new $L();", type.getName(), type.getName());
                writer.write("");
                writer.openBlock("private $L() {", "}", type.getName(), () -> {
                });
                writer.write("");
                writer.write("/** The client's utilities: CloudFront signing uses none of its configuration. */");
                writer.openBlock("static $L create($T configuration) {", "}", type.getName(), CONFIGURATION,
                                 () -> writer.write("return INSTANCE;"));
                writer.write("");
                writer.write("@Override");
                writer.openBlock("public $T getSignedUrlWithCannedPolicy($T request) {", "}", url, canned, () -> {
                    writer.write("String policy = cannedPolicy(request.resourceUrl(), request.expirationDate());");
                    writer.write("String signature = urlSafe(sign(policy, request.privateKey(), "
                                 + "request.hashAlgorithm()));");
                    writer.write("return signedUrl(request.resourceUrl(), \"Expires=\" + "
                                 + "request.expirationDate().getEpochSecond(), signature, request.keyPairId(),");
                    writer.write("        request.hashAlgorithm());");
                });
                writer.write("");
                writer.write("@Override");
                writer.openBlock("public $T getSignedUrlWithCustomPolicy($T request) {", "}", url, custom, () -> {
                    writer.write("String policy = customPolicy(request.resourceUrlPattern() != null ? "
                                 + "request.resourceUrlPattern()");
                    writer.write("        : request.resourceUrl(), request.activeDate(), request.expirationDate(), "
                                 + "request.ipRange());");
                    writer.write("String signature = urlSafe(sign(policy, request.privateKey(), "
                                 + "request.hashAlgorithm()));");
                    writer.write("return signedUrl(request.resourceUrl(), \"Policy=\" + urlSafe(policy.getBytes("
                                 + "$T.UTF_8)), signature,", UTF_8);
                    writer.write("        request.keyPairId(), request.hashAlgorithm());");
                });
                writer.write("");
                writer.write("@Override");
                writer.openBlock("public $T getCookiesForCannedPolicy($T request) {", "}", cannedCookies, canned,
                                 () -> {
                        writer.write("String policy = cannedPolicy(request.resourceUrl(), request.expirationDate());");
                        writer.write("String signature = urlSafe(sign(policy, request.privateKey(), "
                                     + "request.hashAlgorithm()));");
                        writer.write("return new $T(request.resourceUrl(), \"CloudFront-Key-Pair-Id=\" + "
                                     + "request.keyPairId(),", cannedCookies);
                        writer.write("        \"CloudFront-Signature=\" + signature, \"CloudFront-Expires=\" + "
                                     + "request.expirationDate().getEpochSecond(),");
                        writer.write("        hashCookie(request.hashAlgorithm()));");
                    });
                writer.write("");
                writer.write("@Override");
                writer.openBlock("public $T getCookiesForCustomPolicy($T request) {", "}", customCookies, custom,
                                 () -> {
                        writer.write("String policy = customPolicy(request.resourceUrlPattern() != null ? "
                                     + "request.resourceUrlPattern()");
                        writer.write("        : request.resourceUrl(), request.activeDate(), "
                                     + "request.expirationDate(), request.ipRange());");
                        writer.write("String signature = urlSafe(sign(policy, request.privateKey(), "
                                     + "request.hashAlgorithm()));");
                        writer.write("return new $T(request.resourceUrl(), \"CloudFront-Key-Pair-Id=\" + "
                                     + "request.keyPairId(),", customCookies);
                        writer.write("        \"CloudFront-Signature=\" + signature, \"CloudFront-Policy=\" + "
                                     + "urlSafe(policy.getBytes($T.UTF_8)),", UTF_8);
                        writer.write("        hashCookie(request.hashAlgorithm()));");
                    });
                writer.write("");
                renderHelpers(writer, url, hash);
            });
        });
    }

    private static void renderHelpers(AwsJavaWriter writer, Symbol url, Symbol hash) {
        writer.openBlock("private static $T signedUrl(String resourceUrl, String policyParameter, String signature, "
                         + "String keyPairId,", "}", url, () -> {
                writer.write("        $T hashAlgorithm) {", hash);
                writer.write("$T uri = $T.create(resourceUrl);", URI, URI);
                writer.write("String encodedPath = uri.getRawPath() + (uri.getQuery() != null ? \"?\" + "
                             + "uri.getRawQuery() + \"&\" : \"?\")");
                writer.write("        + policyParameter + \"&Signature=\" + signature + \"&Key-Pair-Id=\" + "
                             + "keyPairId");
                writer.write("        + (hashAlgorithm == $T.SHA1 ? \"\" : \"&Hash-Algorithm=\" + "
                             + "hashAlgorithm.id());", hash);
                writer.write("return new $T(uri.getScheme(), uri.getHost(), encodedPath,", url);
                writer.write("        uri.getScheme() + \"://\" + uri.getAuthority() + encodedPath);");
            });
        writer.write("");
        writer.openBlock("private static String hashCookie($T hashAlgorithm) {", "}", hash, () ->
            writer.write("return hashAlgorithm == $T.SHA1 ? null : \"CloudFront-Hash-Algorithm=\" + "
                         + "hashAlgorithm.id();", hash));
        writer.write("");
        writer.write("/** V2's canned policy JSON. */");
        writer.openBlock("static String cannedPolicy(String resourceUrl, $T expirationDate) {", "}", INSTANT, () -> {
            writer.write("validate(resourceUrl, \"resourceUrl\");");
            writer.write("return \"{\\\"Statement\\\":[{\\\"Resource\\\":\\\"\" + resourceUrl");
            writer.write("       + \"\\\",\\\"Condition\\\":{\\\"DateLessThan\\\":{\\\"AWS:EpochTime\\\":\" + "
                         + "expirationDate.getEpochSecond() + \"}}}]}\";");
        });
        writer.write("");
        writer.write("/** V2's custom policy JSON; a {@code null} resource is {@code *}. */");
        writer.openBlock("static String customPolicy(String resource, $T activeDate, $T expirationDate, "
                         + "String ipRange) {", "}", INSTANT, INSTANT, () -> {
                writer.write("String resourceUrl = resource == null ? \"*\" : resource;");
                writer.write("validate(resourceUrl, \"resourceUrl\");");
                writer.write("StringBuilder policy = new StringBuilder(\"{\\\"Statement\\\":[{\\\"Resource\\\":"
                             + "\\\"\").append(resourceUrl)");
                writer.write("        .append(\"\\\",\\\"Condition\\\":{\\\"DateLessThan\\\":{\\\"AWS:EpochTime"
                             + "\\\":\").append(expirationDate.getEpochSecond())");
                writer.write("        .append('}');");
                writer.openBlock("if (ipRange != null) {", "}", () -> {
                    writer.write("validate(ipRange, \"ipAddress\");");
                    writer.write("policy.append(\",\\\"IpAddress\\\":{\\\"AWS:SourceIp\\\":\\\"\").append(ipRange)"
                                 + ".append(\"\\\"}\");");
                });
                writer.openBlock("if (activeDate != null) {", "}", () ->
                    writer.write("policy.append(\",\\\"DateGreaterThan\\\":{\\\"AWS:EpochTime\\\":\")"
                                 + ".append(activeDate.getEpochSecond()).append('}');"));
                writer.write("return policy.append(\"}}]}\").toString();");
            });
        writer.write("");
        writer.write("/** Rejects what V2 rejects in a policy value: quotes, backslashes and control characters. */");
        writer.openBlock("private static void validate(String input, String name) {", "}", () -> {
            writer.openBlock("for (int i = 0; i < input.length(); i++) {", "}", () -> {
                writer.write("char c = input.charAt(i);");
                writer.openBlock("if (c == '\"' || c == '\\\\' || Character.isISOControl(c)) {", "}", () ->
                    writer.write("throw new IllegalArgumentException(name + \" contains invalid characters. The "
                                 + "character '\" + c + \"' at position \" + i + \" is not allowed.\");"));
            });
        });
        writer.write("");
        writer.openBlock("private static byte[] sign(String policy, $T key, $T hashAlgorithm) {", "}", PRIVATE_KEY,
                         hash, () -> {
                writer.write("String algorithm = switch (key.getAlgorithm()) {");
                writer.write("    case \"RSA\" -> hashAlgorithm.id() + \"withRSA\";");
                writer.write("    case \"EC\", \"ECDSA\" -> hashAlgorithm.id() + \"withECDSA\";");
                writer.write("    default -> throw new IllegalArgumentException(\"Unsupported key algorithm for "
                             + "CloudFront signed URL: \" + key.getAlgorithm());");
                writer.write("};");
                writer.write("try {");
                writer.write("    $T signature = $T.getInstance(algorithm);", SIGNATURE, SIGNATURE);
                writer.write("    signature.initSign(key, new $T());", SECURE_RANDOM);
                writer.write("    signature.update(policy.getBytes($T.UTF_8));", UTF_8);
                writer.write("    return signature.sign();");
                writer.write("} catch ($T e) {", GENERAL_SECURITY);
                writer.write("    throw $T.create(\"Could not sign the CloudFront policy\", e);",
                             SDK_CLIENT_EXCEPTION);
                writer.write("}");
            });
        writer.write("");
        writer.write("/** V2's URL-safe base64: {@code +} to {@code -}, {@code =} to {@code _}, {@code /} to "
                     + "{@code ~}. */");
        writer.openBlock("static String urlSafe(byte[] bytes) {", "}", () ->
            writer.write("return $T.getEncoder().encodeToString(bytes).replace('+', '-').replace('=', '_')"
                         + ".replace('/', '~');", BASE64));
    }

    private static void renderHash(WriterDelegator<AwsJavaWriter> writers, Symbol type) {
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/** The hash algorithm of a CloudFront signature. */");
            writer.openBlock("public enum $L {", "}", type.getName(), () -> {
                writer.write("/** SHA-1, CloudFront's default. */");
                writer.write("SHA1(\"SHA1\"),");
                writer.write("/** SHA-256. */");
                writer.write("SHA256(\"SHA256\");");
                writer.write("");
                writer.write("private final String id;");
                writer.write("");
                writer.openBlock("$L(String id) {", "}", type.getName(), () -> writer.write("this.id = id;"));
                writer.write("");
                writer.write("/** The algorithm's CloudFront and JCA name. */");
                writer.openBlock("public String id() {", "}", () -> writer.write("return id;"));
            });
        });
    }

    private static void renderRequest(WriterDelegator<AwsJavaWriter> writers, Symbol type, Symbol hash,
                                      boolean custom) {
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/** The resource, key and expiration of a $L CloudFront signature. */",
                         custom ? "custom-policy" : "canned-policy");
            writer.openBlock("public final class $L {", "}", type.getName(), () -> {
                writer.write("private final String resourceUrl;");
                writer.write("private final $T privateKey;", PRIVATE_KEY);
                writer.write("private final String keyPairId;");
                writer.write("private final $T expirationDate;", INSTANT);
                writer.write("private final $T hashAlgorithm;", hash);
                if (custom) {
                    writer.write("private final $T activeDate;", INSTANT);
                    writer.write("private final String ipRange;");
                    writer.write("private final String resourceUrlPattern;");
                }
                writer.write("");
                writer.openBlock("private $L(Builder builder) {", "}", type.getName(), () -> {
                    writer.write("this.resourceUrl = $T.requireNonNull(builder.resourceUrl, \"resourceUrl\");",
                                 OBJECTS);
                    writer.write("this.privateKey = $T.requireNonNull(builder.privateKey, \"privateKey\");", OBJECTS);
                    writer.write("this.keyPairId = $T.requireNonNull(builder.keyPairId, \"keyPairId\");", OBJECTS);
                    writer.write("this.expirationDate = $T.requireNonNull(builder.expirationDate, "
                                 + "\"expirationDate\");", OBJECTS);
                    writer.write("this.hashAlgorithm = builder.hashAlgorithm != null ? builder.hashAlgorithm "
                                 + ": $T.SHA1;", hash);
                    if (custom) {
                        writer.write("this.activeDate = builder.activeDate;");
                        writer.write("this.ipRange = builder.ipRange;");
                        writer.write("this.resourceUrlPattern = builder.resourceUrlPattern;");
                    }
                });
                writer.write("");
                writer.openBlock("public static Builder builder() {", "}", () -> writer.write("return new Builder();"));
                accessor(writer, "resourceUrl", "String");
                accessor(writer, "privateKey", writer.format("$T", PRIVATE_KEY));
                accessor(writer, "keyPairId", "String");
                accessor(writer, "expirationDate", writer.format("$T", INSTANT));
                accessor(writer, "hashAlgorithm", writer.format("$T", hash));
                if (custom) {
                    accessor(writer, "activeDate", writer.format("$T", INSTANT));
                    accessor(writer, "ipRange", "String");
                    accessor(writer, "resourceUrlPattern", "String");
                }
                writer.write("");
                writer.write("/** Builder; the resource URL, private key, key pair ID and expiration date are "
                             + "required. Not thread safe. */");
                writer.openBlock("public static final class Builder {", "}", () -> {
                    writer.write("private String resourceUrl;");
                    writer.write("private $T privateKey;", PRIVATE_KEY);
                    writer.write("private String keyPairId;");
                    writer.write("private $T expirationDate;", INSTANT);
                    writer.write("private $T hashAlgorithm;", hash);
                    if (custom) {
                        writer.write("private $T activeDate;", INSTANT);
                        writer.write("private String ipRange;");
                        writer.write("private String resourceUrlPattern;");
                    }
                    writer.write("");
                    writer.openBlock("private Builder() {", "}", () -> {
                    });
                    setter(writer, "resourceUrl", "String");
                    setter(writer, "privateKey", writer.format("$T", PRIVATE_KEY));
                    setter(writer, "keyPairId", "String");
                    setter(writer, "expirationDate", writer.format("$T", INSTANT));
                    setter(writer, "hashAlgorithm", writer.format("$T", hash));
                    if (custom) {
                        setter(writer, "activeDate", writer.format("$T", INSTANT));
                        setter(writer, "ipRange", "String");
                        setter(writer, "resourceUrlPattern", "String");
                    }
                    writer.write("");
                    writer.openBlock("public $L build() {", "}", type.getName(),
                                     () -> writer.write("return new $L(this);", type.getName()));
                });
            });
        });
    }

    private static void renderSignedUrl(WriterDelegator<AwsJavaWriter> writers, Symbol type) {
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/** A signed CloudFront URL. */");
            writer.openBlock("public final class $L {", "}", type.getName(), () -> {
                writer.write("private final String protocol;");
                writer.write("private final String domain;");
                writer.write("private final String encodedPath;");
                writer.write("private final String url;");
                writer.write("");
                writer.openBlock("public $L(String protocol, String domain, String encodedPath, String url) {", "}",
                                 type.getName(), () -> {
                        writer.write("this.protocol = protocol;");
                        writer.write("this.domain = domain;");
                        writer.write("this.encodedPath = encodedPath;");
                        writer.write("this.url = url;");
                    });
                accessor(writer, "protocol", "String");
                accessor(writer, "domain", "String");
                writer.write("");
                writer.write("/** The encoded path and query, signature included. */");
                writer.openBlock("public String encodedPath() {", "}", () -> writer.write("return encodedPath;"));
                writer.write("");
                writer.write("/** The signed URL. */");
                writer.openBlock("public String url() {", "}", () -> writer.write("return url;"));
                writer.write("");
                writer.write("/** A GET request of the signed URL. */");
                writer.openBlock("public $T createHttpGetRequest() {", "}", HTTP_REQUEST, () ->
                    writer.write("return getRequest(url).build();"));
                renderGetRequest(writer);
                writer.write("");
                writer.write("@Override");
                writer.openBlock("public String toString() {", "}", () -> writer.write("return url;"));
            });
        });
    }

    private static void renderCookies(WriterDelegator<AwsJavaWriter> writers, Symbol type, String policyAccessor,
                                      String policyCookie) {
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/** Signed CloudFront cookies: each value is a {@code name=value} {@code Cookie} entry; "
                         + "the {@code $L} one included. */", policyCookie);
            writer.openBlock("public final class $L {", "}", type.getName(), () -> {
                writer.write("private final String resourceUrl;");
                writer.write("private final String keyPairIdHeaderValue;");
                writer.write("private final String signatureHeaderValue;");
                writer.write("private final String $L;", policyAccessor);
                writer.write("private final String hashAlgorithmHeaderValue;");
                writer.write("");
                writer.openBlock("public $L(String resourceUrl, String keyPairIdHeaderValue, String "
                                 + "signatureHeaderValue, String $L,", "}", type.getName(), policyAccessor, () -> {
                        writer.write("        String hashAlgorithmHeaderValue) {");
                        writer.write("this.resourceUrl = resourceUrl;");
                        writer.write("this.keyPairIdHeaderValue = keyPairIdHeaderValue;");
                        writer.write("this.signatureHeaderValue = signatureHeaderValue;");
                        writer.write("this.$L = $L;", policyAccessor, policyAccessor);
                        writer.write("this.hashAlgorithmHeaderValue = hashAlgorithmHeaderValue;");
                    });
                accessor(writer, "resourceUrl", "String");
                accessor(writer, "keyPairIdHeaderValue", "String");
                accessor(writer, "signatureHeaderValue", "String");
                accessor(writer, policyAccessor, "String");
                writer.write("");
                writer.write("/** The {@code CloudFront-Hash-Algorithm} cookie, absent for SHA-1. */");
                writer.openBlock("public $T<String> hashAlgorithmHeaderValue() {", "}", OPTIONAL,
                                 () -> writer.write("return $T.ofNullable(hashAlgorithmHeaderValue);", OPTIONAL));
                writer.write("");
                writer.write("/** A GET request of the resource carrying the cookies. */");
                writer.openBlock("public $T createHttpGetRequest() {", "}", HTTP_REQUEST, () -> {
                    writer.write("String cookie = $L + \"; \" + signatureHeaderValue + \"; \" + "
                                 + "keyPairIdHeaderValue", policyAccessor);
                    writer.write("        + (hashAlgorithmHeaderValue == null ? \"\" : \"; \" + "
                                 + "hashAlgorithmHeaderValue);");
                    writer.write("return getRequest(resourceUrl).putHeader(\"Cookie\", cookie).build();");
                });
                renderGetRequest(writer);
            });
        });
    }

    private static void renderGetRequest(AwsJavaWriter writer) {
        writer.write("");
        writer.openBlock("private static $T.Builder getRequest(String url) {", "}", HTTP_REQUEST, () -> {
            writer.write("$T uri = $T.create(url);", URI, URI);
            writer.write("$T.Builder request = $T.builder().method(\"GET\").scheme(uri.getScheme())", HTTP_REQUEST,
                         HTTP_REQUEST);
            writer.write("        .host(uri.getHost())");
            writer.write("        .encodedPath(uri.getRawPath() == null || uri.getRawPath().isEmpty() ? \"/\" "
                         + ": uri.getRawPath());");
            writer.openBlock("if (uri.getPort() != -1) {", "}", () -> writer.write("request.port(uri.getPort());"));
            writer.openBlock("if (uri.getRawQuery() != null) {", "}", () -> {
                writer.openBlock("for (String parameter : uri.getRawQuery().split(\"&\")) {", "}", () -> {
                    writer.write("int equals = parameter.indexOf('=');");
                    writer.write("request.addRawQueryParameter(equals == -1 ? parameter : parameter.substring(0, "
                                 + "equals),");
                    writer.write("        equals == -1 ? null : parameter.substring(equals + 1));");
                });
            });
            writer.write("return request;");
        });
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
