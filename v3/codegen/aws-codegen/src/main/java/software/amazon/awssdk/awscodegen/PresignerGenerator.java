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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.awscodegen.ClientSurfaceClassifier.PresignerSurface;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.WriterDelegator;

/**
 * Generates a service's Modern Auth presigner (design "Paginators, waiters, presigners, and utilities"; Requirements
 * 7.10, 16.5, 18.1-18.3) where its {@link ClientSurfaceClassifier} classifies one: the public
 * {@code <root>.presigner.<Name>} interface with V2's {@code presign<Operation>} methods and its builder, the
 * package-private {@code Default<Name>}, and per presigned operation the {@code <root>.presigner.model}
 * {@code <Operation>PresignRequest} and {@code Presigned<Operation>Request} types.
 *
 * <p>The presigner is a signing-only client of the service: its builder shares the client's options and Modern Auth
 * setup and builds a runtime without a transport, and each method runs the operation through the one engine's
 * {@code presign} entry point, which signs with the auth scheme selected for the operation, adjusted for query-string
 * signing, and never transmits. There is no {@code signer(...)} option and no legacy {@code Signer} or
 * {@code Presigner} contract (Requirements 18.1, 18.2).
 */
final class PresignerGenerator {
    private static final Symbol CONSUMER = jdk("java.util.function", "Consumer");
    private static final Symbol DURATION = jdk("java.time", "Duration");
    private static final Symbol INSTANT = jdk("java.time", "Instant");
    private static final Symbol URL = jdk("java.net", "URL");
    private static final Symbol URI = jdk("java.net", "URI");
    private static final Symbol MALFORMED_URL = jdk("java.net", "MalformedURLException");
    private static final Symbol LIST = jdk("java.util", "List");
    private static final Symbol MAP = jdk("java.util", "Map");
    private static final Symbol OBJECTS = jdk("java.util", "Objects");
    private static final Symbol REGION = jdk("software.amazon.awssdk.regions", "Region");
    private static final Symbol IDENTITY_PROVIDER = jdk("software.amazon.awssdk.identity.spi", "IdentityProvider");
    private static final Symbol CREDENTIALS = jdk("software.amazon.awssdk.identity.spi", "AwsCredentialsIdentity");
    private static final Symbol INTERCEPTOR = jdk("software.amazon.awssdk.pipeline", "ExecutionInterceptor");
    private static final Symbol PLAN = jdk("software.amazon.awssdk.pipeline", "PipelinePlan");
    private static final Symbol AWS_OPERATION = jdk("software.amazon.awssdk.pipeline", "AwsOperation");
    private static final Symbol PRESIGN_SPEC = jdk("software.amazon.awssdk.pipeline", "PresignSpec");
    private static final Symbol PRESIGNED_HTTP = jdk("software.amazon.awssdk.pipeline", "PresignedHttpRequest");
    private static final Symbol BUILD_STATE = jdk("software.amazon.awssdk.pipeline.client", "AwsClientBuildState");
    private static final Symbol PARTS = jdk("software.amazon.awssdk.pipeline.client", "AwsClientParts");
    private static final Symbol PRESIGNING = jdk("software.amazon.awssdk.auth.aws", "AwsV4Presigning");
    private static final Symbol HTTP_REQUEST = jdk("software.amazon.awssdk.http", "SdkHttpRequest");
    private static final Symbol SDK_CLIENT_EXCEPTION = jdk("software.amazon.awssdk.core.exception",
                                                           "SdkClientException");

    private PresignerGenerator() {
    }

    /** One presigned operation of the plan. */
    private record Presigned(ClientPlan.Operation operation, Symbol request, Symbol result, String member) {
    }

