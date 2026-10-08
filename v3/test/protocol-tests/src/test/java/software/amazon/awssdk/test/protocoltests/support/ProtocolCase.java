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

package software.amazon.awssdk.test.protocoltests.support;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.protocoltests.traits.HttpMessageTestCase;
import software.amazon.smithy.protocoltests.traits.eventstream.EventStreamTestCase;

/**
 * One case of the pinned Smithy protocol test models, bound to the service and operation it is declared for.
 *
 * @param kind the kind of test trait that declares the case
 * @param service the test service
 * @param protocol the protocol the case targets
 * @param operation the operation the case runs through (for error cases, an operation that binds the error)
 * @param error the error structure of an error case, else {@code null}
 * @param id the case ID
 * @param http the HTTP request, response or malformed-request case, else {@code null}
 * @param eventStream the event stream case, else {@code null}
 * @param notApplicable why the case does not apply to a V3 client, or {@code null} if it applies
 */
public record ProtocolCase(Kind kind, ShapeId service, ShapeId protocol, ShapeId operation, ShapeId error, String id,
                           HttpMessageTestCase http, EventStreamTestCase eventStream, String notApplicable) {

    /** The test traits that declare protocol cases. */
    public enum Kind {
        REQUEST("request"),
        RESPONSE("response"),
        ERROR("error"),
        MALFORMED_REQUEST("malformed-request"),
        EVENT_STREAM("event-stream");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public ProtocolCase {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(protocol, "protocol");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(id, "id");
    }

    /**
     * The stable identity of the case, used in the approved-omission list and the report:
     * {@code <kind> <service> <operation> [<error>] <case ID>}.
     */
    public String key() {
        return kind.label() + " " + service.getName() + " " + operation.getName() + " "
               + (error == null ? "" : error.getName() + " ") + id;
    }

    /** The short protocol name, for example {@code restJson1}. */
    public String protocolName() {
        return protocol.getName();
    }

    public boolean applicable() {
        return notApplicable == null;
    }

    public Optional<HttpMessageTestCase> httpCase() {
        return Optional.ofNullable(http);
    }

    @Override
    public String toString() {
        return key();
    }
}
