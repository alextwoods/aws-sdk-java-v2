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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Requirements 7.8, 7.9, 8.6 and 8.7: the generated paginator and waiter sources of a small synthetic service,
 * compared against the expected V2-shaped signatures. {@code GeneratedPaginatorWaiterTest} in {@code codegen-tests}
 * compiles and runs them.
 */
class PaginatorWaiterGenerationTest {
    private static final String ROOT = "software/amazon/awssdk/services/shelf/";

    private static final String MODEL = """
        $version: "2"
        namespace example.shelf

        use smithy.waiters#waitable

        @aws.protocols#awsJson1_0
        @auth([])
        service Shelf {
            version: "2024-01-01"
            operations: [ListBooks, GetBook]
        }

        @readonly
        @paginated(inputToken: "token", outputToken: "page.next", items: "page.books", pageSize: "limit")
        operation ListBooks {
            input := {
                token: String
                limit: Integer
            }
            output := {
                page: Page
            }
        }

        structure Page {
            next: String
            books: Books
        }

        list Books {
            member: Book
        }

        structure Book {
            title: String
        }

        @readonly
        @waitable(
            BookShelved: {
                minDelay: 3
                maxDelay: 30
                acceptors: [
                    { state: "success", matcher: { output: { path: "shelved", expected: "true",
                                                             comparator: "booleanEquals" } } }
                    { state: "retry", matcher: { errorType: "NoSuchBook" } }
                    { state: "failure", matcher: { success: false } }
                ]
            }
        )
        operation GetBook {
            input := { @required title: String }
            output := { shelved: Boolean }
            errors: [NoSuchBook]
        }

        @error("client")
        structure NoSuchBook {}
        """;

    @TempDir
    Path temp;

    @Test
    void theClientsExposeV2PaginatorAndWaiterEntryPoints() throws IOException {
        AwsCodegenResult result = generate(MODEL);
        String sync = read(result, "ShelfClient.java");
        assertContains(sync, "    default ListBooksIterable listBooksPaginator(ListBooksRequest request) {\n"
                             + "        return new ListBooksIterable(this, request);\n    }\n");
        assertContains(sync, "    default ListBooksIterable listBooksPaginator(Consumer<ListBooksRequest.Builder> "
                             + "request) {\n");
        assertContains(sync, "    default ShelfWaiter waiter() {\n"
                             + "        return ShelfWaiter.builder().client(this).build();\n    }\n");
        assertFalse(sync.contains("getBookPaginator"), sync);
        String async = read(result, "ShelfAsyncClient.java");
        assertContains(async, "    default ListBooksPublisher listBooksPaginator(ListBooksRequest request) {\n");
        assertContains(async, "    default ShelfAsyncWaiter waiter() {\n");
        // The default async client lends its own retry scheduler to the waiter.
        assertContains(read(result, "DefaultShelfAsyncClient.java"),
                       "parts.runtime().retryScheduler().ifPresent(builder::retryScheduler);");
    }

    @Test
    void theSyncPaginatorFollowsTheNestedModeledTokenAndItems() throws IOException {
        String iterable = read(generate(MODEL), "paginators/ListBooksIterable.java");
        assertContains(iterable, "public final class ListBooksIterable implements SdkIterable<ListBooksResponse> {");
        assertContains(iterable, "return client.listBooks(firstRequest.toBuilder().token("
                                 + "ListBooksIterable.nextToken(previousPage)).build());");
        assertContains(iterable, "return PaginationTokens.hasToken(ListBooksIterable.nextToken(previousPage));");
        assertContains(iterable, "    public SdkIterable<Book> books() {\n"
                                 + "        return flatMapIterable(ListBooksIterable::pageItems);\n    }\n");
        assertContains(iterable, "    static String nextToken(ListBooksResponse page) {\n"
                                 + "        if (page == null) {\n            return null;\n        }\n"
                                 + "        Page step1 = page.page();\n");
        assertContains(iterable, "        return step1.next();\n");
    }

    @Test
    void theAsyncPaginatorIsADemandDrivenPublisher() throws IOException {
        String publisher = read(generate(MODEL), "paginators/ListBooksPublisher.java");
        assertContains(publisher, "public final class ListBooksPublisher implements SdkPublisher<ListBooksResponse> {");
        assertContains(publisher, "PagePublisher.create(new AsyncPageFetcher<ListBooksResponse>() {");
        assertContains(publisher, "}).subscribe(subscriber);");
    }

