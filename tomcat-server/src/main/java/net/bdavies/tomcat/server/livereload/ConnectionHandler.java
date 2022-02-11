package net.bdavies.tomcat.server.livereload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author ben.davies
 */
@Slf4j
@RequiredArgsConstructor
public class ConnectionHandler implements Runnable {
    private final Socket socket;
    private final Server server;

    @Override
    public void run() {
        try(val is = socket.getInputStream()) {
            setupConnection(is);
        } catch (Exception e) {
            log.error("Failed to handle socket {}:{}", socket.getInetAddress().getHostAddress(), socket.getPort());
        }
    }

    private void setupConnection(InputStream is) throws Exception {
        try(OutputStream os = socket.getOutputStream()) {
            WebsocketConnection connection = WebsocketConnectionFactory.createConnection(socket, is, os);
            runWebSocket(connection);
        } finally {
            socket.close();
        }
    }

    private void runWebSocket(WebsocketConnection connection) throws IOException {
        try {
            server.addConnection(connection);
            connection.run();
        } finally {
            connection.close();
            server.removeConnection(connection);
        }
    }
}
