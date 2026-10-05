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

package software.amazon.awssdk.http.apache.internal;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.apache.http.HttpHost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;

/**
 * The {@code http} and {@code https} socket factories. Both hand every socket they create to the exchange (through
 * the {@link #SOCKET_LISTENER} context attribute) before blocking on it, so {@code abort()} can close a socket that is
 * still connecting or handshaking; the TLS factory tags handshake failures with {@link TlsHandshakeException}.
 */
final class SocketFactories {
    /** Context attribute holding the {@link SocketListener} of the exchange. */
    static final String SOCKET_LISTENER = "software.amazon.awssdk.http.apache.socket-listener";

    private SocketFactories() {
    }

    /** Receives the sockets of one exchange as they are created. */
    interface SocketListener {
        /** Remembers {@code socket}; closes it and throws if the exchange is already aborted. */
        void socketCreated(Socket socket) throws IOException;
    }

    static Registry<ConnectionSocketFactory> registry(SSLConnectionSocketFactory tls) {
        return RegistryBuilder.<ConnectionSocketFactory>create()
                              .register("http", new Plain())
                              .register("https", new Tls(tls))
                              .build();
    }

    private static Socket announce(Socket socket, HttpContext context) throws IOException {
        Object listener = context == null ? null : context.getAttribute(SOCKET_LISTENER);
        if (listener instanceof SocketListener l) {
            l.socketCreated(socket);
        }
        return socket;
    }

    private static Socket connectPlain(int connectTimeout, Socket socket, InetSocketAddress remote,
                                       InetSocketAddress local, HttpContext context) throws IOException {
        Socket sock = socket != null ? socket : announce(new Socket(), context);
        if (local != null) {
            sock.bind(local);
        }
        try {
            sock.connect(remote, connectTimeout);
        } catch (IOException e) {
            closeQuietly(sock);
            throw e;
        }
        return sock;
    }

    static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // Best effort.
        }
    }

    /** Plain TCP. */
    static final class Plain implements ConnectionSocketFactory {
        @Override
        public Socket createSocket(HttpContext context) throws IOException {
            return announce(new Socket(), context);
        }

        @Override
        public Socket connectSocket(int connectTimeout, Socket socket, HttpHost host, InetSocketAddress remote,
                                    InetSocketAddress local, HttpContext context) throws IOException {
            return connectPlain(connectTimeout, socket, remote, local, context);
        }
    }

    /** TCP then TLS through Apache's {@link SSLConnectionSocketFactory} (handshake and hostname verification). */
    static final class Tls implements LayeredConnectionSocketFactory {
        private final SSLConnectionSocketFactory delegate;

        Tls(SSLConnectionSocketFactory delegate) {
            this.delegate = delegate;
        }

        @Override
        public Socket createSocket(HttpContext context) throws IOException {
            return announce(new Socket(), context);
        }

        @Override
        public Socket connectSocket(int connectTimeout, Socket socket, HttpHost host, InetSocketAddress remote,
                                    InetSocketAddress local, HttpContext context) throws IOException {
            Socket plain = connectPlain(connectTimeout, socket, remote, local, context);
            return createLayeredSocket(plain, host.getHostName(), remote.getPort(), context);
        }

        @Override
        public Socket createLayeredSocket(Socket socket, String target, int port, HttpContext context)
            throws IOException {
            try {
                return announce(delegate.createLayeredSocket(socket, target, port, context), context);
            } catch (IOException e) {
                closeQuietly(socket);
                throw e instanceof TlsHandshakeException ? e : new TlsHandshakeException(e);
            }
        }
    }
}
