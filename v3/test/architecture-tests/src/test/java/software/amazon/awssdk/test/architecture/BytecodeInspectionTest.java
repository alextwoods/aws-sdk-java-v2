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

package software.amazon.awssdk.test.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Negative and positive fixtures for {@link BytecodeInspector} and {@link ClassFile}. Stub sources impersonate the
 * forbidden libraries; fixture classes that use them are compiled with the JDK compiler and inspected as a module's
 * {@code target/classes}.
 */
class BytecodeInspectionTest {

    private static final String MODULE = "fixture";

    @TempDir
    static Path work;

    /** Classes present on the fixture module's runtime class path. */
    private static Path runtimeStubs;
    private static Map<String, ClassFile> serviceClasses;
    private static Map<String, ClassFile> apiClasses;
    private static DependencyPolicy policy;

    @BeforeAll
    static void compileFixtures() throws IOException {
        policy = DependencyPolicy.load();
        runtimeStubs = compile("runtime-stubs", List.of(),
            "package software.amazon.smithy.model.shapes; public final class ShapeId { }",
            "package software.amazon.smithy.model.loader; public final class ParserUtils {"
            + " public static void consumeIdentifier() { } }",
            "package software.amazon.smithy.model.loader; public final class ModelAssembler { }",
            "package software.amazon.smithy.codegen.core; public interface SymbolProvider { }",
            "package io.netty.channel; public interface Channel { }",
            "package org.apache.hc.client5.http.classic; public interface HttpClient { }",
            "package io.opentelemetry.sdk; public final class OpenTelemetrySdk { }",
            "package ch.qos.logback.classic; public class Logger { }",
            "package ch.qos.logback.classic; public class LoggerContext { }",
            "package ch.qos.logback.core; public class LogbackException extends RuntimeException { }",
            "package org.apache.logging.log4j.core; public interface Appender { }",
            "package org.slf4j.simple; public class SimpleLogger { }",
            "package org.slf4j; public interface Logger { }");
        // A V2-only class: compiled against, but absent from the fixture module's runtime class path.
        Path absentStubs = compile("absent-stubs", List.of(),
            "package software.amazon.awssdk.core; public final class SdkBytes { }");

        serviceClasses = ClassPath.readClasses(compile("service-classes", List.of(runtimeStubs, absentStubs),
            "package fixture.service; public class UsesModelAssembler { Object assemble() {"
            + " return new software.amazon.smithy.model.loader.ModelAssembler(); } }",
            "package fixture.service; public class UsesShapeIdentity { private"
            + " software.amazon.smithy.model.shapes.ShapeId id; void parse() {"
            + " software.amazon.smithy.model.loader.ParserUtils.consumeIdentifier(); } }",
            "package fixture.service; class UsesCodegen implements software.amazon.smithy.codegen.core.SymbolProvider"
            + " { }",
            "package fixture.service; class UsesNetty { void send(io.netty.channel.Channel channel) { } }",
            "package fixture.service; class UsesApacheHttpClient {"
            + " org.apache.hc.client5.http.classic.HttpClient client; }",
            "package fixture.service; class UsesOpenTelemetrySdk { Object sdk() {"
            + " return new io.opentelemetry.sdk.OpenTelemetrySdk(); } }",
            "package fixture.service; public class UsesLogback { private ch.qos.logback.classic.Logger logger; }",
            "package fixture.service; class UsesV2 { Object bytes() {"
            + " return new software.amazon.awssdk.core.SdkBytes(); } }",
            "package fixture.service; public class UsesJdkAndItself { java.util.List<UsesShapeIdentity> list;"
            + " java.util.logging.Logger jul; Runnable task = () -> { }; }"));

        apiClasses = ClassPath.readClasses(compile("api-classes", List.of(runtimeStubs),
            "package fixture.api; public class LeakyReturn { public ch.qos.logback.classic.Logger logger() {"
            + " return null; } }",
            "package fixture.api; public class LeakyField { public ch.qos.logback.classic.Logger logger; }",
            "package fixture.api; public class LeakyProtectedParameter {"
            + " protected void configure(org.slf4j.simple.SimpleLogger logger) { } }",
            "package fixture.api; public class LeakySuperclass extends ch.qos.logback.classic.LoggerContext { }",
            "package fixture.api; public abstract class LeakyInterface"
            + " implements org.apache.logging.log4j.core.Appender { }",
            "package fixture.api; public class LeakyTypeArgument {"
            + " public java.util.List<? extends ch.qos.logback.classic.Logger> loggers() { return null; } }",
            "package fixture.api; public class LeakyThrows { public void run() throws"
            + " ch.qos.logback.core.LogbackException { } }",
            "package fixture.api; public class LeakyTypeBound<T extends ch.qos.logback.classic.Logger> { }",
            "package fixture.api; public class PrivateUse { private ch.qos.logback.classic.Logger logger;"
            + " private void use(ch.qos.logback.classic.Logger l) { } void internal(ch.qos.logback.classic.Logger l)"
            + " { } public void ok() { } }",
            "package fixture.api; class PackagePrivate { public ch.qos.logback.classic.Logger logger() {"
            + " return null; } }",
            "package fixture.api; public class Outer { private static class Hidden {"
            + " public ch.qos.logback.classic.Logger logger() { return null; } } }",
            "package fixture.api; public class FacadeOnly { public org.slf4j.Logger slf4j() { return null; }"
            + " public java.util.logging.Logger jul() { return null; } }"));
    }

