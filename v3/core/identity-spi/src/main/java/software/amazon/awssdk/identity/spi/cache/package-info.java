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

/**
 * The refreshing identity cache shared by identity providers and identity-caching auth schemes (for example S3
 * Express session credentials), implementing the V3 refresh contract of
 * {@link software.amazon.awssdk.identity.spi.RefreshableIdentityProvider}.
 */
package software.amazon.awssdk.identity.spi.cache;