    @Test
    void theWaitersCarryTheModeledAcceptorsAndDelays() throws IOException {
        AwsCodegenResult result = generate(MODEL);
        String api = read(result, "waiters/ShelfWaiter.java");
        assertContains(api, "public interface ShelfWaiter extends AutoCloseable {");
        assertContains(api, "    WaiterResponse<GetBookResponse> waitUntilBookShelved(GetBookRequest request, "
                            + "WaiterOverrideConfiguration overrideConfiguration);\n");
        assertContains(api, "    static Builder builder() {\n");
        assertContains(api, "        Builder client(ShelfClient client);\n");
        String implementation = read(result, "waiters/DefaultShelfWaiter.java");
        assertContains(implementation, "Waiter.of(\"BookShelved\", bookShelvedAcceptors(request), "
                                       + "Duration.ofSeconds(3), Duration.ofSeconds(30), overrides);");
        assertContains(implementation, "WaiterAcceptor.<GetBookResponse>onResponse(WaiterState.SUCCESS, response -> "
                                       + "WaiterMatchers.booleanEquals(");
        assertContains(implementation, "WaiterAcceptor.<GetBookResponse>onException(WaiterState.RETRY, exception -> "
                                       + "exception instanceof NoSuchBookException");
        assertContains(implementation, "WaiterAcceptor.<GetBookResponse>onException(WaiterState.FAILURE, "
                                       + "exception -> true)");
        // Ownership: only a client the waiter created is closed.
        assertContains(implementation, "        if (ownsClient) {\n            client.close();\n        }\n");
        String async = read(result, "waiters/DefaultShelfAsyncWaiter.java");
        assertContains(async, "return waiter.runAsync(() -> client.getBook(request), delays, overrideConfiguration);");
        assertContains(async, "Builder scheduledExecutorService(ScheduledExecutorService value)");
    }

    @Test
    void anAcceptorPathOutsideTheSupportedSubsetFailsGeneration() {
        String unsupported = MODEL.replace("path: \"shelved\"", "path: \"[shelved][0]\"");
        assertThrows(AwsCodegenException.class, () -> generate(unsupported));
    }

    /** EC2-style nested list projections ({@code Reservations[].Instances[].State.Name}) resolve every member. */
    @Test
    void aFlattenedNestedProjectionPathResolvesItsMembers() throws IOException {
        String nested = withSuccessPath("shelves[].books[].title", "done", "allStringEquals")
                            .replace("output := { shelved: Boolean }", "output := { shelves: Shelves }")
                        + """

                          list Shelves {
                              member: Bookcase
                          }

                          structure Bookcase {
                              books: Books
                          }
                          """;
        String implementation = read(generate(nested), "waiters/DefaultShelfWaiter.java");
        assertContains(implementation, "WaiterMatchers.project(WaiterMatchers.flatten(WaiterMatchers.project("
                                       + "WaiterMatchers.flatten(WaiterMatchers.field(response, GetBookResponse.class, "
                                       + "GetBookResponse::shelves)), element1 -> WaiterMatchers.field(element1, "
                                       + "Bookcase.class, Bookcase::books))), element1 -> WaiterMatchers.field("
                                       + "element1, Book.class, Book::title)),\n");
    }

    /** SES-style map value projections ({@code VerificationAttributes.*.VerificationStatus}). */
    @Test
    void aMapValueProjectionPathProjectsTheMapValues() throws IOException {
        String mapped = withSuccessPath("statuses.*.state", "done", "allStringEquals")
                            .replace("output := { shelved: Boolean }", "output := { statuses: Statuses }")
                        + """

                          map Statuses {
                              key: String
                              value: Status
                          }

                          structure Status {
                              state: String
                          }
                          """;
        String implementation = read(generate(mapped), "waiters/DefaultShelfWaiter.java");
        assertContains(implementation, "WaiterMatchers.project(WaiterMatchers.values(WaiterMatchers.field(response, "
                                       + "GetBookResponse.class, GetBookResponse::statuses)), element1 -> "
                                       + "WaiterMatchers.field(element1, Status.class, Status::state)),\n");
    }

