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
    public final String batchFile;

    private LogAppendArgs(Integer timestamp, String token, User user,
            Boolean isArrival, Integer roomId, String logFile, String batchFile)  {
        super(token, logFile);
        this.timestamp = timestamp;
        this.user = user;
        this.isArrival = isArrival;
        this.roomId = roomId;
        this.batchFile = batchFile;
    }

    public static LogAppendArgs parse(String[] args) {
        Integer timestamp = null;
        String token = null;
        User user = null;
        Boolean isArrival = null;
        Integer roomId = null;
        String logFile = null;
        String batchFile = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-T": timestamp = Integer.parseInt(args[++i]); break;
                case "-K": token = args[++i]; break;
                case "-E": 
                    if (args[++i].startsWith("-")) Utils.exitInvalid();    
                    user = new User(args[++i], User.UserType.EMPLOYEE); break;
                case "-G": 
                    if (args[++i].startsWith("-")) Utils.exitInvalid();
                    user = new User(args[++i], User.UserType.GUEST); break;
                case "-A": isArrival = true; break;
                case "-L": isArrival = false; break;
                case "-R": roomId = Integer.parseInt(args[++i]); break;
                default:
                    if (logFile == null && !args[i].startsWith("-")) {
                        logFile = args[i];
                    } else {
                        throw new IllegalArgumentException("Argumentos inválidos");
                    }
            }
        }

        return new LogAppendArgs(timestamp, token, user, isArrival, roomId, logFile, batchFile);
    }
}