package software.amazon.awssdk.services.demo.model;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.ApiService;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.SimpleApiOperation;
import software.amazon.awssdk.schema.serde.ErrorRegistry;
import software.amazon.awssdk.services.demo.internal.DemoServiceSchemas;

/**
 * The operations of the demo#DemoService service and its error registry, shared by the sync and async clients.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DemoOperations {
  private static final String NS = "demo";

  private static final ShapeId THING_NOT_FOUND = ShapeId.fromParts(NS, "ThingNotFound");

  private static final ShapeId SIGV4 = ShapeId.fromParts("aws.auth", "sigv4");

  /**
   * The DemoService service.
   */
  public static final ApiService SERVICE = () -> DemoServiceSchemas.SERVICE;

  /**
   * The service's error registry: every modeled error of the service, by shape ID, and the custom codes of its
   * {@code awsQueryError} errors.
   */
  public static final ErrorRegistry ERROR_REGISTRY = ErrorRegistry.builder()
    .putLazyType(THING_NOT_FOUND, () -> DemoSchemas.THING_NOT_FOUND)
    .build();

  /**
   * The DeleteThing operation.
   */
  public static final ApiOperation<DeleteThingRequest, DeleteThingResponse> DELETE_THING = SimpleApiOperation.<DeleteThingRequest, DeleteThingResponse>builder()
    .schema(DemoServiceSchemas.DELETE_THING)
    .inputSchema(() -> DeleteThingRequest.SCHEMA)
    .outputSchema(() -> DeleteThingResponse.SCHEMA)
    .errorIds(THING_NOT_FOUND)
    .errorRegistry(ERROR_REGISTRY)
    .effectiveAuthSchemes(SIGV4)
    .service(SERVICE)
    .build();

  /**
   * The GetThing operation.
   */
  public static final ApiOperation<GetThingRequest, GetThingResponse> GET_THING = SimpleApiOperation.<GetThingRequest, GetThingResponse>builder()
    .schema(DemoServiceSchemas.GET_THING)
    .inputSchema(() -> GetThingRequest.SCHEMA)
    .outputSchema(() -> GetThingResponse.SCHEMA)
    .errorIds(THING_NOT_FOUND)
    .errorRegistry(ERROR_REGISTRY)
    .effectiveAuthSchemes(SIGV4)
    .service(SERVICE)
    .build();

  private DemoOperations() {
  }
}
