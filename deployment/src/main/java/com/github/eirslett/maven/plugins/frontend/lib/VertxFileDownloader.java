package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.jboss.logging.Logger;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class VertxFileDownloader implements FileDownloader {
    private static final Logger LOG = Logger.getLogger(VertxFileDownloader.class);
    private final Vertx vertx;
    private WebClient webClient;

    public VertxFileDownloader(Vertx vertx) {
        this.vertx = vertx;
        webClient = WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                .setFollowRedirects(true)
                .setTrustAll(true)
                .setKeepAlive(true));
    }

    public void download(String downloadUrl, String destination, String userName, String password) throws DownloadException {
        System.setProperty("https.protocols", "TLSv1.2");
        String fixedDownloadUrl = downloadUrl;

        try {
            fixedDownloadUrl = FilenameUtils.separatorsToUnix(fixedDownloadUrl);
            URI downloadURI = new URI(fixedDownloadUrl);
            final Path destinationPath = Path.of(destination);
            if ("file".equalsIgnoreCase(downloadURI.getScheme())) {
                Files.copy(Paths.get(downloadURI), destinationPath);
            } else {
                final HttpRequest<Buffer> request = webClient.getAbs(downloadUrl);
                final CountDownLatch latch = new CountDownLatch(1);
                Future<HttpResponse<Buffer>> future = request.send();
                future.onComplete((r) -> latch.countDown());
                latch.await(5, TimeUnit.MINUTES);
                if (future.succeeded()) {
                    Files.write(destinationPath, future.result().body().getBytes());
                } else {
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
