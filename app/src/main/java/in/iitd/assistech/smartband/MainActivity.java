package in.iitd.assistech.smartband;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

import static in.iitd.assistech.smartband.Tab3.notificationListItems;
import static in.iitd.assistech.smartband.Tab3.soundListItems;

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

    private static boolean[] startNotifListState;
    private static boolean[] startSoundListState;


    static Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.getData().getInt("what") == PROB_MSG_HNDL){
                //TODO create an instance of Tab2 fragment and call editValue()
                double hornProb = msg.getData().getDouble("hornProb");
                double barkProb = msg.getData().getDouble("dogBarkProb");
                double gunShotProb = 0.0;//msg.getData().getDouble("gunShotProb");
                double ambientProb = msg.getData().getDouble("ambientProb");
                boolean[] notifState = adapter.getInitialNotifListState();
                adapter.editTab2Text(hornProb, barkProb, gunShotProb, ambientProb, notifState);
            }
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(this);
//        SharedPreferences.Editor editor = app_preferences.edit();
//        boolean[] notifState = getFinalNotifState();
//        editor.putBoolean("Vibration", notifState[0]);
//        editor.putBoolean("Sound", notifState[1]);
//        editor.putBoolean("FlashLight", notifState[2]);
//        editor.putBoolean("FlashScreen", notifState[3]);
//        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(this);

        boolean[] notifState = new boolean[notificationListItems.length];
        for (int i=0; i<notificationListItems.length; i++){
            notifState[i] = app_preferences.getBoolean(notificationListItems[i], true);
        }
        boolean[] soundState =  new boolean[soundListItems.length];
        for (int i=0; i<soundListItems.length; i++){
            soundState[i] = app_preferences.getBoolean(soundListItems[i], true);
        }

        startNotifListState = notifState;
        startSoundListState = soundState;
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = app_preferences.edit();
        if(adapter.getInitialNotifListState() != null){
            boolean[] notifState = adapter.getInitialNotifListState();
            for (int i=0; i<notificationListItems.length; i++){
                editor.putBoolean(notificationListItems[i], notifState[i]);
            }
            editor.commit();
        }

        if(adapter.getInitialSoundListState() != null){
            boolean[] soundState = adapter.getInitialSoundListState();
            for (int i=0; i<soundListItems.length; i++){
                editor.putBoolean(soundListItems[i], soundState[i]);
            }
            editor.commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        //TODO: Hardware Acceleration
//        getWindow().setFlags(
//                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
//                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);


        //Adding toolbar to the activity
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        //Initializing the tablayout
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);

        //Initializing viewPager
        viewPager = (ViewPager) findViewById(R.id.pager);
        tabLayout.setupWithViewPager(viewPager);

        //Adding the tabs using addTab() method
        tabLayout.addTab(tabLayout.newTab());
        tabLayout.addTab(tabLayout.newTab());
        tabLayout.addTab(tabLayout.newTab());
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        //Creating our pager adapter
        adapter = new Pager(getSupportFragmentManager(), tabLayout.getTabCount());
        //Adding adapter to pager
        viewPager.setAdapter(adapter);

        tabLayout.getTabAt(0).setText("Chat");
        tabLayout.getTabAt(1).setText("Sound");
        tabLayout.getTabAt(2).setText("Settings");

        //Adding onTabSelectedListener to swipe views
//        tabLayout.setOnTabSelectedListener(this);
        tabLayout.addOnTabSelectedListener(this);

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

        /**-------------------------------**/

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
            Log.e(TAG, e.toString());
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
        tabLayout.getTabAt(tab.getPosition()).select();
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
//        double gunShotProb = (outProb[2]);
        double ambientProb = (outProb[2]);

        final Message msg = new Message();
        final Bundle bundle = new Bundle();
        bundle.putInt("what", PROB_MSG_HNDL);
        bundle.putDouble("hornProb", hornProb);
        bundle.putDouble("dogBarkProb", dogBarkProb);
//        bundle.putDouble("gunShotProb", gunShotProb);
        bundle.putDouble("ambientProb", ambientProb);
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    static boolean[] getStartNotifListState(){
        return startNotifListState;
    }

    static boolean[] getStartSoundListState(){
        return startSoundListState;
    }
}