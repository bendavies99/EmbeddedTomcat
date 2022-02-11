package net.bdavies.tomcat.server.livereload;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * @author ben.davies
 */
@Slf4j
public class WebsocketOutputStream extends FilterOutputStream {
    public WebsocketOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        this.out.write(b, off, len);
    }

    void writeHttp(InputStream content, String contentType) throws IOException {
        byte[] bytes = getByteArray(content);
        writeHeaders("HTTP/1.1 200 OK", "Content-Type: " + contentType, "Content-Length: " + bytes.length,
                "Connection: close");
        write(bytes);
        flush();
    }

    private byte[] getByteArray(InputStream content) throws IOException {
        val r = new BufferedReader(new InputStreamReader(content));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            builder.append(line).append("\r\n");
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    void writeHeaders(String... headers) throws IOException {
        StringBuilder response = new StringBuilder();
        for (String header : headers) {
            response.append(header).append("\r\n");
        }
        response.append("\r\n");
        write(response.toString().getBytes());
    }
}
