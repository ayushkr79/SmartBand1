package in.iitd.assistech.smartband;

import android.content.Context;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TimingLogger;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Main_Activity";

    static double[][] inputWeights;
    static double[][] layerWeights;
    static double[][] inputBias;
    static double[][] outputBias;

    static double[][] meanTrain;
    static double[][] devTrain;

    double[][] inputFeat;

    double hornProb;
    double cryProb;
    double ambientProb;

    private static int TW = 50; //analysis frame duration (ms)
    private static int TS = 25; //analysis frame shit (ms)
    private static double alpha = 0.97; //pre-emphasis factor
    private static int[] range = {300, 5000}; // frequency range
    private static int M = 26; //number of filterbank channels
    private static int N = 20; // number of mfcc
    private static int L = 22; //liftering coefficient

    MFCCMatlab mfcc; // instance of class MFCCMatlab

    /***Variables for audiorecord*/
    private static final int RECORDER_SAMPLERATE = 48000;

    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;

    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private static final int RECORD_TIME_DURATION = 500; //0.5 seconds

    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    int bufferSize;
    int BufferElements2Rec;
    int BytesPerElement;
    short[] sData;
    /**---------------------------**/

    double[] speech; //TODO: audio as array. Currently reading from an array in sheet
    //TODO: Change the sampling frequency. This is for testing only
    int sampFreq = 48000; // FS - sampling frequency of audio

    /**Hurray**/
    /**The final shit that we need s here**/
    double[] featSound; //160 features of 250 ms audio
    /****/

    TimingLogger timings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timings = new TimingLogger(TAG, "PrepNN :");
        prepNN();
        timings.addSplit("After PrepNN");
        timings.dumpToLog();

        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    }

    //Load the weights and bias in the form of matrix from sheet in Assets Folder
    private void prepNN() {
        try{
            AssetManager am = getAssets();
            InputStream is = am.open("NN_Weights.xls");
            final Workbook wb = Workbook.getWorkbook(is);

            Sheet inputWeightSheet = wb.getSheet("InputWeight");
            int inputWeightSheetRows = inputWeightSheet.getRows();
            int inputWeightSheetColumns = inputWeightSheet.getColumns();
            inputWeights = new double[inputWeightSheetRows][inputWeightSheetColumns];

            for (int i = 0; i<inputWeightSheetRows; i++){
                for (int j=0; j<inputWeightSheetColumns; j++){
                    Cell z = inputWeightSheet.getCell(j, i);
                    inputWeights[i][j] = Double.parseDouble(z.getContents());
                }
            }

            Sheet layerWeightSheet = wb.getSheet("LayerWeight");
            int layerWeightSheetRows = layerWeightSheet.getRows();
            int layerWeightSheetColumns = layerWeightSheet.getColumns();
            layerWeights = new double[layerWeightSheetRows][layerWeightSheetColumns];

            for (int i = 0; i<layerWeightSheetRows; i++){
                for (int j=0; j<layerWeightSheetColumns; j++){
                    Cell z = layerWeightSheet.getCell(j, i);
                    layerWeights[i][j] = Double.parseDouble(z.getContents());
                }
            }

            Sheet inputBiasSheet = wb.getSheet("InputBias");
            int inputBiasSheetRows = inputBiasSheet.getRows();
            int inputBiasSheetColumns = inputBiasSheet.getColumns();
            inputBias = new double[inputBiasSheetRows][inputBiasSheetColumns];

            for (int i=0; i<inputBiasSheetRows; i++){
                for (int j=0; j<inputBiasSheetColumns; j++){
                    Cell z = inputBiasSheet.getCell(j, i);
                    inputBias[i][j] = Double.parseDouble(z.getContents());
                }
            }

            Sheet outputBiasSheet = wb.getSheet("OutputBias");
            int outputBiasSheetRows = outputBiasSheet.getRows();
            int outputBiasSheetColumns = outputBiasSheet.getColumns();
            outputBias = new double[outputBiasSheetRows][outputBiasSheetColumns];

            for (int i=0; i<outputBiasSheetRows; i++){
                for (int j=0; j<outputBiasSheetColumns; j++){
                    Cell z = outputBiasSheet.getCell(j, i);
                    outputBias[i][j] = Double.parseDouble(z.getContents());
                }
            }

            Sheet meanTrainSheet = wb.getSheet("MeanTrain");
            int meanTrainSheetRows = meanTrainSheet.getRows();
            int meanTrainSheetColumns = meanTrainSheet.getColumns();
            meanTrain = new double[meanTrainSheetRows][meanTrainSheetColumns];

            for (int i=0; i<meanTrainSheetRows; i++){
                for (int j=0; j<meanTrainSheetColumns; j++){
                    Cell z = meanTrainSheet.getCell(j, i);
                    meanTrain[i][j] = Double.parseDouble(z.getContents());
                }
            }

            Sheet devTrainSheet = wb.getSheet("DevTrain");
            int devTrainSheetRows = devTrainSheet.getRows();
            int devTrainSheetColumns = devTrainSheet.getColumns();
            devTrain = new double[devTrainSheetRows][devTrainSheetColumns];

            for (int i=0; i<devTrainSheetRows; i++){
                for (int j=0; j<devTrainSheetColumns; j++){
                    Cell z = devTrainSheet.getCell(j, i);
                    devTrain[i][j] = Double.parseDouble(z.getContents());
                }
            }


            Sheet inputFeatSheet = wb.getSheet("InputFeat");
            int inputFeatSheetRows = inputFeatSheet.getRows();
            int inputFeatSheetColumns = inputFeatSheet.getColumns();
            inputFeat = new double[inputFeatSheetRows][inputFeatSheetColumns];

            for (int i=0; i<inputFeatSheetRows; i++){
                for (int j=0; j<inputFeatSheetColumns; j++){
                    Cell z = inputFeatSheet.getCell(j, i);
                    inputFeat[i][j] = Double.parseDouble(z.getContents());
                }
            }
        } catch (Exception e){
        }
    }

    /**-----------------Using AudioClipRecorder Class-----------**/


    /**---------------------------------------------------------**/

    /**-----------Extra Added for AudioRecord------------**/
    public void processMicSound(View v){

        BufferElements2Rec = RECORDER_SAMPLERATE * RECORD_TIME_DURATION/1000; // number of 16 bits for 3 seconds
        //BufferElements2Rec = 24000;
        System.out.println(BufferElements2Rec);

        BytesPerElement = 2; // 2 bytes in 16bit format
        if (bufferSize != AudioRecord.ERROR_BAD_VALUE && bufferSize > 0){
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);


            recorder.startRecording();
        }

        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                while (isRecording) {
                    sData = new short[BufferElements2Rec];
                    // gets the voice output from microphone to byte format
                    recorder.read(sData, 0, BufferElements2Rec);
                    System.out.println("Short writing to file" + Arrays.toString(sData));
                    newProcessAudioMic(sData);
                    isRecording = false;
                }
            }
        }, "AudioRecorder Thread");
        recordingThread.start();

        /*TimerTask stopRec = new TimerTask() {
            @Override
            public void run() {
                isRecording = false;
                recorder.stop();
                recorder.release();
                recorder = null;
                recordingThread = null;
            }
        };
        Timer timer = new Timer();
        timer.schedule(stopRec, 50,3000); //stopRec every 3 seconds, with a 50 ms delay for the first time of execution.
*/
    }

    public void stopRecording(View v) {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }
    /**-------------------------------**/

    public void loadWeight(View v){
        //displayWeight(inputFeat);
    }

    //TODO: Add the process and handle onClick of Button 3
    public void processAudioEvent(View v){
        speech = new double[inputFeat.length];
        for(int i=0; i<inputFeat.length; i++){
            speech[i] = inputFeat[i][0];
        }

        featSound = new double[160];
        mfcc = new MFCCMatlab(speech, sampFreq, TW, TS, alpha, range, M, N, L);
        featSound = mfcc.getFeatSound();
        displayWeight(featSound);
    }

    public void newProcessAudioMic(short[] sData){
        featSound = new double[160];
        mfcc = new MFCCMatlab(sData, sampFreq, TW, TS, alpha, range, M, N, L);
        featSound = mfcc.getFeatSound();
        displayWeight(featSound);
    }
    public void calcOutput(View v){
        TextView hornValue = (TextView)findViewById(R.id.hornValue);
        TextView cryValue = (TextView)findViewById(R.id.cryValue);
        TextView ambientValue = (TextView)findViewById(R.id.ambientValue);

        //TODO: Check that inputFeat is 160x1 vector. Replace it with featSound at later stage.
        //TODO: Uncomment the code in for loop
        //Normalize input feature with training mean and deviation
        for (int i=0; i<featSound.length; i++){
            featSound[i] = (featSound[i]-meanTrain[i][0])/devTrain[i][0];
        }

        double[] hiddenNodes = new double[inputWeights.length];
        for (int i=0; i<inputWeights.length; i++){
            double sum = 0;
            for (int j=0; j<inputWeights[0].length; j++){
                sum += inputWeights[i][j]*featSound[j];
            }
            hiddenNodes[i] = tansig(sum + inputBias[i][0]);
        }

        double[] outputNodes = new double[layerWeights.length];
        for (int i=0; i<layerWeights.length; i++){
            double sum = 0;
            for (int j=0; j<layerWeights[0].length; j++){
                sum += layerWeights[i][j]*hiddenNodes[j];
            }
            outputNodes[i] = sum + outputBias[i][0];
        }

        displayWeight(outputNodes);

        double sum = 0.0;
        for (int i=0; i<outputNodes.length; i++){
            sum += Math.exp(outputNodes[i]);
        }

        for (int i=0; i<outputNodes.length; i++){
            outputNodes[i] = softmax(outputNodes[i], sum);
        }

        hornProb = outputNodes[0];
        ambientProb = outputNodes[1];
        cryProb = outputNodes[2];

        for(int i=0; i<outputNodes.length; i++){
            System.out.println("Main Activity line 243  " + Double.toString(outputNodes[i]));
        }

        hornValue.setText(Double.toString(hornProb));
        cryValue.setText(Double.toString(cryProb));
        ambientValue.setText(Double.toString(ambientProb));
    }

    public void displayWeight(double[] value){
        TextView inputWeight = (TextView)findViewById(R.id.inputWeight);
        //inputWeight.setText(Double.toString(value[0][0]));
        for(int i=0; i<value.length; i++){
            //for(int j=0; j<value.length; j++){
            inputWeight.append(Double.toString(value[i]));
            inputWeight.append("\n");
            //}
        }
    }

    private double tansig(double x) {
        return ((2.0 / (1 + Math.exp(-2*x))) - 1.0);
    }

    private double softmax(double outputNode, double sum) {
        double temp = Math.exp(outputNode);
        System.out.println("Main Activity line 268 - temp " + Double.toString(outputNode)+ ","+ Double.toString(temp));
        System.out.println("Main Activity line 269 - SUM " + Double.toString(sum));
        return (temp/sum);
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
}