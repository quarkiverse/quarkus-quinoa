package io.quarkiverse.quinoa.deployment.packagemanager;

import static io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerInstall.PNPM_MJS_PATH;
import static io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerInstall.PNPM_PATH;
import static io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerInstall.resolvePnpmPath;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackageManagerInstallPnpmPathTest {

    @TempDir
    Path installDir;

    @Test
    void shouldUseCjsEntrypointForPnpmBefore12() throws IOException {
        createFile(PNPM_PATH);
        assertThat(resolvePnpmPath(installDir)).isEqualTo(PNPM_PATH);
    }

    @Test
    void shouldUseMjsEntrypointForPnpm12() throws IOException {
        createFile(PNPM_MJS_PATH);
        assertThat(resolvePnpmPath(installDir)).isEqualTo(PNPM_MJS_PATH);
    }

    @Test
    void shouldPreferCjsEntrypointWhenBothExist() throws IOException {
        createFile(PNPM_PATH);
        createFile(PNPM_MJS_PATH);
        assertThat(resolvePnpmPath(installDir)).isEqualTo(PNPM_PATH);
    }

    @Test
    void shouldDefaultToCjsEntrypointWhenNoneExist() {
        assertThat(resolvePnpmPath(installDir)).isEqualTo(PNPM_PATH);
    }

    private void createFile(String path) throws IOException {
        Path file = installDir.resolve(path);
        Files.createDirectories(file.getParent());
        Files.createFile(file);
    }
}
