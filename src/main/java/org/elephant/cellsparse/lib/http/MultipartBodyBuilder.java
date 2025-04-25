package org.elephant.cellsparse.lib.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MultipartBodyBuilder {
    private static final String CRLF = "\r\n";
    private final String boundary;
    private final ByteArrayOutputStream baos;

    public MultipartBodyBuilder(String boundary) {
        this.boundary = boundary;
        this.baos = new ByteArrayOutputStream();
    }

    public void addFormField(String name, String value) throws IOException {
        baos.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Disposition: form-data; name=\"" + escapeQuotes(name) + "\"" + CRLF)
                .getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Type: text/plain; charset=UTF-8" + CRLF).getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF.getBytes(StandardCharsets.UTF_8));
        baos.write(value.getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF.getBytes(StandardCharsets.UTF_8));
    }

    public void addJsonField(String name, String json) throws IOException {
        baos.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Disposition: form-data; name=\"" + escapeQuotes(name) + "\"" + CRLF)
                .getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Type: application/json; charset=UTF-8" + CRLF).getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF.getBytes(StandardCharsets.UTF_8));
        baos.write(json.getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF.getBytes(StandardCharsets.UTF_8));
    }

    public void addFilePart(String fieldName, String fileName, String contentType, byte[] fileData) throws IOException {
        baos.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Disposition: form-data; name=\"" + escapeQuotes(fieldName) +
                "\"; filename=\"" + escapeQuotes(fileName) + "\"" + CRLF).getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Type: " + contentType + CRLF).getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF.getBytes(StandardCharsets.UTF_8));
        baos.write(fileData);
        baos.write(CRLF.getBytes(StandardCharsets.UTF_8));
    }

    public byte[] build() throws IOException {
        baos.write(("--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8));
        return baos.toByteArray();
    }

    private String escapeQuotes(String input) {
        return input.replace("\"", "\\\"");
    }

    /**
     * Get the boundary string.
     * 
     * @return The boundary string.
     */
    public String getBoundary() {
        return boundary;
    }
}