    /** Auto Scaling-style {@code contains} over a projected multi-select list. */
    @Test
    void aContainsOverAProjectedMultiSelectListPathCompiles() throws IOException {
        String path = "contains(shelves[].[length(books[?title=='x']) >= minimum][], `false`)";
        String nested = withSuccessPath(path, "false", "booleanEquals")
                            .replace("output := { shelved: Boolean }", "output := { shelves: Shelves }")
                        + """

                          list Shelves {
                              member: Bookcase
                          }

                          structure Bookcase {
                              books: Books
                              minimum: Integer
                          }
                          """;
        String implementation = read(generate(nested), "waiters/DefaultShelfWaiter.java");
        // shelves[].[...][] parses as a projection of the flattened shelves onto the multi-select list, flattened
        // and projected again by the trailing [].
        assertContains(implementation, "WaiterMatchers.contains(WaiterMatchers.project(WaiterMatchers.flatten("
                                       + "WaiterMatchers.project(WaiterMatchers.flatten(WaiterMatchers.field(response, "
                                       + "GetBookResponse.class, GetBookResponse::shelves)), element1 -> "
                                       + "WaiterMatchers.multiSelect(element1, "
                                       + "element2 -> WaiterMatchers.list(WaiterMatchers.compare(WaiterMatchers.length("
                                       + "WaiterMatchers.project(WaiterMatchers.filter(WaiterMatchers.field(element2, "
                                       + "Bookcase.class, Bookcase::books), element3 -> WaiterMatchers.compare("
                                       + "WaiterMatchers.field(element3, Book.class, Book::title), \"==\", \"x\")), "
                                       + "element3 -> element3)), \">=\", WaiterMatchers.field(element2, "
                                       + "Bookcase.class, Bookcase::minimum)))))), element1 -> element1), false),\n");
    }

    @Test
    void aStructureValueProjectionPathIsOutsideTheSupportedSubset() {
        String unsupported = withSuccessPath("*.state", "done", "allStringEquals")
                                 .replace("output := { shelved: Boolean }", "output := { status: Status }")
                             + """

                               structure Status {
                                   state: String
                               }
                               """;
        AwsCodegenException e = assertThrows(AwsCodegenException.class, () -> generate(unsupported));
        assertTrue(e.getMessage().contains("ObjectProjectionExpression is outside the supported waiter subset"),
                   e.getMessage());
    }

    /** {@link #MODEL} with the success acceptor of {@code BookShelved} matching {@code path} instead. */
    private static String withSuccessPath(String path, String expected, String comparator) {
        String model = MODEL.replaceFirst("path: \"shelved\", expected: \"true\",\\s+comparator: \"booleanEquals\"",
                                          "path: \"" + path + "\", expected: \"" + expected + "\", comparator: \""
                                          + comparator + "\"");
        assertTrue(model.contains(path), model);
        return model;
    }

    @Test
    void aServiceWithoutPaginationOrWaitersGetsNeither() throws IOException {
        String paged = MODEL.replaceAll("@paginated\\([^)]*\\)", "");
        String plain = paged.substring(0, paged.indexOf("@waitable("))
                       + paged.substring(paged.indexOf("operation GetBook"));
        AwsCodegenResult result = generate(plain);
        List<Path> files = result.generatedFiles();
        assertFalse(files.stream().anyMatch(p -> p.toString().contains("paginators")
                                                  || p.toString().contains("waiters")), files.toString());
        assertFalse(read(result, "ShelfClient.java").contains("waiter()"));
    }

    private AwsCodegenResult generate(String model) throws IOException {
        Path models = Files.createDirectories(temp.resolve("model"));
        Files.writeString(models.resolve("shelf.smithy"), model, StandardCharsets.UTF_8);
        return AwsCodegen.generate(new AwsCodegenRequest("example.shelf#Shelf", "software.amazon.awssdk.services.shelf",
                                                         models, temp.resolve("out-" + System.nanoTime())),
                                   List.of());
    }

    private static String read(AwsCodegenResult result, String name) throws IOException {
        return Files.readString(result.sourceRoot().resolve(ROOT + name));
    }

    private static void assertContains(String source, String expected) {
        assertTrue(source.contains(expected), () -> "Expected\n" + expected + "\nin\n" + source);
    }
}
