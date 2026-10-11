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

package software.amazon.awssdk.codegen.c2j2smithy;

import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The {@code com.amazonaws.c2j} marker traits that the converter emits for C2J information with no standard Smithy or
 * AWS trait. Each marker is defined in the bundled {@code META-INF/smithy/c2j-markers.smithy}, so a converted model
 * validates when it is assembled with this module on the class path.
 *
 * <p>Every time the converter emits a marker it also records a {@link ConversionGapReport} entry for the marker's
 * {@link #c2jProperty()}, with the marker's {@link #impact()}. C2J information that has a standard trait is emitted
 * only as that trait; no marker duplicates it.
 */
public enum C2jMarkerTrait {
    METADATA("metadata", "metadata",
             "C2J metadata properties with no standard trait (for example globalEndpoint, checksumFormat, "
             + "resultWrapped, or a targetPrefix that cannot be the service shape name) are only available from this "
             + "marker, which holds just those properties."),
    REQUIRED("required", "shape.required",
             "The declaration order of required members. @required is also emitted, so only the order is marker-only."),
    OPERATION_NAME("operationName", "operation.name",
                   "The wire action name, when it differs from the operations-map key or the operation shape was "
                   + "renamed to avoid a collision with a shape name, is only available from this marker."),
    ERRORS("errors", "operation.errors",
           "The declared order and duplicates of operation errors. Operation errors are also emitted, so only the order "
           + "and duplicates are marker-only."),
    HTTP("http", "operation.http",
         "The verbatim method, requestUri and responseCode of an operation with no @http trait (RPC protocols ignore "
         + "HTTP bindings)."),
    HOST_PREFIX("hostPrefix", "operation.endpoint.hostPrefix",
                "An endpoint host prefix whose labels cannot be bound with @hostLabel (the member is missing, not "
                + "required, or not a string) is only available from this marker; the operation has no @endpoint."),
    AUTHTYPE("authtype", "operation.authtype",
             "A per-operation authtype with no standard auth trait (for example custom) is only available from this "
             + "marker."),
    RESULT_WRAPPER("resultWrapper", "operation.output.resultWrapper",
                   "An awsQuery result wrapper element name other than <OperationName>Result is only available from "
                   + "this marker."),
    OUTPUT_DOC("outputDoc", "operation.output.documentation",
               "Documentation on the operation output reference (return type Javadoc) is only available from this "
               + "marker."),
    WRAPPER("wrapper", "shape.wrapper",
            "Result wrapping of a query or rest-xml structure is only available from this marker."),
    SYNTHETIC("synthetic", "shape.synthetic",
              "The SDK-synthesized shape flag is only available from this marker."),
    INPUT_META("inputMeta", "operation.input",
               "The input reference documentation is only available from this marker, and so are the rest-xml "
               + "request root element name and xmlNamespace when they cannot go on the input structure as @xmlName "
               + "and @xmlNamespace (operations sharing the structure disagree, or another shape targets it)."),
    LOCATION_NAME("locationName", "member.locationName",
                  "The C2J URI label name of a member whose label the converter renamed to the member name. The wire "
                  + "is unchanged; only the original label name is marker-only."),
    SHAPE_NAME("shapeName", "shape.name",
               "The C2J shape name of a shape the converter renamed (a case-insensitive shape name collision) or "
               + "synthesized (a copy of a shared target for @mediaType). Generated names and customizations keyed by "
               + "the C2J name only see it on this marker."),
    RESERVED_HEADER("reservedHeader", "member.location.header(reserved)",
                    "A member bound to a header Smithy forbids binding (for example Content-Length) is unbound. The "
                    + "header name is only available from this marker, so the member is not sent as that header."),
    DEPRECATED_NAME("deprecatedName", "member.deprecatedName",
                    "The deprecated alternate name of a member (deprecated getter and setter) is only available from "
                    + "this marker."),
    ALTERNATE_BEAN_PROPERTY_NAME("alternateBeanPropertyName", "member.alternateBeanPropertyName",
                                 "The alternate bean property name of a member is only available from this marker."),
    AUTHORIZERS("authorizers", "authorizers",
                "API Gateway custom authorizer definitions are only available from this marker."),
    AUTHORIZER("authorizer", "operation.authorizer",
               "The custom authorizer an operation uses is only available from this marker.");

    /**
     * The namespace of every marker trait.
     */
    public static final String NAMESPACE = "com.amazonaws.c2j";

    private final ShapeId traitId;
    private final String c2jProperty;
    private final String impact;

    C2jMarkerTrait(String traitName, String c2jProperty, String impact) {
        this.traitId = ShapeId.fromParts(NAMESPACE, traitName);
        this.c2jProperty = c2jProperty;
        this.impact = impact;
    }

    /**
     * The ShapeId of the marker trait, for example {@code com.amazonaws.c2j#operationName}.
     */
    public ShapeId traitId() {
        return traitId;
    }

    /**
     * The C2J property the marker carries, for example {@code operation.name}.
     */
    public String c2jProperty() {
        return c2jProperty;
    }

    /**
     * The generated-code impact of the property having no standard Smithy or AWS trait.
     */
    public String impact() {
        return impact;
    }
}
