package net.bdavies.tomcat.server.livereload;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static net.bdavies.tomcat.server.livereload.Constants.*;

/**
 * @author ben.davies
 */
@Slf4j
@Getter
class WebsocketFrame {
    private final FrameType type;
    private final byte[] payload;

    WebsocketFrame(FrameType type, byte[] payload) {
        if (payload == null || type == null) {
            throw new IllegalStateException("Type or payload cannot be null");
        }
        this.payload = payload;
        this.type = type;
    }

    WebsocketFrame(String payload) {
        this(DEFAULT_FRAME_TYPE, payload == null ? EMPTY_BYTE_ARRAY : payload.getBytes(StandardCharsets.UTF_8));
    }

    WebsocketFrame(FrameType type) {
        this(type, EMPTY_BYTE_ARRAY);
    }

    @Override
    public String toString() {
        return new String(payload);
    }

    void write(OutputStream os) throws IOException {

        os.write(0x80 | type.getCode());
        if (payload.length < 126) {
            os.write(payload.length & 0x7F);
        } else {
          os.write(0x7E);
          os.write(payload.length >> 8 & 0xFF);
          os.write(payload.length & 0xFF);
        }
        os.write(payload);
        os.flush();
    }

    static WebsocketFrame readFromStream(WebsocketInputStream is) throws IOException {
        int firstByte = is.checkedRead();
        if ((firstByte & 0x80) == 0) throw new IllegalStateException("Fragmented frames are not supported!");
        int maskAndLength = is.checkedRead();
        boolean hasMask = (maskAndLength & 0x80) != 0;
        int length = (maskAndLength & 0x7F);
        if (length == 127) throw new IllegalStateException("Large frames are not supported");
        if (length == 126) {
            length = ((is.checkedRead()) << 8 | is.checkedRead());
        }
        byte[] mask = new byte[4];
        if (hasMask) {
            is.readFully(mask, 0, mask.length);
        }

        byte[] payload = new byte[length];
        is.readFully(payload, 0, length);
        if (hasMask) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= mask[i % 4];
            }
        }

        return new WebsocketFrame(FrameType.fromCode(firstByte & 0x0F), payload);
    }
}
