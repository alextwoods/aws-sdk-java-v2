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

package software.amazon.awssdk.tools.paritymanifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Each V2 baseline scanner against small fixture inputs written by the test. */
class InventorySourcesTest {

    private static final BaselineIdentity BASELINE = new BaselineIdentity("2.1.0", null, null, null, "test");

    @TempDir
    Path dir;

    @Test
    void artifactsAndServicesFollowTheAggregatorTreeAndPublishedCriterion() throws IOException {
        pom(dir, "<groupId>software.amazon.awssdk</groupId><artifactId>aws-sdk-java-pom</artifactId>"
                 + "<packaging>pom</packaging><modules><module>core</module><module>services</module>"
                 + "<module>build-tools</module><module>release-scripts</module><module>test/test-utils</module>"
                 + "</modules><profiles><profile><modules><module>v3</module></modules></profile></profiles>");
        pom(dir.resolve("core"), "<parent><groupId>software.amazon.awssdk</groupId></parent>"
                                 + "<artifactId>sdk-core</artifactId>");
        pom(dir.resolve("services"), "<parent><groupId>software.amazon.awssdk</groupId></parent>"
                                     + "<artifactId>services</artifactId><packaging>pom</packaging>"
                                     + "<modules><module>s3</module></modules>");
        pom(dir.resolve("services/s3"), "<artifactId>s3</artifactId>");
        pom(dir.resolve("build-tools"), "<artifactId>build-tools</artifactId><build><plugins><plugin>"
                                        + "<artifactId>maven-deploy-plugin</artifactId>"
                                        + "<configuration><skip>true</skip></configuration></plugin></plugins></build>");
        pom(dir.resolve("release-scripts"), "<artifactId>release-scripts</artifactId>");
        pom(dir.resolve("test/test-utils"), "<artifactId>test-utils</artifactId>");
        pom(dir.resolve("v3"), "<artifactId>v3</artifactId>");

        assertEquals(List.of("artifact:software.amazon.awssdk:aws-sdk-java-pom", "artifact:software.amazon.awssdk:s3",
                             "artifact:software.amazon.awssdk:sdk-core", "artifact:software.amazon.awssdk:services"),
                     ids(new ArtifactInventorySource().scan(context())));
        assertEquals(List.of("service:s3"), ids(new ServiceInventorySource().scan(context())));
    }

    @Test
    void httpModesComeFromRegisteredHttpServices() throws IOException {
        service("apache-client", HttpModeInventorySource.SYNC_SERVICE, "a.ApacheSdkHttpService");
        service("aws-crt-client", HttpModeInventorySource.SYNC_SERVICE, "c.CrtSync");
        service("aws-crt-client", HttpModeInventorySource.ASYNC_SERVICE, "# comment\nc.CrtAsync");
        service("netty-nio-client", HttpModeInventorySource.ASYNC_SERVICE, "n.Netty");
        service("empty-client", HttpModeInventorySource.SYNC_SERVICE, "# only a comment");

        List<InventoryItem> items = new HttpModeInventorySource().scan(context());
        assertEquals(List.of("http-mode:apache-client:sync", "http-mode:aws-crt-client:async",
                             "http-mode:aws-crt-client:sync", "http-mode:netty-nio-client:async"), ids(items));
        assertTrue(items.get(1).v2Surface().contains("c.CrtAsync"));
    }

