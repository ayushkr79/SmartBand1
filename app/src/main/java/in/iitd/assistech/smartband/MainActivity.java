package in.iitd.assistech.smartband;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import java.io.InputStream;
import java.util.Arrays;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

public class MainActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener,
       OnTabEvent{

    private static final String TAG = "MainActivity";

    private TabLayout tabLayout;
    private ViewPager viewPager;
    static Pager adapter;

    static double[][] inputWeights;
    static double[][] layerWeights;
    static double[][] inputBias;
    static double[][] outputBias;
    static double[][] meanTrain;
    static double[][] devTrain;

    static boolean isPrepNN = false;
    double[][] inputFeat;

    private static int PROB_MSG_HNDL = 123;

    /**---------------**/
    MainProcessing mProcessing;
    /**---------------**/

    /***Variables for audiorecord*/
    private static int REQUEST_MICROPHONE = 101;
    public static final int RECORDER_SAMPLERATE = 44100;
    public static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    public static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public static final int RECORD_TIME_DURATION = 500; //0.5 seconds

    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    int bufferSize;
    int BufferElements2Rec;
    int BytesPerElement;
    public static short[] sData;


    static Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.getData().getInt("what") == PROB_MSG_HNDL){
                //TODO create an instance of Tab2 fragment and call editValue()
                double hornProb = msg.getData().getDouble("hornProb");
                double gunShotProb = msg.getData().getDouble("gunShotProb");
                double dogBarkProb = msg.getData().getDouble("dogBarkProb");
                adapter.editTab2Text(hornProb, gunShotProb, dogBarkProb);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);


        //Adding toolbar to the activity
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

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
                                sData = new short[BufferElements2Rec];
                                // gets the voice output from microphone to byte format
                                recorder.read(sData, 0, BufferElements2Rec);

                                //TODO: Properly make the partition of sData
//                                sDataPart1 = Arrays.copyOfRange(sData, 0, PROCESS_LENGTH);

//                                TODO:processAudioEvent();
                                mProcessing = new MainProcessing(MainActivity.this);
                                mProcessing.processAudioEvent();
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

    static void setProbOut(double[] outProb){
//        adapter.editTab2Text(outProb[0], outProb[1], outProb[2]);
        double hornProb = (outProb[0]);
        double dogBarkProb = (outProb[1]);
        double gunShotProb = (outProb[2]);

        final Message msg = new Message();
        final Bundle bundle = new Bundle();
        bundle.putInt("what", PROB_MSG_HNDL);
        bundle.putDouble("hornProb", hornProb);
        bundle.putDouble("dogBarkProb", dogBarkProb);
        bundle.putDouble("gunShotProb", gunShotProb);
        msg.setData(bundle);
        handler.sendMessage(msg);
    }
}