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

package software.amazon.awssdk.policybuilder.iam;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiPredicate;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

/**
 * Properties of the policy builder over generated policies: written JSON (compact and pretty) reads back to a policy
 * that re-writes to the same text; a policy already in the writer's grouped order reads back equal; and equality,
 * hash, string rendering and builder copies form a consistent value contract (what V2 checked with EqualsVerifier).
 */
class IamPolicyPropertyTest {
    private static final IamPolicyWriter COMPACT = IamPolicyWriter.create();
    private static final IamPolicyWriter PRETTY = IamPolicyWriter.builder().prettyPrint(true).build();
    private static final IamPolicyReader READER = IamPolicyReader.create();

    @Property(tries = 300)
    void writtenJson_readsBackAndRewritesIdentically(@ForAll("policies") IamPolicy policy) {
        for (IamPolicyWriter writer : List.of(COMPACT, PRETTY)) {
            String json = writer.writeToString(policy);

            assertThat(READER.read(json).toJson(writer)).isEqualTo(json);
            assertThat(writer.writeToBytes(policy)).isEqualTo(json.getBytes(StandardCharsets.UTF_8));
        }
        assertThat(READER.read(policy.toJson(COMPACT))).isEqualTo(READER.read(policy.toJson(PRETTY)));
    }

    @Property(tries = 300)
    void groupedPolicies_roundTripToAnEqualPolicy(@ForAll("groupedPolicies") IamPolicy policy) {
        assertThat(IamPolicy.fromJson(policy.toJson())).isEqualTo(policy);
        assertThat(READER.read(policy.toJson(PRETTY))).isEqualTo(policy);
    }

    @Property(tries = 300)
    void equalsHashCodeAndCopies_formAValueContract(@ForAll("policies") IamPolicy a, @ForAll("policies") IamPolicy b) {
        assertThat(a.toBuilder().build()).isEqualTo(a).hasSameHashCodeAs(a).hasToString(a.toString());
        assertThat(a.copy(p -> { })).isEqualTo(a);
        assertThat(a).isNotEqualTo(null).isNotEqualTo(a.toString());
        assertThat(a.equals(b)).isEqualTo(b.equals(a));
        if (a.equals(b)) {
            assertThat(a).hasSameHashCodeAs(b);
        }
        for (IamStatement statement : a.statements()) {
            assertThat(statement.toBuilder().build()).isEqualTo(statement).hasSameHashCodeAs(statement)
                                                     .hasToString(statement.toString());
            statement.principals().forEach(p -> assertThat(p.toBuilder().build()).isEqualTo(p)
                                                                                .hasSameHashCodeAs(p));
            statement.conditions().forEach(c -> assertThat(c.toBuilder().build()).isEqualTo(c)
                                                                                .hasSameHashCodeAs(c));
        }
    }

    @Provide
    Arbitrary<IamPolicy> policies() {
        return policies(false);
    }

    @Provide
    Arbitrary<IamPolicy> groupedPolicies() {
        return policies(true);
    }

    private static Arbitrary<IamPolicy> policies(boolean grouped) {
        return Combinators.combine(text().injectNull(0.3), text().injectNull(0.3),
                                   statement(grouped).list().ofMinSize(1).ofMaxSize(3))
                          .as((version, id, statements) -> IamPolicy.builder().version(version).id(id)
                                                                    .statements(statements).build());
    }

    private static Arbitrary<IamStatement> statement(boolean grouped) {
        Arbitrary<List<String>> ids = text().list().ofMaxSize(3);
        Arbitrary<IamStatement.Builder> header =
            Combinators.combine(text().injectNull(0.3), Arbitraries.of("Allow", "Deny"), principals(grouped),
                                principals(grouped), conditions(grouped))
                       .as((sid, effect, principals, notPrincipals, conditions) ->
                               IamStatement.builder().sid(sid).effect(effect).principals(principals)
                                           .notPrincipals(notPrincipals).conditions(conditions));
        return Combinators.combine(header, ids, ids, ids, ids)
                          .as((builder, actions, notActions, resources, notResources) ->
                                  builder.actionIds(actions).notActionIds(notActions).resourceIds(resources)
                                         .notResourceIds(notResources).build());
    }

    private static Arbitrary<List<IamPrincipal>> principals(boolean grouped) {
        Arbitrary<List<IamPrincipal>> some =
            Combinators.combine(Arbitraries.of("AWS", "Service", "Federated", "CanonicalUser", "Custom"), text())
                       .as(IamPrincipal::create).list().ofMaxSize(4);
        if (grouped) {
            some = some.map(list -> grouped(list, (x, y) -> x.type().equals(y.type()), (x, y) -> false));
        }
        return Arbitraries.frequencyOf(Tuple.of(8, some), Tuple.of(1, Arbitraries.just(List.of(IamPrincipal.ALL))));
    }

    private static Arbitrary<List<IamCondition>> conditions(boolean grouped) {
        Arbitrary<List<IamCondition>> some =
            Combinators.combine(Arbitraries.of("StringEquals", "NumericLessThan", "Bool", "ForAnyValue:StringLike"),
                                Arbitraries.of("aws:SourceIp", "aws:PrincipalAccount", "s3:prefix"), text())
                       .as(IamCondition::create).list().ofMaxSize(4);
        if (!grouped) {
            return some;
        }
        return some.map(list -> grouped(list, (x, y) -> x.operator().equals(y.operator()),
                                        (x, y) -> x.operator().equals(y.operator()) && x.key().equals(y.key())));
    }

    /**
     * {@code list} in the order the writer groups it: by first appearance of the outer group, then of the inner
     * group, keeping the relative order of equal elements (a stable sort).
     */
    private static <T> List<T> grouped(List<T> list, BiPredicate<T, T> outer, BiPredicate<T, T> inner) {
        return list.stream()
                   .sorted(Comparator.comparingInt((T t) -> firstIndex(list, t, outer))
                                     .thenComparingInt(t -> firstIndex(list, t, inner)))
                   .toList();
    }

    private static <T> int firstIndex(List<T> list, T element, BiPredicate<T, T> sameGroup) {
        for (int i = 0; i < list.size(); i++) {
            if (sameGroup.test(list.get(i), element)) {
                return i;
            }
        }
        return -1;
    }

    /** Strings with JSON-special, control, non-ASCII and surrogate characters. */
    private static Arbitrary<String> text() {
        return Arbitraries.oneOf(Arbitraries.strings().alpha().numeric().ofMaxLength(8),
                                 Arbitraries.strings().all().ofMaxLength(6),
                                 Arbitraries.of("*", "arn:aws:s3:::bucket/*", "\"quoted\"", "back\\slash", "\u00e9",
                                                "\uD83D\uDE00", "\uDC00", "line\nbreak", "\u0001", "/"));
    }
}