    @Test
    void configurationKeysAreReadFromSettingEnumsAndProfileConstants() throws IOException {
        write(dir.resolve("S.java"), """
            /** Mentions FAKE("not.a.key") in a comment. */
            public enum S implements SystemSetting {
                /** The region. */
                AWS_REGION("aws.region", null),
                // AWS_IGNORED("aws.ignored"),
                @Deprecated
                AWS_OLD("aws.old", "x"),
                AWS_TIMEOUT("aws.timeout", Duration.ofSeconds(1).toString()),
                NO_PROPERTY(null, null);
                private final String p;
                S(String p, String d) { this.p = p; }
            }
            """);
        write(dir.resolve("P.java"), """
            public final class P {
                public static final String REGION = "region";
                @Deprecated
                public static final String OLD_KEY = "old_key";
                private static final String HIDDEN = "hidden";
            }
            """);
        write(dir.resolve("E.java"), "public enum E { HTTP_PROXY(\"http_proxy\"); E(String e) { } }");
        ConfigurationInventorySource source = new ConfigurationInventorySource(List.of(
            new ConfigurationInventorySource.Holder("sdk-core", "S.java",
                                                    ConfigurationInventorySource.Style.ENV_NAME_AND_SYSPROP_ARG),
            new ConfigurationInventorySource.Holder("profiles", "P.java",
                                                    ConfigurationInventorySource.Style.PROFILE_CONSTANTS),
            new ConfigurationInventorySource.Holder("utils", "E.java", ConfigurationInventorySource.Style.ENV_ARG)));

        Map<String, InventoryItem> items = byId(source.scan(context()));
        assertEquals(List.of("config:env:AWS_OLD", "config:env:AWS_REGION", "config:env:AWS_TIMEOUT",
                             "config:env:NO_PROPERTY", "config:env:http_proxy", "config:profile:old_key",
                             "config:profile:region", "config:sysprop:aws.old", "config:sysprop:aws.region",
                             "config:sysprop:aws.timeout"),
                     List.copyOf(items.keySet()));
        assertTrue(items.get("config:env:AWS_OLD").deprecated());
        assertTrue(items.get("config:profile:old_key").deprecated());
        assertFalse(items.get("config:env:AWS_REGION").deprecated());
    }

    @Test
    void publicApiComesFromBytecodeExcludingInternalAndSyntheticMembers() throws IOException {
        Path jar = compileJar(Map.of(
            "software/amazon/awssdk/annotations/SdkInternalApi.java",
            "package software.amazon.awssdk.annotations; public @interface SdkInternalApi { }",
            "p/Api.java", """
                package p;
                import java.util.function.Supplier;
                public class Api implements Comparable<Api> {
                    public static final String NAME = "n";
                    protected int counter;
                    private int hidden;
                    public Api() { }
                    @Deprecated public void old() { }
                    @software.amazon.awssdk.annotations.SdkInternalApi public void internalUse() { }
                    void packagePrivate() { }
                    public int compareTo(Api o) { return 0; }
                    public Supplier<String> lambda() { return () -> NAME; }
                    public static class Nested { public void m() { } }
                    private static class Hidden { public void m() { } }
                    @Deprecated public interface OldNested { void m(); }
                }
                """,
            "p/Final.java", "package p; public final class Final { protected void p() { } public void q() { } }",
            "p/Internal.java",
            "package p; @software.amazon.awssdk.annotations.SdkInternalApi public class Internal { public void m() { } }",
            "p/internal/Impl.java", "package p.internal; public class Impl { public void m() { } }"));

        Map<String, InventoryItem> items = byId(PublicApiInventorySource.scanJar(jar, "fixture", "fixture.jar"));
        assertEquals(List.of("api:p.Api", "api:p.Api#<init>()V", "api:p.Api#NAME:Ljava/lang/String;",
                             "api:p.Api#compareTo(Lp/Api;)I", "api:p.Api#counter:I", "api:p.Api#lambda()"
                                                                                     + "Ljava/util/function/Supplier;",
                             "api:p.Api#old()V", "api:p.Api$Nested", "api:p.Api$Nested#<init>()V",
                             "api:p.Api$Nested#m()V", "api:p.Api$OldNested", "api:p.Api$OldNested#m()V",
                             "api:p.Final", "api:p.Final#<init>()V", "api:p.Final#q()V",
                             "api:software.amazon.awssdk.annotations.SdkInternalApi"),
                     List.copyOf(items.keySet()));
        assertTrue(items.get("api:p.Api#old()V").deprecated());
        assertFalse(items.get("api:p.Api#compareTo(Lp/Api;)I").deprecated());
        assertTrue(items.get("api:p.Api$OldNested").deprecated());
        assertTrue(items.get("api:p.Api$OldNested#m()V").deprecated(), "members of deprecated types are deprecated");
        assertEquals("fixture.jar!/p/Api.class", items.get("api:p.Api").source().path());
    }

