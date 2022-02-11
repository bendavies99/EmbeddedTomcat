package net.bdavies.tomcat.server.livereload;

import lombok.extern.slf4j.Slf4j;

/**
 * @author ben.davies
 */
@Slf4j
public class ConnectionLostException extends Exception {
    public ConnectionLostException(String host, int port) {
        super("Lost connection to " + host + ":" + port);
    }
}
