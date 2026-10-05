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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.metadata.ModelProvenance.ModelFile;
import software.amazon.awssdk.awscore.metadata.ModelProvenance.RevisionKind;
import software.amazon.smithy.model.shapes.ShapeId;

class ModelProvenanceTest {

    private static final String SERVICE = "example.weather.v2026#WeatherService_2026";
    private static final String SHA1_REVISION = "89abcdef0123456789abcdef0123456789abcdef";
    private static final String SHA256_REVISION = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String DIGEST = "sha256:" + "a".repeat(64);
    private static final String FILE_DIGEST = "sha256:" + "b".repeat(64);
    private static final String FILES = "[" + file("a.smithy") + ", " + file("nested/b.json") + "]";

    @Test
    void parsesExactServiceIdRevisionAndModelFiles() {
        ModelProvenance provenance = parse(json(SERVICE, SHA1_REVISION, "git-commit", DIGEST, FILES));

        assertEquals(SERVICE, provenance.serviceShapeId());
        assertEquals(ShapeId.from(SERVICE), provenance.serviceId());
        assertEquals(SHA1_REVISION, provenance.modelRevision());
        assertEquals(RevisionKind.GIT_COMMIT, provenance.revisionKind());
        assertEquals(DIGEST, provenance.modelDigest());
        assertEquals(List.of(new ModelFile("a.smithy", FILE_DIGEST), new ModelFile("nested/b.json", FILE_DIGEST)),
                     provenance.modelFiles());
        assertEquals(provenance, parse(json(SERVICE, SHA1_REVISION, "git-commit", DIGEST, FILES)));
    }

    @Test
    void acceptsSha256CommitsAndUnpinnedModelDigestRevisions() {
        assertEquals(SHA256_REVISION, parse(json(SERVICE, SHA256_REVISION, "git-commit", DIGEST, FILES))
                                                     .modelRevision());
        ModelProvenance unpinned = parse(json(SERVICE, DIGEST, "model-digest", DIGEST, FILES));
        assertEquals(RevisionKind.MODEL_DIGEST, unpinned.revisionKind());
        assertEquals(DIGEST, unpinned.modelRevision());
    }

    @Test
    void rejectsAbbreviatedNormalizedOrMismatchedRevisions() {
        assertInvalid(json(SERVICE, SHA1_REVISION.substring(0, 7), "git-commit", DIGEST, FILES), "model.revision");
        assertInvalid(json(SERVICE, SHA1_REVISION.substring(0, 39), "git-commit", DIGEST, FILES), "model.revision");
        assertInvalid(json(SERVICE, SHA1_REVISION + "0", "git-commit", DIGEST, FILES), "model.revision");
        assertInvalid(json(SERVICE, SHA1_REVISION.toUpperCase(), "git-commit", DIGEST, FILES), "model.revision");
        assertInvalid(json(SERVICE, "sha256:" + "c".repeat(64), "model-digest", DIGEST, FILES), "model.revision");
        assertInvalid(json(SERVICE, SHA1_REVISION, "branch", DIGEST, FILES), "model.revisionKind");
    }

    @Test
    void rejectsServiceIdsThatAreNotAbsoluteShapeIds() {
        assertInvalid(json("WeatherService", SHA1_REVISION, "git-commit", DIGEST, FILES), "service");
        assertInvalid(json(SERVICE + "$member", SHA1_REVISION, "git-commit", DIGEST, FILES), "service");
        assertInvalid(json(" " + SERVICE, SHA1_REVISION, "git-commit", DIGEST, FILES), "service");
    }

    @Test
    void rejectsStructuralViolations() {
        String valid = json(SERVICE, SHA1_REVISION, "git-commit", DIGEST, FILES);
        assertInvalid(valid.replace("\"formatVersion\": 1", "\"formatVersion\": 2"), "formatVersion");
        assertInvalid(valid.replace("\"formatVersion\": 1", "\"formatVersion\": \"1\""), "formatVersion");
        assertInvalid(valid.replace("\"formatVersion\": 1,", "\"formatVersion\": 1, \"extra\": true,"), "$");
        assertInvalid(valid.replace("\"formatVersion\": 1,", ""), "$");
        assertInvalid(valid.replace(DIGEST + "\", \"files\"", "sha256:ABC\", \"files\""), "model.digest");
        assertInvalid(json(SERVICE, SHA1_REVISION, "git-commit", DIGEST, "[]"), "model.files");
        assertInvalid(json(SERVICE, SHA1_REVISION, "git-commit", DIGEST, "[" + file("b.smithy") + ", "
                                                                           + file("a.smithy") + "]"),
                      "model.files[1].path");
        assertInvalid(json(SERVICE, SHA1_REVISION, "git-commit", DIGEST, "[" + file("/abs.smithy") + "]"),
                      "model.files[0].path");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                                  () -> ModelProvenance.parse(bytes("{\"formatVersion\": ")));
        assertTrue(e.getMessage().contains("not valid JSON"), e.getMessage());
    }

    @Test
    void loadsTheResourceOfTheServicePackageAndChecksItsIdentity() {
        String revision = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        ModelProvenance provenance = ModelProvenance.load(ModelProvenanceTest.class, SERVICE, revision);

        assertEquals(SERVICE, provenance.serviceShapeId());
        assertEquals(revision, provenance.modelRevision());
        assertEquals(List.of("nested/operations.smithy", "weather.smithy"),
                     provenance.modelFiles().stream().map(ModelFile::path).toList());
        assertEquals("META-INF/aws-sdk-java-v3/services/software.amazon.awssdk.awscore.metadata/model-provenance.json",
                     ModelProvenance.resourceName(ModelProvenanceTest.class.getPackageName()));

        IllegalStateException otherService = assertThrows(IllegalStateException.class, () -> ModelProvenance.load(
            ModelProvenanceTest.class, "example.weather.v2026#Other", revision));
        assertTrue(otherService.getMessage().contains("records service " + SERVICE), otherService.getMessage());
        assertThrows(IllegalStateException.class, () -> ModelProvenance.load(
            ModelProvenanceTest.class, SERVICE, SHA1_REVISION));
        IllegalStateException missing = assertThrows(IllegalStateException.class, () -> ModelProvenance.load(
            ShapeId.class, SERVICE, revision));
        assertTrue(missing.getMessage().contains("is missing"), missing.getMessage());
    }

    private static String file(String path) {
        return "{\"digest\": \"" + FILE_DIGEST + "\", \"path\": \"" + path + "\"}";
    }

    private static String json(String service, String revision, String kind, String digest, String files) {
        return "{\"formatVersion\": 1, \"model\": {\"digest\": \"" + digest + "\", \"files\": " + files
               + ", \"revision\": \"" + revision + "\", \"revisionKind\": \"" + kind + "\"}, \"service\": \""
               + service + "\"}";
    }

    private static void assertInvalid(String json, String member) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                                  () -> ModelProvenance.parse(bytes(json)));
        assertTrue(e.getMessage().startsWith("Model provenance member " + member + " "), e.getMessage());
    }

    private static byte[] bytes(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private static ModelProvenance parse(String json) {
        return ModelProvenance.parse(bytes(json));
    }
}
