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

package software.amazon.awssdk.messagemanager.sns.internal;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

/**
 * Checks that a signing certificate is issued to the expected SNS signing name, with the rules of the Apache
 * HttpClient 5 {@code DefaultHostnameVerifier} V2 used for this check (RFC 2818 section 3.1, strict wildcards):
 * <ul>
 *   <li>if the certificate has DNS subject alternative names, one of them must match; otherwise the first (most
 *       specific) common name of the subject must match;</li>
 *   <li>names are compared case-insensitively ({@link Locale#ROOT});</li>
 *   <li>an identity may hold one {@code *}, which matches any characters of the expected name other than {@code .}
 *       (so it never spans labels), with the identity's text before and after it matching literally.</li>
 * </ul>
 * Thread safe and stateless.
 */
final class SigningCertificateNameVerifier {
    /** The {@code dNSName} type of {@link X509Certificate#getSubjectAlternativeNames()} entries. */
    private static final int DNS_NAME = 2;

    private SigningCertificateNameVerifier() {
    }

    /** True if {@code certificate} is issued to {@code expectedName}. */
    static boolean matches(String expectedName, X509Certificate certificate) {
        String name = expectedName.toLowerCase(Locale.ROOT);
        List<String> dnsNames = dnsSubjectAltNames(certificate);
        if (!dnsNames.isEmpty()) {
            return dnsNames.stream().anyMatch(identity -> matchesIdentity(name, identity.toLowerCase(Locale.ROOT)));
        }
        String commonName = commonName(certificate.getSubjectX500Principal());
        return commonName != null && matchesIdentity(name, commonName.toLowerCase(Locale.ROOT));
    }

    static boolean matchesIdentity(String name, String identity) {
        int asterisk = identity.indexOf('*');
        if (asterisk < 0) {
            return name.equals(identity);
        }
        String prefix = identity.substring(0, asterisk);
        String suffix = identity.substring(asterisk + 1);
        if (name.length() < prefix.length() + suffix.length() || !name.startsWith(prefix) || !name.endsWith(suffix)) {
            return false;
        }
        return name.substring(prefix.length(), name.length() - suffix.length()).indexOf('.') < 0;
    }

    private static List<String> dnsSubjectAltNames(X509Certificate certificate) {
        Collection<List<?>> entries;
        try {
            entries = certificate.getSubjectAlternativeNames();
        } catch (CertificateParsingException e) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        if (entries != null) {
            for (List<?> entry : entries) {
                if (entry.size() >= 2 && entry.get(0) instanceof Integer type && type == DNS_NAME
                    && entry.get(1) instanceof String value) {
                    names.add(value);
                }
            }
        }
        return names;
    }

    /** The common name of the most specific (leftmost) RDN that has one, or {@code null}. */
    private static String commonName(X500Principal subject) {
        LdapName name;
        try {
            name = new LdapName(subject.getName(X500Principal.RFC2253));
        } catch (InvalidNameException e) {
            return null;
        }
        List<Rdn> rdns = name.getRdns();
        for (int i = rdns.size() - 1; i >= 0; i--) {
            String commonName = commonName(rdns.get(i));
            if (commonName != null) {
                return commonName;
            }
        }
        return null;
    }

    private static String commonName(Rdn rdn) {
        // Rdn attributes ignore the case of attribute IDs, so this finds cn, CN, ... of a multi-valued RDN too.
        Attribute commonName = rdn.toAttributes().get("cn");
        if (commonName == null) {
            return null;
        }
        try {
            Object value = commonName.get();
            return value == null ? null : value.toString();
        } catch (NamingException e) {
            return null;
        }
    }
}
