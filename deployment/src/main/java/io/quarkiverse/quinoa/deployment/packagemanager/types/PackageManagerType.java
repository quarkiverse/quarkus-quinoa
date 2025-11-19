package io.quarkiverse.quinoa.deployment.packagemanager.types;

import static io.quarkiverse.quinoa.deployment.packagemanager.PackageManagerRunner.isWindows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum PackageManagerType {
    // Order matters for detection
    PNPM("pnpm", "pnpm-lock.yaml", "install --frozen-lockfile"),
    NPM("npm", "package-lock.json", "ci"),
    YARN("yarn", "yarn.lock", "install --frozen-lockfile"),
    YARN_BERRY("yarn", "yarn.lock", "install --immutable"),
    BUN("bun", "bun.lock", "install --frozen-lockfile");

    public static final String YARN_BERRY_CONFIG_FILE = ".yarnrc.yml";

    private static final Map<String, PackageManagerType> TYPES = Arrays.stream(values()).sequential()
            .collect(Collectors.toMap(PackageManagerType::getBinary, Function.identity(), (x, y) -> x, LinkedHashMap::new));

    private final String binary;

    private final String lockFile;

    private final String ciCommand;

    PackageManagerType(String binary, String lockFile, String ciCommand) {
        this.binary = binary;
        this.lockFile = lockFile;
        this.ciCommand = ciCommand;
    }

    public String getBinary() {
        return binary;
    }

    public String getOSBinary() {
        return isWindows() ? binary + ".cmd" : binary;
    }

    public String getLockFile() {
        return lockFile;
    }

    public String ciCommand() {
        return ciCommand;
    }

    public static PackageManagerType resolveConfiguredPackageManagerType(String configuredBinary,
            PackageManagerType detectedType) {
        final PackageManagerType configuredBinaryType = resolveBinaryType(configuredBinary);
        if (YARN_BERRY.equals(detectedType) && YARN.equals(configuredBinaryType)) {
            return YARN_BERRY;
        }
        return configuredBinaryType;
    }

    public static PackageManagerType detectPackageManagerType(Path directory) {
        if (Files.isRegularFile(directory.resolve(PackageManagerType.YARN.getLockFile()))) {
            return isYarnBerry(directory) ? YARN_BERRY : YARN;
        } else if (Files.isRegularFile(directory.resolve(PNPM.getLockFile()))) {
            return PNPM;
        } else {
            return NPM;
        }
    }

    public static boolean isYarnBerry(Path directory) {
        return Files.isRegularFile(directory.resolve(YARN_BERRY_CONFIG_FILE));
    }

    public static PackageManagerType resolveBinaryType(String binary) {
        for (Map.Entry<String, PackageManagerType> e : TYPES.entrySet()) {
            if (binary.contains(e.getKey())) {
                return e.getValue();
            }
        }
        throw new UnsupportedOperationException("Unsupported package manager binary: " + binary);
    }
}
