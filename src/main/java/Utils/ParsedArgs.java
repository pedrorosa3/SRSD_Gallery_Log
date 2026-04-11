package Utils;

public abstract class ParsedArgs {
    public final String token;
    public final String logFile;

    protected ParsedArgs(String token, String logFile) {
        this.token = token;
        this.logFile = logFile;
    }
}