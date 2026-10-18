// SDK-owned awsQuery service for the hand-written protocol tests that cannot be Smithy test cases (task 7.21): Smithy
// rejects enum values the model does not define in test params, so UnknownEnumValueProtocolTest calls this service
// through ProtocolTestHarness.respondWith (legacy-disposition.csv rows json-core-output.json#5, #7, #9 and #10). It has
// no Smithy cases.
$version: "2.0"

namespace software.amazon.awssdk.protocoltests.query

use aws.api#service
use aws.auth#sigv4
use aws.protocols#awsQuery

@service(sdkId: "Sdk Query Protocol")
@sigv4(name: "sdkqueryprotocol")
@awsQuery
@xmlNamespace(uri: "https://example.com/")
@title("SDK Query Protocol Test Service")
service SdkQuery {
    version: "2020-01-08"
    operations: [
        SdkEnums
    ]
}

/// Enum values in a member, a list element, a map key and a map value.
operation SdkEnums {
    output: SdkEnumsOutput
}

@output
structure SdkEnumsOutput {
    enumMember: SdkEnum
    enumList: SdkEnumList
    enumKeyMap: SdkEnumKeyMap
    enumValueMap: SdkEnumValueMap
}

enum SdkEnum {
    ENUM_VALUE_1 = "EnumValue1"
    ENUM_VALUE_2 = "EnumValue2"
}

list SdkEnumList {
    member: SdkEnum
}

map SdkEnumKeyMap {
    key: SdkEnum
    value: String
}

map SdkEnumValueMap {
    key: String
    value: SdkEnum
}
