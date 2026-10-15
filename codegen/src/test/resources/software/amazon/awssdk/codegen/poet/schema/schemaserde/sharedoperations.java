package software.amazon.awssdk.services.shared.model;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.ApiService;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.SimpleApiOperation;
import software.amazon.awssdk.schema.serde.ErrorRegistry;
import software.amazon.awssdk.services.shared.internal.SharedServiceSchemas;

/**
 * The operations of the shared#SharedService service and its error registry, shared by the sync and async clients.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class SharedOperations {
  private static final ShapeId NO_AUTH = ShapeId.fromParts("smithy.api", "noAuth");

  /**
   * The SharedService service.
   */
  public static final ApiService SERVICE = () -> SharedServiceSchemas.SERVICE;

  /**
   * The service's error registry: every modeled error of the service, by shape ID, and the custom codes of its
   * {@code awsQueryError} errors.
   */
  public static final ErrorRegistry ERROR_REGISTRY = ErrorRegistry.builder()
    .build();

  /**
   * The Disable operation.
   */
  public static final ApiOperation<DisableRequest, DisableResponse> DISABLE = SimpleApiOperation.<DisableRequest, DisableResponse>builder()
    .schema(SharedServiceSchemas.DISABLE)
    .inputSchema(() -> DisableRequest.SCHEMA)
    .outputSchema(() -> DisableResponse.SCHEMA)
    .errorRegistry(ERROR_REGISTRY)
    .effectiveAuthSchemes(NO_AUTH)
    .service(SERVICE)
    .build();

  /**
   * The Echo operation.
   */
  public static final ApiOperation<EchoRequest, EchoResponse> ECHO = SimpleApiOperation.<EchoRequest, EchoResponse>builder()
    .schema(SharedServiceSchemas.ECHO)
    .inputSchema(() -> EchoRequest.SCHEMA)
    .outputSchema(() -> EchoResponse.SCHEMA)
    .errorRegistry(ERROR_REGISTRY)
    .effectiveAuthSchemes(NO_AUTH)
    .service(SERVICE)
    .build();

  /**
   * The Enable operation.
   */
  public static final ApiOperation<EnableRequest, EnableResponse> ENABLE = SimpleApiOperation.<EnableRequest, EnableResponse>builder()
    .schema(SharedServiceSchemas.ENABLE)
    .inputSchema(() -> EnableRequest.SCHEMA)
    .outputSchema(() -> EnableResponse.SCHEMA)
    .errorRegistry(ERROR_REGISTRY)
    .effectiveAuthSchemes(NO_AUTH)
    .service(SERVICE)
    .build();

  private SharedOperations() {
  }
}
