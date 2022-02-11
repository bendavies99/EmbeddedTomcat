package net.bdavies.tomcat.server.livereload;

import lombok.extern.slf4j.Slf4j;

/**
 * @author ben.davies
 */
@Slf4j
public class Constants {
    public static final int DEFAULT_LR_PORT = 35729;
    public static final int READ_DATA_TIMEOUT = 4000; // 4 seconds
    public static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public static final FrameType DEFAULT_FRAME_TYPE = FrameType.TEXT;
}
