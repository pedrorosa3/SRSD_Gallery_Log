package services;

import java.util.HashSet;
import java.util.stream.Stream;

import Utils.LogReadArgs;
import Utils.Utils;
import models.*;

public class LogReadTerminal {
    public static void main(String[] args) {
        LogReadArgs parsedArgs = LogReadArgs.parse(args);
        validateArgs(parsedArgs);
        LogReadFile.process(parsedArgs);
    }

    private static void validateArgs(LogReadArgs parsedArgs) {
        if (parsedArgs.token == null || parsedArgs.token.isEmpty()) { Utils.exitInvalid(); }
        if (parsedArgs.logFile == null || parsedArgs.logFile.isEmpty()) { Utils.exitInvalid(); }
        long activeFlags = Stream.of(parsedArgs.isState, parsedArgs.isRooms, parsedArgs.isIntersect)
                .filter(flag -> flag).count();
        if (activeFlags != 1) { Utils.exitInvalid(); }

        HashSet<User> userSet = new HashSet<User>(parsedArgs.users);
        if (userSet.size() != parsedArgs.users.size()) { Utils.exitInvalid(); }
        if (parsedArgs.isState && !parsedArgs.users.isEmpty()) { Utils.exitInvalid(); }
        if (parsedArgs.isRooms && parsedArgs.users.isEmpty()) { Utils.exitInvalid(); }
        if (parsedArgs.isIntersect && parsedArgs.users.size() < 2) { Utils.exitInvalid(); }
    }
}