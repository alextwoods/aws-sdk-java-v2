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

package software.amazon.awssdk.archtests.serde;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import software.amazon.awssdk.archtests.ClassFiles;

/**
 * Finds references to legacy per-operation serde types in compiled classes (task 5.3; Requirements 37.2, 36.4).
 *
 * <p>Every class is read with ASM through a {@link ClassRemapper}, which reports each class name the class file
 * mentions: the class itself, its supertypes, field, method and local-variable descriptors and generic signatures,
 * {@code throws} clauses, instruction operands (owners of field and method references, {@code new},
 * {@code checkcast}, {@code instanceof}, {@code anewarray}), {@code ldc} class and method-type constants, method
 * handles and bootstrap arguments of {@code invokedynamic}, annotations and type annotations, and {@code InnerClasses}
 * entries. Each referenced type is then checked against the rules of {@link Rule}. String constants, such as a class
 * name passed to {@code Class.forName}, are not type references and are not checked.
 *
 * <p>No allowlist is needed for the classes scanned today: the types the new path shares with the legacy path
 * ({@code ClientExecutionParams}, {@code ExecutionContext}, {@code ExceptionReportingUtils}, the reused legacy
 * request stages, {@code SdkAsyncHttpResponseHandler}) do not match any rule.
 */
final class MarshallerDependencyScanner {

    /**
     * Package prefixes (internal form, ending in {@code /} so that {@code protocols/json/} does not match
     * {@code protocols/jsoncore/}) of the legacy protocol modules, enumerated from {@code core/protocols}:
     * {@code protocol-core}, {@code aws-json-protocol}, {@code aws-query-protocol}, {@code aws-xml-protocol},
     * {@code aws-cbor-protocol} and {@code smithy-rpcv2-protocol}. The new Client_Protocol modules live under
     * {@code software/amazon/awssdk/protocols/client/} and do not match.
     */
    static final List<String> LEGACY_PROTOCOL_PACKAGES = Collections.unmodifiableList(Arrays.asList(
        "software/amazon/awssdk/protocols/core/",
        "software/amazon/awssdk/protocols/json/",
        "software/amazon/awssdk/protocols/query/",
        "software/amazon/awssdk/protocols/xml/",
        "software/amazon/awssdk/protocols/cbor/",
        "software/amazon/awssdk/protocols/rpcv2/"));

    /**
     * The legacy response handler interfaces of {@code sdk-core}. The sync per-operation handlers (generated through
     * {@code JsonOperationMetadata}, the protocols' {@code JsonResponseHandler}, {@code AwsXmlResponseHandler},
     * {@code AwsQueryResponseHandler} and error unmarshallers) implement {@code HttpResponseHandler}, and the
     * decorators that {@code BaseClientHandler} and the legacy pipeline wrap them in ({@code CombinedResponseHandler},
     * {@code MetricCollectingHttpResponseHandler}, {@code AttachHttpMetadataResponseHandler}) implement it as well. The
     * async ones ({@code AsyncResponseHandler}, {@code CombinedResponseAsyncHttpResponseHandler},
     * {@code AsyncStreamingResponseHandler}) implement {@code TransformingAsyncResponseHandler}.
     */
    static final List<String> LEGACY_RESPONSE_HANDLER_TYPES = Collections.unmodifiableList(Arrays.asList(
        "software/amazon/awssdk/core/http/HttpResponseHandler",
        "software/amazon/awssdk/core/internal/http/TransformingAsyncResponseHandler"));

