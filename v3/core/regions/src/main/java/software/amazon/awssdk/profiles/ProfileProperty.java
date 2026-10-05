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

package software.amazon.awssdk.profiles;

/** Names of the shared-configuration profile properties the SDK reads, with the V2 constant names. */
public final class ProfileProperty {
    public static final String AWS_ACCESS_KEY_ID = "aws_access_key_id";
    public static final String AWS_SECRET_ACCESS_KEY = "aws_secret_access_key";
    public static final String AWS_SESSION_TOKEN = "aws_session_token";
    public static final String AWS_ACCOUNT_ID = "aws_account_id";
    public static final String ROLE_ARN = "role_arn";
    public static final String ROLE_SESSION_NAME = "role_session_name";
    public static final String DURATION_SECONDS = "duration_seconds";
    public static final String EXTERNAL_ID = "external_id";
    public static final String SOURCE_PROFILE = "source_profile";
    public static final String CREDENTIAL_SOURCE = "credential_source";
    public static final String WEB_IDENTITY_TOKEN_FILE = "web_identity_token_file";
    public static final String CREDENTIAL_PROCESS = "credential_process";
    public static final String LOGIN_SESSION = "login_session";
    public static final String REGION = "region";
    public static final String SSO_REGION = "sso_region";
    public static final String SSO_ROLE_NAME = "sso_role_name";
    public static final String SSO_ACCOUNT_ID = "sso_account_id";
    public static final String SSO_START_URL = "sso_start_url";
    /** The name of the {@code [sso-session <name>]} section a profile uses. */
    public static final String SSO_SESSION = "sso_session";
    public static final String EC2_METADATA_SERVICE_ENDPOINT_MODE = "ec2_metadata_service_endpoint_mode";
    public static final String EC2_METADATA_SERVICE_ENDPOINT = "ec2_metadata_service_endpoint";
    public static final String EC2_METADATA_V1_DISABLED = "ec2_metadata_v1_disabled";
    public static final String AUTH_SCHEME_PREFERENCE = "auth_scheme_preference";
    public static final String SIGV4A_SIGNING_REGION_SET = "sigv4a_signing_region_set";
    public static final String STS_REGIONAL_ENDPOINTS = "sts_regional_endpoints";

    private ProfileProperty() {
    }
}
