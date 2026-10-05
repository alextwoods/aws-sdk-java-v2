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

package software.amazon.awssdk.protocols.restxml;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.protocols.core.ProtocolContext;
import software.amazon.awssdk.protocols.core.binding.HttpMessageBindings;
import software.amazon.awssdk.protocols.core.error.ModeledErrors;
import software.amazon.awssdk.protocols.core.serde.HttpBindingReader;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;

/**
 * The per-client {@code restXml} runtime. Wire rules (Smithy {@code aws.protocols#restXml}):
 * <ul>
 *   <li>the request method, path, query and headers follow the Smithy HTTP binding traits
 *       ({@link software.amazon.awssdk.protocols.core.serde.HttpBindingWriter}); responses and errors are read with
 *       {@link HttpBindingReader};</li>
 *   <li>the request body is an XML document of the body members under the input's root element (its
 *       {@code xmlName}, else its shape name), sent whenever the input has body members and absent otherwise; the
 *       service's {@code xmlNamespace} is the root's default namespace;</li>
 *   <li>an {@code httpPayload} member is the whole body: a structure or union as the payload member's element (no
 *       body when absent), a blob as its raw bytes and a string or enum as its text (no body when absent);</li>
 *   <li>{@code Content-Type} is set only when there is a body, unless an {@code httpHeader("Content-Type")} member
 *       supplies it: {@code application/xml} for XML, else the payload target's {@code mediaType},
 *       {@code application/octet-stream} for blobs or {@code text/plain} for strings;</li>
 *   <li>response body members are the children of the body's root element (whatever it is named); a structure or
 *       union payload is the root element itself; a blank body has no body members;</li>
 *   <li>errors are {@code <ErrorResponse><Error>...</Error><RequestId/></ErrorResponse>}, or with
 *       {@code noErrorWrapping} (S3) a bare {@code <Error>...<RequestId/></Error>}; either form is read whatever the
 *       option, as S3-compatible endpoints and proxies are not consistent. The error
 *       code is the {@code <Code>}; the modeled error's members are the {@code <Error>} element's children plus its
 *       HTTP-bound members. The request ID is the envelope's {@code RequestId}, else the {@code x-amz-request-id}
 *       (or {@code x-amzn-RequestId}) header; the extended request ID is {@code x-amz-id-2}. A blank, non-XML or
 *       non-envelope body is a generic error.</li>
 * </ul>
 */
final class RestXmlClientProtocol implements ClientProtocol {
    private final ProtocolContext context;
    private final ModeledErrors errors;
    private final XmlNamespaceTrait serviceNamespace;
    private final Map<ShapeId, HttpBindingReader> registryErrorReaders = new ConcurrentHashMap<>();

    RestXmlClientProtocol(ProtocolContext context) {
        this.context = context;
        this.errors = ModeledErrors.of(context);
        this.serviceNamespace = context.service().xmlNamespace().orElse(null);
    }

    @Override
    public ShapeId protocolId() {
        return context.selected().protocolId();
    }

    @Override
    public OperationProtocol operation(OperationSchemas operation) {
        return new RestXmlOperationProtocol(this, operation);
    }

    ProtocolContext context() {
        return context;
    }

    ModeledErrors errors() {
        return errors;
    }

    /** The service's {@code xmlNamespace}, the default namespace of request documents; {@code null} if none. */
    XmlNamespaceTrait serviceNamespace() {
        return serviceNamespace;
    }

    /** The reader of a modeled error that the operation does not declare (resolved through the registry). */
    HttpBindingReader registryErrorReader(Schema error) {
        return registryErrorReaders.computeIfAbsent(
            error.id(), id -> HttpBindingReader.of(HttpMessageBindings.of(error, HttpMessageBindings.Direction.RESPONSE)));
    }
}
