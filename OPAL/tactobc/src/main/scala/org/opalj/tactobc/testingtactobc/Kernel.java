package org.opalj.tactobc.testingtactobc;

import java.util.Random;

public class Kernel {
    public Kernel() {
    }

    public static double measureFFT(int N, double mintime, Random R) {
        double[] x = RandomVector(2 * N, R);
        double[] oldx = NewVectorCopy(x);
        long cycles = 1L;
        Stopwatch Q = new Stopwatch();

        while(true) {
            Q.start();

            for(int i = 0; (long)i < cycles; ++i) {
                FFT.transform(x);
                FFT.inverse(x);
            }

            Q.stop();
            if (Q.read() >= mintime) {
                double EPS = 1.0E-10;
                if (FFT.test(x) / (double)N > 1.0E-10) {
                    return 0.0;
                }

                return FFT.num_flops(N) * (double)cycles / Q.read() * 1.0E-6;
            }

            cycles *= 2L;
        }
    }

    public static double measureSOR(int N, double min_time, Random R) {
        double[][] G = RandomMatrix(N, N, R);
        Stopwatch Q = new Stopwatch();
        long cycles = 1L;

        while(true) {
            Q.start();
            SOR.execute(1.25, G, cycles);
            Q.stop();
            if (Q.read() >= min_time) {
                return SOR.num_flops(N, N, cycles) / Q.read() * 1.0E-6;
            }

            cycles *= 2L;
        }
    }

    public static double measureMonteCarlo(double min_time, Random R) {
        Stopwatch Q = new Stopwatch();
        long cycles = 1L;

        while(true) {
            Q.start();
            MonteCarlo.integrate(cycles);
            Q.stop();
            if (Q.read() >= min_time) {
                return MonteCarlo.num_flops(cycles) / Q.read() * 1.0E-6;
            }

            cycles *= 2L;
        }
    }

    public static double measureSparseMatmult(int N, int nz, double min_time, Random R) {
        double[] x = RandomVector(N, R);
        double[] y = new double[N];
        int nr = nz / N;
        int anz = nr * N;
        double[] val = RandomVector(anz, R);
        int[] col = new int[anz];
        int[] row = new int[N + 1];
        row[0] = 0;

        for(int r = 0; r < N; ++r) {
            int rowr = row[r];
            row[r + 1] = rowr + nr;
            int step = r / nr;
            if (step < 1) {
                step = 1;
            }

            for(int i = 0; i < nr; ++i) {
                col[rowr + i] = i * step;
            }
        }

        Stopwatch Q = new Stopwatch();
        long cycles = 1L;

        while(true) {
            Q.start();
            SparseCompRow.matmult(y, val, row, col, x, cycles);
            Q.stop();
            if (Q.read() >= min_time) {
                return SparseCompRow.num_flops(N, nz, cycles) / Q.read() * 1.0E-6;
            }

            cycles *= 2L;
        }
    }

    public static double measureLU(int N, double min_time, Random R) {
        double[][] A = RandomMatrix(N, N, R);
        double[][] lu = new double[N][N];
        int[] pivot = new int[N];
        Stopwatch Q = new Stopwatch();
        long cycles = 1L;

        while(true) {
            Q.start();

            for(int i = 0; (long)i < cycles; ++i) {
                CopyMatrix(lu, A);
                LU.factor(lu, pivot);
            }

            Q.stop();
            if (Q.read() >= min_time) {
                double[] b = RandomVector(N, R);
                double[] x = NewVectorCopy(b);
                LU.solve(lu, pivot, x);
                double EPS = 1.0E-12;
                if (normabs(b, matvec(A, x)) / (double)N > 1.0E-12) {
                    return 0.0;
                }

                return LU.num_flops(N) * (double)cycles / Q.read() * 1.0E-6;
            }

            cycles *= 2L;
        }
    }

    private static double[] NewVectorCopy(double[] x) {
        int N = x.length;
        double[] y = new double[N];
        System.arraycopy(x, 0, y, 0, N);
        return y;
    }

    private static double normabs(double[] x, double[] y) {
        int N = x.length;
        double sum = 0.0;

        for(int i = 0; i < N; ++i) {
            sum += Math.abs(x[i] - y[i]);
        }

        return sum;
    }

    private static void CopyMatrix(double[][] B, double[][] A) {
        int M = A.length;
        int N = A[0].length;
        int remainder = N & 3;

        for(int i = 0; i < M; ++i) {
            double[] Bi = B[i];
            double[] Ai = A[i];
            System.arraycopy(Ai, 0, Bi, 0, remainder);

            for(int j = remainder; j < N; j += 4) {
                Bi[j] = Ai[j];
                Bi[j + 1] = Ai[j + 1];
                Bi[j + 2] = Ai[j + 2];
                Bi[j + 3] = Ai[j + 3];
            }
        }

    }

    private static double[][] RandomMatrix(int M, int N, Random R) {
        double[][] A = new double[M][N];

        for(int i = 0; i < N; ++i) {
            for(int j = 0; j < N; ++j) {
                A[i][j] = R.nextDouble();
            }
        }

        return A;
    }

    private static double[] RandomVector(int N, Random R) {
        double[] A = new double[N];

        for(int i = 0; i < N; ++i) {
            A[i] = R.nextDouble();
        }

        return A;
    }

    private static double[] matvec(double[][] A, double[] x) {
        int N = x.length;
        double[] y = new double[N];
        matvec(A, x, y);
        return y;
    }

    private static void matvec(double[][] A, double[] x, double[] y) {
        int M = A.length;
        int N = A[0].length;

        for(int i = 0; i < M; ++i) {
            double sum = 0.0;
            double[] Ai = A[i];

            for(int j = 0; j < N; ++j) {
                sum += Ai[j] * x[j];
            }

            y[i] = sum;
        }

    }
}
