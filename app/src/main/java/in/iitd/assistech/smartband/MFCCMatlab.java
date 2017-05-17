package in.iitd.assistech.smartband;


import android.graphics.Matrix;
import android.util.Log;
import android.util.TimingLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;

import static java.lang.Math.exp;
import static java.lang.Math.log;

public class MFCCMatlab {

    private static final String TAG = "MFCCMatlab";

    static double[] speech; //Input speech signal as vector
    static int FS; //Sampling Frequency
    static int TW; //Analysis frame duration
    static int TS; //Analysis frame shift
    static double alpha; //pre-emphasis coefficient
    static int[] range; //frequency range for filterbank analysis
    static int M; //number of filterbank channels
    static int N; //number of cepstral coefficient
    static int L; //cepstral sine lifter parameter

    static int NW; //frame duration (samples)
    static int NS; //frame shift (samples)
    static int nfft; // length of FFT analysis
    static int K; //length of uniques part of the FFT

    double[][] CC;
    double[][] final_CC;
    double[][] FBE;
    double[][] frames;
    double[][] MAG;

    double[] featSound;


    public MFCCMatlab(double[] speech, int FS, int TW, int TS, double alpha,
                      int[] range, int M, int N, int L){
        this.speech = speech;
        this.FS = FS;
        this.TW = TW;
        this.TS = TS;
        this.alpha = alpha;
        this.range = range;
        this.M = M;
        this.N = N;
        this.L = L;

        NW = (int) Math.round(Math.pow(10, -3)*TW*FS);
        NS = (int) Math.round(Math.pow(10, -3)*TS*FS);

        //next power of 2 greater than NW
        nfft = NW == 0 ? 0 : 32 - Integer.numberOfLeadingZeros(NW - 1);
        nfft = (int)Math.pow(2, nfft);

        K = nfft/2 + 1;

        /*
        System.out.println("MFCCMatlab line 59: --------------");
        System.out.println("speech[0] : " + speech[0]);
        System.out.println("FS : " + FS);
        System.out.println("TW : " + TW);
        System.out.println("TS : " + TS);
        System.out.println("alpha : " + alpha);
        System.out.println("range :" + range[0] + ", " + range[1]);
        System.out.println("M : " + M);
        System.out.println("N : " + N);
        System.out.println("L : " + L);

        System.out.println("NW : " + NW);
        System.out.println("NS : " + NS);
        System.out.println("nfft : "   + nfft);
        System.out.println("K : " + K);
        */

        TimingLogger timingsMFCC = new TimingLogger(TAG, "MFCCMatlab setter");
        //TODO: Minimise time of these two next line

        process();

        timingsMFCC.addSplit("process in MFCCMatlab");

        calcFeatSound();

        timingsMFCC.addSplit("calcFeatSound in MFCCMatlab");
        timingsMFCC.dumpToLog();


        /*
        System.out.println();
        System.out.println("These are FeatSound!!!!!!!!!!");
        System.out.println(Arrays.toString(getFeatSound()));
        Log.e("Beware", "This is it");
        System.out.println("These are CC values!!!!!!!!!!");
        System.out.println(Arrays.toString(CC[0]));
        */
    }

