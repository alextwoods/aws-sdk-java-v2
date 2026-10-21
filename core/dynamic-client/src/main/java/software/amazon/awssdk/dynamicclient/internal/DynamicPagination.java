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

package software.amazon.awssdk.dynamicclient.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.util.PaginatorUtils;
import software.amazon.awssdk.dynamicclient.schema.StructDocument;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.PaginatedTrait;

/**
 * The page-to-page step of a dynamic paginator, from the operation's {@code smithy.api#paginated} trait merged with the
 * service's: reads the output token of a page and puts it into the input token member of the next request, as a
 * generated paginator's page fetcher does with the POJO getters and the request builder.
 *
 * <p>As in generated paginators, there is a next page when the output token is set and, for a string, list or map,
 * not empty ({@link PaginatorUtils#isOutputTokenAvailable}). The output token may be a path of member names separated
 * by dots, for example {@code Meta.NextToken}.
 */
@SdkInternalApi
@ThreadSafe
public final class DynamicPagination {
    private final ApiOperation<?, ?> operation;
    private final String inputToken;
    private final String[] outputTokenPath;

    private DynamicPagination(ApiOperation<?, ?> operation, String inputToken, String outputToken) {
        this.operation = operation;
        this.inputToken = inputToken;
        this.outputTokenPath = outputToken.split("\\.");
    }

    /**
     * Creates the pagination of an operation.
     *
     * @param operation the operation
     * @param paginated its {@code paginated} trait merged with the service's, or null
     * @return the pagination
     * @throws IllegalArgumentException if the operation is not paginated, or its merged trait has no input or output
     *     token (generated clients have no paginator for such an operation either)
     */
    public static DynamicPagination of(ApiOperation<?, ?> operation, PaginatedTrait paginated) {
        if (paginated == null || !paginated.getInputToken().isPresent() || !paginated.getOutputToken().isPresent()) {
            throw new IllegalArgumentException("Operation " + operation.schema().id() + " is not paginated: it needs a "
                                               + "smithy.api#paginated trait with an inputToken and an outputToken, set on "
                                               + "the operation or inherited from the service. Call it with call(...) "
                                               + "instead.");
        }
        return new DynamicPagination(operation, paginated.getInputToken().get(), paginated.getOutputToken().get());
    }

    /**
     * Whether there is a page after this one.
     *
     * @param page an output document
     * @return true if the page's output token is set and not empty
     */
    public boolean hasNextPage(Document page) {
        Document token = outputToken(page);
        return PaginatorUtils.isOutputTokenAvailable(token == null ? null : token.asObject());
    }

    /**
     * The input of the request for the page after {@code page}: the first request's input with the page's output token
     * in the input token member.
     *
     * @param firstInput the input of the first request, already coerced to the input schema
     * @param page the previous page, whose output token is set
     * @return the next input, coerced to the input schema
     */
    public StructDocument nextInput(StructDocument firstInput, Document page) {
        Map<String, Document> members = new LinkedHashMap<>(firstInput.asMap());
        members.put(inputToken, outputToken(page));
        return StructDocument.coerce(operation.inputSchema(), Document.of(members), operation.service().schema().id());
    }

    private Document outputToken(Document page) {
        Document current = page;
        for (String name : outputTokenPath) {
            if (current == null || current.type() != ShapeType.STRUCTURE) {
                return null;
            }
            current = current.getMember(name);
        }
        return current;
    }
}
