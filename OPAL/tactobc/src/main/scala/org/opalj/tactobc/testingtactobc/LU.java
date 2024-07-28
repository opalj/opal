package org.opalj.tactobc.testingtactobc;

public class LU {
    private final double[][] LU_;
    private final int[] pivot_;

    public LU(double[][] A) {
        int M = A.length;
        int N = A[0].length;
        this.LU_ = new double[M][N];
        insert_copy(this.LU_, A);
        this.pivot_ = new int[M];
        factor(this.LU_, this.pivot_);
    }

    public static double num_flops(int N) {
        return 2.0 * (double)N * (double)N * (double)N / 3.0;
    }

    protected static double[] new_copy(double[] x) {
        int N = x.length;
        double[] T = new double[N];
        System.arraycopy(x, 0, T, 0, N);
        return T;
    }

    protected static double[][] new_copy(double[][] A) {
        int M = A.length;
        int N = A[0].length;
        double[][] T = new double[M][N];

        for(int i = 0; i < M; ++i) {
            double[] Ti = T[i];
            double[] Ai = A[i];
            System.arraycopy(Ai, 0, Ti, 0, N);
        }

        return T;
    }

    public static int[] new_copy(int[] x) {
        int N = x.length;
        int[] T = new int[N];
        System.arraycopy(x, 0, T, 0, N);
        return T;
    }

    protected static void insert_copy(double[][] B, double[][] A) {
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

    public static int factor(double[][] A, int[] pivot) {
        int N = A.length;
        int M = A[0].length;
        int minMN = Math.min(M, N);

        for(int j = 0; j < minMN; ++j) {
            int jp = j;
            double t = Math.abs(A[j][j]);

            int ii;
            for(ii = j + 1; ii < M; ++ii) {
                double ab = Math.abs(A[ii][j]);
                if (ab > t) {
                    jp = ii;
                    t = ab;
                }
            }

            pivot[j] = jp;
            if (A[jp][j] == 0.0) {
                return 1;
            }

            if (jp != j) {
                double[] tA = A[j];
                A[j] = A[jp];
                A[jp] = tA;
            }

            if (j < M - 1) {
                double recp = 1.0 / A[j][j];

                for(int k = j + 1; k < M; ++k) {
                    A[k][j] *= recp;
                }
            }

            if (j < minMN - 1) {
                for(ii = j + 1; ii < M; ++ii) {
                    double[] Aii = A[ii];
                    double[] Aj = A[j];
                    double AiiJ = Aii[j];

                    for(int jj = j + 1; jj < N; ++jj) {
                        Aii[jj] -= AiiJ * Aj[jj];
                    }
                }
            }
        }

        return 0;
    }

    public static void solve(double[][] LU, int[] pvt, double[] b) {
        int M = LU.length;
        int N = LU[0].length;
        int ii = 0;

        int i;
        for(i = 0; i < M; ++i) {
            int ip = pvt[i];
            double sum = b[ip];
            b[ip] = b[i];
            if (ii == 0) {
                for(int j = ii; j < i; ++j) {
                    sum -= LU[i][j] * b[j];
                }
            } else if (sum == 0.0) {
                ii = i;
            }

            b[i] = sum;
        }

        for(i = N - 1; i >= 0; --i) {
            double sum = b[i];

            for(int j = i + 1; j < N; ++j) {
                sum -= LU[i][j] * b[j];
            }

            b[i] = sum / LU[i][i];
        }

    }

    public double[][] getLU() {
        return new_copy(this.LU_);
    }

    public int[] getPivot() {
        return new_copy(this.pivot_);
    }

    public double[] solve(double[] b) {
        double[] x = new_copy(b);
        solve(this.LU_, this.pivot_, x);
        return x;
    }
}
