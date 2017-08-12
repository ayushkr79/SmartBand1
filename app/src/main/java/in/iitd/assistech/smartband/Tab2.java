package in.iitd.assistech.smartband;

import android.app.NotificationManager;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.SimpleTimeZone;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.os.ParcelFileDescriptor.MODE_APPEND;
import static com.facebook.FacebookSdk.getApplicationContext;

public class Tab2 extends Fragment implements View.OnClickListener{

    private static final String TAG = "TAB2";

    private View view;
    private TextView hornValue;
    private TextView barkValue;
    private TextView gunShotValue;
    private TextView ambientValue;
    private Button micReadButton;
    private Button stopRecordButton;

    private ImageButton startButton;
    private ImageButton stopButton;
    private ListView historyListView;
    private List<String> histListItems;
    private HistoryNotifAdapter historyNotifAdapter;

    private OnTabEvent mListener;

    public Tab2(){

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.tab2, container, false);

        hornValue = (TextView)view.findViewById(R.id.hornValue);
        barkValue = (TextView)view.findViewById(R.id.barkValue);
        gunShotValue = (TextView)view.findViewById(R.id.gunShotValue);
        ambientValue = (TextView)view.findViewById(R.id.ambientValue);
        micReadButton = (Button)view.findViewById(R.id.micReadButton);
        stopRecordButton = (Button)view.findViewById(R.id.stopRecordButton);

        startButton = (ImageButton)view.findViewById(R.id.start_button);
        stopButton = (ImageButton)view.findViewById(R.id.stop_button);
        historyListView = (ListView)view.findViewById(R.id.historyListView);

        micReadButton.setOnClickListener(this);
        startButton.setOnClickListener(this);
        stopRecordButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        historyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                historyNotifAdapter.notifyDataSetChanged();
            }
        });

        histListItems = new ArrayList<String>();
        historyNotifAdapter = new HistoryNotifAdapter(getActivity(), histListItems);
        historyListView.setAdapter(historyNotifAdapter);


        return view;
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.micReadButton:
                //TODO
                mListener.onButtonClick("MicReadButton");
                break;
            case R.id.stopRecordButton:
                //TODO
                mListener.onButtonClick("StopRecordButton");
                break;

            case R.id.start_button:
                //TODO
                mListener.onButtonClick("MicReadButton");
                startButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);
                break;
            case R.id.stop_button:
                //TODO
                mListener.onButtonClick("StopRecordButton");
                startButton.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnTabEvent) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnTabEvent interface");
        }
    }

    public void editValue(double hornProb, double barkProb, double gunShotProb, double ambientProb, boolean[] notifState){
//        if(hornProb>0.85){
//            histListItems.add("Horn Detected");
//            historyNotifAdapter.notifyDataSetChanged();
//            NotificationCompat.Builder mBuilder =
//                    new NotificationCompat.Builder(getActivity())
//                            .setSmallIcon(R.drawable.notif_icon)
//                            .setContentTitle("My notification")
//                            .setContentText("Hello World!");
//
//            int mNotificationId = 001;
//            // Gets an instance of the NotificationManager service
//            NotificationManager mNotifyMgr =
//                    (NotificationManager) getContext().getSystemService(NOTIFICATION_SERVICE);
//            // Builds the notification and issues it.
//            mNotifyMgr.notify(mNotificationId, mBuilder.build());
//            Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
//            // Vibrate for 500 milliseconds
//            v.vibrate(500);
//        }

        double[] output = {hornProb, barkProb, gunShotProb, ambientProb};
        String[] textOut = {"Horn Detected", "DogBark Detected", "GunShot Detected", "Ambience"};
        String[] textDes = {"There might be a car around!",
                        "Beware of DOGS!!!",
                        "Firearms, CALL THE POLICE!!!", "Ambience"};
        String filename = "notif_log_smartband.csv";
        for (int i=0; i<(output.length-1); i++){
            if(output[i]>0.85){
                histListItems.add(textOut[i]);

                writeToExcel(filename, output, textOut[i]);

                historyNotifAdapter.notifyDataSetChanged();
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(getActivity())
                                .setSmallIcon(R.drawable.notif_icon)
                                .setContentTitle(textOut[i])
                                .setContentText(textDes[i]);

                int mNotificationId = 001;
                // Gets an instance of the NotificationManager service
                NotificationManager mNotifyMgr =
                        (NotificationManager) getContext().getSystemService(NOTIFICATION_SERVICE);
                // Builds the notification and issues it.
                mNotifyMgr.notify(mNotificationId, mBuilder.build());

                //Vibrate
                if(notifState[0]){
                    Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                    // Vibrate for 500 milliseconds
//                    v.vibrate(500);
                }
            }
        }
        hornValue.setText(String.format("%.2g%n", hornProb));
        barkValue.setText(String.format("%.2g%n", barkProb));
        gunShotValue.setText(String.format("%.2g%n", gunShotProb));
        ambientValue.setText(String.format("%.2g%n", ambientProb));
    }

    public void writeToExcel(String filename, double[] output, String detail){
        File external = Environment.getExternalStorageDirectory();
        String sdcardPath = external.getPath() + "/SmartBand/";
        // to this path add a new directory path
        File file = new File(external, filename);
        try{
            if (!file.exists()){
                file.createNewFile();
            }
            FileOutputStream fos  = getContext().openFileOutput(filename, MODE_APPEND);
//                            Writer out = new BufferedWriter(new OutputStreamWriter(openFileOutput(file.getName(), MODE_APPEND)));

            android.text.format.DateFormat df = new android.text.format.DateFormat();
            Date date = new Date();

            FileWriter filewriter = new FileWriter(sdcardPath + filename, true);
            BufferedWriter out = new BufferedWriter(filewriter);
//            StringBuilder sb = new StringBuilder(dateString.length());
//            sb.append(dateString);
            Log.e(TAG, "Time: " + date.toString());

            String data = date.toString() + ",";
            for(int i=0; i<output.length; i++){
                data += output[i] + ",";
            }
            data += detail + "\n";

            out.write(data);
            out.close();
            filewriter.close();

            fos.close();
        }catch(Exception e){
            Log.e(TAG, e.toString() + " FileOutputStream");
        }
    }
}
