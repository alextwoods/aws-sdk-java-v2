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
 * The module-path and native-image smoke application (task 18.9; Requirements 16.10, 19.13). It requires the V3
 * artifacts by their stable module names only: a generated service, the identity and region modules a caller
 * configures, the HTTP SPI and every supported HTTP Implementation.
 */
module software.amazon.awssdk.test.modulepath.smoke {
    requires software.amazon.awssdk.services.dynamodb;
    requires software.amazon.awssdk.identity.spi;
    requires software.amazon.awssdk.identity.providers;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.http;
    requires software.amazon.awssdk.http.apache;
    requires software.amazon.awssdk.http.apache5;
    requires software.amazon.awssdk.http.urlconnection;
    requires software.amazon.awssdk.http.nio.netty;
    requires software.amazon.awssdk.http.crt;
    // Apache HttpClient 5 (under apache5-client) links jdk.net.Sockets in a static initializer. Automatic modules
    // cannot require system modules, so a module-path application of apache5-client must resolve jdk.net itself
    // (here, or with --add-modules jdk.net); without it Apache5HttpClient.create() fails with NoClassDefFoundError.
    requires jdk.net;
    // The loopback fake service.
    requires jdk.httpserver;
}
