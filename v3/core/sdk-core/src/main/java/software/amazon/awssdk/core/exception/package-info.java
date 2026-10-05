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
 * The V2-compatible base exception hierarchy ({@code SdkException}, {@code SdkClientException},
 * {@code SdkServiceException}), the model-validation failure {@code MissingRequiredMembersException}, and the V3
 * families of the error taxonomy: {@code SdkClientConfigurationException} (client construction),
 * {@code SdkTimeoutException} (API-call and attempt timeouts) and {@code SdkCloseException} (aggregated close
 * failures).
 */
package software.amazon.awssdk.core.exception;
