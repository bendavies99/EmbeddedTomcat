package net.bdavies.tomcat.server.livereload;

import lombok.extern.slf4j.Slf4j;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author ben.davies
 */
@Slf4j
public class WebsocketInputStream extends FilterInputStream {
    private static final int BUFFER_SIZE = 4096;
    private static final String HEADER_END = "\r\n\r\n";

    protected WebsocketInputStream(InputStream in) {
        super(in);
    }

    String readHTTPHeader() throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        StringBuilder content = new StringBuilder(BUFFER_SIZE);
        while (content.indexOf(HEADER_END) == -1) {
            int amountRead = checkedRead(buffer, 0, BUFFER_SIZE);
            content.append(new String(buffer, 0, amountRead));
        }
        return content.substring(0, content.indexOf(HEADER_END));
    }

    private int checkedRead(byte[] buffer, int offset, int length) throws IOException {
        int amountRead = read(buffer, offset, length);
        if (amountRead == -1) {
            throw new IOException("end of stream");
        }
        return amountRead;
    }

    /**
     * Read a single byte from the stream (checking that the end of the stream hasn't been
     * reached).
     * @return the content
     * @throws IOException in case of I/O errors
     */
    int checkedRead() throws IOException {
        int b = read();
        if (b == -1) {
            throw new IOException("End of stream");
        }
        return (b & 0xff);
    }

    void readFully(byte[] buffer, int offset, int length) throws IOException {
        while (length > 0) {
            int amountRead = checkedRead(buffer, offset, length);
            offset += amountRead;
            length -= amountRead;
        }
    }
}
