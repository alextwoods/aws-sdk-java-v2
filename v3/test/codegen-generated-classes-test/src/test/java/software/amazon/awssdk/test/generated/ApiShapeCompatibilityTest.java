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

package software.amazon.awssdk.test.generated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.test.generated.support.CompatibilityFixtures;
import software.amazon.awssdk.test.generated.support.CompatibilityFixtures.Fixture;
import software.amazon.awssdk.test.generated.support.CompatibilityFixtures.Target;

/**
 * Compares the generated V3 API with the V2 signatures and hierarchy recorded for each source- or binary-preserved
 * entry of the test scope (Requirements 17.1, 17.2, 19.12; {@code compatibility-fixtures.json}).
 *
 * <ul>
 *   <li>{@code SIGNATURE}: the V3 owner exists and is public (or protected, for a protected member or when the V2
 *       owner was a protected nested type as its type fixture records); the member resolves through the owner's hierarchy (JVMS
 *       5.4.3) with the same static-ness and no narrower access; for the binary dimension the owner keeps its type
 *       kind and the member has the exact V2 JVM descriptor (bridge methods count, as they link); for the source
 *       dimension every V2 erased parameter type is assignable to the V3 parameter and the V3 return or field type is
 *       assignable to the V2 one. The V2 callers of a protected member are subclasses, so its V3 owner must not become
 *       final.</li>
 *   <li>{@code HIERARCHY}: the V3 type keeps its type kind, does not become final when V2 callers may extend it
 *       (binary), and is a subtype of every required V2 supertype (JDK supertypes, the superclass chain of
 *       exceptions, and binary-preserved supertypes).</li>
 * </ul>
 */
class ApiShapeCompatibilityTest {
    private static final ClassLoader V3 = ApiShapeCompatibilityTest.class.getClassLoader();

