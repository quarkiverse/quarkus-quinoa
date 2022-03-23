package io.quarkiverse.quinoa.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class QuinoaConfig {

    /**
     * Indicate if the extension should be enabled
     * Default is true if the UI directory exists and dev and prod mode
     * Default is false in test mode (to avoid building the ui during backend tests)
     */
    @ConfigItem(name = ConfigItem.PARENT)
    Optional<Boolean> enable;

    /**
     * Path to the frontend root directory.
     * If not set ${project.root}/src/main/ui/ will be used
     * If set to an absolute path then the absolute path will be used, otherwise the path
     * will be considered relative to the project root
     */
    @ConfigItem
    public Optional<String> uiDir;

    /**
     * Path of the directory which contains the resulting ui built files.
     * If not set build/ will be used
     * If set to an absolute path then the absolute path will be used, otherwise the path
     * will be considered relative to the ui path
     */
    @ConfigItem
    public Optional<String> buildDir;

    /**
     * Name of the package manager binary.
     * If not set "npm" will be used.
     * Only npm and yarn are supported for the moment
     */
    @ConfigItem
    public Optional<String> packageManager;

    /**
     * Indicate if the UI should also be tested during the build phase (i.e: npm test)
     * To be used in a {@link io.quarkus.test.junit.QuarkusTestProfile} to have UI test running during a
     * {@link io.quarkus.test.junit.QuarkusTest}
     * Default is ![](../../../../../../../../../../../../Downloads/logo192.png)false
     */
    @ConfigItem(name = "run-ui-tests")
    Optional<Boolean> runUITests;

    /**
     * Install the packages using a frozen lockfile. Don’t generate a lockfile and fail if an update is needed (useful in CI).
     * If not set it is true if environment CI=true, else it is false
     */
    @ConfigItem
    public Optional<Boolean> frozenLockfile;

    /**
     * Always install packages before building.
     * If not set, it will install packages only if the node_modules directory is absent.
     */
    @ConfigItem
    public Optional<Boolean> alwaysInstallPackages;

}
