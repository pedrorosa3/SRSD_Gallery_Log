package services;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import Utils.*;
import crypto.Encryption;
import models.DBEntry;
import models.DecryptedData;
import models.LineSeekResult;
import models.LogEntry;
import models.User;
import repository.Database;

public class LogAppendTerminal {

    public static void main(String[] args) {
        if (args.length == 0) {
            Utils.exitInvalid();
        }

        // Verificar se é modo Batch (-B)
        if (args[0].equals("-B") && args.length == 2) {
            processBatch(args[1]);
            System.exit(0); // Batch file existe e foi processado
        }

        // Modo Normal
        try {
            LogAppendArgs parsedArgs = LogAppendArgs.parse(args);
            validateArgs(parsedArgs);
            String dbFilePath = ("repository" + File.separator + parsedArgs.logFile + "_db");
            File db = new File(dbFilePath);
            File logFile = new File(parsedArgs.logFile);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            if (!Encryption.checkIntegrity(parsedArgs.logFile, parsedArgs.token)) {
                Utils.exitInvalid();
            }
            if (!db.exists() || !Encryption.checkDbIntegrity(parsedArgs.logFile, dbFilePath, parsedArgs.token)) {
                createDatabase(parsedArgs.logFile, parsedArgs.token);
            }
            boolean validated = validateAndAppend(parsedArgs);
            if (!validated) {
                Utils.exitInvalid();
            }
        } catch (Exception e) {
            Utils.exitInvalid();
        }
    }

    private static void validateArgs(LogAppendArgs cmd) throws Exception {
        // Validações Sintáticas Iniciais
        if (cmd.logFile == null || cmd.timestamp == null || cmd.token == null) {
            Utils.exitInvalid();
        }
        if (cmd.timestamp < 1 || cmd.timestamp > 1073741823) {
            Utils.exitInvalid();
        }
        if (cmd.roomId != null && (cmd.roomId < 0 || cmd.roomId > 1073741823)) {
            Utils.exitInvalid();
        }

        // Não pode ter ambos Employee e Guest, nem faltar ambos
        if ((cmd.user == null)
                || (cmd.user.getUserType() != User.UserType.EMPLOYEE
                        && cmd.user.getUserType() != User.UserType.GUEST)) {
            Utils.exitInvalid();
        }
        // Tem de ter exatamente a flag -A ou a flag -L (nem as duas em simultâneo, nem
        // nenhuma)
        if (cmd.isArrival == null) {
            Utils.exitInvalid();
        }

        String name = cmd.user.getUserName();
        if (!name.matches("[a-zA-Z]+") || name.length() > 27) {
            Utils.exitInvalid();
        }
        if (!cmd.token.matches("[a-zA-Z0-9]+")) {
            Utils.exitInvalid();
        }
    }

    private static boolean validateAndAppend(LogAppendArgs cmd) throws Exception {
        Path path = Paths.get(cmd.logFile);
        if (!Files.exists(path)) {
            Files.createFile(path);
        }

        // validar a cronologia do tempo

        String lastLine = null;
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            while ((line = br.readLine()) != null) {
                lastLine = line;
            }
        }

        // decrypt last log
        DecryptedData log = null;
        int lastTimestamp = 0;

        if (!(lastLine == null)) {
            log = Encryption.decryptLog(lastLine, cmd.token);
            if (log == null || log.plaintext() == null) {
                throw new Exception("Decrypt Failed");
            }
            String[] row = log.plaintext().split(",");
            lastTimestamp = Integer.parseInt(row[0]);
        }

        // validate timestamp
        if (lastTimestamp != -1 && cmd.timestamp <= lastTimestamp) {
            return false;
        }

        // lógica de validação da galeria
        String name = cmd.user.getUserName();
        String dbFilePath = ("repository" + File.separator + cmd.logFile + "_db");
        DBEntry userState = Database.GetEntry(dbFilePath, cmd.token, name);
        boolean isCurrentlyInGallery = false;
        Integer currentRoom = null;
        String lastAction = null;

