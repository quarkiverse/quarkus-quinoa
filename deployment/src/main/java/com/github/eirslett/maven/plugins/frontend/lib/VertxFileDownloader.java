package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.jboss.logging.Logger;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;

public class VertxFileDownloader implements FileDownloader {
    private static final Logger LOG = Logger.getLogger(VertxFileDownloader.class);
    private final Vertx vertx;
    private final WebClient webClient;

    public VertxFileDownloader(Vertx vertx) {
        this.vertx = vertx;

        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPort = System.getProperty("http.proxyPort");
        String proxyUser = System.getProperty("http.proxyUser");
        String proxyPassword = System.getProperty("http.proxyPassword");

        WebClientOptions options = new WebClientOptions()
                .setSsl(true)
                .setFollowRedirects(true)
                .setTrustAll(true)
                .setKeepAlive(true);

        // Set the proxy if it's configured
        if (proxyHost != null && proxyPort != null) {
            options.setProxyOptions(new ProxyOptions()
                    .setHost(proxyHost)
                    .setPort(Integer.parseInt(proxyPort)));

            // Optionally, add authentication
            if (proxyUser != null && proxyPassword != null) {
                options.getProxyOptions().setUsername(proxyUser).setPassword(proxyPassword);
            }
        }

        this.webClient = WebClient.create(vertx, options);
    }

    public void download(String downloadUrl, String destination, String userName, String password,
            Map<String, String> httpHeaders) throws DownloadException {
        System.setProperty("https.protocols", "TLSv1.2");
        String fixedDownloadUrl = downloadUrl;

        try {
            fixedDownloadUrl = FilenameUtils.separatorsToUnix(fixedDownloadUrl);
            URI downloadURI = new URI(fixedDownloadUrl);
            final Path destinationPath = Path.of(destination);
            if ("file".equalsIgnoreCase(downloadURI.getScheme())) {
                Files.copy(Paths.get(downloadURI), destinationPath);
            } else {
                final CountDownLatch latch = new CountDownLatch(1);
                Files.deleteIfExists(destinationPath);
                final AsyncFile destinationFile = vertx.fileSystem().openBlocking(destination, new OpenOptions());
                final HttpRequest<Buffer> httpRequest = webClient.getAbs(downloadUrl);
                if (userName != null && password != null) {
                    httpRequest.basicAuthentication(userName, password);
                }
                if (httpHeaders != null) {
                    for (Map.Entry<String, String> httpHeader : httpHeaders.entrySet()) {
                        httpRequest.putHeader(httpHeader.getKey(), httpHeader.getValue());
                    }
                }

                final Future<HttpResponse<Void>> future = httpRequest
                        .expect(ResponsePredicate.SC_SUCCESS)
                        .as(BodyCodec.pipe(destinationFile))
                        .send();
                future.onComplete((r) -> latch.countDown());
                latch.await(5, TimeUnit.MINUTES);
                if (!future.succeeded()) {
                    throw new DownloadException("Could not download " + downloadUrl, future.cause());
                }
            }
        } catch (URISyntaxException | IOException e) {
            throw new DownloadException("Could not download " + downloadUrl, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}