    static void generate(AwsCodegenContext context, ClientPlan client, ClientSurfaceClassifier classifier) {
        Optional<PresignerSurface> surface = classifier.presigner(context.service());
        if (surface.isEmpty()) {
            return;
        }
        String root = context.settings().packageName();
        List<Presigned> operations = new ArrayList<>();
        for (String name : surface.get().operations()) {
            client.operations().stream().filter(o -> o.name().equals(name) && !o.unitInput()).findFirst()
                  .ifPresent(o -> operations.add(new Presigned(
                      o, type(root + ".presigner.model", name + "PresignRequest"),
                      type(root + ".presigner.model", "Presigned" + name + "Request"),
                      AwsSymbolProvider.lowerLeadingCapitals(name) + "Request")));
        }
        if (operations.isEmpty()) {
            return;
        }
        Symbol presigner = type(root + ".presigner", surface.get().typeName());
        Symbol implementation = type(root + ".presigner", "Default" + surface.get().typeName());
        WriterDelegator<AwsJavaWriter> writers = context.writerDelegator();
        renderInterface(writers, client, presigner, implementation, operations);
        renderDefault(writers, client, surface.get(), presigner, implementation, operations);
        for (Presigned operation : operations) {
            renderPresignRequest(writers, operation);
            renderPresignedRequest(writers, operation);
        }
    }

