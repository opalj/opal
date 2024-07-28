package org.opalj.tactobc.testingtactobc;

import java.util.Random;

public class MonteCarlo {
    static final int SEED = 113;

    public MonteCarlo() {
    }

    public static double num_flops(long Num_samples) {
        return (double)Num_samples * 4.0;
    }

    public static double integrate(long Num_samples) {
        Random R = new Random(113);
        long under_curve = 0L;

        for(long count = 0L; count < Num_samples; ++count) {
            double x = R.nextDouble();
            double y = R.nextDouble();
            if (x * x + y * y <= 1.0) {
                ++under_curve;
            }
        }

        return (double)under_curve / (double)Num_samples * 4.0;
    }
}