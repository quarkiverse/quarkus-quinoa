package io.quarkiverse.quinoa.deployment.testing;

import static org.apache.commons.io.file.PathUtils.copyFileToDirectory;
import static org.apache.commons.io.file.PathUtils.delete;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import io.quarkus.test.QuarkusUnitTest;

/**
 * This class is in a separate module for QuarkusUnitTest classloading issues
 */
public class QuinoaQuarkusUnitTest {

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

    private String initialLockFile = "package-lock.json";
    private String osName;
    private Boolean ci = false;

    private QuinoaQuarkusUnitTest() {
    }

    public static QuinoaQuarkusUnitTest create() {
        return new QuinoaQuarkusUnitTest();
    }

    public QuinoaQuarkusUnitTest initialLockFile(LockFile lockFile) {
        this.initialLockFile = lockFile.getFileName();
        return this;
    }

    public QuinoaQuarkusUnitTest osName(String osName) {
        this.osName = osName;
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
                        prepareLockFile(initialLockFile);
                        if (ci != null) {
                            System.setProperty("CI", ci.toString());
                        }
                        if (osName != null) {
                            System.setProperty("os.name", osName);
                        }
                    }
                })
                .setAfterAllCustomizer(new Runnable() {
                    @Override
                    public void run() {
                        deleteLockFiles();
                        safeCleanProperty("ci", CI);
                        safeCleanProperty("os.name", OS_NAME);
                    }
                })
                .setLogRecordPredicate(log -> true)
                .overrideConfigKey("quarkus.quinoa.ui-dir", "src/test/webui");
    }

    private static void safeCleanProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    private static void prepareLockFile(String toUse) {
        deleteLockFiles();
        if (toUse != null) {
            try {
                copyFileToDirectory(Path.of("src/test/resources/lockfiles/", toUse), Path.of("src/test/webui"));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static void deleteLockFiles() {
        try {
            delete(Path.of("src/test/webui/package-lock.json"));
            delete(Path.of("src/test/webui/pnpm-lock.yaml"));
            delete(Path.of("src/test/webui/yarn.lock"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
