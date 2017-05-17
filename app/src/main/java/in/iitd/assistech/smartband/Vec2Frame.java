package in.iitd.assistech.smartband;


import android.util.Log;
import android.util.TimingLogger;

import java.util.Arrays;

public class Vec2Frame {

    private static final String TAG = "Vec2Frame";

    double[] vec; // input vector, speech
    int NW; // frame length
    int NS; // frame shift
    String direction; // specifies if the frames in B are rows or columns, here 'cols'
    //String window;
    /** Hanning window is used**/


    double[][] frames; //output matrix of frames
    int[][] indexes; //
    boolean padding;

    int L; //length of input vector
    int M; //number of frames

    public Vec2Frame(double[] vec, int NW, int NS, String direction, boolean padding){
        this.vec = vec;
        this.NW = NW;
        this.NS = NS;
        this.direction = direction;
        this.padding = padding;

        L = vec.length;
        M = (int)Math.floor((L-NW)*1.0/((NS+1)*1.0));

        TimingLogger timingVec2Frame = new TimingLogger(TAG, "Vec2Frame - setter");

        createFrames();

        timingVec2Frame.addSplit("Method - createFrames");
        timingVec2Frame.dumpToLog();
    }

    public void createFrames(){

        TimingLogger timings = new TimingLogger(TAG, "Method - createFrames");

        int E = (L-((M-1)*NS+NW));

        timings.addSplit("Calculate E");

        //only made for case as no padding is required
        if(!padding){
            //TODO: Check if it is (M-1)*NS+NW) (originally written) or (M-1)*NW+NS
            //vec = Arrays.copyOfRange(vec, 0, ((M-1)*NW+NS));
        } else{
            Log.e(TAG, "Padding is not false");
        }

        timings.addSplit("Check padding");

        if(direction == "cols"){

            indexes = new int[NW][M];
            frames = new double[NW][M];

            int[] inds = new int[NW];
            for (int i=0; i<NW; i++){
                inds[i] = i+1;
            }

            timings.addSplit("Calculate inds");

            int[] indf = new int[M];
            for (int i=0; i<M; i++){
                indf[i] = NS*i;
            }

            timings.addSplit("Calculate indf");

            //TODO: Remove this
            System.out.println("Vec2Frame line 71 - ");
            for (int i=0; i<NW; i++){
                for (int j=0; j<M; j++){
                    int temp = inds[i]+indf[j];
                    indexes[i][j] = temp;
                    frames[i][j] = vec[temp];
                }
            }

            timings.addSplit("Calculate indexes and frames");

            frames = HanningWindow(frames);

            timings.addSplit("Method HanningWindow");
            timings.dumpToLog();

        } else {
            Log.e(TAG, "direction is not cols");
        }
    }

    public double[][] HanningWindow(double[][] input){

        TimingLogger timings1 = new TimingLogger(TAG, "Method HanningWindow");

        double[][] output = new double[input.length][input[0].length];

        double[] temp = new double[NW];
        for (int i=0; i<NW; i++){
            temp[i] = 0.5*(1 - Math.cos(2*Math.PI*i/NW));
        }
        timings1.addSplit("Calculate temp");

        /*
        double[][] tempDiag = new double[NW][NW];
        for(int i=0; i<NW; i++){
            for (int j=0; j<NW; j++){
                if(i==j){
                    tempDiag[i][j] = temp[i];
                } else{
                    tempDiag[i][j] = 0;
                }
            }
        }

        timings1.addSplit("tempDiag");

        for (int i = 0; i < NW; i++) {
            for (int j = 0; j < input[0].length; j++) {
                for (int k = 0; k < tempDiag[0].length; k++) {
                    output[i][j] += tempDiag[i][k] * input[k][j];
                }
            }
        }
        */
        for(int i=0; i<NW; i++){
            for(int j=0; j<input[0].length; j++){
                output[i][j] = input[i][j]*temp[i];
            }
        }

        timings1.addSplit("Calculate Output");
        timings1.dumpToLog();

        return output;
    }

    public double[][] getFrames(){
        return frames;
    }

    public int[][] getIndexes(){
        return indexes;
    }

}