    @Test
    void publicApiDefaultVersionIsTheHighestReleaseWithEveryJar() throws IOException {
        for (String v : List.of("2.1.0", "2.2.0", "2.10.0", "2.11.0")) {
            touch(PublicApiInventorySource.jar(dir, "a", v));
        }
        for (String v : List.of("2.1.0", "2.2.0", "2.10.0")) {
            touch(PublicApiInventorySource.jar(dir, "b", v));
        }
        assertEquals("2.10.0", PublicApiInventorySource.defaultVersion(dir, List.of("a", "b"), "2.11.0-SNAPSHOT"));
        assertEquals("2.2.0", PublicApiInventorySource.defaultVersion(dir, List.of("a", "b"), "2.9.0"));
    }

    @Test
    void behaviorsCoverEveryRequiredCategory() throws IOException {
        List<String> ids = ids(new BehaviorInventorySource().scan(context()));
        for (String category : List.of("retry.", "endpoint.", "checksum.", "compression.", "credentials.", "region.",
                                       "profile.", "paginators.", "waiters.", "presigning.", "event-streams.",
                                       "telemetry.", "logging.", "native-image.", "module-path.",
                                       "service-defaults.", "dynamodb-enhanced.", "s3-transfer-manager.",
                                       "iam-policy-builder.", "s3-event-notifications.", "sns-message-manager.")) {
            assertTrue(ids.stream().anyMatch(id -> id.startsWith("behavior:" + category)), category);
        }
        ids.forEach(id -> assertTrue(EntryKind.BEHAVIOR.accepts(id), id));
    }

    private Path compileJar(Map<String, String> sources) throws IOException {
        Path src = dir.resolve("src");
        Path classes = dir.resolve("classes");
        Files.createDirectories(classes);
        for (Map.Entry<String, String> e : sources.entrySet()) {
            write(src.resolve(e.getKey()), e.getValue());
        }
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        List<String> args = new java.util.ArrayList<>(List.of("-d", classes.toString()));
        sources.keySet().forEach(k -> args.add(src.resolve(k).toString()));
        assertEquals(0, javac.run(null, null, null, args.toArray(String[]::new)));
        Path jar = dir.resolve("fixture.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar));
             Stream<Path> files = Files.walk(classes)) {
            for (Path f : files.filter(Files::isRegularFile).sorted().toList()) {
                out.putNextEntry(new JarEntry(classes.relativize(f).toString().replace('\\', '/')));
                out.write(Files.readAllBytes(f));
                out.closeEntry();
            }
        }
        return jar;
    }

    private InventorySource.Context context() {
        return new InventorySource.Context(dir, BASELINE);
    }

    private void service(String module, String service, String content) throws IOException {
        pom(dir.resolve("http-clients").resolve(module), "<artifactId>" + module + "</artifactId>");
        write(dir.resolve("http-clients").resolve(module).resolve("src/main/resources/META-INF/services/" + service),
              content + "\n");
    }

    private static void pom(Path moduleDir, String body) throws IOException {
        write(moduleDir.resolve("pom.xml"), "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">" + body + "</project>");
    }

    private static void touch(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        try (OutputStream ignored = Files.newOutputStream(file)) {
            // empty marker jar
        }
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static List<String> ids(List<InventoryItem> items) {
        return items.stream().map(InventoryItem::id).toList();
    }

    private static Map<String, InventoryItem> byId(List<InventoryItem> items) {
        return items.stream().collect(Collectors.toMap(InventoryItem::id, Function.identity(), (a, b) -> a,
                                                       java.util.TreeMap::new));
    }
}
