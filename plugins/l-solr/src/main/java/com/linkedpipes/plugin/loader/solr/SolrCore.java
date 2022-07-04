package com.linkedpipes.plugin.loader.solr;

import com.linkedpipes.etl.executor.api.v1.LpException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.List;

class SolrCore {

    private static final Logger LOG = LoggerFactory.getLogger(SolrCore.class);

    private final String serverUrl;

    private final String coreName;

    private String authorizationHeader = null;

    public SolrCore(String url, String coreName) {
        this.serverUrl = url;
        this.coreName = coreName;
    }

    public void setCredentials(String userName, String password) {
        String auth = userName + ":" + password;
        byte[] authBytes = auth.getBytes(Charset.forName("ISO-8859-1"));
        byte[] encodedAuth = Base64.encodeBase64(authBytes);
        this.authorizationHeader = "Basic " + new String(encodedAuth);
    }


    public void deleteData() throws LpException {
        URL url = createDeleteUrl();
        try {
            executePostWithEmptyBody(url);
        } catch (IOException | LpException ex) {
            throw new LpException("Can't delete data.", ex);
        }
    }

    private void executePostWithEmptyBody(URL url)
            throws IOException, LpException {
        HttpURLConnection connection = null;
        try {
            connection = createHttpConnection(url);
            connection.connect();
            checkResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private URL createDeleteUrl() throws LpException {
        try {
            return new URL(serverUrl + "/" + coreName +
                    "/update?stream.body=<delete><query>*:*</query></delete>");
        } catch (MalformedURLException ex) {
            throw new LpException("Invalid Solr URL.", ex);
        }
    }

    public void uploadFile(File file) throws LpException {
        uploadFile(file, createUpdateUrl());
    }

    private URL createUpdateUrl() throws LpException {
        try {
            return new URL(serverUrl + "/" + coreName + "/update/json/docs");
        } catch (MalformedURLException ex) {
            throw new LpException("Invalid Solr URL.", ex);
        }
    }

    private void uploadFile(File file, URL url) throws LpException {
        HttpURLConnection connection = null;
        try (InputStream is = new FileInputStream(file)) {
            connection = createHttpConnection(url);
            connection.connect();
            try (OutputStream out = connection.getOutputStream()) {
                IOUtils.copy(is, out);
            }
            checkResponse(connection);
        } catch (IOException ex) {
            throw new LpException("Can't upload file.", ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private HttpURLConnection createHttpConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setAllowUserInteraction(false);
        connection.setRequestProperty("Content-type", "text/json");
        // Use chunk mode with auto chunk size. This is necessary for
        // large data, otherwise HttpURLConnection tries to store all
        // the data to calculate length (for header).
        connection.setChunkedStreamingMode(0);
        addAuthorizationHeader(connection);
        return connection;
    }

    private void addAuthorizationHeader(URLConnection connection) {
        if (authorizationHeader == null) {
            return;
        }
        connection.setRequestProperty("Authorization", authorizationHeader);
    }

    private void checkResponse(HttpURLConnection connection)
            throws IOException, LpException {
        int responseCode = connection.getResponseCode();
        LOG.info("Response code is {}", responseCode);
        printResponseMessage(connection);
        if (responseCode >= 400) {
            // Print error response.
            StringBuilder errorMessage = new StringBuilder();
            List<String> response = IOUtils.readLines(
                    connection.getErrorStream());
            for (String line : response) {
                errorMessage.append(line);
                errorMessage.append("\n");
            }
            LOG.error("Response (error): {}", errorMessage);
            throw new LpException("Request failed {} : {}",
                    responseCode, errorMessage);
        }
    }

    private void printResponseMessage(HttpURLConnection connection) {
        StringBuilder logString = new StringBuilder();
        try {
            List<String> response = IOUtils.readLines(
                    connection.getInputStream());
            for (String line : response) {
                logString.append(line);
                logString.append("\n");
            }
            LOG.debug("Response: {}", logString);
        } catch (IOException ex) {
            LOG.error("Can't read response.", ex);
        }
    }

    public void commit() throws LpException {
        URL url = createCommitUrl();
        try {
            executePostWithEmptyBody(url);
        } catch (IOException | LpException ex) {
            throw new LpException("Can't commit.", ex);
        }
    }

    private URL createCommitUrl() throws LpException {
        try {
            return new URL(serverUrl + "/" + coreName + "/update?commit=true");
        } catch (MalformedURLException ex) {
            throw new LpException("Invalid Solr URL.", ex);
        }
    }

}
