package services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import models.*;

public class LogReadTerminal {
    public static void main(String[] args) {
        ParsedArgs parsedArgs = parseArgs(args);
        validateArgs(parsedArgs);
        LogReadFile.process(parsedArgs);
    }

    private static ParsedArgs parseArgs(String [] args) {
        ParsedArgs parsedArgs = new ParsedArgs();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-K":
                    parsedArgs.token = args[++i];
                    break;
                case "-S":
                    parsedArgs.isState = true;
                    break;
                case "-R":
                    parsedArgs.isRooms = true;
                    break;
                case "-I":
                    parsedArgs.isIntersect = true;
                    break;
                case "-E":
                    if (args[++i].startsWith("-")) {
                        exitInvalid();
                    }
                    parsedArgs.users.add(new User(args[i], User.UserType.EMPLOYEE));
                    break;
                case "-G":
                    if (args[++i].startsWith("-")) {
                        exitInvalid();
                    }
                    parsedArgs.users.add(new User(args[i], User.UserType.GUEST));
                    break;
                default:
                    parsedArgs.logFilePath = arg;
                    break;
            }
        }
        return parsedArgs;
    }

    private static void validateArgs(ParsedArgs parsedArgs) {
        if (parsedArgs.token == null || parsedArgs.token.isEmpty()) {
            exitInvalid();
        }
        if (parsedArgs.logFilePath == null || parsedArgs.logFilePath.isEmpty()) {
            exitInvalid();
        }
        long activeFlags = Stream.of(parsedArgs.isState, parsedArgs.isRooms, parsedArgs.isIntersect)
                .filter(flag -> flag).count();
        if (activeFlags != 1) {
            exitInvalid();
        }

        HashSet<User> userSet = new HashSet<User>(parsedArgs.users);
        if (userSet.size() != parsedArgs.users.size()) {
            exitInvalid();
        }
        
        if (parsedArgs.isState && !parsedArgs.users.isEmpty()) {
            exitInvalid();
        }
        if (parsedArgs.isRooms && parsedArgs.users.isEmpty()) {
            exitInvalid();
        }
        if (parsedArgs.isIntersect && parsedArgs.users.size() < 2) {
            exitInvalid();
        }
    }

    private static void exitInvalid() {
        System.out.println("invalid");
        System.exit(111);
    }


    public static class ParsedArgs {
        String token;
        String logFilePath;
        boolean isState;
        boolean isRooms;
        boolean isIntersect;
        List<User> users = new ArrayList<>();
    }
}