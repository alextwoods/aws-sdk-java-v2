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
 * The XML body reader and writer shared by the XML-based protocol runtimes ({@code awsQuery}, {@code ec2Query} and
 * {@code restXml}): a secure StAX parser into an {@link software.amazon.awssdk.protocols.core.xml.XmlElement}
 * tree ({@link software.amazon.awssdk.protocols.core.xml.XmlTreeReader}), a schema-guided
 * {@code ShapeDeserializer} over it honoring {@code xmlName}, {@code xmlFlattened}, {@code xmlAttribute} and
 * {@code timestampFormat} ({@link software.amazon.awssdk.protocols.core.xml.XmlShapeDeserializer}), the AWS
 * XML error envelope ({@link software.amazon.awssdk.protocols.core.xml.XmlErrorEnvelope}), and the inverse
 * schema-guided {@code ShapeSerializer} writing request bodies, which also honors {@code xmlNamespace}
 * ({@link software.amazon.awssdk.protocols.core.xml.XmlShapeSerializer}).
 *
 * <p>It depends only on {@code protocol-core}, Smithy Java core and the JDK ({@code javax.xml.stream}), so it can
 * move to a shared XML module without API changes.
 */
package software.amazon.awssdk.protocols.core.xml;
