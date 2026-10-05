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

import java.util.Optional;
import software.amazon.awssdk.awscodegen.ClientSurfaceClassifier.UtilitiesSurface;
import software.amazon.awssdk.awscodegen.ClientSurfaceClassifier.UtilityKind;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.WriterDelegator;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Generates a service's utility entry point where its {@link ClientSurfaceClassifier} classifies one (design
 * "Paginators, waiters, presigners, and utilities"; Requirements 7.10, 16.5): a codegen integration keyed by the
 * service shape ID, never a V2 sidecar. Utilities take public V3 model and configuration types and use only public
 * runtime APIs (Modern Auth signers and identity providers), never pipeline internals.
 *
 * <p>Implemented: {@link UtilityKind#RDS_AUTH_TOKEN}, V2's {@code RdsUtilities}: the public interface and its
 * builder, the package-private {@code DefaultRdsUtilities} (also behind the sync client's {@code utilities()}), and
 * {@code model.GenerateAuthenticationTokenRequest}. The token is a SigV4 query-string presigned
 * {@code GET /?Action=connect&DBUser=...} for signing name {@code rds-db}, valid 15 minutes, without its scheme, as
 * V2 produces it. {@link UtilityKind#DSQL_AUTH_TOKEN}, V2's {@code DsqlUtilities}, is the same mechanism for signing
 * name {@code dsql}: {@code GET /?Action=DbConnect} or {@code DbConnectAdmin}, valid for
 * {@code model.GenerateAuthTokenRequest.expiresIn()} (900 seconds by default). {@link UtilityKind#S3} is
 * {@link S3UtilitiesGenerator}'s and {@link UtilityKind#CLOUDFRONT} is {@link CloudFrontUtilitiesGenerator}'s.
 */
final class UtilitiesGenerator {
    private static final Symbol CONSUMER = jdk("java.util.function", "Consumer");
    private static final Symbol DURATION = jdk("java.time", "Duration");
    private static final Symbol OBJECTS = jdk("java.util", "Objects");
    private static final Symbol COMPLETION_EXCEPTION = jdk("java.util.concurrent", "CompletionException");
    private static final Symbol REGION = jdk("software.amazon.awssdk.regions", "Region");
    private static final Symbol IDENTITY_PROVIDER = jdk("software.amazon.awssdk.identity.spi", "IdentityProvider");
    private static final Symbol CREDENTIALS = jdk("software.amazon.awssdk.identity.spi", "AwsCredentialsIdentity");
    private static final Symbol CONFIGURATION = jdk("software.amazon.awssdk.core.client.config",
                                                    "SdkClientConfiguration");
    private static final Symbol AWS_CLIENT_OPTION = jdk("software.amazon.awssdk.awscore.client.config",
                                                        "AwsClientOption");
    private static final Symbol HTTP_REQUEST = jdk("software.amazon.awssdk.http", "SdkHttpRequest");
    private static final Symbol SIGN_REQUEST = jdk("software.amazon.awssdk.auth.api", "SignRequest");
    private static final Symbol SIGNER = jdk("software.amazon.awssdk.auth.aws", "AwsV4HttpSigner");
    private static final Symbol FAMILY = jdk("software.amazon.awssdk.auth.aws", "AwsV4FamilyHttpSigner");
    private static final Symbol SDK_CLIENT_EXCEPTION = jdk("software.amazon.awssdk.core.exception",
                                                           "SdkClientException");

    private UtilitiesGenerator() {
    }

    /** Whether this generator emits utilities of {@code kind} for {@code service}. */
    static boolean implemented(ServiceShape service, UtilityKind kind) {
        return switch (kind) {
            case RDS_AUTH_TOKEN, DSQL_AUTH_TOKEN, CLOUDFRONT -> true;
            case S3 -> S3UtilitiesGenerator.supported(service);
        };
    }

    static void generate(AwsCodegenContext context, ClientPlan client, ClientSurfaceClassifier classifier) {
        Optional<UtilitiesSurface> surface = classifier.utilities(context.service());
        if (surface.isEmpty() || !implemented(context.service(), surface.get().kind())
            || client.utilities() == null) {
            return;
        }
        String root = context.settings().packageName();
        Symbol type = client.utilities();
        Symbol implementation = type(root, "Default" + type.getName());
        WriterDelegator<AwsJavaWriter> writers = context.writerDelegator();
        switch (surface.get().kind()) {
            case RDS_AUTH_TOKEN -> {
                Symbol request = type(root + ".model", "GenerateAuthenticationTokenRequest");
                renderRdsInterface(writers, type, implementation, request);
                renderRdsDefault(writers, type, implementation, request);
                renderRdsRequest(writers, request);
            }
            case DSQL_AUTH_TOKEN -> {
                Symbol request = type(root + ".model", "GenerateAuthTokenRequest");
                renderDsqlInterface(writers, type, implementation, request);
                renderDsqlDefault(writers, type, implementation, request);
                renderDsqlRequest(writers, request);
            }
            case S3 -> S3UtilitiesGenerator.generate(context, type);
            case CLOUDFRONT -> CloudFrontUtilitiesGenerator.generate(context, type);
            default -> throw new IllegalStateException("Unimplemented utilities " + surface.get().kind());
        }
    }

    private static void renderRdsInterface(WriterDelegator<AwsJavaWriter> writers, Symbol type,
                                           Symbol implementation, Symbol request) {
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/** Utilities of the service: IAM database authentication tokens. Thread safe. */");
            writer.openBlock("public interface $L {", "}", type.getName(), () -> {
                writer.write("/** A builder of utilities. */");
                writer.openBlock("static Builder builder() {", "}",
                                 () -> writer.write("return new $T.DefaultBuilder();", implementation));
                writer.write("");
                writer.write("/**");
                writer.write(" * An authentication token for an IAM database user: a SigV4 presigned {@code connect} "
                             + "request,");
                writer.write(" * valid 15 minutes, signed with the request's (else these utilities') region and "
                             + "credentials.");
                writer.write(" */");
                writer.write("String generateAuthenticationToken($T request);", request);
                writer.write("");
                writer.write("/** {@link #generateAuthenticationToken($T)} of the request built by "
                             + "{@code request}. */", request);
                writer.openBlock("default String generateAuthenticationToken($T<$T.Builder> request) {", "}",
                                 CONSUMER, request, () -> {
                        writer.write("$T.Builder builder = $T.builder();", request, request);
                        writer.write("request.accept(builder);");
                        writer.write("return generateAuthenticationToken(builder.build());");
                    });
                writer.write("");
                writer.write("/** Builder of utilities; not thread safe. */");
                writer.openBlock("interface Builder {", "}", () -> {
                    writer.write("/** The default region of the tokens. */");
                    writer.write("Builder region($T region);", REGION);
                    writer.write("");
                    writer.write("/** The default provider of the credentials the tokens are signed with. */");
                    writer.write("Builder credentialsProvider($T<? extends $T> credentialsProvider);",
                                 IDENTITY_PROVIDER, CREDENTIALS);
                    writer.write("");
                    writer.write("$L build();", type.getName());
                });
            });
        });
    }

    private static void renderRdsDefault(WriterDelegator<AwsJavaWriter> writers, Symbol utilities, Symbol type,
                                         Symbol request) {
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/** Default {@link $T}: signs with the public Modern Auth SigV4 signer. */", utilities);
            writer.openBlock("final class $L implements $T {", "}", type.getName(), utilities, () -> {
                writer.write("private static final $T EXPIRATION = $T.ofMinutes(15);", DURATION, DURATION);
                writer.write("private final $T region;", REGION);
                writer.write("private final $T<? extends $T> credentialsProvider;", IDENTITY_PROVIDER, CREDENTIALS);
                writer.write("");
                writer.openBlock("private $L($T region, $T<? extends $T> credentialsProvider) {", "}",
                                 type.getName(), REGION, IDENTITY_PROVIDER, CREDENTIALS, () -> {
                        writer.write("this.region = region;");
                        writer.write("this.credentialsProvider = credentialsProvider;");
                    });
                writer.write("");
                writer.write("/** Utilities sharing a client's resolved region and credentials provider. */");
                writer.openBlock("static $L create($T configuration) {", "}", type.getName(), CONFIGURATION, () -> {
                    writer.write("return new $L(configuration.get($T.AWS_REGION).orElse(null),", type.getName(),
                                 AWS_CLIENT_OPTION);
                    writer.write("        configuration.get($T.CREDENTIALS_IDENTITY_PROVIDER).orElse(null));",
                                 AWS_CLIENT_OPTION);
                });
                writer.write("");
                writer.write("@Override");
                writer.openBlock("public String generateAuthenticationToken($T request) {", "}", request, () -> {
                    writer.write("$T.requireNonNull(request, \"request\");", OBJECTS);
                    writer.write("$T signingRegion = request.region() != null ? request.region() : region;", REGION);
                    writer.write("$T<? extends $T> provider = request.credentialsProvider() != null", IDENTITY_PROVIDER,
                                 CREDENTIALS);
                    writer.write("        ? request.credentialsProvider() : credentialsProvider;");
                    writer.openBlock("if (signingRegion == null || provider == null) {", "}", () ->
                        writer.write("throw $T.create(\"A region and a credentials provider are required to "
                                     + "generate an authentication token\");", SDK_CLIENT_EXCEPTION));
                    writer.write("$T credentials;", CREDENTIALS);
                    writer.write("try {");
                    writer.write("    credentials = provider.resolveIdentity().join();");
                    writer.write("} catch ($T e) {", COMPLETION_EXCEPTION);
                    writer.write("    throw $T.create(\"Unable to resolve the credentials of the token\", "
                                 + "e.getCause());", SDK_CLIENT_EXCEPTION);
                    writer.write("}");
                    writer.write("$T connect = $T.builder().method(\"GET\").scheme(\"https\")", HTTP_REQUEST,
                                 HTTP_REQUEST);
                    writer.write("        .host(request.hostname()).port(request.port()).encodedPath(\"/\")");
                    writer.write("        .addQueryParameter(\"Action\", \"connect\")");
                    writer.write("        .addQueryParameter(\"DBUser\", request.username())");
                    writer.write("        .build();");
                    writer.write("$T signed = $T.create().sign($T.builder(credentials)", HTTP_REQUEST, SIGNER,
                                 SIGN_REQUEST);
                    writer.write("        .request(connect)");
                    writer.write("        .putProperty($T.REGION_NAME, signingRegion.id())", SIGNER);
                    writer.write("        .putProperty($T.SERVICE_SIGNING_NAME, \"rds-db\")", FAMILY);
                    writer.write("        .putProperty($T.AUTH_LOCATION, $T.AuthLocation.QUERY_STRING)", FAMILY,
                                 FAMILY);
                    writer.write("        .putProperty($T.EXPIRATION_DURATION, EXPIRATION)", FAMILY);
                    writer.write("        .build()).request();");
                    writer.write("// The token is the presigned URL without its scheme, as V2 generates it.");
                    writer.write("return signed.authority() + signed.encodedPath() + \"?\" + signed.encodedQuery();");
                });
                writer.write("");
                writer.write("/** Default {@link $T.Builder}. */", utilities);
                writer.openBlock("static final class DefaultBuilder implements $T.Builder {", "}", utilities, () -> {
                    writer.write("private $T region;", REGION);
                    writer.write("private $T<? extends $T> credentialsProvider;", IDENTITY_PROVIDER, CREDENTIALS);
                    writer.write("");
                    writer.write("@Override");
                    writer.openBlock("public $T.Builder region($T region) {", "}", utilities, REGION, () -> {
                        writer.write("this.region = region;");
                        writer.write("return this;");
                    });
                    writer.write("");
                    writer.write("@Override");
                    writer.openBlock("public $T.Builder credentialsProvider($T<? extends $T> credentialsProvider) {",
                                     "}", utilities, IDENTITY_PROVIDER, CREDENTIALS, () -> {
                            writer.write("this.credentialsProvider = credentialsProvider;");
                            writer.write("return this;");
                        });
                    writer.write("");
                    writer.write("@Override");
                    writer.openBlock("public $T build() {", "}", utilities,
                                     () -> writer.write("return new $L(region, credentialsProvider);",
                                                        type.getName()));
                });
            });
        });
    }

    private static void renderRdsRequest(WriterDelegator<AwsJavaWriter> writers, Symbol type) {
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/** The database endpoint and user of an IAM database authentication token. */");
            writer.openBlock("public final class $L {", "}", type.getName(), () -> {
                writer.write("private final String hostname;");
                writer.write("private final int port;");
                writer.write("private final String username;");
                writer.write("private final $T region;", REGION);
                writer.write("private final $T<? extends $T> credentialsProvider;", IDENTITY_PROVIDER, CREDENTIALS);
                writer.write("");
                writer.openBlock("private $L(Builder builder) {", "}", type.getName(), () -> {
                    writer.write("this.hostname = $T.requireNonNull(builder.hostname, \"hostname\");", OBJECTS);
                    writer.write("this.port = $T.requireNonNull(builder.port, \"port\");", OBJECTS);
                    writer.write("this.username = $T.requireNonNull(builder.username, \"username\");", OBJECTS);
                    writer.write("this.region = builder.region;");
                    writer.write("this.credentialsProvider = builder.credentialsProvider;");
                });
                writer.write("");
                writer.openBlock("public static Builder builder() {", "}", () -> writer.write("return new Builder();"));
                writer.write("");
                writer.openBlock("public String hostname() {", "}", () -> writer.write("return hostname;"));
                writer.write("");
                writer.openBlock("public int port() {", "}", () -> writer.write("return port;"));
                writer.write("");
                writer.openBlock("public String username() {", "}", () -> writer.write("return username;"));
                writer.write("");
                writer.write("/** The token's region, or {@code null} for the utilities' region. */");
                writer.openBlock("public $T region() {", "}", REGION, () -> writer.write("return region;"));
                writer.write("");
                writer.write("/** The token's credentials provider, or {@code null} for the utilities' provider. */");
                writer.openBlock("public $T<? extends $T> credentialsProvider() {", "}", IDENTITY_PROVIDER,
                                 CREDENTIALS, () -> writer.write("return credentialsProvider;"));
                writer.write("");
                writer.write("/** Builder; hostname, port and username are required. Not thread safe. */");
                writer.openBlock("public static final class Builder {", "}", () -> {
                    writer.write("private String hostname;");
                    writer.write("private Integer port;");
                    writer.write("private String username;");
                    writer.write("private $T region;", REGION);
                    writer.write("private $T<? extends $T> credentialsProvider;", IDENTITY_PROVIDER, CREDENTIALS);
                    writer.write("");
                    writer.openBlock("private Builder() {", "}", () -> {
                    });
                    setter(writer, "hostname", "String");
                    setter(writer, "port", "int");
                    setter(writer, "username", "String");
                    setter(writer, "region", writer.format("$T", REGION));
                    setter(writer, "credentialsProvider",
                           writer.format("$T<? extends $T>", IDENTITY_PROVIDER, CREDENTIALS));
                    writer.write("");
                    writer.openBlock("public $L build() {", "}", type.getName(),
                                     () -> writer.write("return new $L(this);", type.getName()));
                });
            });
        });
    }

    private static void renderDsqlInterface(WriterDelegator<AwsJavaWriter> writers, Symbol type,
                                            Symbol implementation, Symbol request) {
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/** Utilities of the service: Aurora DSQL authentication tokens. Thread safe. */");
            writer.openBlock("public interface $L {", "}", type.getName(), () -> {
                writer.write("/** A builder of utilities. */");
                writer.openBlock("static Builder builder() {", "}",
                                 () -> writer.write("return new $T.DefaultBuilder();", implementation));
                for (String[] method : new String[][] {{"generateDbConnectAuthToken", "DbConnect"},
                                                       {"generateDbConnectAdminAuthToken", "DbConnectAdmin"}}) {
                    writer.write("");
                    writer.write("/**");
                    writer.write(" * An authentication token: a SigV4 presigned {@code $L} request for signing name "
                                 + "{@code dsql},", method[1]);
                    writer.write(" * valid for the request's expiration (default 900 seconds), signed with the "
                                 + "request's (else");
                    writer.write(" * these utilities') region and credentials.");
                    writer.write(" */");
                    writer.write("String $L($T request);", method[0], request);
                    writer.write("");
                    writer.write("/** {@link #$L($T)} of the request built by {@code request}. */", method[0],
                                 request);
                    writer.openBlock("default String $L($T<$T.Builder> request) {", "}", method[0], CONSUMER,
                                     request, () -> {
                            writer.write("$T.Builder builder = $T.builder();", request, request);
                            writer.write("request.accept(builder);");
                            writer.write("return $L(builder.build());", method[0]);
                        });
                }
                writer.write("");
                writer.write("/** Builder of utilities; not thread safe. */");
                writer.openBlock("interface Builder {", "}", () -> {
                    writer.write("/** The default region of the tokens. */");
                    writer.write("Builder region($T region);", REGION);
                    writer.write("");
                    writer.write("/** The default provider of the credentials the tokens are signed with. */");
                    writer.write("Builder credentialsProvider($T<? extends $T> credentialsProvider);",
                                 IDENTITY_PROVIDER, CREDENTIALS);
                    writer.write("");
                    writer.write("$L build();", type.getName());
                });
            });
        });
    }

    private static void renderDsqlDefault(WriterDelegator<AwsJavaWriter> writers, Symbol utilities, Symbol type,
                                          Symbol request) {
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/** Default {@link $T}: signs with the public Modern Auth SigV4 signer. */", utilities);
            writer.openBlock("final class $L implements $T {", "}", type.getName(), utilities, () -> {
                writer.write("private final $T region;", REGION);
                writer.write("private final $T<? extends $T> credentialsProvider;", IDENTITY_PROVIDER, CREDENTIALS);
                writer.write("");
                writer.openBlock("private $L($T region, $T<? extends $T> credentialsProvider) {", "}",
                                 type.getName(), REGION, IDENTITY_PROVIDER, CREDENTIALS, () -> {
                        writer.write("this.region = region;");
                        writer.write("this.credentialsProvider = credentialsProvider;");
                    });
                writer.write("");
                writer.write("/** Utilities sharing a client's resolved region and credentials provider. */");
                writer.openBlock("static $L create($T configuration) {", "}", type.getName(), CONFIGURATION, () -> {
                    writer.write("return new $L(configuration.get($T.AWS_REGION).orElse(null),", type.getName(),
                                 AWS_CLIENT_OPTION);
                    writer.write("        configuration.get($T.CREDENTIALS_IDENTITY_PROVIDER).orElse(null));",
                                 AWS_CLIENT_OPTION);
                });
                writer.write("");
                writer.write("@Override");
                writer.openBlock("public String generateDbConnectAuthToken($T request) {", "}", request,
                                 () -> writer.write("return token(request, \"DbConnect\");"));
                writer.write("");
                writer.write("@Override");
                writer.openBlock("public String generateDbConnectAdminAuthToken($T request) {", "}", request,
                                 () -> writer.write("return token(request, \"DbConnectAdmin\");"));
                writer.write("");
                writer.openBlock("private String token($T request, String action) {", "}", request, () -> {
                    writer.write("$T.requireNonNull(request, \"request\");", OBJECTS);
                    writer.write("$T signingRegion = request.region() != null ? request.region() : region;", REGION);
                    writer.write("$T<? extends $T> provider = request.credentialsProvider() != null", IDENTITY_PROVIDER,
                                 CREDENTIALS);
                    writer.write("        ? request.credentialsProvider() : credentialsProvider;");
                    writer.openBlock("if (signingRegion == null || provider == null) {", "}", () ->
                        writer.write("throw $T.create(\"A region and a credentials provider are required to "
                                     + "generate an authentication token\");", SDK_CLIENT_EXCEPTION));
                    writer.write("$T credentials;", CREDENTIALS);
                    writer.write("try {");
                    writer.write("    credentials = provider.resolveIdentity().join();");
                    writer.write("} catch ($T e) {", COMPLETION_EXCEPTION);
                    writer.write("    throw $T.create(\"Unable to resolve the credentials of the token\", "
                                 + "e.getCause());", SDK_CLIENT_EXCEPTION);
                    writer.write("}");
                    writer.write("$T connect = $T.builder().method(\"GET\").scheme(\"https\")", HTTP_REQUEST,
                                 HTTP_REQUEST);
                    writer.write("        .host(request.hostname()).encodedPath(\"/\")");
                    writer.write("        .addQueryParameter(\"Action\", action)");
                    writer.write("        .build();");
                    writer.write("$T signed = $T.create().sign($T.builder(credentials)", HTTP_REQUEST, SIGNER,
                                 SIGN_REQUEST);
                    writer.write("        .request(connect)");
                    writer.write("        .putProperty($T.REGION_NAME, signingRegion.id())", SIGNER);
                    writer.write("        .putProperty($T.SERVICE_SIGNING_NAME, \"dsql\")", FAMILY);
                    writer.write("        .putProperty($T.AUTH_LOCATION, $T.AuthLocation.QUERY_STRING)", FAMILY,
                                 FAMILY);
                    writer.write("        .putProperty($T.EXPIRATION_DURATION, request.expiresIn())", FAMILY);
                    writer.write("        .build()).request();");
                    writer.write("// The token is the presigned URL without its scheme, as V2 generates it.");
                    writer.write("return signed.authority() + signed.encodedPath() + \"?\" + signed.encodedQuery();");
                });
                writer.write("");
                writer.write("/** Default {@link $T.Builder}. */", utilities);
                writer.openBlock("static final class DefaultBuilder implements $T.Builder {", "}", utilities, () -> {
                    writer.write("private $T region;", REGION);
                    writer.write("private $T<? extends $T> credentialsProvider;", IDENTITY_PROVIDER, CREDENTIALS);
                    writer.write("");
                    writer.write("@Override");
                    writer.openBlock("public $T.Builder region($T region) {", "}", utilities, REGION, () -> {
                        writer.write("this.region = region;");
                        writer.write("return this;");
                    });
                    writer.write("");
                    writer.write("@Override");
                    writer.openBlock("public $T.Builder credentialsProvider($T<? extends $T> credentialsProvider) {",
                                     "}", utilities, IDENTITY_PROVIDER, CREDENTIALS, () -> {
                            writer.write("this.credentialsProvider = credentialsProvider;");
                            writer.write("return this;");
                        });
                    writer.write("");
                    writer.write("@Override");
                    writer.openBlock("public $T build() {", "}", utilities,
                                     () -> writer.write("return new $L(region, credentialsProvider);",
                                                        type.getName()));
                });
            });
        });
    }

    private static void renderDsqlRequest(WriterDelegator<AwsJavaWriter> writers, Symbol type) {
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/** The cluster endpoint, and optionally the region, credentials and expiration, of a "
                         + "DSQL token. */");
            writer.openBlock("public final class $L {", "}", type.getName(), () -> {
                writer.write("/** V2's default token expiration. */");
                writer.write("private static final $T DEFAULT_EXPIRES_IN = $T.ofSeconds(900);", DURATION, DURATION);
                writer.write("private final String hostname;");
                writer.write("private final $T expiresIn;", DURATION);
                writer.write("private final $T region;", REGION);
                writer.write("private final $T<? extends $T> credentialsProvider;", IDENTITY_PROVIDER, CREDENTIALS);
                writer.write("");
                writer.openBlock("private $L(Builder builder) {", "}", type.getName(), () -> {
                    writer.write("this.hostname = $T.requireNonNull(builder.hostname, \"hostname\");", OBJECTS);
                    writer.write("this.expiresIn = builder.expiresIn != null ? builder.expiresIn "
                                 + ": DEFAULT_EXPIRES_IN;");
                    writer.write("this.region = builder.region;");
                    writer.write("this.credentialsProvider = builder.credentialsProvider;");
                });
                writer.write("");
                writer.openBlock("public static Builder builder() {", "}", () -> writer.write("return new Builder();"));
                writer.write("");
                writer.openBlock("public String hostname() {", "}", () -> writer.write("return hostname;"));
                writer.write("");
                writer.write("/** How long the token is valid; 900 seconds unless set. */");
                writer.openBlock("public $T expiresIn() {", "}", DURATION, () -> writer.write("return expiresIn;"));
                writer.write("");
                writer.write("/** The token's region, or {@code null} for the utilities' region. */");
                writer.openBlock("public $T region() {", "}", REGION, () -> writer.write("return region;"));
                writer.write("");
                writer.write("/** The token's credentials provider, or {@code null} for the utilities' provider. */");
                writer.openBlock("public $T<? extends $T> credentialsProvider() {", "}", IDENTITY_PROVIDER,
                                 CREDENTIALS, () -> writer.write("return credentialsProvider;"));
                writer.write("");
                writer.write("/** Builder; hostname is required. Not thread safe. */");
                writer.openBlock("public static final class Builder {", "}", () -> {
                    writer.write("private String hostname;");
                    writer.write("private $T expiresIn;", DURATION);
                    writer.write("private $T region;", REGION);
                    writer.write("private $T<? extends $T> credentialsProvider;", IDENTITY_PROVIDER, CREDENTIALS);
                    writer.write("");
                    writer.openBlock("private Builder() {", "}", () -> {
                    });
                    setter(writer, "hostname", "String");
                    setter(writer, "expiresIn", writer.format("$T", DURATION));
                    setter(writer, "region", writer.format("$T", REGION));
                    setter(writer, "credentialsProvider",
                           writer.format("$T<? extends $T>", IDENTITY_PROVIDER, CREDENTIALS));
                    writer.write("");
                    writer.openBlock("public $L build() {", "}", type.getName(),
                                     () -> writer.write("return new $L(this);", type.getName()));
                });
            });
        });
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
