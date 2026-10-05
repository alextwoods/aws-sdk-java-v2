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

package software.amazon.awssdk.regions.providers;

import java.util.Objects;
import java.util.function.Supplier;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.regions.Region;

/**
 * The {@code region} property of a profile, with the V2 name. The profile file is read when the region is requested.
 */
public final class AwsProfileRegionProvider implements AwsRegionProvider {
    private final Supplier<ProfileFile> profileFile;
    private final String profileName;

    /** The region of profile {@code profileName} of the file {@code profileFile} supplies. */
    public AwsProfileRegionProvider(Supplier<ProfileFile> profileFile, String profileName) {
        this.profileFile = Objects.requireNonNull(profileFile, "profileFile");
        this.profileName = Objects.requireNonNull(profileName, "profileName");
    }

    @Override
    public Region getRegion() {
        return profileFile.get()
                          .profile(profileName)
                          .flatMap(profile -> profile.property(ProfileProperty.REGION))
                          .filter(value -> !value.isBlank())
                          .map(value -> Region.of(value.strip()))
                          .orElseThrow(() -> SdkClientException.create("No region provided in profile: " + profileName));
    }
}
