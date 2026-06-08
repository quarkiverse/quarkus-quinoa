package io.quarkiverse.quinoa.deployment.sbom;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.quarkiverse.quinoa.deployment.packagemanager.types.PackageManagerType;
import io.smallrye.common.os.OS;

/**
 * Runs cdxgen to generate a CycloneDX SBOM for the UI project.
 * <p>
 * The tool is executed via the package manager's dlx command (npx, pnpm dlx,
 * yarn dlx, or bunx) in the UI directory and writes CycloneDX JSON to a
 * temporary file.
 */
public final class CdxgenRunner {

    private static final Logger LOG = Logger.getLogger(CdxgenRunner.class);

    private CdxgenRunner() {
    }

    /**
     * Runs cdxgen in the given directory and writes CycloneDX JSON to the
     * specified output file. The caller is responsible for deleting the file.
     *
     * @param uiDir the UI project directory
     * @param paths additional PATH entries (e.g., managed node installation bin dirs)
     * @param packageManagerType the detected package manager type
     * @param version cdxgen version to use, or empty for latest
     * @param timeoutSeconds maximum time to wait for cdxgen to complete
     * @param outputFile path where cdxgen should write the CycloneDX JSON
     * @throws IOException if the process cannot be started or the output file is missing
     * @throws CdxgenException if cdxgen exits with a non-zero status or times out
     */
    public static void generate(Path uiDir, List<String> paths, PackageManagerType packageManagerType,
            Optional<String> version, int timeoutSeconds, Path outputFile) throws IOException {
        final String cdxgenPackage = version
                .map(v -> "@cyclonedx/cdxgen@" + v)
                .orElse("@cyclonedx/cdxgen");

        LOG.infof("Running cdxgen (%s) in %s with timeout %ds", cdxgenPackage, uiDir, timeoutSeconds);

        final List<String> command = new ArrayList<>();
        if (OS.WINDOWS.isCurrent()) {
            command.add("cmd.exe");
            command.add("/c");
        }
        addDlxCommand(command, packageManagerType);
        command.add(cdxgenPackage);
        command.add("--no-babel");
        command.add("-o");
        command.add(outputFile.toAbsolutePath().toString());

        final ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(uiDir.toFile());
        pb.redirectErrorStream(true);

        if (paths != null && !paths.isEmpty()) {
            extendPath(pb.environment(), paths);
        }

        final Process process = pb.start();
        try {
            // Drain stdout/stderr on a separate thread to prevent the pipe
            // buffer from filling up and blocking the process.
            final OutputDrainer drainer = new OutputDrainer(process);
            drainer.start();

            final boolean completed;
            try {
                completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CdxgenException("cdxgen was interrupted");
            }

            if (!completed) {
                throw new CdxgenException("cdxgen timed out after " + timeoutSeconds + " seconds");
            }

            try {
                drainer.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (process.exitValue() != 0) {
                throw new CdxgenException(
                        "cdxgen exited with code " + process.exitValue() + ": " + drainer.getOutput());
            }
        } finally {
            process.destroyForcibly();
        }

        if (!Files.isRegularFile(outputFile) || Files.size(outputFile) == 0) {
            throw new CdxgenException("cdxgen did not produce output");
        }
    }

    private static void addDlxCommand(List<String> command, PackageManagerType packageManagerType) {
        switch (packageManagerType) {
            case PNPM:
                command.add("pnpm");
                command.add("dlx");
                break;
            case YARN_BERRY:
                command.add("yarn");
                command.add("dlx");
                break;
            case BUN:
                command.add("bunx");
                break;
            default:
                command.add("npx");
                command.add("--yes");
                break;
        }
    }

    static void extendPath(Map<String, String> env, List<String> paths) {
        final String pathKey = OS.WINDOWS.isCurrent() ? findPathKey(env) : "PATH";
        final String existing = env.getOrDefault(pathKey, "");
        final StringBuilder newPath = new StringBuilder();
        for (String p : paths) {
            newPath.append(p).append(File.pathSeparator);
        }
        newPath.append(existing);
        env.put(pathKey, newPath.toString());
    }

    private static String findPathKey(Map<String, String> env) {
        for (String key : env.keySet()) {
            if ("PATH".equalsIgnoreCase(key)) {
                return key;
            }
        }
        return "PATH";
    }

    private static class OutputDrainer extends Thread {
        private final Process process;
        private final StringBuilder buf = new StringBuilder();

        OutputDrainer(Process process) {
            super("cdxgen-output-drainer");
            setDaemon(true);
            this.process = process;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    buf.append(line).append(System.lineSeparator());
                }
            } catch (IOException e) {
                buf.append("[error reading output: ").append(e.getMessage()).append(']');
            }
        }

        String getOutput() {
            return buf.toString();
        }
    }

    public static class CdxgenException extends RuntimeException {
        public CdxgenException(String message) {
            super(message);
        }
    }
}
