package test.ops.model;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.ApiService;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.SimpleApiOperation;
import software.amazon.awssdk.schema.serde.ErrorRegistry;
import test.ops.internal.OpsServiceSchemas;

/**
 * The operations of the ops.test#OpsService service and its error registry, shared by the sync and async clients.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class OpsOperations {
    private static final String NS = "ops.test";

    private static final ShapeId ITEM_MISSING = ShapeId.fromParts(NS, "ItemMissing");

    private static final ShapeId MEMBER_ERROR = ShapeId.fromParts(NS, "MemberError");

    private static final ShapeId OTHER_MISSING = ShapeId.fromParts(NS, "OtherMissing");

    private static final ShapeId SERVER_FAULT = ShapeId.fromParts(NS, "ServerFault");

    private static final ShapeId SIGV4 = ShapeId.fromParts("aws.auth", "sigv4");

    private static final ShapeId HTTP_BEARER_AUTH = ShapeId.fromParts("smithy.api", "httpBearerAuth");

    private static final ShapeId NO_AUTH = ShapeId.fromParts("smithy.api", "noAuth");

    /**
     * The OpsService service.
     */
    public static final ApiService SERVICE = () -> OpsServiceSchemas.SERVICE;

    /**
     * The service's error registry: every modeled error of the service, by shape ID, and the custom codes of its
     * {@code awsQueryError} errors.
     */
    public static final ErrorRegistry ERROR_REGISTRY = ErrorRegistry.builder()
            .putLazyType(ITEM_MISSING, () -> OpsSchemas.ITEM_MISSING)
            .errorCode("Missing", ITEM_MISSING)
            .putLazyType(MEMBER_ERROR, () -> OpsSchemas.MEMBER_ERROR)
            .putLazyType(OTHER_MISSING, () -> OpsSchemas.OTHER_MISSING)
            .putLazyType(SERVER_FAULT, () -> OpsSchemas.SERVER_FAULT)
            .build();

    /**
     * The GetItem operation.
     */
    public static final ApiOperation<SerializableStruct, SerializableStruct> GET_ITEM =
            SimpleApiOperation.<SerializableStruct, SerializableStruct> builder()
                    .schema(OpsServiceSchemas.GET_ITEM)
                    .inputSchema(() -> OpsSchemas.GET_ITEM_INPUT)
                    .outputSchema(() -> OpsSchemas.GET_ITEM_OUTPUT)
                    .errorIds(OTHER_MISSING, ITEM_MISSING, SERVER_FAULT)
                    .errorRegistry(ERROR_REGISTRY)
                    .effectiveAuthSchemes(SIGV4, HTTP_BEARER_AUTH)
                    .service(SERVICE)
                    .build();

    /**
     * The Ping operation.
     */
    public static final ApiOperation<SerializableStruct, SerializableStruct> PING =
            SimpleApiOperation.<SerializableStruct, SerializableStruct> builder()
                    .schema(OpsServiceSchemas.PING)
                    .inputSchema(() -> PreludeSchemas.UNIT)
                    .outputSchema(() -> PreludeSchemas.UNIT)
                    .errorIds(SERVER_FAULT)
                    .errorRegistry(ERROR_REGISTRY)
                    .effectiveAuthSchemes(NO_AUTH)
                    .service(SERVICE)
                    .build();

    /**
     * The PutItem operation.
     */
    public static final ApiOperation<SerializableStruct, SerializableStruct> PUT_ITEM =
            SimpleApiOperation.<SerializableStruct, SerializableStruct> builder()
                    .schema(OpsServiceSchemas.PUT_ITEM)
                    .inputSchema(() -> OpsSchemas.PUT_ITEM_INPUT)
                    .outputSchema(() -> PreludeSchemas.UNIT)
                    .errorIds(SERVER_FAULT)
                    .errorRegistry(ERROR_REGISTRY)
                    .effectiveAuthSchemes(SIGV4, HTTP_BEARER_AUTH, NO_AUTH)
                    .service(SERVICE)
                    .build();

    /**
     * The SignedOnly operation.
     */
    public static final ApiOperation<SerializableStruct, SerializableStruct> SIGNED_ONLY =
            SimpleApiOperation.<SerializableStruct, SerializableStruct> builder()
                    .schema(OpsServiceSchemas.SIGNED_ONLY)
                    .inputSchema(() -> OpsSchemas.SIGNED_ONLY_INPUT)
                    .outputSchema(() -> OpsSchemas.SIGNED_ONLY_OUTPUT)
                    .errorIds(ITEM_MISSING, SERVER_FAULT)
                    .errorRegistry(ERROR_REGISTRY)
                    .effectiveAuthSchemes(SIGV4)
                    .service(SERVICE)
                    .build();

    private OpsOperations() {
    }
}