    public void process(){
        TimingLogger timingMfccProcess = new TimingLogger(TAG, "MFCCMatlab - process");
        if(maximum(speech) <= 1){
            for(int i=0; i<speech.length; i++){
                speech[i] = speech[i]*Math.pow(2, 15);
            }
        }

        timingMfccProcess.addSplit("Check magnitude of speech");

        /**speech = filter( [1 -alpha], 1, speech );**/
        double[] b = {1.0, -alpha};
        speech = filter( b, 1.0, speech );

        timingMfccProcess.addSplit("Method - filter");

        /**frames = vec2frames( speech, Nw, Ns, 'cols', window, false );**/
        Vec2Frame vec2frame = new Vec2Frame(speech, NW, NS, "cols", false);
        frames = vec2frame.getFrames();

        timingMfccProcess.addSplit("Vec2Frame");

        /**MAG = abs( fft(frames,nfft,1) );**/
        MAG = new double[nfft][frames[0].length];

        for(int j=0; j<frames[0].length; j++){
            Complex[] temp = new Complex[nfft];
            for(int i=0; i<frames.length; i++){
                temp[i] = new Complex(frames[i][j], 0.0);
            }
            for(int i=frames.length; i<nfft; i++){
                temp[i] = new Complex(0.0, 0.0);
            }

            Complex[] fft_temp = fft(temp);

            for(int i=0; i<nfft; i++){
                MAG[i][j] = fft_temp[i].abs();
            }
        }
        /**Using Multithreading**/
        //Creates a pool of 5 concurrent thread workers
        /*
        ExecutorService es = Executors.newFixedThreadPool(5);

        //List of results for each row computation task
        List<Future<Void>> results = new ArrayList<Future<Void>>();
        try{
            for(int col=0; col<frames[0].length; col++){
                final int workCol = col;

                //The main part. You can submit Callable or Runnable
                // tasks to the ExecutorService, and it will run them
                // for you in the number of threads you have allocated.
                // If you put more than 5 tasks, they will just patiently
                // wait for a task to finish and release a thread, then run.
                Future<Void> task = es.submit(new Callable<Void>(){
                    @Override
                    public Void call(){
                        for(int row=0; row<frames.length; row++){
                            //do something for each column of workRow
                            Complex[] temp = new Complex[nfft];
                            for(int i=0; i<frames.length; i++){
                                temp[i] = new Complex(frames[i][workCol], 0.0);
                            }
                            for(int i=frames.length; i<nfft; i++){
                                temp[i] = new Complex(0.0, 0.0);
                            }

                            Complex[] fft_temp = fft(temp);

                            for(int i=0; i<nfft; i++){
                                MAG[i][workCol] = fft_temp[i].abs();
                            }
                        }
                        return null;
                    }
                });
                //Store the work task in the list.
                results.add(task);
            }
        }finally{
            //Make sure thread-pool is shutdown and all worker
            //threads are released.
            es.shutdown();
        }

        for(Future<Void> task : results){
            try{
                //This will wait for threads to finish.
                // i.e. same as Thread.join()
                task.get();
            }catch(ExecutionException e){
                //One of the tasks threw an exception!
                throw new RuntimeException(e);
            }catch (InterruptedException e){
            }
        }
        */

        timingMfccProcess.addSplit("Calculate MAG");


        /**H = trifbank( M, K, R, fs, hz2mel, mel2hz );**/
        /**FBE = H * MAG(1:K,:);**/
        //FBE = M x frames[0].length
        double[][] H;
        TrifBank trifBank = new TrifBank(M, K, range, FS);
        H = trifBank.getH();

        timingMfccProcess.addSplit("TrifBank getH");

        FBE = new double[M][MAG[0].length];
        for(int i=0; i<M; i++){
            for(int j=0; j<MAG[0].length; j++){
                double sum = 0;
                for(int k=0; k< K; k++){
                    sum += H[i][k]*MAG[k][j];
                }
                FBE[i][j] = sum;
            }
        }

        timingMfccProcess.addSplit("Calculate FBE");

        double[][] DCT = dctm();

        timingMfccProcess.addSplit("Method dctm");

        if(DCT[0].length != FBE.length){
            Log.e(TAG, "matrix size of DCT and FBE don't match");
        }

        timingMfccProcess.addSplit("Check length of DCT");

        /**CC =  DCT * log( FBE );**/
        CC = new double[DCT.length][FBE[0].length];
        for(int i=0; i<DCT.length; i++){
            for(int j=0; j<FBE[0].length; j++){
                double sum = 0;
                for(int k=0; k<DCT[0].length; k++){
                    sum += DCT[i][k]*(Math.log10(FBE[k][j]));
                }
                CC[i][j] = sum;
            }
        }

        timingMfccProcess.addSplit("CC =  DCT * log( FBE )");

        /**CC = diag( lifter ) * CC;**/
        double[] lifter = ceplifter();

        timingMfccProcess.addSplit("Method ceplifter");

        double[][] diagLifter = new double[lifter.length][lifter.length];
        for(int i=0; i<lifter.length; i++){
            for(int j=0; j<lifter.length; j++){
                if(i==j){
                    diagLifter[i][i] = lifter[i];
                } else{
                    diagLifter[i][j] = 0;
                }
            }
        }

        final_CC = new double[diagLifter.length][CC[0].length];
        for(int i=0; i<diagLifter.length; i++){
            for(int j=0; j<CC[0].length; j++){
                double sum = 0;
                for(int k=0; k<diagLifter[0].length; k++){
                    sum += diagLifter[i][k]*CC[k][j];
                }
                final_CC[i][j] = sum;
            }
        }

        timingMfccProcess.addSplit("CC = diag( lifter ) * CC");
        timingMfccProcess.dumpToLog();
    }

    private void calcFeatSound() {
        double[][] transposeCC = transpose(final_CC);
        double[][] newCC = new double[8][transposeCC[0].length];
        for(int i=0; i<newCC.length; i++){
            for(int j=0; j<(newCC[0].length); j++){ //TODO Add +1 in length
                newCC[i][j] = transposeCC[i][j];
            }
        }

        featSound = new double[newCC.length*newCC[0].length];
        featSound = mat2array(newCC);
        //TODO: currently values of featSound are half of what they should be, find out why and then remove this for loop
        for (int i=0; i<featSound.length; i++){
            featSound[i] = 2*featSound[i];
        }
        System.out.println("Length of featSound : " + featSound.length);
    }

