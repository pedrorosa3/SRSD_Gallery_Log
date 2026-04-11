package Utils;

public class Utils {
    public static void exitInvalid() {
        System.out.println("invalid");
        System.exit(111);
    }

    // Função para imprimir erro no modo batch sem sair do programa
    public static void invalidBatchLine() {
        System.out.println("invalid");
    }
}