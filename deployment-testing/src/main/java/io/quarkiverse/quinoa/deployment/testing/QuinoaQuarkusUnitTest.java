package io.quarkiverse.quinoa.deployment.testing;

import static org.apache.commons.io.file.PathUtils.copyDirectory;
import static org.apache.commons.io.file.PathUtils.copyFileToDirectory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.deployment.util.FileUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * This class is in a separate module for QuarkusUnitTest classloading issues
 */
public class QuinoaQuarkusUnitTest {

    public static final String TARGET_TEST_WEBUI = "target/test-webui";

    public enum LockFile {
        YARN("yarn.lock"),
        NPM("package-lock.json"),
        PNPM("pnpm-lock.yaml");

        private final String fileName;

        LockFile(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }
    }

    private static final String OS_NAME = System.getProperty("os.name");
    private static final String CI = System.getProperty("CI");
    private final Path testDir;
    private boolean nodeModules = false;
    private String initialLockfile = "package-lock.json";
    private Boolean ci = false;

    private QuinoaQuarkusUnitTest(Path testDir) {
        this.testDir = testDir;
    }

    public static QuinoaQuarkusUnitTest create(String name) {
        return new QuinoaQuarkusUnitTest(getWebUITestDirPath(name));
    }

    public QuinoaQuarkusUnitTest initialLockfile(LockFile lockFile) {
        this.initialLockfile = lockFile.getFileName();
        return this;
    }

    public QuinoaQuarkusUnitTest nodeModules() {
        this.nodeModules = true;
        return this;
    }

    public QuinoaQuarkusUnitTest noLockfile() {
        this.initialLockfile = null;
        return this;
    }

    public QuinoaQuarkusUnitTest ci(Boolean ci) {
        this.ci = ci;
        return this;
    }

    public QuarkusUnitTest toQuarkusUnitTest() {
        return new QuarkusUnitTest()
                .setAllowTestClassOutsideDeployment(true)
                .setBeforeAllCustomizer(new Runnable() {
                    @Override
                    public void run() {
                        prepareTestWebUI(testDir, nodeModules);
                        prepareLockFile(testDir, initialLockfile);
                        if (ci != null) {
                            System.setProperty("CI", ci.toString());
                        }
                    }
                })
                .setAfterAllCustomizer(new Runnable() {
                    @Override
                    public void run() {
                        safeCleanProperty("ci", CI);
                    }
                })
                .setLogRecordPredicate(log -> true)
                .overrideConfigKey("quarkus.quinoa.ui-dir", testDir.toString());
    }

    public static Path getWebUITestDirPath(String name) {
        return Path.of(TARGET_TEST_WEBUI + "-" + name);
    }

    public static String systemBinary(String base) {
        return isWindows() ? base + ".cmd" : base;
    }

    public static boolean isWindows() {
        return OS_NAME != null && OS_NAME.startsWith("Windows");
    }

    public static void prepareTestWebUI(Path testDir, boolean nodeModules) {
        final Path webUI = Path.of("src/test/webui/");
        try {
            FileUtil.deleteDirectory(testDir);
            copyDirectory(webUI, testDir);
            if (nodeModules) {
                Files.createDirectory(testDir.resolve("node_modules"));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error while preparing the test web ui directory.", e);
        }
    }

    private static void safeCleanProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    private static void prepareLockFile(Path testDir, String toUse) {
        if (toUse != null) {
            try {
                copyFileToDirectory(Path.of("src/test/resources/lockfiles/", toUse), testDir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

}
