package org.opalj.tactobc.testingtactobc;

public class SOR {
    public SOR() {
    }

    public static double num_flops(int M, int N, long num_iterations) {
        return ((double)M - 1.0) * ((double)N - 1.0) * (double)num_iterations * 6.0;
    }

    public static void execute(double omega, double[][] G, long num_iterations) {
        int M = G.length;
        int N = G[0].length;
        double omega_over_four = omega * 0.25;
        double one_minus_omega = 1.0 - omega;
        int Mm1 = M - 1;
        int Nm1 = N - 1;

        for(long p = 0L; p < num_iterations; ++p) {
            for(int i = 1; i < Mm1; ++i) {
                double[] Gi = G[i];
                double[] Gim1 = G[i - 1];
                double[] Gip1 = G[i + 1];

                for(int j = 1; j < Nm1; ++j) {
                    Gi[j] = omega_over_four * (Gim1[j] + Gip1[j] + Gi[j - 1] + Gi[j + 1]) + one_minus_omega * Gi[j];
                }
            }
        }

    }
}
