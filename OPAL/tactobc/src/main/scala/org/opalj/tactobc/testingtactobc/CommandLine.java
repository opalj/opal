package org.opalj.tactobc.testingtactobc;

import java.util.Random;

public class CommandLine {
    private static final String[] DUMP_PROPERTIES = new String[]{"java.vendor", "java.version", "os.arch", "os.name", "os.version"};

    public CommandLine() {
    }

    public static void main(String[] args) {
        System.out.println();
        System.out.println("SciMark " + CommandLine.class.getPackage().getImplementationVersion());
        System.out.println();
        double min_time = 2.0;
        int FFT_size = 1024;
        int SOR_size = 100;
        int Sparse_size_M = 1000;
        int Sparse_size_nz = 5000;
        int LU_size = 100;
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("-h") || args[0].equalsIgnoreCase("-help")) {
                System.out.println("Usage: [-large] [minimum_time]");
                return;
            }

            int current_arg = 0;
            if (args[current_arg].equalsIgnoreCase("-large")) {
                FFT_size = 1048576;
                SOR_size = 1000;
                Sparse_size_M = 100000;
                Sparse_size_nz = 1000000;
                LU_size = 1000;
                ++current_arg;
            }

            if (args.length > current_arg) {
                min_time = Double.parseDouble(args[current_arg]);
            }
        }

        double[] res = new double[6];
        Random R = new Random(101010);
        res[1] = Kernel.measureFFT(FFT_size, min_time, R);
        res[2] = Kernel.measureSOR(SOR_size, min_time, R);
        res[3] = Kernel.measureMonteCarlo(min_time, R);
        res[4] = Kernel.measureSparseMatmult(Sparse_size_M, Sparse_size_nz, min_time, R);
        res[5] = Kernel.measureLU(LU_size, min_time, R);
        res[0] = (res[1] + res[2] + res[3] + res[4] + res[5]) / 5.0;
        System.out.format("Composite Score: %.2f%n", res[0]);
        System.out.format("FFT (%s): %.2f%n", FFT_size, res[1]);
        System.out.format("SOR (%s x %s): %.2f%n", Integer.valueOf(SOR_size), Integer.valueOf(SOR_size), res[2]);
        System.out.format("Monte Carlo: %.2f%n", res[3]);
        System.out.format("Sparse matmult (N=%s, nz=%s): %.2f%n", Sparse_size_M, Sparse_size_nz, res[4]);
        System.out.format("LU (%s x %s ): %.2f%n", Integer.valueOf(LU_size), Integer.valueOf(LU_size), res[5]);
        System.out.println();
        String[] var10 = DUMP_PROPERTIES;
        int var11 = var10.length;

        for(int var12 = 0; var12 < var11; ++var12) {
            String property = var10[var12];
            System.out.println(property + ": " + System.getProperty(property));
        }

    }
}

