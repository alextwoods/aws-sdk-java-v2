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

import java.util.List;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.WriterDelegator;

/**
 * Emits a service's paginators from a {@link PaginatorPlan} (design "Paginators, waiters, presigners, and
 * utilities"; Requirements 7.8, 8.6).
 *
 * <ul>
 *   <li>{@code <Operation>Iterable}: a lazy {@code SdkIterable} of response pages over the exact client that created
 *       it. Each iterator calls the operation for the first page on the first pull, then with the previous page's
 *       output token while that token is present and non-empty. With modeled items, an items method flattens the
 *       pages' items lazily.</li>
 *   <li>{@code <Operation>Publisher}: an {@code SdkPublisher} of response pages that calls for one page per unit of
 *       downstream demand, with at most one call in flight, and cancels the in-flight call on cancellation.</li>
 * </ul>
 */
final class PaginatorGenerator {
    private static final Symbol OBJECTS = jdk("java.util", "Objects");
    private static final Symbol ITERATOR = jdk("java.util", "Iterator");
    private static final Symbol COMPLETABLE_FUTURE = jdk("java.util.concurrent", "CompletableFuture");
    private static final Symbol SUBSCRIBER = jdk("org.reactivestreams", "Subscriber");
    private static final Symbol SDK_ITERABLE = jdk("software.amazon.awssdk.core.pagination.sync", "SdkIterable");
    private static final Symbol SYNC_FETCHER = jdk("software.amazon.awssdk.core.pagination.sync", "SyncPageFetcher");
    private static final Symbol SDK_PUBLISHER = jdk("software.amazon.awssdk.core.async", "SdkPublisher");
    private static final Symbol ASYNC_FETCHER = jdk("software.amazon.awssdk.core.pagination.async",
                                                    "AsyncPageFetcher");
    private static final Symbol PAGE_PUBLISHER = jdk("software.amazon.awssdk.core.pagination.async",
                                                     "PagePublisher");
    private static final Symbol TOKENS = jdk("software.amazon.awssdk.core.pagination", "PaginationTokens");

    private PaginatorGenerator() {
    }

    static void generate(AwsCodegenContext context, ClientPlan client) {
        render(PaginatorPlan.of(context, client), context.writerDelegator());
    }

    static void render(PaginatorPlan plan, WriterDelegator<AwsJavaWriter> writers) {
        for (PaginatorPlan.Paginator paginator : plan.paginators()) {
            renderIterable(plan, paginator, writers);
            if (paginator.publisher() != null) {
                renderPublisher(plan, paginator, writers);
            }
        }
    }

