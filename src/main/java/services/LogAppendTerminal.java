package services;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import crypto.Encryption;
import models.DBEntry;
import models.User;
import repository.Database;

public class LogAppendTerminal {

    private static final int ERROR_CODE = 111;
    private static final String DB_FILENAME = "repository/log_db.bin";

    // Classe para armazenar os argumentos processados
    static class CommandArgs {
        Integer timestamp = null;
        String token = null;
        String employee = null;
        String guest = null;
        Boolean isArrival = null;
        Integer roomId = null;
        String logFile = null;
        String batchFile = null;
    }

    public static void main(String[] args) {
        if (args.length == 0)
            invalidExit();

        // 1. Verificar se é modo Batch (-B)
        if (args[0].equals("-B") && args.length == 2) {
            processBatch(args[1]);
            System.exit(0); // Batch file existe e foi processado
        }

        // 2. Modo Normal
        try {
            CommandArgs parsedArgs = parseArgs(args);
            validateArgs(parsedArgs);
            File db = new File(DB_FILENAME);
            if (!db.exists()) {
                createDatabase(parsedArgs.logFile, parsedArgs.token);
            }
            validateAndAppendCached(parsedArgs);
        } catch (Exception e) {
            invalidExit();
        }
    }

    // Função para sair com erro 111
    private static void invalidExit() {
        System.out.println("invalid");
        System.exit(ERROR_CODE);
    }

    // Função para imprimir erro no modo batch sem sair do programa
    private static void invalidBatchLine() {
        System.out.println("invalid");
    }

