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

package software.amazon.awssdk.dynamic.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.core.pagination.PaginationTokens;
import software.amazon.awssdk.dynamic.model.DynamicPagination;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.dynamicschemas.StructDocument;

/**
 * The modeled-token logic dynamic paginators share, over the sdk-core page fetchers generated paginators use: a page
 * follows while the previous page's output token is present and non-empty ({@link PaginationTokens#hasToken}), and
 * each next page is the first input with the input token set to that output token, exactly as a generated
 * {@code <Operation>Iterable}/{@code <Operation>Publisher} computes it with its getter chain and
 * {@code toBuilder()}.
 */
final class DynamicPaginators {

    private DynamicPaginators() {
    }

    /**
     * A paginated operation's first page call and its pagination.
     *
     * @param pagination the modeled pagination
     * @param first the prepared first page call
     */
    record Paged(DynamicPagination pagination, DynamicCalls.Call first) {
        Paged {
            Objects.requireNonNull(pagination, "pagination");
            Objects.requireNonNull(first, "first");
        }

        /** Whether a page follows {@code previous}, by its modeled output token. */
        boolean hasNextPage(Document previous) {
            Document token = path(previous, pagination.outputToken());
            return PaginationTokens.hasToken(token == null ? null : token.asObject());
        }

        /** The input of the page after {@code previous}, or the first input when {@code previous} is {@code null}. */
        StructDocument input(Document previous) {
            StructDocument input = first.input();
            if (previous == null) {
                return input;
            }
            Map<String, Document> members = new LinkedHashMap<>(input.asStringMap());
            members.put(pagination.inputToken(), path(previous, pagination.outputToken()));
            return StructDocument.of(input.schema(), Document.of(members), first.operation().service());
        }

        /** The modeled items of {@code page}; empty when absent. */
        List<Document> items(Document page) {
            Document items = path(page, pagination.items());
            return items == null ? List.of() : items.asList();
        }
    }

    /** The member at {@code path} from {@code value}, null-propagating. */
    private static Document path(Document value, List<String> path) {
        Document current = value;
        for (String member : path) {
            if (current == null) {
                return null;
            }
            current = current.getMember(member);
        }
        return current;
    }
}
