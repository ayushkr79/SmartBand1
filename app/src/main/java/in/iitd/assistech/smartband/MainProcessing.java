package in.iitd.assistech.smartband;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.util.TimingLogger;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Arrays;

import jxl.Workbook;
import jxl.write.WritableWorkbook;

import static android.os.ParcelFileDescriptor.MODE_APPEND;
import static in.iitd.assistech.smartband.MainActivity.RECORDER_SAMPLERATE;

import static in.iitd.assistech.smartband.MainActivity.adapter;
import static in.iitd.assistech.smartband.MainActivity.inputBias;
import static in.iitd.assistech.smartband.MainActivity.inputWeights;
import static in.iitd.assistech.smartband.MainActivity.layerWeights;
import static in.iitd.assistech.smartband.MainActivity.outputBias;
import static in.iitd.assistech.smartband.MainActivity.meanTrain;
import static in.iitd.assistech.smartband.MainActivity.devTrain;
import static in.iitd.assistech.smartband.MainActivity.sData;
import static in.iitd.assistech.smartband.MainActivity.setProbOut;

public class MainProcessing {

    private Context context;

    private static final String TAG = "MainProcessing";
    private static int PROB_MSG_HNDL = 123;

    public static int TW = 50; //analysis frame duration (ms)
    public static int TS = (int)Math.floor(0.75*TW); //analysis frame shit (ms)
    public static int frames = 3;
    public static double alpha = 0.97; //pre-emphasis factor
    public static int[] range = {300, 5000}; // frequency range
    public static int M = 26; //number of filterbank channels
    public static int N = 13; // number of mfcc
    public static int L = 22; //liftering coefficient
    public static final int PROCESS_LENGTH = (int)Math.floor((TW+(frames-1)*TS + 10)*RECORDER_SAMPLERATE/1000);//RECORDER_SAMPLERATE*RECORD_TIME_DURATION/3000;

    short[] sDataPart1 = new short[PROCESS_LENGTH];
    short[] sDataPart2 = new short[PROCESS_LENGTH];
    short[] sDataPart3 = new short[PROCESS_LENGTH];
    short[] sDataPart4 = new short[PROCESS_LENGTH];

    private MFCCMatlab mfcc; // instance of class MFCCMatlab

    /**Hurray**/
    /**The final shit that we need is here**/
    double[] featSound; //160 features of 250 ms audio
    double[] featSound2; //160 features of 250 ms audio
    double[] featSound3; //160 features of 250 ms audio
    double[] featSound4; //160 features of 250 ms audio
    /****/

    double hornProb;
    double gunShotProb;
    double dogBarkProb;

    public MainProcessing(Context c){
        this.context = c;
        this.sDataPart1 = Arrays.copyOfRange(sData, 0, PROCESS_LENGTH);
    }

    public void processAudioEvent() {
        if (!(MainActivity.isPrepNN)) {
            Toast.makeText(context, "Press Again", Toast.LENGTH_SHORT).show();
        } else {
            processAudioPart();
        }
    }

    public void processAudioPart(){
        Thread featThread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {

                    long startTime = System.currentTimeMillis();
                    mfcc = new MFCCMatlab(sDataPart1); //, RECORDER_SAMPLERATE, TW, TS, alpha, range, M, N, L);
                    featSound = new double[mfcc.getFeatSoundLength()];
                    featSound = mfcc.getFeatSound();

                    /**-----------WRITE TO EXCEL---------------**/
                    String filename = "SmartBand1000.csv";
                    int inputNode = mfcc.getFeatSoundLength();
                    long stopTime = System.currentTimeMillis();
                    long timeTaken = stopTime - startTime;
                    Log.e(TAG, Long.toString(timeTaken) + ": Time Taken");
                    writeToExcel(filename, frames, TW, TS, M, N, inputNode, timeTaken);
                    Log.e(TAG, "File writer" + timeTaken);
                    /**----------------------------------------**/

                    double[] outputNodes1 = getOutputProb(featSound);
                    setProbOut(outputNodes1); //Update the UI
                }
            }
        });
        featThread.start();
    }

    private double[] getOutputProb(double[] featSound){
        //TODO: Check that inputFeat is 160x1 vector. Replace it with featSound at later stage.
        //Normalize input feature with training mean and deviation
        //TODO: REMOVE THIS BELOW COMMENT
        for (int i = 0; i < featSound.length; i++) {
            featSound[i] = (featSound[i] - meanTrain[i][0]) / devTrain[i][0];
        }

        double[] hiddenNodes = new double[inputWeights.length];
        Log.e(TAG, Integer.toString(featSound.length) + " featSound Length");
        Log.e(TAG, Integer.toString(inputWeights.length) + " featSound Length " + Integer.toString(inputWeights[0].length));
        for (int i = 0; i < inputWeights.length; i++) {
            double sum = 0;
            for (int j = 0; j < inputWeights[0].length; j++) {
                sum += inputWeights[i][j] * featSound[j];
            }
            hiddenNodes[i] = tansig(sum + inputBias[i][0]);
        }

        double[] outputNodes = new double[layerWeights.length];
        for (int i = 0; i < layerWeights.length; i++) {
            double sum = 0;
            for (int j = 0; j < layerWeights[0].length; j++) {
                sum += layerWeights[i][j] * hiddenNodes[j];
            }
            outputNodes[i] = sum + outputBias[i][0];
        }

        double sum = 0.0;
        for (int i = 0; i < outputNodes.length; i++) {
            sum += Math.exp(outputNodes[i]);
        }

        for (int i = 0; i < outputNodes.length; i++) {
            outputNodes[i] = softmax(outputNodes[i], sum);
        }
        Log.e(TAG, Integer.toString(outputNodes.length));
        return outputNodes;
    }

    private double tansig(double x) {
        return ((2.0 / (1 + Math.exp(-2*x))) - 1.0);
    }

    private double softmax(double outputNode, double sum) {
        double temp = Math.exp(outputNode);
        return (temp/sum);
    }

    public void writeToExcel(String filename, int frames, int frameLength, int frameShift, int numMFCC, int filterbankChannel, int inputNode, long timeTaken){
        File external = Environment.getExternalStorageDirectory();
        String sdcardPath = external.getPath() + "/SmartBand/";
        // to this path add a new directory path
        File file = new File(external, filename);
        try{
            if (!file.exists()){
                file.createNewFile();
            }
            FileOutputStream fos  = context.openFileOutput(filename, MODE_APPEND);
//                            Writer out = new BufferedWriter(new OutputStreamWriter(openFileOutput(file.getName(), MODE_APPEND)));

            FileWriter filewriter = new FileWriter(sdcardPath + filename, true);
            BufferedWriter out = new BufferedWriter(filewriter);
            String data = frameLength + "," + frames + ","+ frameShift + "," + "," + filterbankChannel + "," + numMFCC + "," + inputNode + "," + timeTaken + "\n";
            out.write(data);
            out.close();
            filewriter.close();

            fos.close();
            Log.e(TAG, "File writer" + timeTaken);
        }catch(Exception e){
            Log.e(TAG, e.toString() + " FileOutputStream");
        }
    }

}