        if (userState != null) {
            isCurrentlyInGallery = true;
            currentRoom = userState.room();
            lastAction = userState.lastAction();
        }

        boolean isCurrentlyInRoom = (currentRoom != null && currentRoom != -1 && lastAction.equals("IN"));

        if (cmd.isArrival) {
            if (cmd.roomId == null) {
                // Chegada à galeria (-A sem -R)
                // Não pode estar já na galeria nem já dentro de uma sala
                if (isCurrentlyInGallery || isCurrentlyInRoom) {
                    return false;
                }
            } else {
                // Chegada a uma sala (-A com -R)
                // TEM de estar na galeria, e NÃO pode estar noutra sala
                if (!isCurrentlyInGallery || isCurrentlyInRoom) {
                    return false;
                }
            }
        } else {
            if (cmd.roomId == null) {
                // Saída da galeria (-L sem -R)
                // TEM de estar na galeria, e NÃO pode estar dentro de nenhuma sala
                if (!isCurrentlyInGallery || isCurrentlyInRoom) {
                    return false;
                }
            } else {
                // Saída de uma sala (-L com -R)
                // TEM de estar exatamente nesta sala para poder sair
                if (!isCurrentlyInRoom || !currentRoom.equals(cmd.roomId)) {
                    return false;
                }
            }
        }

        // formatar a nova entrada em plaintext
        String actionStr = cmd.isArrival ? "IN" : "OUT";
        String userStr = (cmd.user.getUserType() == User.UserType.EMPLOYEE) ? "-E" : "-G";
        String roomStr = (cmd.roomId != null) ? cmd.roomId.toString() : "GALLERY";

        long currOffset = Files.size(path);
        long lastOffset = Database.GetLastUserOffset(dbFilePath, cmd.token, name);

        String paddedName = String.format("%-27s", name);
        String paddedRoom = String.format("%-10s", roomStr);
        String plainText = cmd.timestamp + "," +
                paddedRoom + "," +
                userStr + "," + paddedName + "," +
                actionStr + "," +
                lastOffset;

        // obter o último MAC
        byte[] lastMac = null;
        LineSeekResult result = Utils.readNthLastLine(cmd.logFile, 1);
        if (result != null && result.line() != null && !result.line().isEmpty()) {
            lastLine = result.line().trim();
            log = Encryption.decryptLog(lastLine, cmd.token);
            if (log == null || log.plaintext() == null) {
                throw new Exception("Decrypt Failed");
            }
            lastMac = log.logMac();
        }

        // update db
        int room = -1;
        if (cmd.roomId != null) {
            room = cmd.roomId;
        }

        DBEntry newEntry = new DBEntry(cmd.user, actionStr, room, currOffset, cmd.timestamp);
        try {
            Database.UpdateEntry(dbFilePath, cmd.token, newEntry);
        } catch (IllegalArgumentException e) {
            Database.CreateEntry(dbFilePath, cmd.token, newEntry);
        }

        // serializa DB
        byte[] dbContent = Files.readAllBytes(Paths.get(dbFilePath));

        // encriptar e gerar MAC
        String encryptedRow = Encryption.encryptLog(plainText, cmd.token, lastMac, dbContent);

        // escrever (Append) no ficheiro de log
        try (FileWriter fw = new FileWriter(cmd.logFile, true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)) {
            out.println(encryptedRow);
        } catch (IOException e) {
            // rollback da DB
            if (userState == null)
                Database.DeleteEntry(dbFilePath, cmd.token, name);
            else
                Database.UpdateEntry(dbFilePath, cmd.token, userState);
            return false;
        }

