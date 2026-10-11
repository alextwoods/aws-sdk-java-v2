// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//
// Trait definitions for the com.amazonaws.c2j marker traits emitted by
// software.amazon.awssdk.codegen.c2j2smithy.C2jToSmithyConverter (see C2jMarkerTrait).
//
// Markers carry C2J information that has no standard Smithy or AWS trait. Every marker the converter emits is
// also recorded in the conversion gap report. These definitions let a converted model validate when it is
// assembled with ModelAssembler#discoverModels and this module on the class path.
$version: "2.0"

namespace com.amazonaws.c2j

// ----- service -----

/// The C2J metadata properties that no standard trait carries.
@trait(selector: "service")
document metadata

/// The verbatim C2J top-level authorizers block (API Gateway custom authorizers).
@trait(selector: "service")
document authorizers

// ----- operations -----

/// The C2J operation name field, when it differs from the operation shape name.
@trait(selector: "operation")
string operationName

/// The verbatim C2J operation errors array, in declared order with duplicates.
@trait(selector: "operation")
document errors

/// The verbatim C2J operation http block (method, requestUri, responseCode), for operations with no @http trait.
@trait(selector: "operation")
document http

/// A C2J operation endpoint.hostPrefix that could not be converted to @endpoint and @hostLabel.
@trait(selector: "operation")
string hostPrefix

/// A C2J operation authtype with no standard auth trait.
@trait(selector: "operation")
string authtype

/// The C2J operation output.resultWrapper (awsQuery), when it is not <OperationName>Result.
@trait(selector: "operation")
string resultWrapper

/// The documentation on the C2J operation output reference.
@trait(selector: "operation")
string outputDoc

/// The verbatim C2J operation input reference (locationName, xmlNamespace, documentation).
@trait(selector: "operation")
document inputMeta

/// The C2J operation authorizer name.
@trait(selector: "operation")
string authorizer

// ----- shapes -----

/// The verbatim C2J structure required array, in declaration order.
@trait(selector: ":not(:is(member, operation, resource, service))")
list required {
    member: String
}

/// Marks a C2J structure with wrapper: true.
@trait(selector: ":not(:is(member, operation, resource, service))")
boolean wrapper

/// Marks a C2J structure with synthetic: true.
@trait(selector: ":not(:is(member, operation, resource, service))")
boolean synthetic

/// The C2J name of a shape the converter renamed or synthesized.
@trait(selector: ":not(:is(member, operation, resource, service))")
string shapeName

// ----- members -----

/// The C2J locationName of a URI label member whose label was renamed to the member name.
@trait(selector: "member")
string locationName

/// The name of a reserved header the member was bound to in C2J. The member is unbound.
@trait(selector: "member")
string reservedHeader

/// The C2J deprecatedName of a member.
@trait(selector: "member")
string deprecatedName

/// The C2J alternateBeanPropertyName of a member.
@trait(selector: "member")
string alternateBeanPropertyName
