package Utils;

import java.io.IOException;
import java.io.RandomAccessFile;

import models.LineSeekResult;

public class Utils {
    public static void exitInvalid() {
        System.out.println("invalid");
        System.exit(111);
    }

    public static void exitInvalidIntegration() {
        System.out.println("integration violation");
        System.exit(111);
    }

    // Função para imprimir erro no modo batch sem sair do programa
    public static void invalidBatchLine() {
        System.out.println("invalid");
    }

    public static LineSeekResult readNthLastLine(String filePath, int n) throws IOException {
        if (n <= 0)
            throw new IllegalArgumentException("n must be >= 1");

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            long pointer = raf.length() - 1;

            if (pointer < 0)
                return null;

            int linesFound = 0;

            while (pointer >= 0) {
                raf.seek(pointer);
                byte b = raf.readByte();
                if (b != '\n' && b != '\r')
                    break;
                pointer--;
            }

            for (; pointer >= 0; pointer--) {
                raf.seek(pointer);
                if (raf.readByte() == '\n') {
                    linesFound++;
                    if (linesFound == n) {
                        pointer++;
                        break;
                    }
                }
            }

            if (pointer < 0)
                pointer = 0;

            raf.seek(pointer);
            return new LineSeekResult(raf.readLine(), pointer);
        }
    }
}