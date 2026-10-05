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

package software.amazon.awssdk.dynamic.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.ShapeId;

/** Modeled pagination and waiters of a runtime service, as aws-codegen reads them (Requirement 14.15). */
class DynamicPaginationWaiterTest {

    private static final String MODEL = """
        $version: "2"
        namespace example.paged

        use smithy.waiters#waitable

        @aws.protocols#awsJson1_1
        @paginated(inputToken: "token", outputToken: "page.next", pageSize: "limit")
        service Paged {
            operations: [ListThings, ScanThings, GetThing, NoToken]
        }

        @paginated(items: "page.things")
        operation ListThings {
            input := { token: String, limit: Integer }
            output := { page: Page }
        }

        @paginated(inputToken: "start", outputToken: "last", items: "rows")
        operation ScanThings {
            input := { start: String, limit: Integer }
            output := { last: String, rows: Rows }
        }

        @waitable(
            ThingReady: {
                minDelay: 1
                maxDelay: 30
                acceptors: [{ state: "success", matcher: { output: { path: "state", expected: "READY", comparator: "stringEquals" } } }]
            }
            ThingGone: {
                acceptors: [{ state: "success", matcher: { errorType: "NotFound" } }]
            }
        )
        operation GetThing {
            input := { id: String }
            output := { state: String }
            errors: [NotFound]
        }

        operation NoToken {}

        structure Page {
            next: String
            things: Rows
        }

        list Rows {
            member: String
        }

        @error("client")
        structure NotFound {}
        """;

    private static DynamicService service() {
        return TestModels.load(MODEL).service();
    }

    @Test
    void paginatedOperationsMergeServiceDefaultsLikeTheGenerator() {
        DynamicService service = service();
        assertEquals(List.of("ListThings", "ScanThings"), service.paginatedOperationNames());

        DynamicPagination list = service.pagination("ListThings");
        assertEquals("token", list.inputToken());
        assertEquals(List.of("page", "next"), list.outputToken());
        assertEquals(List.of("page", "things"), list.items());
        assertEquals(Optional.of("limit"), list.pageSizeMember());

        DynamicPagination scan = service.pagination("ScanThings");
        assertEquals("start", scan.inputToken());
        assertEquals(List.of("last"), scan.outputToken());
        assertEquals(List.of("rows"), scan.items());
        assertEquals(Optional.of("limit"), scan.pageSizeMember());
        assertEquals(ShapeId.from("example.paged#Paged"), service.id());
    }

    @Test
    void unpaginatedAndUnknownOperationsAreRejectedWithLexicalDiagnostics() {
        DynamicService service = service();
        DynamicModelException notPaginated = assertThrows(DynamicModelException.class,
                                                          () -> service.pagination("GetThing"));
        assertEquals(DynamicModelException.Reason.NOT_PAGINATED, notPaginated.reason());
        assertEquals(Optional.of("GetThing"), notPaginated.operationName());
        assertEquals(List.of("ListThings", "ScanThings"), notPaginated.available());
        assertTrue(notPaginated.getMessage().contains("ListThings, ScanThings"), notPaginated.getMessage());

        DynamicModelException unknown = assertThrows(DynamicModelException.class, () -> service.pagination("List"));
        assertEquals(DynamicModelException.Reason.UNKNOWN_OPERATION, unknown.reason());
    }

    @Test
    void waitersAreFoundByExactNameWithModeledAcceptorsAndDelays() {
        DynamicService service = service();
        assertEquals(List.of("ThingGone", "ThingReady"), service.waiterNames());
        DynamicWaiterDefinition ready = service.waiter("ThingReady");
        assertEquals("GetThing", ready.operationName());
        assertEquals(1, ready.waiter().getMinDelay());
        assertEquals(30, ready.waiter().getMaxDelay());
        assertEquals(1, ready.waiter().getAcceptors().size());
        assertEquals(2, service.waiter("ThingGone").waiter().getMinDelay(), "the Smithy default minDelay");

        DynamicModelException unknown = assertThrows(DynamicModelException.class, () -> service.waiter("thingReady"));
        assertEquals(DynamicModelException.Reason.UNKNOWN_WAITER, unknown.reason());
        assertEquals(Optional.of("thingReady"), unknown.waiterName());
        assertEquals(Optional.of(ShapeId.from("example.paged#Paged")), unknown.service());
        assertEquals(List.of("ThingGone", "ThingReady"), unknown.available());
        assertTrue(unknown.getMessage().contains("'thingReady'") && unknown.getMessage().contains("ThingGone, ThingReady"),
                   unknown.getMessage());
    }
}
