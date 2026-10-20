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

package software.amazon.awssdk.schemaserde.sourcecompat.fixtures;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.services.account.AccountAsyncClient;
import software.amazon.awssdk.services.account.AccountClient;
import software.amazon.awssdk.services.account.model.AccountException;
import software.amazon.awssdk.services.account.model.AlternateContact;
import software.amazon.awssdk.services.account.model.AlternateContactType;
import software.amazon.awssdk.services.account.model.ContactInformation;
import software.amazon.awssdk.services.account.model.GetAlternateContactRequest;
import software.amazon.awssdk.services.account.model.GetAlternateContactResponse;
import software.amazon.awssdk.services.account.model.GetContactInformationResponse;
import software.amazon.awssdk.services.account.model.Region;
import software.amazon.awssdk.services.account.model.RegionOptStatus;
import software.amazon.awssdk.services.account.model.ResourceNotFoundException;

/**
 * Account, written against the Baseline_Build public API (Requirement 16.8). The feature build generates Account from
 * its native Smithy model; this fixture uses only what the Baseline_Build's C2J-generated module has.
 */
public final class AccountFixture {
    private AccountFixture() {
    }

    public static AccountClient syncClient(SdkHttpClient httpClient, SdkPlugin plugin,
                                           ClientSettings.CallCounter counter) {
        return AccountClient.builder()
                            .region(software.amazon.awssdk.regions.Region.AWS_GLOBAL)
                            .credentialsProvider(ClientSettings.credentials())
                            .overrideConfiguration(ClientSettings.overrideConfiguration(counter))
                            .endpointOverride(ClientSettings.ENDPOINT)
                            .httpClient(httpClient)
                            .addPlugin(plugin)
                            .build();
    }

    public static AccountAsyncClient asyncClient(SdkAsyncHttpClient httpClient, SdkPlugin plugin) {
        return AccountAsyncClient.builder()
                                 .region(software.amazon.awssdk.regions.Region.AWS_GLOBAL)
                                 .credentialsProvider(ClientSettings.credentials())
                                 .endpointOverride(ClientSettings.ENDPOINT)
                                 .httpClient(httpClient)
                                 .addPlugin(plugin)
                                 .build();
    }

    public static GetAlternateContactRequest getAlternateContactRequest() {
        return GetAlternateContactRequest.builder()
                                         .accountId("123456789012")
                                         .alternateContactType(AlternateContactType.BILLING)
                                         .build();
    }

    public static GetAlternateContactResponse getAlternateContactResponse() {
        return GetAlternateContactResponse.builder()
                                          .alternateContact(AlternateContact.builder()
                                                                            .alternateContactType(AlternateContactType.BILLING)
                                                                            .name("Jane")
                                                                            .emailAddress("jane@example.com")
                                                                            .phoneNumber("+1-206-555-0100")
                                                                            .title("Billing")
                                                                            .build())
                                          .build();
    }

    /**
     * Reads a response through its getters: a nested structure with an enum and sensitive strings.
     */
    public static String summarize(GetAlternateContactResponse response) {
        AlternateContact contact = response.alternateContact();
        return contact.alternateContactType() + "/" + contact.alternateContactTypeAsString() + " " + contact.name()
               + " " + contact.emailAddress() + " " + contact.title();
    }

    public static String getAlternateContact(AccountClient client) {
        return summarize(client.getAlternateContact(getAlternateContactRequest()));
    }

    public static GetContactInformationResponse getContactInformationResponse() {
        return GetContactInformationResponse.builder()
                                            .contactInformation(ContactInformation.builder()
                                                                                  .fullName("Jane Doe")
                                                                                  .city("Seattle")
                                                                                  .countryCode("US")
                                                                                  .build())
                                            .build();
    }

    public static CompletableFuture<String> getContactInformationAsync(AccountAsyncClient client) {
        return client.getContactInformation(r -> r.accountId("123456789012"))
                     .thenApply(r -> r.contactInformation().fullName() + " " + r.contactInformation().city() + " "
                                     + r.contactInformation().countryCode());
    }

    public static List<String> regions(AccountClient client) {
        List<String> regions = new ArrayList<>();
        client.listRegionsPaginator(r -> r.regionOptStatusContains(RegionOptStatus.ENABLED, RegionOptStatus.ENABLED_BY_DEFAULT))
              .regions()
              .forEach((Region region) -> regions.add(region.regionName() + "=" + region.regionOptStatus() + "/"
                                                      + region.regionOptStatusAsString()));
        return regions;
    }

    public static ResourceNotFoundException resourceNotFound() {
        return ResourceNotFoundException.builder().message("No alternate contact").statusCode(404).build();
    }

    public static String getMissingAlternateContact(AccountClient client) {
        try {
            client.getAlternateContact(r -> r.alternateContactType(AlternateContactType.SECURITY));
            return "no exception";
        } catch (ResourceNotFoundException e) {
            return "ResourceNotFoundException " + e.statusCode() + " " + e.awsErrorDetails().errorCode();
        } catch (AccountException e) {
            return "AccountException " + e.awsErrorDetails().errorCode();
        }
    }
}
