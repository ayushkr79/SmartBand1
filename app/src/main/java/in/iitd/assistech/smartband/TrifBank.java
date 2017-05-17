package in.iitd.assistech.smartband;

import static java.lang.Math.exp;
import static java.lang.Math.log;

public class TrifBank {

    int M; //number of rows of H i.e. filters
    int K; //number of columns of H (length of frequency response)
    int[] R; //R = {low_freq, high_freq}
    int fs; //sampling frequency

    double[][] H; // MxK triangular filterbank matrix
    double[] F; // frequency vector of 1xK
    double[] C; //C is a vector of filter cutoff frequencies (Hz), 
             //note that C(2:end) also represents filter center frequencies,
             //and the dimension of C is 1x(M+2)

    public TrifBank(int numRows, int numCols, int[] rangeFreq, int sampFreq){
        this.M = numRows;
        this.K = numCols;
        this.R = rangeFreq;
        this.fs = sampFreq;
        calcFilterBank();
    }

    private void calcFilterBank() {
        int f_min = 0;
        int f_low = R[0];
        int f_high = R[1];
        int f_max = (int)Math.floor(0.5*fs);

        // frequency range (Hz), size 1xK
        F = new double[K];
        double[] fw = new double[K];
        for (int i=0; i<K; i++){
            double temp = f_min + ((f_max-f_min)*i) / (K-1);
            F[i] = temp;
            fw[i] = freqToMel(temp);
        }

        //filter cutoff frequencies (Hz) for all filters, size 1x(M+2)
        double[] c = new double[M+2];
        double[] cw = new double[M+2];
        for (int i=0; i<M+2; i++){
            double temp = freqToMel(f_low) + i*( freqToMel(f_high )
                                                    - freqToMel(f_low) )/(M+1);
            c[i] = meltoFreq(temp);
            cw[i] = freqToMel(c[i]);
        }

        H = new double[M][K];
        for(int m=0; m<M; m++){
            for(int k=0; k<K; k++){
                if(F[k]>=c[m] && F[k]<=c[m+1]){
                    H[m][k] = (F[k]-c[m]) / (c[m+1]-c[m]);
                } else if(F[k]>=c[m+1] && F[k]<=c[m+2]){
                    H[m][k] = (c[m+2]-F[k]) / (c[m+2]-c[m+1]);
                } else{
                    H[m][k] = 0;
                }
            }
        }
    }

    //convert frequency to mel
    protected static double freqToMel(double freq){
        return (1127 * log(1 + freq / 700));
    }
    //convert mel to frequency
    private static double meltoFreq(double mel) {
        return (700 * ( exp(mel/1127)-1 ) );
    }

    public double[][] getH() {
        return H;
    }

    public double[] getF() {
        return F;
    }

    public double[] getC() {
        return C;
    }
}
