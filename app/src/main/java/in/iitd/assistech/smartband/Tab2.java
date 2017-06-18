package in.iitd.assistech.smartband;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class Tab2 extends Fragment implements View.OnClickListener{

    private View view;
    private TextView hornValue;
    private TextView cryValue;
    private TextView ambientValue;
    private Button micReadButton;
    private Button stopRecordButton;

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

        micReadButton.setOnClickListener(this);
        stopRecordButton.setOnClickListener(this);

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
