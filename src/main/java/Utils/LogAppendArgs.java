package Utils;

import models.User;

/*
 * Classe para armazenar os argumentos processados
 */
public class LogAppendArgs extends ParsedArgs {
    public final Integer timestamp;
    public final User user;
    public final Boolean isArrival;
    public final Integer roomId;

    private LogAppendArgs(Integer timestamp, String token, User user,
            Boolean isArrival, Integer roomId, String logFile) {
        super(token, logFile);
        this.timestamp = timestamp;
        this.user = user;
        this.isArrival = isArrival;
        this.roomId = roomId;
    }

    public static LogAppendArgs parse(String[] args) {
        Integer timestamp = null;
        String token = null;
        User user = null;
        Boolean isArrival = null;
        Integer roomId = null;
        String logFile = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-T":
                    timestamp = Integer.parseInt(args[++i]);
                    break;
                case "-K":
                    token = args[++i];
                    break;
                case "-E": {
                    String name = args[++i];
                    if (name.startsWith("-")) {
                        System.out.println("args 1");
                        Utils.exitInvalid();
                    }
                    user = new User(name, User.UserType.EMPLOYEE);
                    break;
                }
                case "-G": {
                    String name = args[++i];
                    if (name.startsWith("-")) {
                        System.out.println("args 2");
                        Utils.exitInvalid();
                    }
                    user = new User(name, User.UserType.GUEST);
                    break;
                }
                case "-A":
                    isArrival = true;
                    break;
                case "-L":
                    isArrival = false;
                    break;
                case "-R":
                    roomId = Integer.parseInt(args[++i]);
                    break;
                default:
                    if (logFile == null && !args[i].startsWith("-")) {
                        logFile = args[i];
                    } else {
                        throw new IllegalArgumentException("Argumentos inválidos");
                    }
            }
        }

        return new LogAppendArgs(timestamp, token, user, isArrival, roomId, logFile);
    }
}