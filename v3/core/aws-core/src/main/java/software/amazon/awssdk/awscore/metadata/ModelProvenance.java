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

package software.amazon.awssdk.awscore.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * The model provenance published in every generated service artifact: the exact service shape ID and the untruncated
 * immutable revision of the canonical Smithy model the service was generated from (Requirements 2.12, 22.2).
 *
 * <p>{@code aws-codegen} writes it to {@value #RESOURCE_DIRECTORY}{@code <package>/}{@value #FILE_NAME} of the service
 * artifact, where {@code <package>} is the service's root Java package, so that several services on one class path
 * never share a resource name. Generated services read it through {@link #load(Class, String, String)} with a class
 * of their root package; the resource is parsed with Smithy Java's JSON codec, never with a Smithy model parser.
 *
 * <p>Values are exposed exactly as recorded: the revision is never abbreviated and the service ID is never
 * normalized. Parsing is strict: it rejects unknown or missing members, wrong types, other format versions,
 * abbreviated or upper-case revisions, and service IDs that are not absolute shape IDs.
 */
public final class ModelProvenance {

    /** The only provenance format this reader accepts. */
    public static final int FORMAT_VERSION = 1;

    /** Resource directory of generated service metadata; the service's root Java package follows. */
    public static final String RESOURCE_DIRECTORY = "META-INF/aws-sdk-java-v3/services/";

    /** File name of the provenance resource. */
    public static final String FILE_NAME = "model-provenance.json";

    private static final Pattern GIT_COMMIT = Pattern.compile("[0-9a-f]{40}|[0-9a-f]{64}");
    private static final Pattern DIGEST = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Pattern PACKAGE = Pattern.compile("[a-z_][a-z0-9_]*(\\.[a-z_][a-z0-9_]*)*");
    private static final Pattern SHAPE_ID = Pattern.compile(
        "[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*#[A-Za-z_][A-Za-z0-9_]*");
    private static final JsonCodec JSON = JsonCodec.builder().build();

    private final String serviceShapeId;
    private final String modelRevision;
    private final RevisionKind revisionKind;
    private final String modelDigest;
    private final List<ModelFile> modelFiles;

    private ModelProvenance(String serviceShapeId, String modelRevision, RevisionKind revisionKind,
                            String modelDigest, List<ModelFile> modelFiles) {
        this.serviceShapeId = serviceShapeId;
        this.modelRevision = modelRevision;
        this.revisionKind = revisionKind;
        this.modelDigest = modelDigest;
        this.modelFiles = List.copyOf(modelFiles);
    }

    /** Kind of the recorded model revision. */
    public enum RevisionKind {
        /** The full Git commit object ID of the canonical model source: 40 or 64 lower-case hexadecimal digits. */
        GIT_COMMIT("git-commit"),
        /**
         * Unpinned (development or test) generation: the revision is the model digest. Release artifacts always
         * record a {@link #GIT_COMMIT} revision.
         */
        MODEL_DIGEST("model-digest");

        private final String value;

        RevisionKind(String value) {
            this.value = value;
        }

        /** The value recorded in the resource. */
        public String value() {
            return value;
        }

        static RevisionKind fromValue(String value) {
            for (RevisionKind kind : values()) {
                if (kind.value.equals(value)) {
                    return kind;
                }
            }
            throw invalid("model.revisionKind", "is '" + value + "', expected git-commit or model-digest");
        }
    }

    /**
     * One canonical model file of the generation.
     *
     * @param path path relative to the model directory, with {@code /} separators
     * @param digest {@code sha256:<hex>} of the file content
     */
    public record ModelFile(String path, String digest) {
        public ModelFile {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(digest, "digest");
        }
    }

    /** The class-path resource name of the provenance of the service generated into {@code packageName}. */
    public static String resourceName(String packageName) {
        if (packageName == null || !PACKAGE.matcher(packageName).matches()) {
            throw new IllegalArgumentException("'" + packageName + "' is not a lower-case Java package name");
        }
        return RESOURCE_DIRECTORY + packageName + "/" + FILE_NAME;
    }

    /**
     * Reads the provenance published with the service whose root package contains {@code serviceType}, from the
     * class-path location of {@code serviceType}, and checks that it belongs to the expected generation.
     *
     * @param serviceType a class in the service's root package (generated code passes its service identity type)
     * @param expectedServiceShapeId the exact service shape ID the calling code was generated for
     * @param expectedRevision the exact model revision the calling code was generated from
     * @throws IllegalStateException if the resource is missing, unreadable, invalid, or records another service or
     *         revision (for example because a different service's metadata was merged into the same artifact)
     */
    public static ModelProvenance load(Class<?> serviceType, String expectedServiceShapeId, String expectedRevision) {
        String resource = resourceName(serviceType.getPackageName());
        byte[] content;
        try (InputStream in = serviceType.getResourceAsStream("/" + resource)) {
            if (in == null) {
                throw new IllegalStateException("Model provenance " + resource + " of service "
                                                + expectedServiceShapeId + " is missing from the class path.");
            }
            content = in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read model provenance " + resource, e);
        }
        ModelProvenance provenance;
        try {
            provenance = parse(content);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Model provenance " + resource + " is invalid: " + e.getMessage(), e);
        }
        if (!provenance.serviceShapeId.equals(expectedServiceShapeId)
            || !provenance.modelRevision.equals(expectedRevision)) {
            throw new IllegalStateException("Model provenance " + resource + " records service "
                                            + provenance.serviceShapeId + " at revision " + provenance.modelRevision
                                            + ", but the service was generated as " + expectedServiceShapeId
                                            + " at revision " + expectedRevision + ".");
        }
        return provenance;
    }

    /**
     * Parses a provenance resource.
     *
     * @throws IllegalArgumentException if {@code json} is not a valid format-{@value #FORMAT_VERSION} provenance
     */
    public static ModelProvenance parse(byte[] json) {
        Document root;
        try {
            root = JSON.createDeserializer(json).readDocument();
        } catch (RuntimeException e) {
            // The codec reports malformed input with unchecked serialization exceptions.
            throw new IllegalArgumentException("Model provenance is not valid JSON: " + e.getMessage(), e);
        }
        Map<String, Document> members = object(root, "$", Set.of("formatVersion", "model", "service"));
        Document version = members.get("formatVersion");
        if (!isInteger(version) || version.asLong() != FORMAT_VERSION) {
            throw invalid("formatVersion", "must be " + FORMAT_VERSION);
        }
        String service = string(members.get("service"), "service");
        if (!SHAPE_ID.matcher(service).matches() || !ShapeId.from(service).toString().equals(service)) {
            throw invalid("service", "is '" + service + "', not an absolute Smithy shape ID");
        }
        Map<String, Document> model = object(members.get("model"), "model",
                                             Set.of("digest", "files", "revision", "revisionKind"));
        String digest = digest(model.get("digest"), "model.digest");
        RevisionKind kind = RevisionKind.fromValue(string(model.get("revisionKind"), "model.revisionKind"));
        String revision = string(model.get("revision"), "model.revision");
        if (kind == RevisionKind.GIT_COMMIT && !GIT_COMMIT.matcher(revision).matches()) {
            throw invalid("model.revision", "is '" + revision + "', not a full lower-case Git commit object ID (40 or "
                                            + "64 hexadecimal digits)");
        }
        if (kind == RevisionKind.MODEL_DIGEST && !revision.equals(digest)) {
            throw invalid("model.revision", "of kind model-digest must equal model.digest");
        }
        Document files = model.get("files");
        if (files == null || files.type() != ShapeType.LIST || files.size() == 0) {
            throw invalid("model.files", "must be a non-empty array");
        }
        List<ModelFile> modelFiles = new ArrayList<>();
        String previous = null;
        for (Document file : files.asList()) {
            String where = "model.files[" + modelFiles.size() + "]";
            Map<String, Document> entry = object(file, where, Set.of("digest", "path"));
            String path = string(entry.get("path"), where + ".path");
            if (path.isEmpty() || path.startsWith("/") || path.contains("\\")) {
                throw invalid(where + ".path", "is '" + path + "', not a relative path");
            }
            if (previous != null && previous.compareTo(path) >= 0) {
                throw invalid(where + ".path", "is not sorted after '" + previous + "'");
            }
            previous = path;
            modelFiles.add(new ModelFile(path, digest(entry.get("digest"), where + ".digest")));
        }
        return new ModelProvenance(service, revision, kind, digest, modelFiles);
    }

    /** The exact absolute shape ID of the service, as modeled. */
    public String serviceShapeId() {
        return serviceShapeId;
    }

    /** {@link #serviceShapeId()} as a {@link ShapeId}. */
    public ShapeId serviceId() {
        return ShapeId.from(serviceShapeId);
    }

    /** The exact, untruncated immutable revision of the canonical model source. */
    public String modelRevision() {
        return modelRevision;
    }

    /** The kind of {@link #modelRevision()}. */
    public RevisionKind revisionKind() {
        return revisionKind;
    }

    /** The location-independent digest of the canonical model files, {@code sha256:<hex>}. */
    public String modelDigest() {
        return modelDigest;
    }

    /** Every canonical model file of the generation, sorted by path. */
    public List<ModelFile> modelFiles() {
        return modelFiles;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ModelProvenance other
                            && serviceShapeId.equals(other.serviceShapeId)
                            && modelRevision.equals(other.modelRevision)
                            && revisionKind == other.revisionKind
                            && modelDigest.equals(other.modelDigest)
                            && modelFiles.equals(other.modelFiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceShapeId, modelRevision, revisionKind, modelDigest, modelFiles);
    }

    @Override
    public String toString() {
        return "ModelProvenance[service=" + serviceShapeId + ", revision=" + modelRevision + " (" + revisionKind.value
               + "), digest=" + modelDigest + ", files=" + modelFiles.size() + "]";
    }

    private static Map<String, Document> object(Document document, String where, Set<String> members) {
        if (document == null || document.type() != ShapeType.MAP) {
            throw invalid(where, "must be an object");
        }
        Map<String, Document> map = document.asStringMap();
        if (!map.keySet().equals(members)) {
            throw invalid(where, "has members " + new TreeSet<>(map.keySet()) + ", expected exactly "
                                 + new TreeSet<>(members));
        }
        return map;
    }

    private static String string(Document document, String where) {
        if (document == null || document.type() != ShapeType.STRING) {
            throw invalid(where, "must be a string");
        }
        return document.asString();
    }

    private static String digest(Document document, String where) {
        String digest = string(document, where);
        if (!DIGEST.matcher(digest).matches()) {
            throw invalid(where, "is '" + digest + "', not sha256:<64 lower-case hex digits>");
        }
        return digest;
    }

    private static boolean isInteger(Document document) {
        return document != null && switch (document.type()) {
            case BYTE, SHORT, INTEGER, LONG, BIG_INTEGER -> true;
            default -> false;
        };
    }

    private static IllegalArgumentException invalid(String where, String problem) {
        return new IllegalArgumentException("Model provenance member " + where + " " + problem + ".");
    }
}