    /**
     * Package name segments of the legacy serde packages: generated {@code <service>.transform} marshallers,
     * {@code sdk-core}'s {@code core.runtime.transform} and {@code core.internal.transform} marshaller interfaces, and
     * the legacy protocols' {@code internal.marshall}/{@code unmarshall} packages.
     */
    static final Set<String> SERDE_PACKAGE_SEGMENTS = Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(
        "transform", "marshall", "unmarshall")));

    private static final String SDK_PACKAGE_PREFIX = "software/amazon/awssdk/";

    /**
     * The rules a referenced type is checked against. A type can match more than one rule; each match is reported.
     */
    enum Rule {
        /**
         * A type whose simple name (after the last {@code /} and {@code $}) ends with {@code Marshaller} or
         * {@code Unmarshaller}: generated {@code <Shape>Marshaller}s, {@code sdk-core}'s {@code Marshaller} and
         * {@code StreamingRequestMarshaller}s, the legacy protocols' marshallers and error unmarshallers, and
         * {@code aws-core}'s {@code EventStreamTaggedUnionJsonMarshaller}.
         */
        MARSHALLER_NAME("type named *Marshaller or *Unmarshaller"),

        /**
         * A type in a package with one of the {@link #SERDE_PACKAGE_SEGMENTS}, which catches the rest of the generated
         * {@code transform} package and the legacy serde helpers that are not named {@code *Marshaller}.
         */
        SERDE_PACKAGE("type in a transform, marshall or unmarshall package"),

        /**
         * A type in one of the {@link #LEGACY_PROTOCOL_PACKAGES}: the legacy protocol factories
         * ({@code AwsJsonProtocolFactory}, {@code BaseAwsJsonProtocolFactory}, {@code AwsCborProtocolFactory},
         * {@code SmithyRpcV2CborProtocolFactory}, {@code AwsQueryProtocolFactory}, {@code AwsEc2ProtocolFactory},
         * {@code AwsXmlProtocolFactory}, {@code AwsS3ProtocolFactory}) and the per-operation metadata and handlers they
         * create ({@code JsonOperationMetadata}, {@code ExceptionMetadata}, {@code OperationInfo},
         * {@code ProtocolMarshaller}, {@code JsonResponseHandler}, {@code AwsJsonProtocolErrorUnmarshaller}, ...).
         */
        LEGACY_PROTOCOL("type from a legacy protocol module"),

        /**
         * A type that is, or is a subtype of, one of the {@link #LEGACY_RESPONSE_HANDLER_TYPES}. Subtypes are resolved
         * from the scanned classes first and then from the test class loader, by reading the class files with ASM (no
         * class is loaded or initialized).
         */
        LEGACY_RESPONSE_HANDLER("legacy per-operation response handler (HttpResponseHandler or "
                                + "TransformingAsyncResponseHandler)"),

        /**
         * A type for which {@link #LEGACY_RESPONSE_HANDLER} cannot be decided, because it or one of its supertypes is
         * an SDK type ({@code software/amazon/awssdk/}) that is neither among the scanned classes nor on the test
         * class path. Reported so that a missing test dependency cannot hide a handler.
         */
        UNRESOLVED("SDK type, or a supertype of it, is not on the test classpath; cannot check it for legacy response "
                   + "handlers");

        private final String description;

        Rule(String description) {
            this.description = description;
        }

        String description() {
            return description;
        }
    }

    /**
     * Outcome of resolving a type's hierarchy for {@link Rule#LEGACY_RESPONSE_HANDLER}.
     */
    private enum Resolution {
        HANDLER,
        NOT_HANDLER,
        /** The type, or one of its supertypes, is an SDK type whose class file cannot be found. */
        UNRESOLVED
    }

    private final ClassLoader classLoader;
    private final Map<String, Resolution> responseHandlerCache = new HashMap<>();
    private Map<String, byte[]> scannedClasses = Collections.emptyMap();

    MarshallerDependencyScanner(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    static MarshallerDependencyScanner create() {
        return new MarshallerDependencyScanner(MarshallerDependencyScanner.class.getClassLoader());
    }

    /**
     * Check every class in {@code classes} (internal name to class file bytes) and return one message per violation,
     * sorted. An empty list means none of the classes references a legacy serde type.
     */
    List<String> scan(Map<String, byte[]> classes) {
        this.scannedClasses = classes;
        responseHandlerCache.clear();
        List<String> violations = new ArrayList<>();
        for (byte[] bytes : classes.values()) {
            ClassReader reader = new ClassReader(bytes);
            ReferenceCollector references = new ReferenceCollector();
            reader.accept(new ClassRemapper(new ClassNode(), references), 0);
            String scanned = javaName(reader.getClassName());
            for (String referenced : references.referenced) {
                for (Rule rule : rulesMatching(referenced)) {
                    violations.add(scanned + ": references " + javaName(referenced) + " (" + rule.description() + ")");
                }
            }
        }
        Collections.sort(violations);
        return violations;
    }

    /**
     * The rules {@code internalName} (a referenced type) matches.
     */
    List<Rule> rulesMatching(String internalName) {
        List<Rule> matched = new ArrayList<>();
        String simpleName = simpleName(internalName);
        if (simpleName.endsWith("Marshaller") || simpleName.endsWith("Unmarshaller")) {
            matched.add(Rule.MARSHALLER_NAME);
        }
        if (inSerdePackage(internalName)) {
            matched.add(Rule.SERDE_PACKAGE);
        }
        if (LEGACY_PROTOCOL_PACKAGES.stream().anyMatch(internalName::startsWith)) {
            matched.add(Rule.LEGACY_PROTOCOL);
        }
        Resolution handler = resolveResponseHandler(internalName);
        if (handler == Resolution.HANDLER) {
            matched.add(Rule.LEGACY_RESPONSE_HANDLER);
        } else if (handler == Resolution.UNRESOLVED) {
            matched.add(Rule.UNRESOLVED);
        }
        return matched;
    }

    /**
     * Whether {@code internalName} is, or is a subtype of, a legacy response handler interface.
     */
    private Resolution resolveResponseHandler(String internalName) {
        if (LEGACY_RESPONSE_HANDLER_TYPES.contains(internalName)) {
            return Resolution.HANDLER;
        }
        // Only SDK types and scanned classes can implement the SDK's handler interfaces; JDK and third-party types
        // cannot.
        if (!internalName.startsWith(SDK_PACKAGE_PREFIX) && !scannedClasses.containsKey(internalName)) {
            return Resolution.NOT_HANDLER;
        }
        Resolution cached = responseHandlerCache.get(internalName);
        if (cached != null) {
            return cached;
        }
        byte[] bytes = classFile(internalName);
        if (bytes == null) {
            responseHandlerCache.put(internalName, Resolution.UNRESOLVED);
            return Resolution.UNRESOLVED;
        }
        // Mark the type before descending, so that a malformed cyclic hierarchy terminates.
        responseHandlerCache.put(internalName, Resolution.NOT_HANDLER);
        ClassReader reader = new ClassReader(bytes);
        List<String> supertypes = new ArrayList<>(Arrays.asList(reader.getInterfaces()));
        if (reader.getSuperName() != null) {
            supertypes.add(reader.getSuperName());
        }
        Resolution result = Resolution.NOT_HANDLER;
        for (String supertype : supertypes) {
            Resolution superResolution = resolveResponseHandler(supertype);
            if (superResolution == Resolution.HANDLER) {
                result = Resolution.HANDLER;
                break;
            }
            if (superResolution == Resolution.UNRESOLVED) {
                // An unreadable supertype could be a handler; keep looking for a definite one.
                result = Resolution.UNRESOLVED;
            }
        }
        responseHandlerCache.put(internalName, result);
        return result;
    }

    private byte[] classFile(String internalName) {
        byte[] scanned = scannedClasses.get(internalName);
        if (scanned != null) {
            return scanned;
        }
        try (InputStream in = classLoader.getResourceAsStream(internalName + ".class")) {
            return in == null ? null : ClassFiles.readAll(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read the class file of " + javaName(internalName), e);
        }
    }

    private static boolean inSerdePackage(String internalName) {
        String[] segments = internalName.split("/");
        for (int i = 0; i < segments.length - 1; i++) {
            if (SERDE_PACKAGE_SEGMENTS.contains(segments[i])) {
                return true;
            }
        }
        return false;
    }

    private static String simpleName(String internalName) {
        String name = internalName.substring(internalName.lastIndexOf('/') + 1);
        return name.substring(name.lastIndexOf('$') + 1);
    }

    private static String javaName(String internalName) {
        return internalName.replace('/', '.');
    }

    /**
     * Records every internal class name that appears anywhere in a class.
     */
    private static final class ReferenceCollector extends Remapper {
        private final Set<String> referenced = new TreeSet<>();

        @Override
        public String map(String internalName) {
            referenced.add(internalName);
            return internalName;
        }
    }
}
