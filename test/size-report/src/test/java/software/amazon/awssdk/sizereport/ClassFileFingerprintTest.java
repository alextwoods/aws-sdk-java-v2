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

package software.amazon.awssdk.sizereport;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class ClassFileFingerprintTest {

    @Test
    void of_classFiles_recordsVersionAndDebugAttributes() {
        ClassFileFingerprint fingerprint = ClassFileFingerprint.of(Arrays.asList(SyntheticClasses.client(true),
                                                                                 SyntheticClasses.plain("a/B", 1)));

        assertThat(fingerprint.describe())
            .isEqualTo("class-file version 52 (Java 8); debug attributes: source, lines; MethodParameters: no");
    }

    @Test
    void equals_differentDebugAttributes_notEqual() {
        ClassFileFingerprint withDebug = ClassFileFingerprint.of(Collections.singletonList(SyntheticClasses.client(true)));
        ClassFileFingerprint withoutDebug = ClassFileFingerprint.of(Collections.singletonList(
            SyntheticClasses.plain("a/B", 1)));

        assertThat(withDebug).isNotEqualTo(withoutDebug);
        assertThat(withDebug).isEqualTo(ClassFileFingerprint.of(Collections.singletonList(
            SyntheticClasses.client(false))));
    }

    @Test
    void equalsHashCode_allFields_verified() {
        EqualsVerifier.forClass(ClassFileFingerprint.class)
                      .withNonnullFields("majorVersions")
                      .verify();
    }
}
