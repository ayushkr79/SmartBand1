package in.iitd.assistech.smartband;

import android.content.Context;
import android.graphics.Point;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class Tab2 extends Fragment implements View.OnClickListener{

    private static final String TAG = "TAB2";

    private View view;
    private TextView hornValue;
    private TextView cryValue;
    private TextView ambientValue;
    private Button micReadButton;
    private Button stopRecordButton;

    private ImageButton startButton;
    private ImageButton stopButton;

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
        cryValue = (TextView)view.findViewById(R.id.cryValue);
        ambientValue = (TextView)view.findViewById(R.id.ambientValue);
        micReadButton = (Button)view.findViewById(R.id.micReadButton);
        stopRecordButton = (Button)view.findViewById(R.id.stopRecordButton);

        startButton = (ImageButton)view.findViewById(R.id.start_button);
        startButton.setImageResource(R.drawable.start_button_small);

        stopButton = (ImageButton)view.findViewById(R.id.stop_button);
        stopButton.setImageResource(R.drawable.stop_button);

        micReadButton.setOnClickListener(this);
        startButton.setOnClickListener(this);
        stopRecordButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);

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

    public void editValue(double hornProb, double cryProb, double ambientProb){
        hornValue.setText(String.format("%.2g%n", hornProb));
        cryValue.setText(String.format("%.2g%n", cryProb));
        ambientValue.setText(String.format("%.2g%n", ambientProb));
    }
}
