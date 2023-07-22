package org.openjfx;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class PDFManager {

    public String getPdfAsBase64(String fileUrl) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestMethod("GET");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // Use try-with-resources to ensure the InputStream is closed after use
        try (InputStream inputStream = httpURLConnection.getInputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
        } finally {
            httpURLConnection.disconnect();
        }

        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
    }
}
