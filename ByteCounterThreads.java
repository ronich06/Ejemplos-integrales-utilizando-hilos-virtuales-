/*
Autores:
	Andrés León Orozco 
	Eduardo Ojeda Paladino 
	Rony Chinchilla Azofeifa
	Kairo Chacon Maleaños
*/

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ByteCounterThreads {
    private static final int BUFFER_LEN = 1000;
    private static final int NUM_THREADS = 4; // Número de hilos
    private static final int NUM_CONSUMERS = 4; // Número de consumidores

    private static final Lock commonLock = new ReentrantLock();

    private static byte[] buffer = new byte[BUFFER_LEN];
    private static long posP = 0;
    private static long posC = 0;

    private static boolean flag = true;

    private static int[] solutionArray = new int[256];
    private static int[] solutionAux = new int[256];

    private static String filePath;
    private static long fileLen;
    private static long consumersFinalPos;

    public static void main(String[] args) {
        int[] threadCounts = {500, 1000, 2000, 40000, 60000, 70000, 500000, 1000000}; // Puedes ajustar esta lista según tus necesidades

        for (int numThreads : threadCounts) {
            // Reinicializar variables y objetos
            buffer = new byte[BUFFER_LEN];
            posP = 0;
            posC = 0;
            flag = true;
            solutionArray = new int[256];
            solutionAux = new int[256];
            fileLen = 0;
            consumersFinalPos = 0;

            for (int i = 0; i < 256; i++) solutionAux[i] = i;

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
            Thread[] consumerThreads = new Thread[4];

            for (int i = 0; i < numThreads; i++) {
                producerThreads[i] = new Thread(new ReadingFileTask());
                producerThreads[i].start();
            }

            for (int i = 0; i < NUM_CONSUMERS; i++) {
                consumerThreads[i] = new Thread(new AddingToArrayTask());
                consumerThreads[i].start();
            }

            try {
                for (int i = 0; i < numThreads; i++) {
                    producerThreads[i].join();
                }

                for (int i = 0; i < NUM_CONSUMERS; i++) {
                    consumerThreads[i].join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;

            System.out.println("Con " + numThreads + " hilos, el tiempo de ejecución fue de " + elapsedTime + " milisegundos.");

            mergeSort(solutionArray, solutionAux, 256);

             /*for (int i = 0; i < 256; i++) {
                if (solutionArray[i] != 0) {
                    System.out.println(solutionAux[i] + " aparece " + solutionArray[i] + " veces");
                }
            }*/
        }
    }

     private static class ReadingFileTask implements Runnable {
        @Override
        public void run() {
            try {
                FileInputStream fileInputStream = new FileInputStream(filePath);
                while (true) {
                    synchronized (commonLock) {
                        if (posP >= fileLen) {
                            flag = false;
                            break;
                        }

                        fileInputStream.getChannel().position(posP);
                        int bytesRead = fileInputStream.read(buffer, (int) (posP % BUFFER_LEN), 1);

                        if (bytesRead == -1) {
                            flag = false;
                            break;
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
                fileInputStream.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static class AddingToArrayTask implements Runnable {
        @Override
        public void run() {
            while (fileLen > 0) {
                synchronized (commonLock) {
                    if (posC >= posP && flag) {
                        try {
                            commonLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (fileLen <= 0) {
                        break;
                    }
                    solutionArray[buffer[(int) (posC % BUFFER_LEN)] & 0xFF]++;
                    posC++;
                    fileLen--;
                    commonLock.notifyAll();
                }
            }
        }
    }

    private static void merge(int arr1[], int arr2[], int left1[], int left2[], int leftSize, int right1[], int right2[], int rightSize) {
        int i = 0, j = 0, k = 0;
        while (i < leftSize && j < rightSize) {
            if (left1[i] >= right1[j]) {
                arr1[k] = left1[i];
                arr2[k] = left2[i];
                i++;
            } else {
                arr1[k] = right1[j];
                arr2[k] = right2[j];
                j++;
            }
            k++;
        }

        while (i < leftSize) {
            arr1[k] = left1[i];
            arr2[k] = left2[i];
            i++;
            k++;
        }

        while (j < rightSize) {
            arr1[k] = right1[j];
            arr2[k] = right2[j];
            j++;
            k++;
        }
    }

    private static void mergeSort(int arr1[], int arr2[], int size) {
        if (size < 2)
            return;

        int mid = size / 2;
        int[] left1 = new int[mid];
        int[] left2 = new int[mid];
        int[] right1 = new int[size - mid];
        int[] right2 = new int[size - mid];

        for (int i = 0; i < mid; i++) {
            left1[i] = arr1[i];
            left2[i] = arr2[i];
        }

        for (int i = mid; i < size; i++) {
            right1[i - mid] = arr1[i];
            right2[i - mid] = arr2[i];
        }

        mergeSort(left1, left2, mid);
        mergeSort(right1, right2, size - mid);
        merge(arr1, arr2, left1, left2, mid, right1, right2, size - mid);
    }
}
