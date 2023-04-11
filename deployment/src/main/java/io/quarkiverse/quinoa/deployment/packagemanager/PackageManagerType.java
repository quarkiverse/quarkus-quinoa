package io.quarkiverse.quinoa.deployment.packagemanager;

public enum PackageManagerType {

    YARN("yarn", "yarn.lock"),
    NPM("npm", "package-lock.json"),
    PNPM("pnpm", "pnpm-lock.yaml");

    private final String command;
    private final String lockFile;

    PackageManagerType(String command, String lockFile) {
        this.command = command;
        this.lockFile = lockFile;
    }

    public String getCommand() {
        return command;
    }

    public String getLockFile() {
        return lockFile;
    }
}