    private static void renderInterface(WriterDelegator<AwsJavaWriter> writers, ClientPlan client, Symbol type,
                                        Symbol implementation, List<Presigned> operations) {
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/**");
            writer.write(" * Presigns requests of {@code $L} for transmission by whoever holds them, through the "
                         + "Modern Auth", client.service());
            writer.write(" * scheme the service's client selects: the client's endpoint, auth-scheme and identity "
                         + "resolution, ended");
            writer.write(" * after signing. Nothing is transmitted. Thread safe; {@link #close()} releases what the "
                         + "SDK created.");
            writer.write(" */");
            writer.openBlock("public interface $L extends AutoCloseable {", "}", type.getName(), () -> {
                writer.write("/** A presigner configured from the default configuration sources. */");
                writer.openBlock("static $L create() {", "}", type.getName(),
                                 () -> writer.write("return builder().build();"));
                writer.write("");
                writer.write("/** A builder of a presigner. */");
                writer.openBlock("static Builder builder() {", "}",
                                 () -> writer.write("return new $T.DefaultBuilder();", implementation));
                for (Presigned operation : operations) {
                    String name = operation.operation().name();
                    writer.write("");
                    writer.write("/** Presigns a {@code $L} request. */", name);
                    writer.write("$T presign$L($T request);", operation.result(), name, operation.request());
                    writer.write("");
                    writer.write("/** Presigns a {@code $L} request built by {@code request}. */", name);
                    writer.openBlock("default $T presign$L($T<$T.Builder> request) {", "}", operation.result(), name,
                                     CONSUMER, operation.request(), () -> {
                            writer.write("$T.Builder builder = $T.builder();", operation.request(),
                                         operation.request());
                            writer.write("request.accept(builder);");
                            writer.write("return presign$L(builder.build());", name);
                        });
                }
                writer.write("");
                writer.write("/** Closes the resources the SDK created for this presigner; idempotent. */");
                writer.write("@Override");
                writer.write("void close();");
                writer.write("");
                writer.write("/** Builder of a presigner; not thread safe. It has no signer option. */");
                writer.openBlock("interface Builder {", "}", () -> {
                    writer.write("/** The region requests are presigned for. */");
                    writer.write("Builder region($T region);", REGION);
                    writer.write("");
                    writer.write("/** The provider of the AWS credentials requests are signed with. */");
                    writer.write("Builder credentialsProvider($T<? extends $T> credentialsProvider);",
                                 IDENTITY_PROVIDER, CREDENTIALS);
                    writer.write("");
                    writer.write("/** The endpoint requests are presigned for, instead of the resolved one. */");
                    writer.write("Builder endpointOverride($T endpointOverride);", URI);
                    writer.write("");
                    writer.write("/** Appends an interceptor of every presigned call, up to signing. */");
                    writer.write("Builder addExecutionInterceptor($T interceptor);", INTERCEPTOR);
                    writer.write("");
                    writer.write("/** Resolves the configuration once and builds the presigner. */");
                    writer.write("$L build();", type.getName());
                });
            });
        });
    }

    private static void renderDefault(WriterDelegator<AwsJavaWriter> writers, ClientPlan client,
                                      PresignerSurface surface, Symbol presigner, Symbol type,
                                      List<Presigned> operations) {
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/**");
            writer.write(" * Default {@link $T}: each presigned operation's static binding and plan, over the "
                         + "signing-only", presigner);
            writer.write(" * runtime of its builder's resolved configuration. Thread safe.");
            writer.write(" */");
            writer.openBlock("final class $L implements $T {", "}", type.getName(), presigner, () -> {
                for (Presigned presigned : operations) {
                    ClientPlan.Operation operation = presigned.operation();
                    writer.write("private static final $T<$T, $T> $L = $T.<$T, $T>builder(", AWS_OPERATION,
                                 operation.input(), operation.output(), operation.constant(), AWS_OPERATION,
                                 operation.input(), operation.output());
                    writer.indent(2);
                    writer.write("$T.$L, $T::builder, $T::builder)", client.operationTable(), operation.constant(),
                                 operation.input(), operation.output());
                    if (client.endpointRules() != null) {
                        writer.write(".endpointBinding($T.$L)", client.endpointRules(), operation.constant());
                    }
                    writer.write(".auth($L)", ClientGenerator.authExpression(writer, operation));
                    writer.write(".build();");
                    writer.dedent(2);
                    writer.write("");
                }
                writer.write("private final $T parts;", PARTS);
                for (Presigned presigned : operations) {
                    ClientPlan.Operation operation = presigned.operation();
                    writer.write("private final $T<$T, $T> $LPlan;", PLAN, operation.input(), operation.output(),
                                 operation.method());
                }
                writer.write("");
                writer.openBlock("$L($T parts) {", "}", type.getName(), PARTS, () -> {
                    writer.write("this.parts = parts;");
                    writer.write("try {");
                    writer.indent();
                    for (Presigned presigned : operations) {
                        writer.write("this.$LPlan = parts.runtime().plan($L);", presigned.operation().method(),
                                     presigned.operation().constant());
                    }
                    writer.dedent();
                    writer.write("} catch (RuntimeException e) {");
                    writer.indent();
                    writer.write("parts.close();");
                    writer.write("throw e;");
                    writer.dedent();
                    writer.write("}");
                });
                for (Presigned presigned : operations) {
                    writeImplementation(writer, surface, presigned);
                }
                writer.write("");
                writer.openBlock("private static $T url($T presigned) {", "}", URL, PRESIGNED_HTTP, () -> {
                    writer.write("try {");
                    writer.write("    return presigned.url().toURL();");
                    writer.write("} catch ($T e) {", MALFORMED_URL);
                    writer.write("    throw $T.create(\"The presigned URL is malformed\", e);", SDK_CLIENT_EXCEPTION);
                    writer.write("}");
                });
                writer.write("");
                writer.write("@Override");
                writer.openBlock("public void close() {", "}", () -> writer.write("parts.close();"));
                writer.write("");
                renderBuilder(writer, client, presigner, type);
            });
        });
    }

    private static void writeImplementation(AwsJavaWriter writer, PresignerSurface surface, Presigned presigned) {
        ClientPlan.Operation operation = presigned.operation();
        writer.write("");
        writer.write("@Override");
        writer.openBlock("public $T presign$L($T request) {", "}", presigned.result(), operation.name(),
                         presigned.request(), () -> {
                writer.write("$T.requireNonNull(request, \"request\");", OBJECTS);
                writer.write("$T expiration = request.signatureDuration();", DURATION);
                String adjustment = switch (surface.rules()) {
                    case S3 -> writer.format("$T.s3QueryString(expiration)", PRESIGNING);
                    case SIGV4 -> writer.format("$T.queryString(expiration, true)", PRESIGNING);
                };
                writer.write("$T presigned = parts.engine().presign($LPlan, request.$L(), null,", PRESIGNED_HTTP,
                             operation.method(), presigned.member());
                writer.write("        new $T(expiration, $L, $T::expiration));", PRESIGN_SPEC, adjustment,
                             PRESIGNING);
                writer.write("return new $T(url(presigned), presigned.expiration(), presigned.signedHeaders(),",
                             presigned.result());
                writer.write("        presigned.isBrowserExecutable(), presigned.httpRequest());");
            });
    }

    private static void renderBuilder(AwsJavaWriter writer, ClientPlan client, Symbol presigner, Symbol type) {
        writer.write("/** Default {@link $T.Builder}: the client's options and Modern Auth setup. */", presigner);
        writer.openBlock("static final class DefaultBuilder implements $T.Builder {", "}", presigner, () -> {
            writer.write("private final $T state = new $T();", BUILD_STATE, BUILD_STATE);
            writer.write("");
            ClientGenerator.writeModernAuth(writer, client, "DefaultBuilder");
            setter(writer, presigner, "region", writer.format("$T", REGION), "region");
            setter(writer, presigner, "credentialsProvider",
                   writer.format("$T<? extends $T>", IDENTITY_PROVIDER, CREDENTIALS), "credentialsProvider");
            setter(writer, presigner, "endpointOverride", writer.format("$T", URI), "endpointOverride");
            setter(writer, presigner, "addExecutionInterceptor", writer.format("$T", INTERCEPTOR), "addInterceptor");
            writer.write("@Override");
            writer.openBlock("public $T build() {", "}", presigner,
                             () -> writer.write("return new $L(state.buildPresigner(definition()));",
                                                type.getName()));
            writer.write("");
            ClientGenerator.writeDefinition(writer, client);
        });
    }

    private static void setter(AwsJavaWriter writer, Symbol presigner, String method, String parameterType,
                               String stateMethod) {
        writer.write("@Override");
        writer.openBlock("public $T.Builder $L($L value) {", "}", presigner, method, parameterType, () -> {
            writer.write("state.$L(value);", stateMethod);
            writer.write("return this;");
        });
        writer.write("");
    }

    private static void renderPresignRequest(WriterDelegator<AwsJavaWriter> writers, Presigned presigned) {
        Symbol type = presigned.request();
        Symbol input = presigned.operation().input();
        String member = presigned.member();
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/** A {@code $L} request to presign, and how long its signature is valid. */",
                         presigned.operation().name());
            writer.openBlock("public final class $L {", "}", type.getName(), () -> {
                writer.write("private final $T signatureDuration;", DURATION);
                writer.write("private final $T $L;", input, member);
                writer.write("");
                writer.openBlock("private $L(Builder builder) {", "}", type.getName(), () -> {
                    writer.write("this.signatureDuration = $T.requireNonNull(builder.signatureDuration, "
                                 + "\"signatureDuration\");", OBJECTS);
                    writer.write("this.$L = $T.requireNonNull(builder.$L, $S);", member, OBJECTS, member, member);
                });
                writer.write("");
                writer.write("/** A builder; the signature duration and the request are required. */");
                writer.openBlock("public static Builder builder() {", "}", () -> writer.write("return new Builder();"));
                writer.write("");
                writer.write("/** How long the presigned request is valid. */");
                writer.openBlock("public $T signatureDuration() {", "}", DURATION,
                                 () -> writer.write("return signatureDuration;"));
                writer.write("");
                writer.write("/** The request to presign. */");
                writer.openBlock("public $T $L() {", "}", input, member, () -> writer.write("return $L;", member));
                writer.write("");
                writer.write("/** Builder; not thread safe. */");
                writer.openBlock("public static final class Builder {", "}", () -> {
                    writer.write("private $T signatureDuration;", DURATION);
                    writer.write("private $T $L;", input, member);
                    writer.write("");
                    writer.openBlock("private Builder() {", "}", () -> {
                    });
                    writer.write("");
                    writer.openBlock("public Builder signatureDuration($T signatureDuration) {", "}", DURATION, () -> {
                        writer.write("this.signatureDuration = signatureDuration;");
                        writer.write("return this;");
                    });
                    writer.write("");
                    writer.openBlock("public Builder $L($T $L) {", "}", member, input, member, () -> {
                        writer.write("this.$L = $L;", member, member);
                        writer.write("return this;");
                    });
                    writer.write("");
                    writer.openBlock("public Builder $L($T<$T.Builder> $L) {", "}", member, CONSUMER, input, member,
                                     () -> {
                            writer.write("$T.Builder builder = $T.builder();", input, input);
                            writer.write("$L.accept(builder);", member);
                            writer.write("return $L(builder.build());", member);
                        });
                    writer.write("");
                    writer.openBlock("public $L build() {", "}", type.getName(),
                                     () -> writer.write("return new $L(this);", type.getName()));
                });
            });
        });
    }

    private static void renderPresignedRequest(WriterDelegator<AwsJavaWriter> writers, Presigned presigned) {
        Symbol type = presigned.result();
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/**");
            writer.write(" * A presigned {@code $L} request: anyone holding it can send it until its expiration, "
                         + "so its", presigned.operation().name());
            writer.write(" * URL is never rendered by {@link #toString()}.");
            writer.write(" */");
            writer.openBlock("public final class $L {", "}", type.getName(), () -> {
                writer.write("private final $T url;", URL);
                writer.write("private final $T expiration;", INSTANT);
                writer.write("private final $T<String, $T<String>> signedHeaders;", MAP, LIST);
                writer.write("private final boolean browserExecutable;");
                writer.write("private final $T httpRequest;", HTTP_REQUEST);
                writer.write("");
                writer.write("/** Created by the presigner. */");
                writer.openBlock(writer.format("public $L($T url, $T expiration, $T<String, $T<String>> "
                                               + "signedHeaders,\n        boolean browserExecutable, $T httpRequest) {",
                                               type.getName(), URL, INSTANT, MAP, LIST, HTTP_REQUEST), "}", () -> {
                        writer.write("this.url = $T.requireNonNull(url, \"url\");", OBJECTS);
                        writer.write("this.expiration = $T.requireNonNull(expiration, \"expiration\");", OBJECTS);
                        writer.write("this.signedHeaders = $T.copyOf(signedHeaders);", MAP);
                        writer.write("this.browserExecutable = browserExecutable;");
                        writer.write("this.httpRequest = $T.requireNonNull(httpRequest, \"httpRequest\");", OBJECTS);
                    });
                writer.write("");
                writer.write("/** The presigned URL, signature included. */");
                writer.openBlock("public $T url() {", "}", URL, () -> writer.write("return url;"));
                writer.write("");
                writer.write("/** When the signature stops being valid. */");
                writer.openBlock("public $T expiration() {", "}", INSTANT, () -> writer.write("return expiration;"));
                writer.write("");
                writer.write("/** The headers the sender must send exactly. */");
                writer.openBlock("public $T<String, $T<String>> signedHeaders() {", "}", MAP, LIST,
                                 () -> writer.write("return signedHeaders;"));
                writer.write("");
                writer.write("/** Whether a browser can send the request by following {@link #url()} alone. */");
                writer.openBlock("public boolean isBrowserExecutable() {", "}",
                                 () -> writer.write("return browserExecutable;"));
                writer.write("");
                writer.write("/** The signed HTTP request. */");
                writer.openBlock("public $T httpRequest() {", "}", HTTP_REQUEST,
                                 () -> writer.write("return httpRequest;"));
                writer.write("");
                writer.write("@Override");
                writer.openBlock("public String toString() {", "}",
                                 () -> writer.write("return \"$L(\" + httpRequest.method() + \" \" "
                                                    + "+ httpRequest.host() + \", expiration=\" + expiration + \")\";",
                                                    type.getName()));
            });
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
