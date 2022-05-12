package io.quarkiverse.quinoa.test;

import static org.apache.commons.io.file.PathUtils.copyFileToDirectory;
import static org.apache.commons.io.file.PathUtils.delete;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

public class QuinoaPrepareWebUI {

    public static void prepare() {
        prepareLockfile("package-lock.json");
        System.setProperty("CI", "false");
    }

    public static void clean() {
        System.clearProperty("CI");
    }

    public static void prepareLockfile(String toUse) {
        deleteLockfiles();
        try {
            copyFileToDirectory(Path.of("src/test/resources/lockfiles/", toUse), Path.of("src/test/webui"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void deleteLockfiles() {
        try {
            delete(Path.of("src/test/webui/package-lock.json"));
            delete(Path.of("src/test/webui/pnpm-lock.yaml"));
            delete(Path.of("src/test/webui/yarn.lock"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
