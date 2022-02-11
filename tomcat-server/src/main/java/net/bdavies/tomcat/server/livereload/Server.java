package net.bdavies.tomcat.server.livereload;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author ben.davies
 */
@Slf4j
public class Server implements Runnable {
    private final ExecutorService service;
    private final List<WebsocketConnection> connections;
    private final int serverPort;
    private ServerSocket socket;
    private final Thread connectionThread;
    private boolean isRunning;

    public Server(int serverPort) {
        this.serverPort = serverPort;
        this.connections = new LinkedList<>();
        this.service = Executors.newCachedThreadPool();
        connectionThread = new Thread(this, "Tomcat-LR-Server-" + serverPort);
        connectionThread.setDaemon(true);
    }

    public synchronized void start() {
        if (isRunning) return;
        isRunning = true;
        connectionThread.start();
    }

    public synchronized void stop() {
        if (!isRunning) return;
        isRunning = false;
        try {
            socket.close();
            connections.forEach(c -> {
                try {
                    c.close();
                } catch (IOException e) {
                    log.error("Failed to stop the live reload server", e);
                }
            });

            service.shutdownNow();
            connectionThread.join(10);
        } catch (InterruptedException | IOException e) {
            log.error("Failed to stop the live reload server", e);
        }
    }

    @Override
    public synchronized void run() {
        try {
            this.socket = new ServerSocket(serverPort);
            log.info("Starting live reload sever on port {}", serverPort);
            acceptConnections();
        } catch (IOException e) {
            log.error("Couldn't start Livereload server on port {}", serverPort, e);
        }
    }

    private synchronized void acceptConnections() {
        while (isRunning && !socket.isClosed()) {
            try {
                Socket userSocket = this.socket.accept();
                userSocket.setSoTimeout(Constants.DEFAULT_LR_PORT);
                this.service.execute(new ConnectionHandler(userSocket, this));
            } catch (SocketTimeoutException e) {
                //Ignore sockets timing out
            } catch (IOException e) {
                log.error("Something went wrong while accepting clients", e);
            }
        }
    }

    public void publishChangeToConnections() {
        this.connections.forEach(WebsocketConnection::publishChange);
    }

    public void addConnection(WebsocketConnection connection) {
        this.connections.add(connection);
    }

    public void removeConnection(WebsocketConnection connection) {
        this.connections.remove(connection);
    }
}