    @Test
    void referencesToEveryForbiddenArtifactClassAreDetected() {
        assertEquals(List.of(
            "fixture: fixture.service.UsesApacheHttpClient references org.apache.hc.client5.http.classic.HttpClient "
            + "(forbidden artifact class [concrete-http])",
            "fixture: fixture.service.UsesCodegen references software.amazon.smithy.codegen.core.SymbolProvider "
            + "(forbidden artifact class [codegen, smithy-model-parser])",
            "fixture: fixture.service.UsesLogback references ch.qos.logback.classic.Logger "
            + "(forbidden artifact class [logging-implementation])",
            "fixture: fixture.service.UsesModelAssembler references software.amazon.smithy.model.loader.ModelAssembler "
            + "(forbidden artifact class [smithy-model-parser])",
            "fixture: fixture.service.UsesNetty references io.netty.channel.Channel "
            + "(forbidden artifact class [concrete-http])",
            "fixture: fixture.service.UsesOpenTelemetrySdk references io.opentelemetry.sdk.OpenTelemetrySdk "
            + "(forbidden artifact class [telemetry-exporter])"),
            BytecodeInspector.forbiddenReferences(MODULE, serviceClasses, policy.serviceTemplate().forbids()));
    }

    @Test
    void dynamicLayerAndModelLibraryClassesAreForbiddenOutsideTheDynamicLayer() throws IOException {
        // Requirements 14.18 and 15.5: the V3 dynamic layer and the Smithy libraries built on the semantic model
        // (waiters, rules engine, AWS traits) are smithy-model-parser classes for services and runtime modules, and
        // allowed in dynamic-model itself.
        Path stubs = compile("dynamic-stubs", List.of(),
            "package software.amazon.awssdk.dynamic.model; public final class DynamicModel { }",
            "package software.amazon.awssdk.dynamic.client; public final class DynamicClient { }",
            "package software.amazon.smithy.waiters; public final class WaitableTrait { }",
            "package software.amazon.smithy.rulesengine.language; public final class EndpointRuleSet { }",
            "package software.amazon.smithy.aws.traits.protocols; public final class AwsJson1_0Trait { }");
        Map<String, ClassFile> classes = ClassPath.readClasses(compile("dynamic-classes", List.of(stubs),
            "package fixture.service; class UsesDynamicModel { software.amazon.awssdk.dynamic.model.DynamicModel m; }",
            "package fixture.service; class UsesDynamicClient { Object c(software.amazon.awssdk.dynamic.client"
            + ".DynamicClient c) { return c; } }",
            "package fixture.service; class UsesWaiters { Object w() {"
            + " return new software.amazon.smithy.waiters.WaitableTrait(); } }",
            "package fixture.service; class UsesRulesEngine {"
            + " software.amazon.smithy.rulesengine.language.EndpointRuleSet rules; }",
            "package fixture.service; class UsesAwsTraits {"
            + " software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait trait; }"));
        List<String> expected = List.of(
            "fixture: fixture.service.UsesAwsTraits references software.amazon.smithy.aws.traits.protocols"
            + ".AwsJson1_0Trait (forbidden artifact class [smithy-model-parser])",
            "fixture: fixture.service.UsesDynamicClient references software.amazon.awssdk.dynamic.client.DynamicClient "
            + "(forbidden artifact class [smithy-model-parser])",
            "fixture: fixture.service.UsesDynamicModel references software.amazon.awssdk.dynamic.model.DynamicModel "
            + "(forbidden artifact class [smithy-model-parser])",
            "fixture: fixture.service.UsesRulesEngine references software.amazon.smithy.rulesengine.language"
            + ".EndpointRuleSet (forbidden artifact class [smithy-model-parser])",
            "fixture: fixture.service.UsesWaiters references software.amazon.smithy.waiters.WaitableTrait "
            + "(forbidden artifact class [smithy-model-parser])");
        assertEquals(expected, BytecodeInspector.forbiddenReferences(MODULE, classes,
                                                                     policy.serviceTemplate().forbids()));
        for (String runtime : List.of("aws-pipeline", "aws-json-protocol", "url-connection-client",
                                      "dynamodb-enhanced")) {
            assertEquals(expected, BytecodeInspector.forbiddenReferences(
                MODULE, classes, policy.rulesFor(runtime).orElseThrow().forbids()), runtime);
        }
        assertEquals(List.of(), BytecodeInspector.forbiddenReferences(
            MODULE, classes, policy.rulesFor("dynamic-model").orElseThrow().forbids()));
    }

