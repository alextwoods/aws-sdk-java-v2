// SDK-owned rpcv2Cbor service for the hand-written protocol tests that cannot be Smithy test cases (task 3.34): Smithy
// rejects enum values the model does not define in test params, so UnknownEnumValueProtocolTest calls this service
// through ProtocolTestHarness.respondWith. legacy-disposition.csv rows json-core-output.json#5, #7, #9 and #10.
$version: "2.0"

namespace software.amazon.awssdk.protocoltests.rpcv2cbor

use aws.api#service
use smithy.protocols#rpcv2Cbor

@service(sdkId: "Sdk RpcV2 Protocol")
@rpcv2Cbor
@title("SDK RpcV2 CBOR Protocol Test Service")
service SdkRpcV2Protocol {
    version: "2025-01-01"
    operations: [
        SdkEnums
    ]
}

/// Enum values in a member, a list element, a map key and a map value.
operation SdkEnums {
    input: SdkEnumsInputOutput
    output: SdkEnumsInputOutput
}

structure SdkEnumsInputOutput {
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
