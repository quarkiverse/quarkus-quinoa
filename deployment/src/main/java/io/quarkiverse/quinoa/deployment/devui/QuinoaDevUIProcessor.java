package io.quarkiverse.quinoa.deployment.devui;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import io.netty.util.internal.StringUtil;
import io.quarkiverse.quinoa.deployment.QuinoaConfig;
import io.quarkiverse.quinoa.deployment.QuinoaDirectoryBuildItem;
import io.quarkiverse.quinoa.deployment.packagemanager.PackageManager;
import io.quarkiverse.quinoa.devui.QuinoaJsonRpcService;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.ExternalPageBuilder;
import io.quarkus.devui.spi.page.FooterPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.PageBuilder;
import io.quarkus.devui.spi.page.WebComponentPageBuilder;

/**
 * Dev UI card for displaying important details such as the Node.js version.
 */
public class QuinoaDevUIProcessor {

    private static final String EXTENSION_NAME = "Quinoa";

    @BuildStep(onlyIf = IsDevelopment.class)
    void createCard(BuildProducer<CardPageBuildItem> cardPageBuildItemBuildProducer,
            BuildProducer<FooterPageBuildItem> footerProducer,
            QuinoaConfig quinoaConfig) {
        final CardPageBuildItem card = new CardPageBuildItem();

        final Optional<String> node = quinoaConfig.packageManagerInstall.nodeVersion;
        if (node.isPresent()) {
            final String nodeVersion = node.get();
            final PageBuilder<ExternalPageBuilder> nodejsPage = Page.externalPageBuilder("Node.js")
                    .icon("font-awesome-brands:square-js")
                    .url("https://nodejs.org/")
                    .doNotEmbed()
                    .staticLabel(nodeVersion);
            card.addPage(nodejsPage);
        }

        final String npmVersion = quinoaConfig.packageManagerInstall.npmVersion;
        if (!StringUtil.isNullOrEmpty(npmVersion)) {
            final PageBuilder<ExternalPageBuilder> nodejsPage = Page.externalPageBuilder("NPM")
                    .icon("font-awesome-brands:square-js")
                    .url("https://www.npmjs.com/")
                    .doNotEmbed()
                    .staticLabel(npmVersion);
            card.addPage(nodejsPage);
        }

        final OptionalInt port = quinoaConfig.devServer.port;
        if (port.isPresent() && port.getAsInt() > 0) {
            final PageBuilder<ExternalPageBuilder> portPage = Page.externalPageBuilder("Port")
                    .icon("font-awesome-solid:plug")
                    .url(String.format("https://localhost:%d", port.getAsInt()))
                    .doNotEmbed()
                    .staticLabel(String.valueOf(port.getAsInt()));
            card.addPage(portPage);
        }

        card.setCustomCard("qwc-quinoa-card.js");
        cardPageBuildItemBuildProducer.produce(card);

        // Node Log Console
        WebComponentPageBuilder nodeLogPageBuilder = Page.webComponentPageBuilder()
                .icon("font-awesome-brands:square-js")
                .title(EXTENSION_NAME)
                .componentLink("qwc-quinoa-log.js");

        footerProducer.produce(new FooterPageBuildItem(nodeLogPageBuilder));
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem registerJsonRpcBackend(Optional<QuinoaDirectoryBuildItem> quinoaDirectoryBuildItem,
            QuinoaConfig quinoaConfig) {
        DevConsoleManager.register("quinoa-install-action",
                install(quinoaDirectoryBuildItem, quinoaConfig));
        return new JsonRPCProvidersBuildItem(QuinoaJsonRpcService.class);
    }

    private Function<Map<String, String>, String> install(Optional<QuinoaDirectoryBuildItem> quinoaDirectoryBuildItem,
            QuinoaConfig quinoaConfig) {
        return (map -> {
            try {
                final PackageManager packageManager = quinoaDirectoryBuildItem.orElseThrow().getPackageManager();

                // install or update packages
                packageManager.install(false);

                return "installed";
            } catch (Exception e) {
                return e.getMessage();
            }
        });
    }
}
