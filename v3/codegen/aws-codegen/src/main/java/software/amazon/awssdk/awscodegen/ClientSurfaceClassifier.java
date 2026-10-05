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

package software.amazon.awssdk.awscodegen;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Classifies which V2 client surfaces are generated for a service (Requirements 7.5-7.7, 8.1, 17.1). It is a plan
 * input of {@link ClientPlan}, so the Parity Manifest can classify each surface once it exists; until then
 * {@link #V2_BASELINE} preserves the V2 generated-client shape: an async client for every service, and the
 * request-builder consumer and (for operations without required input members) no-argument overloads for every
 * operation, and the presigners and service utilities V2 hand-writes (Requirements 7.10, 16.5, 18.3).
 */
interface ClientSurfaceClassifier {

    /** The V2 generated-client shape: every surface preserved. */
    ClientSurfaceClassifier V2_BASELINE = new ClientSurfaceClassifier() {
    };

    /**
     * The V2 presigners, by service shape ID: {@code S3Presigner} with V2's nine presigned S3 operations. V2's
     * {@code PollyPresigner} is not in the baseline: it rewrites {@code SynthesizeSpeech} from a JSON POST to a
     * query-string GET before signing, which needs a declared codegen integration (design "Paginators, waiters,
     * presigners, and utilities").
     */
    Map<ShapeId, PresignerSurface> V2_PRESIGNERS = Map.of(
        ShapeId.from("com.amazonaws.s3#AmazonS3"),
        new PresignerSurface("S3Presigner", PresignRules.S3,
                             List.of("GetObject", "HeadObject", "HeadBucket", "PutObject", "DeleteObject",
                                     "CreateMultipartUpload", "UploadPart", "CompleteMultipartUpload",
                                     "AbortMultipartUpload")));

    /**
     * The V2 generated type names of renamed shapes (V2 {@code renameShapes} customizations), by service shape ID and
     * then by shape ID. S3's {@code Object} and {@code Error} would otherwise be generated as
     * {@code model.Object} and {@code model.Error}, which shadow {@code java.lang} inside the model package and break
     * every generated type that refers to {@code java.lang.Object} by simple name. The Parity Manifest (task 13.x)
     * takes over these declarations once it exists.
     */
    Map<ShapeId, Map<ShapeId, String>> V2_SHAPE_RENAMES = Map.of(
        ShapeId.from("com.amazonaws.s3#AmazonS3"),
        Map.of(ShapeId.from("com.amazonaws.s3#Error"), "S3Error",
               ShapeId.from("com.amazonaws.s3#Object"), "S3Object"),
        // A structure named Builder would declare a nested Builder interface inside class Builder, invalid Java.
        ShapeId.from("com.amazonaws.elasticbeanstalk#AWSElasticBeanstalkService"),
        Map.of(ShapeId.from("com.amazonaws.elasticbeanstalk#Builder"), "PlatformBuilder"),
        ShapeId.from("com.amazonaws.elasticloadbalancing#ElasticLoadBalancing_v7"),
        Map.of(ShapeId.from("com.amazonaws.elasticloadbalancing#AccessPointNotFoundException"),
               "LoadBalancerNotFoundException",
               ShapeId.from("com.amazonaws.elasticloadbalancing#DuplicateAccessPointNameException"),
               "DuplicateLoadBalancerNameException",
               ShapeId.from("com.amazonaws.elasticloadbalancing#TooManyAccessPointsException"),
               "TooManyLoadBalancersException",
               ShapeId.from("com.amazonaws.elasticloadbalancing#InvalidEndPointException"),
               "InvalidInstanceException"),
        ShapeId.from("com.amazonaws.gamelift#GameLift"),
        Map.of(ShapeId.from("com.amazonaws.gamelift#AwsCredentials"), "Credentials"),
        // The modeled error would otherwise be named like the service's base exception.
        ShapeId.from("com.amazonaws.marketplacecommerceanalytics#MarketplaceCommerceAnalytics20150701"),
        Map.of(ShapeId.from("com.amazonaws.marketplacecommerceanalytics#MarketplaceCommerceAnalyticsException"),
               "MarketplaceCommerceAnalyticsServiceException"),
        ShapeId.from("com.amazonaws.mediapackage#MediaPackage"),
        Map.of(ShapeId.from("com.amazonaws.mediapackage#__AdTriggersElement"), "AdTriggersElement",
               ShapeId.from("com.amazonaws.mediapackage#__PeriodTriggersElement"), "PeriodTriggersElement"),
        ShapeId.from("com.amazonaws.mediapackagevod#MediaPackageVod"),
        Map.of(ShapeId.from("com.amazonaws.mediapackagevod#__PeriodTriggersElement"), "PeriodTriggersElement"),
        ShapeId.from("com.amazonaws.pinpoint#Pinpoint"),
        Map.of(ShapeId.from("com.amazonaws.pinpoint#__EndpointTypesElement"), "EndpointTypesElement"));

    /**
     * Base type names of error shapes whose V2 exception name collides with another error of the same service, by
     * service shape ID and then by shape ID. V2 generates both shapes into one class file, so the shape generated last
     * silently replaces the other; V3 fails generation on such a collision ({@code GENERATED_NAME_COLLISION}). The
     * shape V2 actually ships under the shared name (the one whose members the V2 class exposes) keeps it, and the
     * other shape is given a distinct name here. These types are new in V3 and are recorded as adapted surfaces by the
     * Parity Manifest.
     *
     * <p>CloudWatch models both the awsQuery-era errors {@code LimitExceededFault} and {@code ResourceNotFound} and the
     * JSON-era errors {@code LimitExceededException} and {@code ResourceNotFoundException}; V2's
     * {@code ResourceNotFoundException} exposes the {@code ResourceType} and {@code ResourceId} members of the latter.
     */
    Map<ShapeId, Map<ShapeId, String>> COLLIDING_ERROR_RENAMES = Map.of(
        ShapeId.from("com.amazonaws.cloudwatch#GraniteServiceVersion20100801"),
        Map.of(ShapeId.from("com.amazonaws.cloudwatch#LimitExceededFault"), "LimitExceededFaultException",
               ShapeId.from("com.amazonaws.cloudwatch#ResourceNotFound"), "ResourceNotFoundErrorException"));

    /**
     * The service-wide SigV4-family signer defaults of V2's legacy auth types, by service shape ID: the name of the
     * {@code AwsAuthSchemeOptions} factory of the defaults every AWS auth option of the service gets unless the option
     * sets the property itself. S3 (V2 auth type {@code s3}): no payload signing over HTTPS, no double URL encoding,
     * no path normalization.
     */
    Map<ShapeId, String> V2_SIGNER_DEFAULTS = Map.of(
        ShapeId.from("com.amazonaws.s3#AmazonS3"), "s3SignerDefaults");

    /** The V2 service utilities (V2 {@code utilitiesMethod} customizations), by service shape ID. */
    Map<ShapeId, UtilitiesSurface> V2_UTILITIES = Map.of(
        ShapeId.from("com.amazonaws.s3#AmazonS3"),
        new UtilitiesSurface("S3Utilities", UtilityKind.S3, List.of("getUrl", "parseUri")),
        ShapeId.from("com.amazonaws.rds#AmazonRDSv19"),
        new UtilitiesSurface("RdsUtilities", UtilityKind.RDS_AUTH_TOKEN, List.of("generateAuthenticationToken")),
        ShapeId.from("com.amazonaws.dsql#DSQL"),
        new UtilitiesSurface("DsqlUtilities", UtilityKind.DSQL_AUTH_TOKEN,
                             List.of("generateDbConnectAuthToken", "generateDbConnectAdminAuthToken")),
        ShapeId.from("com.amazonaws.cloudfront#Cloudfront2020_05_31"),
        new UtilitiesSurface("CloudFrontUtilities", UtilityKind.CLOUDFRONT,
                             List.of("getSignedUrlWithCannedPolicy", "getSignedUrlWithCustomPolicy",
                                     "getCookiesForCannedPolicy", "getCookiesForCustomPolicy")));

    /** How a presigner adjusts the selected Modern Auth option for presigning. */
    enum PresignRules {
        /** SigV4-family query string with a signed payload. */
        SIGV4,
        /** S3: SigV4-family query string, unsigned payload, S3 canonical URI rules. */
        S3
    }

    /**
     * A generated service presigner.
     *
     * @param typeName the presigner interface, in {@code <root>.presigner}
     * @param rules how the selected auth option is adjusted for presigning
     * @param operations the presigned operations by name, in V2 order; operations the model lacks are skipped
     */
    record PresignerSurface(String typeName, PresignRules rules, List<String> operations) {
        public PresignerSurface {
            operations = List.copyOf(operations);
        }
    }

    /** The behavior of a service utility entry point. */
    enum UtilityKind {
        /** S3: object URLs through endpoint resolution, and S3 URI parsing. */
        S3,
        /** RDS IAM database authentication tokens, presigned {@code connect} requests. */
        RDS_AUTH_TOKEN,
        /** Aurora DSQL authentication tokens, presigned {@code DbConnect}/{@code DbConnectAdmin} requests. */
        DSQL_AUTH_TOKEN,
        /** CloudFront signed URLs and cookies (RSA, not Modern Auth). */
        CLOUDFRONT
    }

    /**
     * A generated service utility entry point, in the service's root package, reached through the client's
     * {@code utilities()} method.
     *
     * @param typeName the utilities type
     * @param kind its behavior
     * @param methods the V2 entry-point methods it preserves
     */
    record UtilitiesSurface(String typeName, UtilityKind kind, List<String> methods) {
        public UtilitiesSurface {
            methods = List.copyOf(methods);
        }
    }

    /** The service's presigner, if it has one. */
    default Optional<PresignerSurface> presigner(ServiceShape service) {
        return Optional.ofNullable(V2_PRESIGNERS.get(service.getId()));
    }

    /** The service's utility entry point, if it has one. */
    default Optional<UtilitiesSurface> utilities(ServiceShape service) {
        return Optional.ofNullable(V2_UTILITIES.get(service.getId()));
    }

    /** Whether the service has an async client. */
    default boolean asyncClient(ServiceShape service) {
        return true;
    }

    /** Whether {@code operation} has the {@code Consumer<Request.Builder>} overloads. */
    default boolean consumerOverload(ServiceShape service, OperationShape operation) {
        return true;
    }

    /**
     * Whether {@code operation}, which has no required input members, has the no-argument overload. V2 generates it
     * only for such operations; the plan never asks for an operation with required input members.
     */
    default boolean noArgumentOverload(ServiceShape service, OperationShape operation) {
        return true;
    }
}
