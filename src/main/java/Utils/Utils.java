package Utils;

public class Utils {
    public static final int LINE_SIZE = 194; // 16 (IV) + 112 (plaintext) + 32 (MAC) + 32 (DB MAC) + 2 (newline)

    public static void exitInvalid() {
        System.out.println("invalid");
        System.exit(111);
    }

    // Função para imprimir erro no modo batch sem sair do programa
    public static void invalidBatchLine() {
        System.out.println("invalid");
    }
}