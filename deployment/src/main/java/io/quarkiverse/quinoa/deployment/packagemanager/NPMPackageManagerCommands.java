package io.quarkiverse.quinoa.deployment.packagemanager;

class NPMPackageManagerCommands implements PackageManagerCommands {
    static final String npm = "npm";
    private final String binary;

    public NPMPackageManagerCommands(String binary) {
        this.binary = binary;
    }

    @Override
    public String binary() {
        return binary;
    }

    @Override
    public Command install(boolean frozenLockfile) {
        if (frozenLockfile) {
            return new Command(binary() + " ci");
        }
        return new Command(binary() + " install");
    }

}