    static Stream<Fixture> shapeFixtures() {
        return CompatibilityFixtures.ofKinds("SIGNATURE", "HIERARCHY");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("shapeFixtures")
    void theV3ApiKeepsTheRecordedShapeForTheClaimedDimension(Fixture fixture) {
        Target target = fixture.target();
        boolean binary = fixture.dimension().equals("binary");
        Class<?> owner = load(target.owner(), fixture);
        assertTrue(Modifier.isPublic(owner.getModifiers())
                   || (target.isProtected() || v2OwnerProtected(target)) && Modifier.isProtected(owner.getModifiers()),
                   fixture.entryId() + ": the V3 type is not accessible");
        if (fixture.kind().equals("HIERARCHY")) {
            assertEquals(target.ownerKind(), typeKind(owner), fixture.entryId() + ": type kind changed");
            if (binary && !target.ownerFinal()) {
                assertFalse(Modifier.isFinal(owner.getModifiers()), fixture.entryId() + ": the V3 type became final");
            }
            List<String> missing = new ArrayList<>();
            for (String supertype : fixture.supertypes()) {
                if (!load(supertype, fixture).isAssignableFrom(owner)) {
                    missing.add(supertype);
                }
            }
            assertEquals(List.of(), missing, fixture.entryId() + ": the V3 type is no longer a subtype of these");
            return;
        }
        if (binary) {
            assertEquals(target.ownerKind(), typeKind(owner), fixture.entryId() + ": owner type kind changed, so "
                                                              + "V2 invoke instructions no longer link");
        }
        Member member = switch (target.kind()) {
            case "FIELD" -> field(owner, target, binary, fixture);
            case "CONSTRUCTOR", "METHOD" -> executable(owner, target, binary, fixture);
            default -> throw new AssertionError("Unexpected target kind " + target.kind());
        };
        assertEquals(target.isStatic(), Modifier.isStatic(member.getModifiers()),
                     fixture.entryId() + ": static-ness changed on " + member);
        int modifiers = member.getModifiers();
        assertTrue(Modifier.isPublic(modifiers) || target.isProtected() && Modifier.isProtected(modifiers),
                   fixture.entryId() + ": access narrowed on " + member);
        if (target.isProtected() && !target.ownerFinal()) {
            assertFalse(Modifier.isFinal(owner.getModifiers()),
                        fixture.entryId() + ": the V3 owner became final, so the V2 subclasses that use this "
                        + "protected member no longer compile or load");
        }
    }

    /**
     * True if the V2 owner of a member target is a protected nested type, as the owner's own in-scope type fixture
     * records (for example the V2 {@code protected static class SnsMessage.BuilderImpl}, whose public members V2
     * subclasses use). Without such a type fixture the owner must be public.
     */
    private static boolean v2OwnerProtected(Target target) {
        return CompatibilityFixtures.ofKinds("HIERARCHY")
                                    .map(Fixture::target)
                                    .anyMatch(t -> t.kind().equals("TYPE") && t.owner().equals(target.owner())
                                                   && t.isProtected());
    }

    private static Field field(Class<?> owner, Target target, boolean binary, Fixture fixture) {
        Class<?> v2Type = descriptorClass(target.descriptor(), fixture);
        for (Class<?> type : hierarchy(owner)) {
            for (Field f : type.getDeclaredFields()) {
                if (f.getName().equals(target.name())
                    && (binary ? f.getType() == v2Type : v2Type.isAssignableFrom(f.getType()))) {
                    return f;
                }
            }
        }
        return fail(fixture.entryId() + ": V3 " + owner.getName() + " has no field " + target.name() + " of type "
                    + v2Type.getName() + (binary ? "" : " or a subtype"));
    }

    private static Executable executable(Class<?> owner, Target target, boolean binary, Fixture fixture) {
        MethodType v2;
        try {
            v2 = MethodType.fromMethodDescriptorString(target.descriptor(), V3);
        } catch (TypeNotPresentException | IllegalArgumentException e) {
            return fail(fixture.entryId() + ": a V2 signature type is missing from V3", e);
        }
        List<Executable> candidates = new ArrayList<>();
        if (target.kind().equals("CONSTRUCTOR")) {
            candidates.addAll(List.of(owner.getDeclaredConstructors()));
        } else {
            for (Class<?> type : hierarchy(owner)) {
                for (Method m : type.getDeclaredMethods()) {
                    if (m.getName().equals(target.name()) && (binary || !m.isBridge())) {
                        candidates.add(m);
                    }
                }
            }
        }
        Optional<Executable> match = candidates.stream().filter(e -> matches(e, v2, binary)).findFirst();
        return match.orElseGet(() -> fail(fixture.entryId() + ": V3 " + owner.getName() + " has no "
                                          + (binary ? "member with the exact descriptor " + target.descriptor()
                                                    : "source-compatible overload of " + target.name() + v2)
                                          + "; candidates: " + candidates));
    }

    private static boolean matches(Executable e, MethodType v2, boolean binary) {
        Class<?>[] parameters = e.getParameterTypes();
        if (parameters.length != v2.parameterCount()) {
            return false;
        }
        for (int i = 0; i < parameters.length; i++) {
            Class<?> v2Parameter = v2.parameterType(i);
            if (binary ? parameters[i] != v2Parameter
                       : parameters[i].isPrimitive() ? parameters[i] != v2Parameter
                                                     : !parameters[i].isAssignableFrom(v2Parameter)) {
                return false;
            }
        }
        if (e instanceof Constructor) {
            return true;
        }
        Class<?> returns = ((Method) e).getReturnType();
        return binary ? returns == v2.returnType()
                      : returns == v2.returnType() || !returns.isPrimitive() && v2.returnType().isAssignableFrom(returns);
    }

    /** The type, its superclasses, then every superinterface: the JVM's member resolution order. */
    private static List<Class<?>> hierarchy(Class<?> type) {
        Set<Class<?>> result = new LinkedHashSet<>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            result.add(c);
        }
        Deque<Class<?>> work = new ArrayDeque<>(result);
        while (!work.isEmpty()) {
            for (Class<?> i : work.removeFirst().getInterfaces()) {
                if (result.add(i)) {
                    work.add(i);
                }
            }
        }
        return List.copyOf(result);
    }

    private static String typeKind(Class<?> type) {
        if (type.isAnnotation()) {
            return "annotation";
        }
        if (type.isInterface()) {
            return "interface";
        }
        return type.isEnum() ? "enum" : "class";
    }

    private static Class<?> descriptorClass(String descriptor, Fixture fixture) {
        try {
            return MethodType.fromMethodDescriptorString("()" + descriptor, V3).returnType();
        } catch (TypeNotPresentException e) {
            return fail(fixture.entryId() + ": the V2 field type is missing from V3", e);
        }
    }

    private static Class<?> load(String name, Fixture fixture) {
        try {
            return Class.forName(name, false, V3);
        } catch (ClassNotFoundException | LinkageError e) {
            return fail(fixture.entryId() + ": V3 lacks " + name, e);
        }
    }
}
