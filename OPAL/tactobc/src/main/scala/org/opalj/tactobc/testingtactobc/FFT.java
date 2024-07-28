package org.opalj.tactobc.testingtactobc;

public class FFT {
    public FFT() {
    }

    public static double num_flops(int N) {
        double logN = (double)log2(N);
        return (5.0 * (double)N - 2.0) * logN + 2.0 * ((double)N + 1.0);
    }

    public static void transform(double[] data) {
        transform_internal(data, -1);
    }

    public static void inverse(double[] data) {
        transform_internal(data, 1);
        int nd = data.length;
        int n = nd / 2;
        double norm = 1.0 / (double)n;

        for(int i = 0; i < nd; ++i) {
            data[i] *= norm;
        }

    }

    public static double test(double[] data) {
        int nd = data.length;
        double[] copy = new double[nd];
        System.arraycopy(data, 0, copy, 0, nd);
        transform(data);
        inverse(data);
        double diff = 0.0;

        for(int i = 0; i < nd; ++i) {
            double d = data[i] - copy[i];
            diff += d * d;
        }

        return Math.sqrt(diff / (double)nd);
    }

    public static double[] makeRandom(int n) {
        int nd = 2 * n;
        double[] data = new double[nd];

        for(int i = 0; i < nd; ++i) {
            data[i] = Math.random();
        }

        return data;
    }

    public static void main(String[] args) {
        int i;
        if (args.length == 0) {
            i = 1024;
            System.out.println("n=" + i + " => RMS Error=" + test(makeRandom(i)));
        }

        for(i = 0; i < args.length; ++i) {
            int n = Integer.parseInt(args[i]);
            System.out.println("n=" + n + " => RMS Error=" + test(makeRandom(n)));
        }

    }

    protected static int log2(int n) {
        int log = 0;

        for(int k = 1; k < n; ++log) {
            k *= 2;
        }

        if (n != 1 << log) {
            throw new Error("FFT: Data length is not a power of 2!: " + n);
        } else {
            return log;
        }
    }

    protected static void transform_internal(double[] data, int direction) {
        if (data.length != 0) {
            int n = data.length / 2;
            if (n != 1) {
                int logn = log2(n);
                bitreverse(data);
                int bit = 0;

                for(int dual = 1; bit < logn; dual *= 2) {
                    double w_real = 1.0;
                    double w_imag = 0.0;
                    double theta = 2.0 * (double)direction * Math.PI / (2.0 * (double)dual);
                    double s = Math.sin(theta);
                    double t = Math.sin(theta / 2.0);
                    double s2 = 2.0 * t * t;

                    int a;
                    int b;
                    int i;
                    double tmp_imag;
                    for(a = 0; a < n; a += 2 * dual) {
                        b = 2 * a;
                        i = 2 * (a + dual);
                        tmp_imag = data[i];
                        double wd_imag = data[i + 1];
                        data[i] = data[b] - tmp_imag;
                        data[i + 1] = data[b + 1] - wd_imag;
                        data[b] += tmp_imag;
                        data[b + 1] += wd_imag;
                    }

                    for(a = 1; a < dual; ++a) {
                        double tmp_real = w_real - s * w_imag - s2 * w_real;
                        tmp_imag = w_imag + s * w_real - s2 * w_imag;
                        w_real = tmp_real;
                        w_imag = tmp_imag;

                        for(b = 0; b < n; b += 2 * dual) {
                            i = 2 * (b + a);
                            int j = 2 * (b + a + dual);
                            double z1_real = data[j];
                            double z1_imag = data[j + 1];
                            double wd_real = w_real * z1_real - w_imag * z1_imag;
                            double wd_imag = w_real * z1_imag + w_imag * z1_real;
                            data[j] = data[i] - wd_real;
                            data[j + 1] = data[i + 1] - wd_imag;
                            data[i] += wd_real;
                            data[i + 1] += wd_imag;
                        }
                    }

                    ++bit;
                }

            }
        }
    }

    protected static void bitreverse(double[] data) {
        int n = data.length / 2;
        int nm1 = n - 1;
        int i = 0;

        for(int j = 0; i < nm1; ++i) {
            int ii = i << 1;
            int jj = j << 1;
            int k = n >> 1;
            if (i < j) {
                double tmp_real = data[ii];
                double tmp_imag = data[ii + 1];
                data[ii] = data[jj];
                data[ii + 1] = data[jj + 1];
                data[jj] = tmp_real;
                data[jj + 1] = tmp_imag;
            }

            while(k <= j) {
                j -= k;
                k >>= 1;
            }

            j += k;
        }

    }
}