    @Test
    void onlyTheModulesForbiddenClassesAreReported() {
        // For example dynamic-model may use the Smithy parser; logging implementations stay forbidden.
        assertEquals(List.of("fixture: fixture.service.UsesLogback references ch.qos.logback.classic.Logger "
                             + "(forbidden artifact class [logging-implementation])"),
                     BytecodeInspector.forbiddenReferences(MODULE, serviceClasses,
                                                           Set.of("logging-implementation", "v2-runtime")));
    }

    @Test
    void referencesOutsideTheRuntimeClassPathAreReported() {
        ClassPath runtime = ClassPath.of(Map.of("fixture:runtime-stubs", runtimeStubs));
        assertEquals(List.of("fixture: software.amazon.awssdk.core.SdkBytes (referenced by fixture.service.UsesV2) is "
                             + "not defined by the JDK, the module, or any artifact in its runtime dependency tree; "
                             + "it can only come from a V2 runtime artifact or a provided, test-scoped or undeclared "
                             + "dependency"),
                     BytecodeInspector.unresolvedReferences(MODULE, serviceClasses, runtime));
        assertEquals("fixture:runtime-stubs",
                     runtime.ownerOf("software.amazon.smithy.model.shapes.ShapeId").orElseThrow());
        assertEquals("JDK", runtime.ownerOf("java.lang.String").orElseThrow());
        assertTrue(runtime.ownerOf("software.amazon.awssdk.core.SdkBytes").isEmpty());
    }

