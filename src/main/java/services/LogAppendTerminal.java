package services;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import Utils.*;
import crypto.Encryption;
import models.DBEntry;
import models.DecryptedLog;
import models.User;
import repository.Database;

public class LogAppendTerminal {

    private static final String DB_FILENAME = "repository/log_db.bin";

    public static void main(String[] args) {
        if (args.length == 0) {
            Utils.exitInvalid();
        }

        // 1. Verificar se é modo Batch (-B)
        if (args[0].equals("-B") && args.length == 2) {
            processBatch(args[1]);
            System.exit(0); // Batch file existe e foi processado
        }

        // 2. Modo Normal
        try {
            LogAppendArgs parsedArgs = LogAppendArgs.parse(args);
            validateArgs(parsedArgs);
            File db = new File(DB_FILENAME);
            if (!db.exists()) {
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
        // 1. Validações Sintáticas Iniciais
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
        int lastTimestamp = Database.GetLastTimestamp(cmd.token);
        if (lastTimestamp != -1 && cmd.timestamp <= lastTimestamp) {
            return false;
        }

        // lógica de validação da galeria
        String name = cmd.user.getUserName();

        DBEntry userState = Database.GetEntry(cmd.token, name);
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
                // TEM de estar na galeria, e NÃO pode estar noutra sala [cite: 701, 702]
                if (!isCurrentlyInGallery || isCurrentlyInRoom) {
                    return false;
                }
            }
        } else {
            if (cmd.roomId == null) {
                // Saída da galeria (-L sem -R)
                // TEM de estar na galeria, e NÃO pode estar dentro de nenhuma sala [cite: 707]
                if (!isCurrentlyInGallery || isCurrentlyInRoom) {
                    return false;
                }
            } else {
                // Saída de uma sala (-L com -R)
                // TEM de estar exatamente nesta sala para poder sair [cite: 708]
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
        long lastOffset = Database.GetLastUserOffset(cmd.token, name);

        String paddedName = String.format("%-27s", name);
        String paddedRoom = String.format("%-10s", roomStr);
        String plainText = cmd.timestamp + "," +
                paddedRoom + "," +
                userStr + "," + paddedName + "," +
                actionStr + "," +
                lastOffset;

        // obter o último MAC
        String lastLine = null;
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            while ((line = br.readLine()) != null) {
                lastLine = line;
            }
        }

        byte[] lastMac = null;
        if (!(lastLine == null)) {
            DecryptedLog log = Encryption.DecryptLog(lastLine, cmd.token);
            if (log == null || log.plaintext() == null) {
                throw new Exception("Decrypt Failed");
            }
            lastMac = log.mac();
        }

        // encriptar e gerar MAC
        String encryptedRow = Encryption.EncryptLog(plainText, cmd.token, lastMac);

        // escrever (Append) no ficheiro de log
        try (FileWriter fw = new FileWriter(cmd.logFile, true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)) {
            out.println(encryptedRow);
        } catch (IOException e) {
            return false;
        }

        // update db
        int room = -1;
        if (cmd.roomId != null) {
            room = cmd.roomId;
        }

        DBEntry newEntry = new DBEntry(cmd.user, actionStr, room, currOffset, cmd.timestamp);
        try {
            Database.UpdateEntry(cmd.token, newEntry);
        } catch (IllegalArgumentException e) {
            Database.CreateEntry(cmd.token, newEntry);
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
                    File db = new File(DB_FILENAME);
                    if (!db.exists()) {
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
            return;
        }

        // 1. Verificar a integridade (MUITO IMPORTANTE)
        if (!Encryption.CheckIntegrity(logPath, token)) {
            // Se a integridade falhar, o comportamento correto para o logappend é "invalid"
            // e exit(111)
            throw new Exception("Integrity Violation");
        }

        // 2. Ler e desencriptar linha a linha
        try (Scanner sc = new Scanner(logFile)) {
            while (sc.hasNextLine()) {
                String lineBase64 = sc.nextLine().trim();
                if (lineBase64.isEmpty())
                    continue;

                DecryptedLog log = Encryption.DecryptLog(lineBase64, token);
                String plainText = log.plaintext();
                if (plainText == null) {
                    throw new Exception("Decrypt Failed");
                }

                // Atualizar o estado em memória baseado na linha lida
                // formato: timestamp,room,type,name,action,byteoffset
                String[] parts = plainText.split(",");
                int ts = Integer.parseInt(parts[0]);
                String roomStr = parts[1];
                Integer roomId = null;
                if (!roomStr.equals("GALLERY"))
                    roomId = Integer.parseInt(roomStr);
                User user = null;
                if (parts[2].equals("-E")) {
                    user = new User(parts[3], User.UserType.EMPLOYEE);
                } else if (parts[2].equals("-G")) {
                    user = new User(parts[3], User.UserType.GUEST);
                } else {
                    Utils.exitInvalid();
                }
                boolean isArrival = parts[4].equals("IN");
                long byteOffset = Long.parseLong(parts[5]);

                if (isArrival) {
                    if (roomId == null)
                        upsertEntry(token, new DBEntry(user, "IN", -1, byteOffset, ts));
                    else {
                        upsertEntry(token, new DBEntry(user, "IN", roomId, byteOffset, ts));
                    }
                } else { // is Departure
                    if (roomId == null) {
                        Database.DeleteEntry(token, user.getUserName());
                    }
                    else {
                        upsertEntry(token, new DBEntry(user, "OUT", -1, byteOffset, ts));
                    }
                }
            }
        }
    }

    private static void upsertEntry(String token, DBEntry entry) throws IOException {
        try {
            Database.UpdateEntry(token, entry);
        } catch (IllegalArgumentException e) {
            Database.CreateEntry(token, entry);
        }
    }
}
