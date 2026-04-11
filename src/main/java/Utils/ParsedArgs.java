package Utils;

import java.util.ArrayList;
import java.util.List;

import models.User;
import models.User.UserType;

public class ParsedArgs {
    public final String token;
    public final String logFilePath;
    public final boolean isState;
    public final boolean isRooms;
    public final boolean isIntersect;
    public final List<User> users;

    private ParsedArgs(String token, String logFilePath, boolean isState,
            boolean isRooms, boolean isIntersect, List<User> users) {
        this.token = token;
        this.logFilePath = logFilePath;
        this.isState = isState;
        this.isRooms = isRooms;
        this.isIntersect = isIntersect;
        this.users = users;
    }

    public static ParsedArgs parse(String[] args) {
        String token = null;
        String logFilePath = null;
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
                    logFilePath = args[i];
                    break;
            }
        }

        return new ParsedArgs(token, logFilePath, isState, isRooms, isIntersect, users);
    }
}