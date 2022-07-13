package io.quarkiverse.quinoa.deployment.packagemanager;

class YarnPackageManagerCommands implements PackageManagerCommands {
    static final String yarn = "yarn";
    private final String binary;

    public YarnPackageManagerCommands(String binary) {
        this.binary = binary;
    }

    @Override
    public String binary() {
        return binary;
    }

    @Override
    public Command install(boolean frozenLockfile) {
        if (frozenLockfile) {
            return new Command(binary() + " install --frozen-lockfile");
        }
        return new Command(binary() + " install");
    }
}
