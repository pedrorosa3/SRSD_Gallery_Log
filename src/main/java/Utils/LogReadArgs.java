package Utils;

import java.util.ArrayList;
import java.util.List;
import models.User;

public class LogReadArgs extends ParsedArgs {
    public final boolean isState;
    public final boolean isRooms;
    public final boolean isIntersect;
    public final List<User> users;

    private LogReadArgs(String token, String logFile, boolean isState,
            boolean isRooms, boolean isIntersect, List<User> users) {
        super(token, logFile);
        this.isState = isState;
        this.isRooms = isRooms;
        this.isIntersect = isIntersect;
        this.users = users;
    }

    public static LogReadArgs parse(String[] args) {
        String token = null;
        String logFile = null;
        boolean isState = false;
        boolean isRooms = false;
        boolean isIntersect = false;
        List<User> users = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-K": token = args[++i]; break;
                case "-S": isState = true; break;
                case "-R": isRooms = true; break;
                case "-I": isIntersect = true; break;
                case "-E":
                    if (args[++i].startsWith("-")) Utils.exitInvalid();
                    users.add(new User(args[i], User.UserType.EMPLOYEE));
                    break;
                case "-G":
                    if (args[++i].startsWith("-")) Utils.exitInvalid();
                    users.add(new User(args[i], User.UserType.GUEST));
                    break;
                default:
                    if (args[i].startsWith("-")) Utils.exitInvalid();
                    logFile = args[i];
                    break;
            }
        }

        return new LogReadArgs(token, logFile, isState, isRooms, isIntersect, users);
    }
}