    private static CommandArgs parseArgs(String[] args) throws Exception {
        CommandArgs cmd = new CommandArgs();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-T":
                    cmd.timestamp = Integer.parseInt(args[++i]);
                    break;
                case "-K":
                    cmd.token = args[++i];
                    break;
                case "-E":
                    cmd.employee = args[++i];
                    break;
                case "-G":
                    cmd.guest = args[++i];
                    break;
                case "-A":
                    cmd.isArrival = true;
                    break;
                case "-L":
                    cmd.isArrival = false;
                    break;
                case "-R":
                    cmd.roomId = Integer.parseInt(args[++i]);
                    break;
                default:
                    // Se não é uma flag, deve ser o nome do ficheiro log
                    if (cmd.logFile == null && !args[i].startsWith("-")) {
                        cmd.logFile = args[i];
                    } else {
                        throw new IllegalArgumentException("Argumentos inválidos");
                    }
            }
        }
        return cmd;
    }

    private static void validateArgs(CommandArgs cmd) throws Exception {
        // 1. Validações Sintáticas Iniciais
        if (cmd.logFile == null || cmd.timestamp == null || cmd.token == null)
            throw new Exception();
        if (cmd.timestamp < 1 || cmd.timestamp > 1073741823)
            throw new Exception();
        if (cmd.roomId != null && (cmd.roomId < 0 || cmd.roomId > 1073741823))
            throw new Exception();

        // Não pode ter ambos Employee e Guest, nem faltar ambos
        if ((cmd.employee != null && cmd.guest != null) || (cmd.employee == null && cmd.guest == null))
            throw new Exception();
        // Tem de ter exatamente a flag -A ou a flag -L (nem as duas em simultâneo, nem
        // nenhuma)
        if (cmd.isArrival == null)
            throw new Exception("invalid");

        String name = cmd.employee != null ? cmd.employee : cmd.guest;
        if (!name.matches("[a-zA-Z]+"))
            throw new Exception();
        if (!cmd.token.matches("[a-zA-Z0-9]+"))
            throw new Exception();
    }

    private static void validateAndAppendCached(CommandArgs cmd) throws Exception {
        // validar a cronologia do tempo
        int lastTimestamp = Database.GetLastTimestamp(cmd.token);
        if (lastTimestamp != -1 && cmd.timestamp <= lastTimestamp) {
            throw new Exception("invalid");
        }

        // lógica de validação da galeria
        String name = cmd.employee != null ? cmd.employee : cmd.guest;

        DBEntry userState = Database.GetEntry(cmd.token, name);
        boolean isCurrentlyInGallery = false;
        Integer currentRoom = null;

        if (userState != null) {
            isCurrentlyInGallery = true;
            currentRoom = userState.room();
        }

        boolean isCurrentlyInRoom = (currentRoom != null);

        if (cmd.isArrival) {
            if (cmd.roomId == null) {
                // Chegada à galeria (-A sem -R)
                // Não pode estar já na galeria nem já dentro de uma sala
                if (isCurrentlyInGallery || isCurrentlyInRoom)
                    throw new Exception("invalid");
            } else {
                // Chegada a uma sala (-A com -R)
                // TEM de estar na galeria, e NÃO pode estar noutra sala [cite: 701, 702]
                if (!isCurrentlyInGallery || isCurrentlyInRoom)
                    throw new Exception("invalid");
            }
        } else {
            if (cmd.roomId == null) {
                // Saída da galeria (-L sem -R)
                // TEM de estar na galeria, e NÃO pode estar dentro de nenhuma sala [cite: 707]
                if (!isCurrentlyInGallery || isCurrentlyInRoom)
                    throw new Exception("invalid");
            } else {
                // Saída de uma sala (-L com -R)
                // TEM de estar exatamente nesta sala para poder sair [cite: 708]
                if (!isCurrentlyInRoom || !currentRoom.equals(cmd.roomId))
                    throw new Exception("invalid");
            }
        }

        // formatar a nova entrada em plaintext
        String actionStr = cmd.isArrival ? "IN" : "OUT";
        String userStr = (cmd.employee != null) ? "-E" : "-G";
        String roomStr = (cmd.roomId != null) ? cmd.roomId.toString() : "GALLERY";

        Path path = Paths.get(cmd.logFile);
        long currOffset = Files.size(path);
        long lastOffset = Database.GetLastUserOffset(cmd.token, name);

        String plainText = cmd.timestamp + "," +
                roomStr + "," +
                userStr + " " + name + "," +
                actionStr + "," +
                roomStr + "," +
                lastOffset;

        // obter o último MAC
        String lastLine = null;
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            while ((line = br.readLine()) != null) {
                lastLine = line;
            }
        }

        String lastLogDecrypted = Encryption.DecryptLog(lastLine, cmd.token);
        if (lastLogDecrypted == null)
            throw new Exception("Decrypt Failed");

        byte[] rowBytes = Base64.getDecoder().decode(lastLogDecrypted);
        byte[] lastMac = Arrays.copyOfRange(rowBytes, rowBytes.length - 32, rowBytes.length);

        // encriptar e gerar MAC
        String encryptedRow = Encryption.EncryptLog(plainText, cmd.token, lastMac);

        // escrever (Append) no ficheiro de log
        try (FileWriter fw = new FileWriter(cmd.logFile, true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)) {
            out.println(encryptedRow);
        } catch (IOException e) {
            throw new Exception("invalid");
        }

        // update db
        DBEntry newEntry = new DBEntry(new User(name, userStr), cmd.roomId, currOffset, cmd.timestamp);
        try {
            Database.UpdateEntry(cmd.token, newEntry);
        } catch (IllegalArgumentException e) {
            Database.CreateEntry(cmd.token, newEntry);
        }
    }

    private static void processBatch(String batchFilePath) {
        File batchFile = new File(batchFilePath);
        if (!batchFile.exists()) {
            invalidExit();
        }

        try (BufferedReader br = new BufferedReader(new FileReader(batchFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;

                // Opção -B não pode aparecer dentro do batch
                if (line.contains("-B")) {
                    invalidBatchLine();
                    continue;
                }

                try {
                    String[] args = line.trim().split("\\s+");
                    CommandArgs parsedArgs = parseArgs(args);
                    File db = new File(DB_FILENAME);
                    if (!db.exists()) {
                        createDatabase(parsedArgs.logFile, parsedArgs.token);
                    } else {
                        validateAndAppendCached(parsedArgs);
                    }
                } catch (Exception e) {
                    invalidBatchLine(); // No batch mode, o erro numa linha não para as restantes
                }
            }
        } catch (IOException e) {
            invalidExit();
        }
    }

    // Método para ler e reconstruir o histórico a partir do ficheiro
    public static void createDatabase(String logPath, String token) throws Exception {
        File logFile = new File(logPath);
        if (!logFile.exists())
            return;

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

                String plainText = Encryption.DecryptLog(lineBase64, token);
                if (plainText == null)
                    throw new Exception("Decrypt Failed");

                // Atualizar o estado em memória baseado na linha lida
                // formato: timestamp,room,type,name,action,byteoffset
                String[] parts = plainText.split(",", -1);
                int ts = Integer.parseInt(parts[0]);
                String room = parts[1];
                Integer roomId = null;
                if (!room.equals("GALLERY"))
                    roomId = Integer.parseInt(room);
                String type = parts[2]; // "-E" ou "-G"
                String name = parts[3];
                boolean isArrival = parts[4].equals("A");
                long byteOffset = Long.parseLong(parts[5]);

                if (isArrival) {
                    if (roomId == null)
                        upsertEntry(token, new DBEntry(new User(name, type), -1, byteOffset, ts));
                    else {
                        upsertEntry(token, new DBEntry(new User(name, type), roomId, byteOffset, ts));
                    }
                } else { // is Departure
                    if (roomId == null)
                        Database.DeleteEntry(token, name);
                    else {
                        upsertEntry(token, new DBEntry(new User(name, type), -1, byteOffset, ts));
                    }
                }
            }
        }
    }

    private static void upsertEntry(String token, DBEntry entry) throws IOException {
        try {
            Database.UpdateEntry(token, entry);
        } catch(IllegalArgumentException e) {
            Database.CreateEntry(token, entry);
        }
    }
}
