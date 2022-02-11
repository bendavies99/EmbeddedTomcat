package net.bdavies.tomcat.server.livereload;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * @author ben.davies
 */
@Slf4j
public enum FrameType {
    CONT(0x00),
    TEXT(0x01),
    BINARY(0x02),
    CLOSE(0x08),
    PING(0x09),
    PONG(0x0A);

    @Getter(AccessLevel.PACKAGE)
    private final int code;

    FrameType(int i) {
        this.code = i;
    }

    static FrameType fromCode(int code) {
        for (val t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        throw new IllegalStateException("Unknown code so type cannot be found: " + code);
    }
}