    private static void renderIterable(PaginatorPlan plan, PaginatorPlan.Paginator paginator,
                                       WriterDelegator<AwsJavaWriter> writers) {
        Symbol type = paginator.iterable();
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/**");
            writer.write(" * The pages of {@code $L}, fetched lazily from the client that created this iterable.",
                         paginator.operationName());
            writer.write(" *");
            writer.write(" * <p>Each iterator calls the operation for the first page on its first pull, then for "
                         + "the next page with the");
            writer.write(" * previous page's modeled output token, and stops when that token is absent or empty. "
                         + "Every page is one");
            writer.write(" * call on the same client, with the same configuration; the iterable creates no client.");
            writer.write(" */");
            writer.openBlock("public final class $L implements $T<$T> {", "}", type.getName(), SDK_ITERABLE,
                             paginator.output(), () -> {
                    writer.write("private final $T client;", plan.syncClient());
                    writer.write("private final $T firstRequest;", paginator.input());
                    writer.write("");
                    writer.write("/** The pages of {@code firstRequest} on {@code client}. */");
                    writer.openBlock("public $L($T client, $T firstRequest) {", "}", type.getName(),
                                     plan.syncClient(), paginator.input(), () -> {
                            writer.write("this.client = $T.requireNonNull(client, \"client\");", OBJECTS);
                            writer.write("this.firstRequest = $T.requireNonNull(firstRequest, \"firstRequest\");",
                                         OBJECTS);
                        });
                    writer.write("");
                    writer.write("@Override");
                    writer.write("public $T<$T> iterator() {", ITERATOR, paginator.output());
                    writer.indent();
                    writer.write("return $T.pages(new $T<$T>() {", SDK_ITERABLE, SYNC_FETCHER, paginator.output());
                    writer.indent();
                    writeHasNextPage(writer, paginator);
                    writer.write("");
                    writer.write("@Override");
                    writer.openBlock("public $T nextPage($T previousPage) {", "}", paginator.output(),
                                     paginator.output(), () -> writeNextPage(writer, paginator));
                    writer.dedent();
                    writer.write("}).iterator();");
                    writer.dedent();
                    writer.write("}");
                    if (paginator.itemsMethod() != null) {
                        writer.write("");
                        writer.write("/** The items of every page, flattened lazily in page order. */");
                        writer.openBlock("public $T<$T> $L() {", "}", SDK_ITERABLE, paginator.itemType(),
                                         paginator.itemsMethod(),
                                         () -> writer.write("return flatMapIterable($L::pageItems);", type.getName()));
                        writer.write("");
                        writer.write("/** The items of {@code page}, or {@code null}. */");
                        writer.openBlock("static $T<$T> pageItems($T page) {", "}", RuntimeSymbols.LIST,
                                         paginator.itemType(), paginator.output(),
                                         () -> writePath(writer, paginator.items()));
                    }
                    writer.write("");
                    writer.write("/** The modeled output token of {@code page}, or {@code null}. */");
                    writer.openBlock("static $T nextToken($T page) {", "}", paginator.tokenType(),
                                     paginator.output(), () -> writePath(writer, paginator.outputToken()));
                });
        });
    }

    private static void renderPublisher(PaginatorPlan plan, PaginatorPlan.Paginator paginator,
                                        WriterDelegator<AwsJavaWriter> writers) {
        Symbol type = paginator.publisher();
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/**");
            writer.write(" * The pages of {@code $L}, fetched on demand from the client that created this publisher.",
                         paginator.operationName());
            writer.write(" *");
            writer.write(" * <p>Each subscription calls for no page until downstream demand is positive, calls for "
                         + "one page per unit of");
            writer.write(" * demand with at most one call in flight, never prefetches, and on cancellation cancels "
                         + "its in-flight call");
            writer.write(" * and calls no further. Pages follow the modeled output token, as {@link $T} does.",
                         paginator.iterable());
            writer.write(" */");
            writer.openBlock("public final class $L implements $T<$T> {", "}", type.getName(), SDK_PUBLISHER,
                             paginator.output(), () -> {
                    writer.write("private final $T client;", plan.asyncClient());
                    writer.write("private final $T firstRequest;", paginator.input());
                    writer.write("");
                    writer.write("/** The pages of {@code firstRequest} on {@code client}. */");
                    writer.openBlock("public $L($T client, $T firstRequest) {", "}", type.getName(),
                                     plan.asyncClient(), paginator.input(), () -> {
                            writer.write("this.client = $T.requireNonNull(client, \"client\");", OBJECTS);
                            writer.write("this.firstRequest = $T.requireNonNull(firstRequest, \"firstRequest\");",
                                         OBJECTS);
                        });
                    writer.write("");
                    writer.write("@Override");
                    writer.write("public void subscribe($T<? super $T> subscriber) {", SUBSCRIBER,
                                 paginator.output());
                    writer.indent();
                    writer.write("$T.create(new $T<$T>() {", PAGE_PUBLISHER, ASYNC_FETCHER, paginator.output());
                    writer.indent();
                    writeHasNextPage(writer, paginator);
                    writer.write("");
                    writer.write("@Override");
                    writer.openBlock("public $T<$T> nextPage($T previousPage) {", "}", COMPLETABLE_FUTURE,
                                     paginator.output(), paginator.output(),
                                     () -> writeNextPage(writer, paginator));
                    writer.dedent();
                    writer.write("}).subscribe(subscriber);");
                    writer.dedent();
                    writer.write("}");
                });
        });
    }

    private static void writeHasNextPage(AwsJavaWriter writer, PaginatorPlan.Paginator paginator) {
        writer.write("@Override");
        writer.openBlock("public boolean hasNextPage($T previousPage) {", "}", paginator.output(),
                         () -> writer.write("return $T.hasToken($T.nextToken(previousPage));", TOKENS,
                                            paginator.iterable()));
    }

    private static void writeNextPage(AwsJavaWriter writer, PaginatorPlan.Paginator paginator) {
        writer.openBlock("if (previousPage == null) {", "}",
                         () -> writer.write("return client.$L(firstRequest);", paginator.operationMethod()));
        writer.write("return client.$L(firstRequest.toBuilder().$L($T.nextToken(previousPage)).build());",
                     paginator.operationMethod(), paginator.inputTokenSetter(), paginator.iterable());
    }

    /** A null-propagating getter chain from {@code page}. */
    private static void writePath(AwsJavaWriter writer, List<PaginatorPlan.Step> path) {
        writer.openBlock("if (page == null) {", "}", () -> writer.write("return null;"));
        String current = "page";
        for (int i = 0; i < path.size() - 1; i++) {
            PaginatorPlan.Step step = path.get(i);
            String next = "step" + (i + 1);
            writer.write("$T $L = $L.$L();", step.type(), next, current, step.getter());
            writer.openBlock("if ($L == null) {", "}", next, () -> writer.write("return null;"));
            current = next;
        }
        writer.write("return $L.$L();", current, path.get(path.size() - 1).getter());
    }

    private static Symbol jdk(String packageName, String name) {
        return Symbol.builder().namespace(packageName, ".").name(name).build();
    }
}
