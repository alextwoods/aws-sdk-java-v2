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

package software.amazon.awssdk.tools.paritymanifest.compat;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.tools.paritymanifest.Classification;
import software.amazon.awssdk.tools.paritymanifest.CompatibilityDimension;

/**
 * One generated compatibility test case for one Parity Manifest entry and one compatibility dimension
 * (Requirements 17.1, 17.2, 17.3, 19.12). The executing suite compiles, links, compares or runs it against the
 * release candidate as its {@link Kind} prescribes.
 *
 * @param id             unique fixture ID: {@code <entryId> <kind>[ <dimension>]}
 * @param entryId        the manifest entry the fixture verifies
 * @param classification the entry's classification
 * @param kind           what the fixture checks
 * @param dimension      the compatibility dimension verified, or {@code null} when the check follows from the
 *                       classification (rejection fixtures, and the behavior fixture of an {@code ADAPTED} or
 *                       {@code EXCLUDED} entry)
 * @param v2Surface      the entry's V2 surface
 * @param target         the V2 type or member under test (from the baseline class files), or {@code null} for
 *                       entries that are not {@code api:} entries
 * @param source         for compile and link fixtures: the complete Java source of the public class
 *                       {@link #CALLER_CLASS} in the unnamed package; otherwise {@code null}
 * @param symbols        for {@link Kind#COMPILE_REJECTED}: names from the entry's own surface, one of which every
 *                       javac error must mention (so the caller fails because of the entry, not a generator defect)
 * @param diagnostic     for rejection fixtures: the migration diagnostic linked to the entry (ID, classification,
 *                       V2 surface, replacement and migration action)
 * @param supertypes     for type fixtures: the binary names of the supertypes the V3 type must keep (see
 *                       {@link CompatibilityFixtureGenerator})
 * @param evidenceIds    for {@link Kind#BEHAVIOR}: the verification tests that establish the behavior
 */
public record CompatibilityFixture(String id,
                                   String entryId,
                                   Classification classification,
                                   Kind kind,
                                   CompatibilityDimension dimension,
                                   String v2Surface,
                                   Target target,
                                   String source,
                                   List<String> symbols,
                                   String diagnostic,
                                   List<String> supertypes,
                                   List<String> evidenceIds) {

    /** The class every generated caller source declares. */
    public static final String CALLER_CLASS = "CompatCaller";

    /** Fixtures sort by entry, then kind, then dimension (none first). */
    public static final Comparator<CompatibilityFixture> ORDER =
        Comparator.comparing(CompatibilityFixture::entryId)
                  .thenComparing(CompatibilityFixture::kind)
                  .thenComparing(CompatibilityFixture::dimension, Comparator.nullsFirst(Comparator.naturalOrder()));

    /** What a fixture checks and how its executor decides the outcome. */
    public enum Kind {
        /** {@link #source} compiles against the candidate class path (source dimension, 17.1, 17.3). */
        COMPILE,
        /**
         * {@link #source} fails to compile against the candidate, on one of {@link #symbols}; the failure is reported
         * with {@link #diagnostic}. Generated for {@code ADAPTED} and {@code EXCLUDED} API entries that do not claim
         * the source dimension, so V2 source never compiles silently against an unverified V3 surface.
         */
        COMPILE_REJECTED,
        /**
         * {@link #source} is compiled against the V2 baseline class path, its bytecode references the target's exact
         * owner, name and JVM descriptor, and loading and running its {@code link()} method against the candidate
         * links that reference without a {@link LinkageError} (binary dimension, 17.2).
         */
        LINK,
        /**
         * Like {@link #LINK}, but compiled V2 bytecode must fail with a {@link LinkageError} against the candidate.
         * Generated for {@code ADAPTED} and {@code EXCLUDED} API entries that do not claim the binary dimension.
         */
        LINK_REJECTED,
        /**
         * The candidate member matches the target's recorded signature for the dimension: owner type kind, name,
         * static-ness and non-narrowed access, plus the exact JVM descriptor (binary) or assignment-compatible
         * erased parameter and return types (source). The V2 callers of a protected member are subclasses, so the
         * candidate owner of a protected member of a non-final V2 owner must not be final.
         */
        SIGNATURE,
        /**
         * The candidate type keeps the target's type kind, does not become final (binary), and is a subtype of every
         * {@link #supertypes} element (exception inheritance and required hierarchy, 17.1, 17.2).
         */
        HIERARCHY,
        /**
         * Every {@link #evidenceIds} test runs against the candidate with at least one test and no failure (behavior
         * and configuration dimensions, and the verified differences of {@code ADAPTED} entries, 19.12).
         */
        BEHAVIOR
    }

    /** The kind of V2 API element a fixture targets. */
    public enum TargetKind {
        TYPE,
        METHOD,
        CONSTRUCTOR,
        FIELD
    }

    /**
     * The V2 API element a fixture targets, with the baseline facts the manifest ID does not carry.
     *
     * @param kind        type, method, constructor or field
     * @param owner       binary name of the type, or of the member's declaring type
     * @param ownerKind   {@code class}, {@code interface}, {@code enum} or {@code annotation}
     * @param ownerFinal  whether the V2 owner type is final
     * @param name        member name ({@code <init>} for constructors), or {@code null} for a type
     * @param descriptor  member JVM descriptor, or {@code null} for a type
     * @param isStatic    whether the V2 member is static ({@code false} for a type)
     * @param isProtected whether the V2 member (or nested type) is protected rather than public
     */
    public record Target(TargetKind kind, String owner, String ownerKind, boolean ownerFinal, String name,
                         String descriptor, boolean isStatic, boolean isProtected) {
        public Target {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(ownerKind, "ownerKind");
        }
    }

    public CompatibilityFixture {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(entryId, "entryId");
        Objects.requireNonNull(classification, "classification");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(v2Surface, "v2Surface");
        symbols = symbols == null ? List.of() : List.copyOf(symbols);
        supertypes = supertypes == null ? List.of() : List.copyOf(supertypes);
        evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
    }

    /** The fixture ID of {@code kind} and {@code dimension} for {@code entryId}. */
    public static String id(String entryId, Kind kind, CompatibilityDimension dimension) {
        return entryId + " " + kind + (dimension == null ? "" : " " + dimension.jsonName());
    }
}
