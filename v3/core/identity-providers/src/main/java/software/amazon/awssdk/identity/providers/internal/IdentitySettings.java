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

package software.amazon.awssdk.identity.providers.internal;

import java.util.Optional;
import software.amazon.awssdk.profiles.SystemSettings;

/**
 * The V2 {@code SdkSystemSetting} names (JVM property, environment variable) the identity providers read. Not a public
 * API.
 */
public enum IdentitySettings {
    AWS_ACCESS_KEY_ID("aws.accessKeyId"),
    AWS_SECRET_ACCESS_KEY("aws.secretAccessKey"),
    AWS_SESSION_TOKEN("aws.sessionToken"),
    AWS_ACCOUNT_ID("aws.accountId"),
    AWS_WEB_IDENTITY_TOKEN_FILE("aws.webIdentityTokenFile"),
    AWS_ROLE_ARN("aws.roleArn"),
    AWS_ROLE_SESSION_NAME("aws.roleSessionName"),
    AWS_REGION("aws.region"),
    AWS_EC2_METADATA_DISABLED("aws.disableEc2Metadata"),
    AWS_EC2_METADATA_V1_DISABLED("aws.disableEc2MetadataV1"),
    AWS_EC2_METADATA_SERVICE_ENDPOINT("aws.ec2MetadataServiceEndpoint"),
    AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE("aws.ec2MetadataServiceEndpointMode"),
    AWS_METADATA_SERVICE_TIMEOUT("aws.ec2MetadataServiceTimeout"),
    AWS_CONTAINER_SERVICE_ENDPOINT("aws.containerServiceEndpoint"),
    AWS_CONTAINER_CREDENTIALS_RELATIVE_URI("aws.containerCredentialsPath"),
    AWS_CONTAINER_CREDENTIALS_FULL_URI("aws.containerCredentialsFullUri"),
    AWS_CONTAINER_AUTHORIZATION_TOKEN("aws.containerAuthorizationToken"),
    AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE("aws.containerAuthorizationTokenFile");

    private final String property;

    IdentitySettings(String property) {
        this.property = property;
    }

    /** The JVM system property. */
    public String property() {
        return property;
    }

    /** The environment variable. */
    public String environmentVariable() {
        return name();
    }

    /** The value in {@code settings}: JVM property first, then environment variable. */
    public Optional<String> value(SystemSettings settings) {
        return settings.value(property, name());
    }

    /** "environment variable (X) or system property (y)", for diagnostics. */
    public String describe() {
        return "environment variable (" + name() + ") or system property (" + property + ")";
    }
}
