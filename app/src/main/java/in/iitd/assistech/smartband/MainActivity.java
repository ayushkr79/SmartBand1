package in.iitd.assistech.smartband;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TimingLogger;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.Callable;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

public class MainActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener,
       OnTabEvent{

    private static final String TAG = "MainActivity";

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private Pager adapter;

    static double[][] inputWeights;
    static double[][] layerWeights;
    static double[][] inputBias;
    static double[][] outputBias;
    static double[][] meanTrain;
    static double[][] devTrain;

    boolean isPrepNN = false;
    double[][] inputFeat;

    double hornProb;
    double cryProb;
    double ambientProb;

    private static int PROB_MSG_HNDL = 123;

    private static int TW = 50; //analysis frame duration (ms)
    private static int TS = 25; //analysis frame shit (ms)
    private static double alpha = 0.97; //pre-emphasis factor
    private static int[] range = {300, 5000}; // frequency range
    private static int M = 26; //number of filterbank channels
    private static int N = 20; // number of mfcc
    private static int L = 22; //liftering coefficient

    MFCCMatlab mfcc; // instance of class MFCCMatlab

    /**---------------**/

    /**---------------**/

    /***Variables for audiorecord*/
    private static int REQUEST_MICROPHONE = 101;
    private static final int RECORDER_SAMPLERATE = 48000;
    private static final int PROCESS_LENGTH = 24000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORD_TIME_DURATION = 3000; //0.5 seconds

    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    int bufferSize;
    int BufferElements2Rec;
    int BytesPerElement;
    short[] sData;
    short[] sDataPart1 = new short[PROCESS_LENGTH];

    /**Hurray**/
    /**The final shit that we need is here**/
    double[] featSound; //160 features of 250 ms audio
    /****/

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.getData().getInt("what") == PROB_MSG_HNDL){
                //TODO create an instance of Tab2 fragment and call editValue()
                adapter.editTab2Text(hornProb, cryProb, ambientProb);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Adding toolbar to the activity
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Initializing the tablayout
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);

        //Adding the tabs using addTab() method
        tabLayout.addTab(tabLayout.newTab().setText("Tab 1"));
        tabLayout.addTab(tabLayout.newTab().setText("Tab 2"));
        tabLayout.addTab(tabLayout.newTab().setText("Tab 3"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        //Initializing viewPager
        viewPager = (ViewPager) findViewById(R.id.pager);
        //Creating our pager adapter
        adapter = new Pager(getSupportFragmentManager(), tabLayout.getTabCount());

        //Adding adapter to pager
        viewPager.setAdapter(adapter);

        //Adding onTabSelectedListener to swipe views
        tabLayout.setOnTabSelectedListener(this);

        /**-------------**/

        /**----------**/
        Thread prepNNThread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this){
                    prepNN();
                    isPrepNN = true;
                }
            }
        });
        prepNNThread.start();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE);

        }

        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    }

    @Override
    public void onButtonClick(String text) {
        if(text == "MicReadButton"){
            processMicSound();
        }else if(text == "StopRecordButton"){
            stopRecording();
        }
    }

    /**--------------Sign In-------------------**/

    /**----------------------------------------**/
    /**----------For Sound Processing and stuff----------**/
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

    public void processMicSound(){
        if(!isRecording){
            //processing = true;
            if(!isPrepNN){
                Toast.makeText(this, "Press Again", Toast.LENGTH_SHORT).show();
            }else{
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
                            synchronized (this){
                                TimingLogger recordLogger = new TimingLogger(TAG, "recordingThread");
                                recordLogger.addSplit("Before recorder");
                                sData = new short[BufferElements2Rec];
                                // gets the voice output from microphone to byte format
                                recorder.read(sData, 0, BufferElements2Rec);
                                recordLogger.addSplit("After recorder");

                                //TODO: Properly make the partition of sData
                                sDataPart1 = Arrays.copyOfRange(sData, 0, PROCESS_LENGTH);
                                //sDataPart2 = Arrays.copyOfRange(sData, PROCESS_LENGTH, 2*PROCESS_LENGTH);
                                //sDataPart3 = Arrays.copyOfRange(sData, 2*PROCESS_LENGTH, 3*PROCESS_LENGTH);
                                //sDataPart4 = Arrays.copyOfRange(sData, 3*PROCESS_LENGTH, 4*PROCESS_LENGTH);
                                //sDataPart5 = Arrays.copyOfRange(sData, 4*PROCESS_LENGTH, 5*PROCESS_LENGTH);
                                //sDataPart6 = Arrays.copyOfRange(sData, 5*PROCESS_LENGTH, 6*PROCESS_LENGTH);

                                recordLogger.addSplit("Copy sData");
                                processAudioEvent();
                                recordLogger.dumpToLog();
                            }
                        }
                    }
                }, "AudioRecorder Thread");
                recordingThread.start();
            }
        } else{
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }

    //Called in processMicSound
    public void processAudioEvent() {
        if (!isPrepNN) {
            Toast.makeText(this, "Press Again", Toast.LENGTH_SHORT).show();
        } else {

            Thread featThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        TimingLogger featThreadLogger = new TimingLogger(TAG, "featThread");
                        featThreadLogger.addSplit("Start");

                        featSound = new double[160];
                        mfcc = new MFCCMatlab(sDataPart1, RECORDER_SAMPLERATE, TW, TS, alpha, range, M, N, L);
                        featSound = mfcc.getFeatSound();

                        //TODO: Check that inputFeat is 160x1 vector. Replace it with featSound at later stage.
                        //Normalize input feature with training mean and deviation
                        for (int i = 0; i < featSound.length; i++) {
                            featSound[i] = (featSound[i] - meanTrain[i][0]) / devTrain[i][0];
                        }

                        double[] hiddenNodes = new double[inputWeights.length];
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

                        hornProb = outputNodes[0];
                        ambientProb = outputNodes[1];
                        cryProb = outputNodes[2];

                        for (int i = 0; i < outputNodes.length; i++) {
                            System.out.println("Main Activity line 243  " + Double.toString(outputNodes[i]));
                        }

                        Message msg = new Message();
                        Bundle bundle = new Bundle();
                        bundle.putInt("what", PROB_MSG_HNDL);
                        msg.setData(bundle);
                        handler.sendMessage(msg);
                        featThreadLogger.addSplit("FeatSound calculated");
                        featThreadLogger.dumpToLog();
                    }
                }
            });
            featThread.start();
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

    /**---------------------------------------**/

    /**-----------For Tab Layout--------------**/
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    /**---------------------------------------**/
}