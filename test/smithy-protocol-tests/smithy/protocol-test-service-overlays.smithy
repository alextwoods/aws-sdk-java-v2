$version: "2.0"

// Traits the SDK code generator needs that the upstream Smithy protocol test models leave out. Applied to the
// imported protocol test models by smithy-build.json; the test cases themselves are not changed.
//
// The SDK derives the client name and package from aws.api#service sdkId, and stage 5 of the codegen pipeline
// requires it. RpcV2Protocol (smithy-protocol-tests) is not an AWS service model, so it has none.
namespace software.amazon.awssdk.protocoltests.overlays

apply smithy.protocoltests.rpcv2Cbor#RpcV2Protocol @aws.api#service(sdkId: "RpcV2 Protocol")
