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

package software.amazon.awssdk.benchmark.harness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Enumerates the benchmark cells (Req 20.1-20.4). The full matrix is, for every launch protocol, every client path
 * and every transport of that path's mode:
 *
 * <ul>
 *     <li>finite {@code success} and {@code retry} cells for each payload class at concurrency 1, 8 and 32
 *     (Req 20.4);</li>
 *     <li>finite modeled-{@code error} cells for each payload class at concurrency 1 (Req 20.3 requires the workload;
 *     20.4 requires the concurrency sweep only for success and retry);</li>
 *     <li>one {@code stream} cell at concurrency 1 where the protocol binds streaming blob payloads, and one
 *     {@code eventstream} cell at concurrency 1 where it supports Event Streams (Req 20.3).</li>
 * </ul>
 *
 * <p>Cells with no operation to measure as a product or model fact are then dropped, each with its documented reason
 * ({@link #inapplicability}).
 *
 * <p>Enumeration order is deterministic (enum declaration order, then concurrency ascending), so a plan's cell list,
 * and anything 19.2 schedules from it, is reproducible. Selections narrow the full matrix with {@link Selection};
 * they never add cells that the rules above exclude.
 */
public final class CellMatrix {

    /** Req 20.4. */
    public static final List<Integer> CONCURRENCY_LEVELS = List.of(1, 8, 32);
    /** Concurrency of modeled-error, streaming and Event Stream cells. */
    public static final int SINGLE = 1;

    private CellMatrix() {
    }

    /** Every cell of the full matrix. */
    public static List<BenchmarkCell> full() {
        return enumerate(Selection.all());
    }

    /** The cells of the full matrix that {@code selection} keeps, in matrix order. */
    public static List<BenchmarkCell> enumerate(Selection selection) {
        List<BenchmarkCell> cells = new ArrayList<>();
        for (BenchProtocol protocol : BenchProtocol.values()) {
            for (ClientPath path : ClientPath.values()) {
                for (Transport transport : Transport.values()) {
                    if (transport.mode() != path.mode()) {
                        continue;
                    }
                    for (PayloadClass payload : PayloadClass.values()) {
                        for (Outcome outcome : Outcome.values()) {
                            for (int c : concurrencyFor(WorkloadKind.FINITE, outcome)) {
                                cells.add(BenchmarkCell.finite(protocol, path, transport, payload, outcome, c));
                            }
                        }
                    }
                    if (protocol.supportsStreamingBlobs()) {
                        cells.add(new BenchmarkCell(protocol, path, transport, WorkloadKind.STREAMING, null,
                                                    Outcome.SUCCESS, SINGLE));
                    }
                    if (protocol.supportsEventStreams()) {
                        cells.add(new BenchmarkCell(protocol, path, transport, WorkloadKind.EVENT_STREAM, null,
                                                    Outcome.SUCCESS, SINGLE));
                    }
                }
            }
        }
        cells.removeIf(c -> inapplicability(c).isPresent());
        cells.removeIf(selection.negate());
        return Collections.unmodifiableList(cells);
    }

    /**
     * Why a well-formed cell is not in the matrix, or empty if it is. A cell is inapplicable only for a product or
     * model fact that leaves it no operation to measure (Req 20.1 measures each path "for every supported protocol
     * applicable to" it; Req 20.3 requires modeled-error and Event Stream workloads, not one on every protocol). Each
     * fact is checked against the product or the fleet models by {@code CellMatrixTest}. A cell that is applicable
     * but merely has no fixture yet stays in the matrix, where release mode refuses it.
     */
    public static Optional<String> inapplicability(BenchmarkCell cell) {
        if (cell.kind() == WorkloadKind.EVENT_STREAM && cell.path() == ClientPath.GENERATED_SYNC) {
            return Optional.of(GENERATED_EVENT_STREAMS_ASYNC_ONLY);
        }
        if (cell.kind() == WorkloadKind.EVENT_STREAM && NO_FLEET_EVENT_STREAM.contains(cell.protocol())) {
            return Optional.of(NO_FLEET_EVENT_STREAM_REASON);
        }
        if (cell.protocol() == BenchProtocol.EC2_QUERY && cell.outcome() == Outcome.MODELED_ERROR) {
            return Optional.of(EC2_NO_MODELED_ERRORS);
        }
        return Optional.empty();
    }

    /** Reason of the generated-sync Event Stream cells. */
    static final String GENERATED_EVENT_STREAMS_ASYNC_ONLY =
        "V3, like V2, generates event-stream operations on the generated Async Client only; the generated Sync Client "
        + "has no event-stream operation to measure";
    /**
     * Protocols on which no fleet service model binds an event stream (an operation input or output member
     * targeting a {@code @streaming} union), so no generated client has one.
     */
    static final Set<BenchProtocol> NO_FLEET_EVENT_STREAM = EnumSet.of(BenchProtocol.AWS_JSON_1_0,
                                                                       BenchProtocol.RPC_V2_CBOR);
    static final String NO_FLEET_EVENT_STREAM_REASON =
        "no fleet service model binds an event stream on this protocol";
    /** Reason of the ec2Query modeled-error cells. */
    static final String EC2_NO_MODELED_ERRORS =
        "the EC2 model (the only ec2Query service) declares no error shapes, so no ec2Query operation has a modeled "
        + "error";

    /** The concurrency levels the matrix runs for a kind and outcome. */
    public static List<Integer> concurrencyFor(WorkloadKind kind, Outcome outcome) {
        if (kind == WorkloadKind.FINITE && outcome != Outcome.MODELED_ERROR) {
            return CONCURRENCY_LEVELS;
        }
        return List.of(SINGLE);
    }

    /**
     * A filter over the matrix dimensions; an empty dimension set means "all". Parsed from the client-facing
     * comma-separated forms ({@code awsJson1_0,restXml}) by {@link #parse}.
     */
    public record Selection(Set<BenchProtocol> protocols, Set<ClientPath> paths, Set<Transport> transports,
                            Set<WorkloadKind> kinds, Set<PayloadClass> payloads, Set<Outcome> outcomes,
                            Set<Integer> concurrency) implements Predicate<BenchmarkCell> {

        public static Selection all() {
            return new Selection(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of());
        }

        @Override
        public boolean test(BenchmarkCell cell) {
            return keeps(protocols, cell.protocol()) && keeps(paths, cell.path())
                   && keeps(transports, cell.transport()) && keeps(kinds, cell.kind())
                   && (payloads.isEmpty() || cell.payload() != null && payloads.contains(cell.payload()))
                   && keeps(outcomes, cell.outcome()) && keeps(concurrency, cell.concurrency());
        }

        private static <T> boolean keeps(Set<T> allowed, T value) {
            return allowed.isEmpty() || allowed.contains(value);
        }

        /**
         * Parses each dimension from a comma-separated list of ids ({@code null} or empty = all).
         */
        public static Selection parse(String protocols, String paths, String transports, String kinds,
                                      String payloads, String outcomes, String concurrency) {
            return new Selection(ids(protocols, BenchProtocol.class, BenchProtocol::fromId),
                                 ids(paths, ClientPath.class, ClientPath::fromId),
                                 ids(transports, Transport.class, Transport::fromId),
                                 ids(kinds, WorkloadKind.class, WorkloadKind::fromId),
                                 ids(payloads, PayloadClass.class, PayloadClass::fromId),
                                 ids(outcomes, Outcome.class, Outcome::fromId),
                                 concurrency == null || concurrency.isBlank() ? Set.of()
                                 : Set.copyOf(Arrays.stream(concurrency.split(",")).map(String::trim)
                                                    .map(Integer::valueOf).toList()));
        }

        private static <E extends Enum<E>> Set<E> ids(String csv, Class<E> type,
                                                      java.util.function.Function<String, E> parse) {
            EnumSet<E> set = EnumSet.noneOf(type);
            if (csv != null && !csv.isBlank()) {
                for (String id : csv.split(",")) {
                    set.add(parse.apply(id.trim()));
                }
            }
            return set;
        }
    }
}