    //Type III DCT matrix routine
    //uses N and M as input
    /**CHECKED: No ERROR**/
    private static double[][] dctm(){
        double[][] DCT = new double[N][M];

        double temp = Math.sqrt(2.0/M);

        double[] a = new double[N];
        for (int i=0; i<N; i++){
            a[i] = i;
        }
        double[][] a1 = repmat(a, 1, M);
        if(a1 == null){
            Log.e(TAG, "null from repmat function in a1");
        }

        double[] b = new double[M];
        for (int i=0; i<M; i++){
            b[i] = (Math.PI*(i+0.5))/M;
        }

        double[][] b1 = repmat(b, N, 1);
        if(b1 == null){
            Log.e(TAG, "null from repmat function in b1");
        }

        for (int i=0; i<N; i++){
            for (int j=0; j<M; j++){
                DCT[i][j] = temp * Math.cos(a1[i][j]*b1[i][j]);
            }
        }

        return DCT;
    }

    public static double[] filter(double[] b, double a, double[] x) {
        double[] filter = new double[x.length];
        filter[0] = 0.0;
        for (int i=1; i<x.length; i++){
            filter[i] = (b[0]*x[i] + b[1]*x[i-1])/a;
        }
        return filter;
    }

    /**CHECKED: No ERROR**/
    private static double[] ceplifter(){
        double[] lifter = new double[N];

        for (int i=0; i<N; i++){
            lifter[i] = 1 + 0.5*L*Math.sin(Math.PI*i/L);
        }
        return lifter;
    }

    //find maximum in array
    private static double  maximum (double [] array){
        double max = 0.0;
        for (int i=0; i<array.length; i++){
            max = Math.max(max, Math.abs(array[i]));
        }
        return  max;
    }

    //form a matrix by copying the column to given number of columns
    private static double[][] repmat(double[] array, int rows, int cols){
        double[][] matrix;
        if(rows == 1){
            matrix = new double[array.length][cols];
            for(int i=0; i<array.length; i++){
                for(int j=0; j<cols; j++){
                    matrix[i][j] = array[i];
                }
            }
            return matrix;
        }
        if(cols == 1){
            matrix = new double[rows][array.length];
            for(int i=0; i<rows; i++){
                for(int j=0; j<array.length; j++){
                    matrix[i][j] = array[j];
                }
            }
            return matrix;
        }
        return null;
    }

    /**CHECKED: No ERROR**/
    public static Complex[] fft(Complex[] x) {
        int n = x.length;
        // base case
        if (n == 1) return new Complex[] { x[0] };

        // radix 2 Cooley-Tukey FFT
        if (n % 2 != 0) { throw new RuntimeException("n is not a power of 2"); }

        // fft of even terms
        Complex[] even = new Complex[n/2];
        for (int k = 0; k < n/2; k++) {
            even[k] = x[2*k];
        }
        Complex[] q = fft(even);

        // fft of odd terms
        Complex[] odd  = even;  // reuse the array
        for (int k = 0; k < n/2; k++) {
            odd[k] = x[2*k + 1];
        }
        Complex[] r = fft(odd);

        // combine
        Complex[] y = new Complex[n];
        for (int k = 0; k < n/2; k++) {
            double kth = -2 * k * Math.PI / n;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            y[k]       = q[k].plus(wk.times(r[k]));
            y[k + n/2] = q[k].minus(wk.times(r[k]));
        }

        return y;
    }

    private double[][] transpose(double[][] input){
        double[][] output = new double[input[0].length][input.length];
        for(int i=0; i<input[0].length; i++){
            for(int j=0; j<input.length; j++){
                output[i][j] = input[j][i];
            }
        }
        return output;
    }

    private double[] mat2array(double[][] input) {
        double[][] matrix = transpose(input);
        double[] array = new double[matrix.length * matrix[0].length];
        for(int i = 0; i < matrix.length; i++) {
            double[] row = matrix[i];
            for(int j = 0; j < row.length; j++) {
                double number = matrix[i][j];
                array[i*row.length+j] = number;
            }
        }
        return array;
    }

    /**GETTERS**/
    public double[][] getCC(){
        return final_CC;
    }

    public double[][] getFBE(){
        return FBE;
    }

    public double[][] getFrames(){
        return frames;
    }

    public double[] getFeatSound() {
        return featSound;
    }
}
