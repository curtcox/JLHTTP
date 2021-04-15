package net.freeutils.httpserver;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;

import static net.freeutils.httpserver.Utils.*;

/**
 * The {@code SocketHandlerThread} handles accepted sockets.
 */
final class SocketHandlerThread extends Thread {

    final Executor executor;
    final int socketTimeout;
    final ServerSocket serv;
    final int port;
    final HTTPServer server;

    SocketHandlerThread(Executor executor, int socketTimeout, ServerSocket serv, int port, HTTPServer server) {
        this.executor = executor;
        this.socketTimeout = socketTimeout;
        this.serv = serv;
        this.port = port;
        this.server = server;
    }

    @Override
    public void run() {
        setName(getClass().getSimpleName() + "-" + port);
        try {
            while (serv != null && !serv.isClosed()) {
                final Socket sock = serv.accept();
                executor.execute(() -> {
                    try {
                        try {
                            sock.setSoTimeout(socketTimeout);
                            sock.setTcpNoDelay(true); // we buffer anyway, so improve latency
                            server.handleConnection(sock.getInputStream(), sock.getOutputStream());
                        } finally {
                            try {
                                // RFC7230#6.6 - close socket gracefully
                                // (except SSL socket which doesn't support half-closing)
                                if (!(sock instanceof SSLSocket)) {
                                    sock.shutdownOutput(); // half-close socket (only output)
                                    transfer(sock.getInputStream(), null, -1); // consume input
                                }
                            } finally {
                                sock.close(); // and finally close socket fully
                            }
                        }
                    } catch (IOException ignore) {}
                });
            }
        } catch (IOException ignore) {}
    }
}
