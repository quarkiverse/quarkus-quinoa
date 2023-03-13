package io.quarkiverse.quinoa.deployment.devui;

import java.util.Optional;
import java.util.OptionalInt;

import io.netty.util.internal.StringUtil;
import io.quarkiverse.quinoa.deployment.QuinoaConfig;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.PageBuilder;

/**
 * Dev UI card for displaying important details such as the Node.js version.
 */
public class QuinoaDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    void createCard(BuildProducer<CardPageBuildItem> cardPageBuildItemBuildProducer, QuinoaConfig quinoaConfig) {
        final CardPageBuildItem card = new CardPageBuildItem("Quinoa");

        final Optional<String> node = quinoaConfig.packageManagerInstall.nodeVersion;
        if (node.isPresent() && !node.isEmpty()) {
            final String nodeVersion = node.get();
            final PageBuilder nodejsPage = Page.externalPageBuilder("Node.js")
                    .icon("font-awesome-brands:square-js")
                    .url("https://nodejs.org/")
                    .isHtmlContent()
                    .staticLabel(nodeVersion);
            card.addPage(nodejsPage);
        }

        final String npmVersion = quinoaConfig.packageManagerInstall.npmVersion;
        if (!StringUtil.isNullOrEmpty(npmVersion)) {
            final PageBuilder nodejsPage = Page.externalPageBuilder("NPM")
                    .icon("font-awesome-brands:square-js")
                    .url("https://www.npmjs.com/")
                    .isHtmlContent()
                    .staticLabel(npmVersion);
            card.addPage(nodejsPage);
        }

        final OptionalInt port = quinoaConfig.devServer.port;
        if (port.isPresent() && port.getAsInt() > 0) {
            final PageBuilder portPage = Page.externalPageBuilder("Port")
                    .icon("font-awesome-solid:plug")
                    .url(String.format("https://localhost:%d", port.getAsInt()))
                    .doNotEmbed()
                    .staticLabel(String.valueOf(port.getAsInt()));
            card.addPage(portPage);
        }

        card.setCustomCard("qwc-quinoa-card.js");

        cardPageBuildItemBuildProducer.produce(card);
    }

}
