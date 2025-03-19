package com.codeclocker.plugin.intellij.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.intellij.openapi.diagnostic.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import org.jetbrains.annotations.Nullable;

public class HttpResponseReader {

  private static final Logger LOG = Logger.getInstance(HttpResponseReader.class);

  @Nullable
  public static String readResponse(HttpURLConnection connection) {
    try (InputStream stream =
            connection.getErrorStream() != null
                ? connection.getErrorStream()
                : connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, UTF_8))) {

      StringBuilder response = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        response.append(line);
      }

      String responseString = response.toString();
      if (isNotBlank(responseString)) {
        LOG.debug("Read response: {}", responseString);
      }

      return responseString;
    } catch (IOException e) {
      LOG.error("Error reading response body: {}", e.getMessage());
      return null;
    }
  }
}
