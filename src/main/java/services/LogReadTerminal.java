package services;

import java.util.HashSet;
import java.util.stream.Stream;

import Utils.ParsedArgs;
import Utils.Utils;
import models.*;

public class LogReadTerminal {
    public static void main(String[] args) {
        ParsedArgs parsedArgs = ParsedArgs.parse(args);
        validateArgs(parsedArgs);
        LogReadFile.process(parsedArgs);
    }

    private static void validateArgs(ParsedArgs parsedArgs) {
        if (parsedArgs.token == null || parsedArgs.token.isEmpty()) { Utils.exitInvalid(); }
        if (parsedArgs.logFilePath == null || parsedArgs.logFilePath.isEmpty()) { Utils.exitInvalid(); }
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