package io.quarkiverse.quinoa.deployment.packagemanager;

class YarnPackageManagerCommands implements PackageManagerCommands {
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
            return new Command(binary() + " install");
        }
        return new Command(binary() + " install");
    }
}