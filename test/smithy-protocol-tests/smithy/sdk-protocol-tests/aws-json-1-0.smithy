// SDK-owned awsJson1_0 service for the hand-written protocol tests that cannot be Smithy test cases (task 7.21):
// UnknownEnumValueProtocolTest (Smithy rejects enum values the model does not define in test params; legacy-disposition.csv
// rows json-core-output.json#5, #7, #9 and #10) and NullDocumentProtocolTest (a null params value is unset; row
// json-core-output.json#35) call this service through ProtocolTestHarness.respondWith. It has no Smithy cases.
$version: "2.0"

namespace software.amazon.awssdk.protocoltests.json10

use aws.api#service
use aws.auth#sigv4
use aws.protocols#awsJson1_0

@service(sdkId: "Sdk Json Rpc 10")
@sigv4(name: "sdkjsonrpc10")
@awsJson1_0
@title("SDK Json 1.0 Protocol Test Service")
service SdkJsonRpc10 {
    version: "2020-07-14"
    operations: [
        SdkEnums
        SdkDocuments
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

/// A document member.
operation SdkDocuments {
    output: SdkDocumentsOutput
}

@output
structure SdkDocumentsOutput {
    documentValue: Document
}
