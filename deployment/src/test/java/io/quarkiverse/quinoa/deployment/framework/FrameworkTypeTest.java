package io.quarkiverse.quinoa.deployment.framework;

import java.io.InputStream;
import java.util.Optional;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.assertj.core.api.Assertions;
import org.jboss.logging.Logger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class FrameworkTypeTest {

    private static final Logger LOG = Logger.getLogger(FrameworkTypeTest.class);

    @ParameterizedTest
    @CsvSource({
            "react-create-app-exact, REACT, start, false",
            "react-create-app-different-dev-script, REACT, dev, true",
            "react-create-app-different-dev-script-envs, REACT, dev, true",
            "react-create-app-multiple-ok, REACT, start, false",
            "react-create-app-multiple-ok-guess, REACT, start, true",
            "react-create-app-multiple-ok-guess-command, REACT, develop, true",
            "react-create-app-multiple-nok,,,",
            "angular-exact, ANGULAR, start, false",
            "angular-envs, ANGULAR, start, false",
            "angular-args, ANGULAR, start, true",
    })
    void testDetection(String jsonResource, String type, String devScript, Boolean customized) {

        ClassLoader classLoader = FrameworkTypeTest.class.getClassLoader();

        LOG.infof("Testing %s", jsonResource);

        InputStream jsonStream = classLoader.getResourceAsStream("frameworks/" + jsonResource + ".json");
        JsonObject jsonObject = Json.createReader(jsonStream).readObject();

        Optional<FrameworkType.DetectedFramework> result = FrameworkType.detectFramework(jsonObject);

        if (type == null) {
            Assertions.assertThat(result).isEmpty();
        } else {
            Assertions.assertThat(result)
                    .isPresent()
                    .contains(new FrameworkType.DetectedFramework(FrameworkType.valueOf(type), devScript, customized));
        }

        LOG.info("\n");
    }
}