    @Test
    void loggingImplementationTypesInPublicOrProtectedApisAreDetected() {
        assertEquals(List.of(
            "fixture: fixture.api.LeakyField field logger exposes logging implementation type "
            + "ch.qos.logback.classic.Logger",
            "fixture: fixture.api.LeakyInterface supertype exposes logging implementation type "
            + "org.apache.logging.log4j.core.Appender",
            "fixture: fixture.api.LeakyProtectedParameter method configure(Lorg/slf4j/simple/SimpleLogger;)V exposes "
            + "logging implementation type org.slf4j.simple.SimpleLogger",
            "fixture: fixture.api.LeakyReturn method logger()Lch/qos/logback/classic/Logger; exposes logging "
            + "implementation type ch.qos.logback.classic.Logger",
            "fixture: fixture.api.LeakySuperclass supertype exposes logging implementation type "
            + "ch.qos.logback.classic.LoggerContext",
            "fixture: fixture.api.LeakyThrows method run()V exposes logging implementation type "
            + "ch.qos.logback.core.LogbackException",
            "fixture: fixture.api.LeakyTypeArgument method loggers()Ljava/util/List; exposes logging implementation "
            + "type ch.qos.logback.classic.Logger",
            "fixture: fixture.api.LeakyTypeBound class signature exposes logging implementation type "
            + "ch.qos.logback.classic.Logger"),
            BytecodeInspector.loggingImplementationApiTypes(MODULE, apiClasses));
        // Private, package-private and facade (SLF4J API, JUL) usage is not an API leak.
        assertEquals(List.of(), BytecodeInspector.loggingImplementationApiTypes(MODULE, serviceClasses));
    }

    @Test
    void classFileReaderCollectsConstantPoolAndMemberReferences() {
        ClassFile type = serviceClasses.get("fixture.service.UsesJdkAndItself");
        assertTrue(type.referencedClasses().containsAll(Set.of("java.util.List", "java.util.logging.Logger",
                                                               "java.lang.Runnable",
                                                               "fixture.service.UsesShapeIdentity")),
                   type.referencedClasses().toString());
        assertFalse(type.referencedClasses().contains(type.name()));
        ClassFile leaky = apiClasses.get("fixture.api.LeakyThrows");
        assertEquals(List.of("ch.qos.logback.core.LogbackException"),
                     leaky.methods().stream().filter(m -> m.name().equals("run")).findFirst().orElseThrow()
                          .exceptions());
    }

    @Test
    void descriptorAndSignatureScannerFindsExactlyTheClassTypes() {
        assertEquals(Set.of("java.lang.String", "ch.qos.logback.classic.Logger"),
                     ClassFile.classNamesIn("(ILjava/lang/String;[[Lch/qos/logback/classic/Logger;J)V"));
        assertEquals(Set.of("java.lang.Object", "java.lang.Comparable", "java.util.AbstractMap", "java.util.List",
                            "io.netty.channel.Channel", "java.io.Serializable"),
                     ClassFile.classNamesIn("<LT:Ljava/lang/Object;K::Ljava/lang/Comparable<TK;>;>"
                                            + "Ljava/util/AbstractMap<TLT;Ljava/util/List<+Lio/netty/channel/Channel;>;>;"
                                            + "Ljava/io/Serializable;"));
        assertEquals(Set.of("com.example.Outer", "com.example.Outer$Inner"),
                     ClassFile.classNamesIn("Lcom/example/Outer<TT;>.Inner<*>;"));
        assertEquals(Set.of("java.lang.Object", "java.io.IOException"),
                     ClassFile.classNamesIn("<T:Ljava/lang/Object;>(TT;)TT;^Ljava/io/IOException;^TX;"));
        assertEquals(Set.of(), ClassFile.classNamesIn(""));
        assertThrows(IllegalArgumentException.class, () -> ClassFile.classNamesIn("Ljava/lang/String"));
        assertThrows(IllegalArgumentException.class, () -> ClassFile.classNamesIn("TT"));
    }

    @Test
    void malformedClassFilesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ClassFile.parse(new byte[] {0, 1, 2, 3, 0, 0, 0, 0}));
        assertThrows(RuntimeException.class, () -> ClassFile.parse(new byte[] {(byte) 0xCA, (byte) 0xFE}));
    }

    private static Path compile(String name, List<Path> classPath, String... sources) throws IOException {
        return JavaFixtures.compile(work, name, classPath, sources);
    }
}
