package net.bdavies.tomcat.server.livereload;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @author ben.davies
 */
@Slf4j
@UtilityClass
public class WebsocketConnectionFactory {
    static WebsocketConnection createConnection(Socket socket, InputStream is, OutputStream os) throws IOException {
        return new WebsocketConnection(socket, is, os);
    }
}
