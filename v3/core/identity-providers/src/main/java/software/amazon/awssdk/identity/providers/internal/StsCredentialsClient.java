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

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import software.amazon.awssdk.auth.api.SignRequest;
import software.amazon.awssdk.auth.aws.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.UriEncoding;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;

/**
 * The STS {@code AssumeRole} (SigV4-signed) and {@code AssumeRoleWithWebIdentity} (unsigned) calls of the role and web
 * identity providers, over the {@code awsQuery} protocol (form-encoded POST, XML response), at the regional STS
 * endpoint {@code https://sts.<region>.<dnsSuffix>}. Identity providers cannot depend on the generated STS artifact,
 * so these two calls are implemented natively. Not a public API.
 */
public final class StsCredentialsClient implements AutoCloseable {
    private static final String VERSION = "2011-06-15";

    private final String region;
    private final URI endpoint;
    private final IdentityHttpClient http;

    public StsCredentialsClient(String region, URI endpointOverride, SdkSyncHttpClient httpClient) {
        this.region = region;
        this.endpoint = endpointOverride != null ? endpointOverride
                                                 : URI.create("https://sts." + region + "." + dnsSuffix(region));
        this.http = IdentityHttpClient.create(httpClient, null);
    }

    /** The DNS suffix of the partition of {@code region}. */
    public static String dnsSuffix(String region) {
        if (region.startsWith("cn-")) {
            return "amazonaws.com.cn";
        }
        if (region.startsWith("us-isob-")) {
            return "sc2s.sgov.gov";
        }
        if (region.startsWith("us-iso-")) {
            return "c2s.ic.gov";
        }
        if (region.startsWith("eu-isoe-")) {
            return "cloud.adc-e.uk";
        }
        if (region.startsWith("us-isof-")) {
            return "csp.hci.ic.gov";
        }
        return "amazonaws.com";
    }

    /** The STS endpoint. */
    public URI endpoint() {
        return endpoint;
    }

    /** {@code AssumeRoleWithWebIdentity}. */
    public AwsSessionCredentialsIdentity assumeRoleWithWebIdentity(String roleArn, String sessionName, String token,
                                                                   Integer durationSeconds, String providerName) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("Action", "AssumeRoleWithWebIdentity");
        form.put("Version", VERSION);
        form.put("RoleArn", roleArn);
        form.put("RoleSessionName", sessionName);
        form.put("WebIdentityToken", token);
        if (durationSeconds != null) {
            form.put("DurationSeconds", durationSeconds.toString());
        }
        return call(request(form), "AssumeRoleWithWebIdentity", providerName);
    }

    /** {@code AssumeRole}, signed with {@code source}. */
    public AwsSessionCredentialsIdentity assumeRole(AwsCredentialsIdentity source, String roleArn, String sessionName,
                                                    String externalId, Integer durationSeconds, String providerName) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("Action", "AssumeRole");
        form.put("Version", VERSION);
        form.put("RoleArn", roleArn);
        form.put("RoleSessionName", sessionName);
        if (externalId != null) {
            form.put("ExternalId", externalId);
        }
        if (durationSeconds != null) {
            form.put("DurationSeconds", durationSeconds.toString());
        }
        SdkHttpRequest signed = AwsV4HttpSigner.create().sign(
            SignRequest.<AwsCredentialsIdentity>builder(source)
                       .request(request(form))
                       .putProperty(AwsV4HttpSigner.REGION_NAME, region)
                       .putProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, "sts")
                       .build()).request();
        return call(signed, "AssumeRole", providerName);
    }

    private SdkHttpRequest request(Map<String, String> form) {
        StringBuilder body = new StringBuilder();
        form.forEach((k, v) -> {
            if (!body.isEmpty()) {
                body.append('&');
            }
            body.append(UriEncoding.encode(k)).append('=').append(UriEncoding.encode(v));
        });
        return IdentityHttpClient.request("POST", endpoint)
                                 .putHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                                 .body(HttpBody.fromUtf8(body.toString()))
                                 .build();
    }

    private AwsSessionCredentialsIdentity call(SdkHttpRequest request, String operation, String providerName) {
        IdentityHttpClient.Response response = http.send(request, Duration.ofSeconds(30));
        Document document = parse(response.body(), operation);
        if (!response.isSuccessful()) {
            throw SdkClientException.create("STS " + operation + " failed with HTTP " + response.statusCode() + ": "
                                            + text(document, "Code") + " - " + text(document, "Message"));
        }
        String accessKeyId = text(document, "AccessKeyId");
        String secretAccessKey = text(document, "SecretAccessKey");
        String sessionToken = text(document, "SessionToken");
        if (accessKeyId == null || secretAccessKey == null || sessionToken == null) {
            throw SdkClientException.create("The STS " + operation + " response has no credentials.");
        }
        Instant expiration = LoadedCredentials.instant(text(document, "Expiration"), "STS " + operation);
        return LoadedCredentials.session(accessKeyId, secretAccessKey, sessionToken, accountId(text(document, "Arn")),
                                         expiration, providerName);
    }

    /** The account ID of an assumed-role ARN ({@code arn:partition:sts::account:assumed-role/...}). */
    static String accountId(String arn) {
        if (arn == null) {
            return null;
        }
        String[] parts = arn.split(":", 6);
        return parts.length >= 5 && !parts[4].isEmpty() ? parts[4] : null;
    }

    private static Document parse(String xml, String operation) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(null);
            return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw SdkClientException.create("Unable to parse the STS " + operation + " response.", e);
        }
    }

    private static String text(Document document, String element) {
        NodeList nodes = document.getElementsByTagNameNS("*", element);
        if (nodes.getLength() == 0) {
            nodes = document.getElementsByTagName(element);
        }
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent().strip();
    }

    @Override
    public void close() {
        http.close();
    }
}