        return true;
    }

    private static void processBatch(String batchFilePath) {
        File batchFile = new File(batchFilePath);
        if (!batchFile.exists()) {
            Utils.exitInvalid();
        }

        try (BufferedReader br = new BufferedReader(new FileReader(batchFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;

                // Opção -B não pode aparecer dentro do batch
                if (line.contains("-B")) {
                    Utils.invalidBatchLine();
                    continue;
                }

                try {
                    String[] args = line.trim().split("\\s+");
                    LogAppendArgs parsedArgs = LogAppendArgs.parse(args);
                    String dbFilePath = ("repository" + File.separator + parsedArgs.logFile + "_db");
                    File db = new File(dbFilePath);
                    File logFile = new File(parsedArgs.logFile);
                    if (!logFile.exists()) {
                        logFile.createNewFile();
                    }
                    if (!Encryption.checkIntegrity(parsedArgs.logFile, parsedArgs.token)) {
                        Utils.invalidBatchLine();
                        continue;
                    }
                    if (!db.exists()
                            || !Encryption.checkDbIntegrity(parsedArgs.logFile, dbFilePath, parsedArgs.token)) {
                        createDatabase(parsedArgs.logFile, parsedArgs.token);
                    }
                    boolean validated = validateAndAppend(parsedArgs);
                    if (!validated) {
                        Utils.invalidBatchLine();
                        continue;
                    }
                } catch (Exception e) {
                    Utils.invalidBatchLine();
                    continue;
                }
            }
        } catch (IOException e) {
            Utils.exitInvalid();
        }
    }

    // Método para ler e reconstruir o histórico a partir do ficheiro
    public static void createDatabase(String logPath, String token) throws Exception {
        File logFile = new File(logPath);
        if (!logFile.exists()) {
            logFile.createNewFile();
            return;
        }

        File dbFile = new File("repository" + File.separator + logPath + "_db");
        File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        if (dbFile.exists()) {
            dbFile.delete();
        }
        dbFile.createNewFile();


        // 2. Ler e desencriptar linha a linha
        try (BufferedReader br = Files.newBufferedReader(logFile.toPath())) {

            String lineBase64;
            long lineIndex = 0;

            while ((lineBase64 = br.readLine()) != null) {
                lineBase64 = lineBase64.trim();
                if (lineBase64.isEmpty()) {
                    continue;
                }

                DecryptedData log = Encryption.decryptLog(lineBase64, token);
                if (log == null || log.plaintext() == null) {
                    throw new Exception("Decrypt Failed");
                }

                // Atualizar o estado em memória baseado na linha lida
                LogEntry entry = LogEntry.parse(log.plaintext());
                boolean isArrival = entry.getAction().equals("IN");
                Integer roomId = entry.getRoom().equals("GALLERY")
                        ? null
                        : Integer.parseInt(entry.getRoom().trim());

                User user = new User(
                        entry.getUserName().trim(),
                        entry.getUserType().equals("-E")
                                ? User.UserType.EMPLOYEE
                                : User.UserType.GUEST);

                long byteOffset = lineIndex++;

                if (isArrival) {
                    if (roomId == null) {
                        upsertEntry(logPath, token, new DBEntry(user, "IN", -1, byteOffset, entry.getTimestamp()));
                    } else {
                        upsertEntry(logPath, token, new DBEntry(user, "IN", roomId, byteOffset, entry.getTimestamp()));
                    }
                } else {
                    if (roomId == null) {
                        Database.DeleteEntry("repository" + File.separator + logPath + "_db", token, user.getUserName());
                    } else {
                        upsertEntry(logPath, token, new DBEntry(user, "OUT", -1, byteOffset, entry.getTimestamp()));
                    }
                }
            }
        }
    }

    private static void upsertEntry(String logPath, String token, DBEntry entry) throws IOException {
        try {
            Database.UpdateEntry("repository" + File.separator + logPath + "_db", token, entry);
        } catch (IllegalArgumentException e) {
            Database.CreateEntry("repository" + File.separator + logPath + "_db", token, entry);
        }
    }
}
