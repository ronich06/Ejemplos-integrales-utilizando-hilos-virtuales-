import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ByteCounterVirtualThreads1 {
    private static final int BUFFER_LEN = 1000;

    private static final Lock commonLock = new ReentrantLock();

    private static byte[] buffer = new byte[BUFFER_LEN];
    private static long posP = 0;
    private static long posC = 0;

    private static boolean flag = true;

    private static int[] solutionArray = new int[256];
    private static int[] solutionAux = new int[256];

    private static String filePath;
    private static long fileLen;

    public static void main(String[] args) {
        int[] threadCounts = { 100, 1000, 10000, 100000, 1000000}; // Puedes ajustar esta lista según tus
                                                                             // necesidades

        for (int numThreads : threadCounts) {
            // Reinicializar variables y objetos
            buffer = new byte[BUFFER_LEN];
            posP = 0;
            posC = 0;
            flag = true;
            solutionArray = new int[256];
            solutionAux = new int[256];
            fileLen = 0;

            for (int i = 0; i < 256; i++)
                solutionAux[i] = i;

            if (args.length > 0) {
                filePath = args[0];
            } else {
                System.err.println("Falta la dirección del archivo");
                System.exit(1);
            }

            FileInputStream fileInputStream;
            try {
                fileInputStream = new FileInputStream(filePath);
            } catch (IOException e) {
                System.err.println("Fallo la apertura del documento");
                System.exit(1);
                return;
            }

            try {
                fileLen = fileInputStream.getChannel().size();
                fileInputStream.close();
            } catch (IOException e) {
                System.err.println("Error al obtener la longitud del archivo");
                System.exit(1);
            }

            long startTime = System.currentTimeMillis();

            Thread[] producerThreads = new Thread[numThreads];
            Thread[] consumerThreads = new Thread[numThreads];

            for (int i = 0; i < numThreads; i++) {
                producerThreads[i] =  Thread.ofVirtual().unstarted(new ReadingFileTask());
                producerThreads[i].start();
            }

            for (int i = 0; i < numThreads; i++) {
                consumerThreads[i] =  Thread.ofVirtual().unstarted(new AddingToArrayTask());
                consumerThreads[i].start();
            }

            try {
                for (int i = 0; i < numThreads; i++) {
                    producerThreads[i].join();
                }

                for (int i = 0; i < numThreads; i++) {
                    consumerThreads[i].join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;

            System.out.println(
                    "Con " + numThreads + " hilos, el tiempo de ejecución fue de " + elapsedTime + " milisegundos.");

        }
    }

    private static class ReadingFileTask implements Runnable {
        @Override
        public void run() {
            try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
                while (true) {
                    synchronized (commonLock) {
                        while (posP >= fileLen) {
                            flag = false;
                            commonLock.notifyAll(); // Avisar a los demás hilos que pueden terminar
                            return;
                        }

                        fileInputStream.getChannel().position(posP);
                        int bytesRead = fileInputStream.read(buffer, (int) (posP % BUFFER_LEN), 1);

                        if (bytesRead == -1) {
                            flag = false;
                            commonLock.notifyAll(); // Avisar a los demás hilos que pueden terminar
                            return;
                        }

                        if (posP >= BUFFER_LEN) {
                            while ((posP + 1) % BUFFER_LEN == posC % BUFFER_LEN) {
                                commonLock.wait();
                            }
                        }
                        posP++;
                        commonLock.notifyAll();
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static class AddingToArrayTask implements Runnable {
        @Override
        public void run() {
            while (true) {
                synchronized (commonLock) {
                    while (posC >= posP && flag) {
                        try {
                            commonLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (!flag) {
                        return; // Terminar si la bandera ya no está establecida
                    }
                    solutionArray[buffer[(int) (posC % BUFFER_LEN)] & 0xFF]++;
                    posC++;
                    fileLen--;
                    commonLock.notifyAll();
                }
            }
        }
    }

}
