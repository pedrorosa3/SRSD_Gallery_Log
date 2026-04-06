package services;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import crypto.Encryption;

public class LogAppendTerminal {

    private static final int ERROR_CODE = 111;

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
        if (args.length == 0) invalidExit();

        // 1. Verificar se é modo Batch (-B)
        if (args[0].equals("-B") && args.length == 2) {
            processBatch(args[1]);
            System.exit(0); // Batch file existe e foi processado 
        }

        // 2. Modo Normal
        try {
            CommandArgs parsedArgs = parseArgs(args);
            validateAndAppend(parsedArgs);
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
                case "-T": cmd.timestamp = Integer.parseInt(args[++i]); break;
                case "-K": cmd.token = args[++i]; break;
                case "-E": cmd.employee = args[++i]; break;
                case "-G": cmd.guest = args[++i]; break;
                case "-A": cmd.isArrival = true; break;
                case "-L": cmd.isArrival = false; break;
                case "-R": cmd.roomId = Integer.parseInt(args[++i]); break;
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

    private static void validateAndAppend(CommandArgs cmd) throws Exception {
        // 1. Validações Sintáticas Iniciais
        if (cmd.logFile == null || cmd.timestamp == null || cmd.token == null) throw new Exception();
        if (cmd.timestamp < 1 || cmd.timestamp > 1073741823) throw new Exception(); 
        if (cmd.roomId != null && (cmd.roomId < 0 || cmd.roomId > 1073741823)) throw new Exception(); 
        
        // Não pode ter ambos Employee e Guest, nem faltar ambos
        if ((cmd.employee != null && cmd.guest != null) || (cmd.employee == null && cmd.guest == null)) throw new Exception();
        
        String name = cmd.employee != null ? cmd.employee : cmd.guest;
        if (!name.matches("[a-zA-Z]+")) throw new Exception();
        if (!cmd.token.matches("[a-zA-Z0-9]+")) throw new Exception();

        // 2. Carregar e Reconstruir o Estado
        GalleryState state = new GalleryState();
        try {
            state.loadFromFile(cmd.logFile, cmd.token);
        } catch (Exception e) {
            // Se a integridade falhar (MAC errado) ou o token for inválido, rejeita
            throw new Exception("invalid");
        }

        // 3. Validar a Cronologia do Tempo
        // O tempo tem de ser estritamente maior que o último registo 
        if (state.lastTimestamp != -1 && cmd.timestamp <= state.lastTimestamp) {
            throw new Exception("invalid"); 
        }

        // 4. Lógica de Validação da Galeria
        String type = (cmd.employee != null) ? "E" : "G";
        String userKey = type + "_" + name;

        boolean isCurrentlyInGallery = state.inGallery.contains(userKey);
        Integer currentRoom = state.inRooms.get(userKey);
        boolean isCurrentlyInRoom = (currentRoom != null);

        if (cmd.isArrival) {
            if (cmd.roomId == null) {
                // Chegada à galeria (-A sem -R)
                // Não pode estar já na galeria nem já dentro de uma sala
                if (isCurrentlyInGallery || isCurrentlyInRoom) throw new Exception("invalid");
            } else {
                // Chegada a uma sala (-A com -R)
                // TEM de estar na galeria, e NÃO pode estar noutra sala [cite: 701, 702]
                if (!isCurrentlyInGallery || isCurrentlyInRoom) throw new Exception("invalid"); 
            }
        } else {
            if (cmd.roomId == null) {
                // Saída da galeria (-L sem -R)
                // TEM de estar na galeria, e NÃO pode estar dentro de nenhuma sala [cite: 707]
                if (!isCurrentlyInGallery || isCurrentlyInRoom) throw new Exception("invalid"); 
            } else {
                // Saída de uma sala (-L com -R)
                // TEM de estar exatamente nesta sala para poder sair [cite: 708]
                if (!isCurrentlyInRoom || !currentRoom.equals(cmd.roomId)) throw new Exception("invalid"); 
            }
        }

        // 5. Formatar a nova entrada em Texto Limpo
        // Usamos um formato simples separado por ponto e vírgula
        String actionStr = cmd.isArrival ? "A" : "L";
        String roomStr = (cmd.roomId != null) ? cmd.roomId.toString() : "";
        String plainText = cmd.timestamp + ";" + type + ";" + name + ";" + actionStr + ";" + roomStr;

        // 6. Encriptar e gerar MAC usando o protocolo do grupo
        String encryptedRow = Encryption.EncryptLog(plainText, cmd.token, state.lastMac);

        // 7. Escrever (Append) no ficheiro de Log
        try (FileWriter fw = new FileWriter(cmd.logFile, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(encryptedRow);
        } catch (IOException e) {
            throw new Exception("invalid");
        }
    }

    private static void processBatch(String batchFilePath) {
        File batchFile = new File(batchFilePath);
        if (!batchFile.exists()) invalidExit(); 

        try (BufferedReader br = new BufferedReader(new FileReader(batchFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                // Opção -B não pode aparecer dentro do batch 
                if (line.contains("-B")) {
                    invalidBatchLine();
                    continue;
                }

                try {
                    String[] args = line.trim().split("\\s+");
                    CommandArgs parsedArgs = parseArgs(args);
                    validateAndAppend(parsedArgs);
                } catch (Exception e) {
                    invalidBatchLine(); // No batch mode, o erro numa linha não para as restantes 
                }
            }
        } catch (IOException e) {
            invalidExit();
        }
    }
    static class GalleryState {
        // Guarda as pessoas na galeria (sem sala atribuída) e as que estão em salas
        // Chave: "E_Nome" ou "G_Nome"
        Set<String> inGallery = new HashSet<>();
        Map<String, Integer> inRooms = new HashMap<>();
        int lastTimestamp = -1;
        byte[] lastMac = null;

        // Método para ler e reconstruir o histórico a partir do ficheiro
        public void loadFromFile(String logPath, String token) throws Exception {
            File logFile = new File(logPath);
            if (!logFile.exists()) return;

            // 1. Verificar a integridade (MUITO IMPORTANTE)
            if (!Encryption.CheckIntegrity(logPath, token)) {
                // Se a integridade falhar, o comportamento correto para o logappend é "invalid" e exit(111)
                throw new Exception("Integrity Violation");
            }

            // 2. Ler e desencriptar linha a linha
            try (Scanner sc = new Scanner(logFile)) {
                while (sc.hasNextLine()) {
                    String lineBase64 = sc.nextLine().trim();
                    if (lineBase64.isEmpty()) continue;

                    String plainText = Encryption.DecryptLog(lineBase64, token);
                    if (plainText == null) throw new Exception("Decrypt Failed");

                    // Extrair o último MAC para o EncryptLog seguinte funcionar
                    byte[] rowBytes = Base64.getDecoder().decode(lineBase64);
                    this.lastMac = Arrays.copyOfRange(rowBytes, rowBytes.length - 32, rowBytes.length);

                    // Atualizar o estado em memória baseado na linha lida
                    // Assumimos formato: Timestamp;TIPO;Nome;Ação;RoomID (ou vazio)
                    String[] parts = plainText.split(";", -1);
                    int ts = Integer.parseInt(parts[0]);
                    String type = parts[1]; // "E" ou "G"
                    String name = parts[2];
                    String userKey = type + "_" + name;
                    boolean isArrival = parts[3].equals("A");
                    Integer roomId = parts[4].isEmpty() ? null : Integer.parseInt(parts[4]);

                    this.lastTimestamp = ts;

                    if (isArrival) {
                        if (roomId == null) this.inGallery.add(userKey);
                        else {
                            this.inGallery.remove(userKey);
                            this.inRooms.put(userKey, roomId);
                        }
                    } else { // is Departure
                        if (roomId == null) this.inGallery.remove(userKey);
                        else {
                            this.inRooms.remove(userKey);
                            this.inGallery.add(userKey); // volta para a galeria (corredores)
                        }
                    }
                }
            }
        }
    }
}
