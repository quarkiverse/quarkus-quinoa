package io.quarkiverse.quinoa.deployment.packagemanager.types;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum PackageManagerType {
    // Order matters for detection
    PNPM("pnpm", "pnpm-lock.yaml", "install --frozen-lockfile"),
    NPM("npm", "package-lock.json", "ci"),
    YARN("yarn", "yarn.lock", "install --immutable"),
    ;

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

    public String getLockFile() {
        return lockFile;
    }

    public String ciCommand() {
        return ciCommand;
    }

    public static PackageManagerType resolveType(String binary) {
        for (Map.Entry<String, PackageManagerType> e : TYPES.entrySet()) {
            if (binary.contains(e.getKey())) {
                return e.getValue();
            }
        }
        throw new UnsupportedOperationException("Unsupported package manager binary: " + binary);
    }
}