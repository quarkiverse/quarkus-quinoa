package io.quarkiverse.quinoa.deployment.packagemanager;

class PNPMCommands implements Commands {

    static String pnpm = "pnpm";
    private final String binary;

    public PNPMCommands(String binary) {
        this.binary = binary;
    }

    @Override
    public String binary() {
        return binary;
    }

    @Override
    public Command install(boolean frozenLockfile) {
        if (frozenLockfile) {
            return new Command(binary(), "install", "--frozen-lockfile");
        }
        return new Command(binary(), "install");
    }
}
