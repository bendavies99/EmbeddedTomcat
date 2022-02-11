package net.bdavies.tomcat.server.livereload;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * @author ben.davies
 */
@Slf4j
public class WebsocketConnection implements Runnable {
    private final Socket socket;
    private final WebsocketInputStream is;
    private final WebsocketOutputStream os;
    private final String header;
    private volatile boolean webSocket;
    private volatile boolean running;

    private static final Pattern WEBSOCKET_KEY_PATTERN = Pattern.compile("^sec-websocket-key:(.*)$",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);


    public WebsocketConnection(Socket socket, InputStream is, OutputStream os) throws IOException {
        this.socket = socket;
        this.is = new WebsocketInputStream(is);
        this.os = new WebsocketOutputStream(os);

        String header = this.is.readHTTPHeader();
        this.header = header;
    }

    @Override
    public void run() {
        try {
            running = true;
            String lowerCaseHeader = this.header.toLowerCase();
            if (lowerCaseHeader.contains("upgrade: websocket") && lowerCaseHeader.contains("sec-websocket-version: 13")) {
                log.debug("Established livereload connection [{}:{}]", socket.getInetAddress(), socket.getPort());
                log.trace("Received header from client [{}]", header);
                runWebSocket();
            }
            if (lowerCaseHeader.contains("get /livereload.js")) {
                try {
                    //Get the livereload.js file from Spring Boot because I am lazy :)
                    URL url = new URL("https://raw.githubusercontent.com/spring-projects/spring-boot/" +
                            "main/spring-boot-project/spring-boot-devtools/src/main/resources/org/springframework/boot/devtools/livereload/livereload.js");
                    URLConnection connection = url.openConnection();
                    val jsIs = connection.getInputStream();
                    this.os.writeHttp(jsIs, "text/javascript");
                } catch (IOException e) {
                    log.error("Failed to get the livereload.js file", e);
                }
            }
        } catch (Exception e) {
            if (e instanceof ConnectionLostException) {
                log.debug("Lost connection to: {}:{}", socket.getInetAddress(), socket.getPort());
            } else {
                log.error("Something went wrong while trying to run the connection {}:{}", this.socket.getInetAddress(), this.socket.getPort(), e);
            }
        }
    }

    private void runWebSocket() throws Exception {
        this.webSocket = true;
        String accept = getAcceptRes();
        this.os.writeHeaders("HTTP/1.1 101 Switching Protocols", "Upgrade: websocket", "Connection: Upgrade",
                "Sec-WebSocket-Accept: " + accept);
        new WebsocketFrame("{\"command\":\"hello\",\"protocols\":[\"http://livereload.com/protocols/official-7\"],"
                + "\"serverName\":\"EmbeddedTomcat\"}").write(this.os);
        while (this.running) {
            readFrame();
        }
    }

    private void readFrame() throws Exception {
        try {
            WebsocketFrame frame = WebsocketFrame.readFromStream(this.is);
            if (frame.getType() == FrameType.PING) {
                writeFrame(new WebsocketFrame(FrameType.PONG));
            } else if (frame.getType() == FrameType.CLOSE) {
                throw new ConnectionLostException(socket.getInetAddress().toString(), socket.getPort());
            } else if (frame.getType() == FrameType.TEXT) {
                log.trace("Received LR Text frame: {}", frame);
            } else {
                throw new IOException("Unknown frame type " + frame + " : " + frame.getType());
            }
        } catch (SocketTimeoutException e) {
            writeFrame(new WebsocketFrame(FrameType.PING));
            val f = WebsocketFrame.readFromStream(is);
            if (f.getType() != FrameType.PONG) {
                throw new IllegalStateException("The host did not return PONG something went wrong");
            }
        }
    }

    private void writeFrame(WebsocketFrame websocketFrame) throws IOException {
        websocketFrame.write(this.os);
    }

    private String getAcceptRes() throws NoSuchAlgorithmException {
        val m = WEBSOCKET_KEY_PATTERN.matcher(this.header);
        if (!m.find()) {
            log.warn("No Websocket key");
            throw new IllegalStateException("No Websocket key");
        }
        String res = m.group(1).trim() + Constants.WEBSOCKET_GUID;
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        messageDigest.update(res.getBytes(StandardCharsets.UTF_8), 0, res.length());
        return Base64.getEncoder().encodeToString(messageDigest.digest());
    }

    public void publishChange() {
        if (this.webSocket) {
            log.debug("Triggering LiveReload");
            try {
                writeFrame(new WebsocketFrame("{\"command\":\"reload\",\"path\":\"/\"}"));
            } catch (IOException e) {
                log.error("Failed to write frame to socket {}:{}", socket.getInetAddress().toString(), socket.getPort());
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WebsocketConnection that = (WebsocketConnection) o;

        return socket != null ? socket.getInetAddress().equals(that.socket.getInetAddress()) && socket.getPort() == that.socket.getPort()
                : that.socket == null;
    }

    void close() throws IOException {
        this.running = false;
        this.webSocket = false;
        this.socket.close();
    }
}
