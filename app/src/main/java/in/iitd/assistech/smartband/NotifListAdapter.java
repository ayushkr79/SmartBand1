package in.iitd.assistech.smartband;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class NotifListAdapter extends BaseAdapter {

    private static final String TAG = "Tab3";

    String[] names;
    Context context;
    LayoutInflater inflter;
    String value;

    boolean[] switchState;

    public NotifListAdapter(Context context, String[] names) {
        this.context = context;
        this.names = names;
        inflter = (LayoutInflater.from(context));
        this.switchState = new boolean[names.length];
        for (int i=0; i<switchState.length; i++){
            switchState[i] = true;
        }
    }

    public NotifListAdapter(Context context, String[] names, boolean[] switchState) {
        this.context = context;
        this.names = names;
        inflter = (LayoutInflater.from(context));
        this.switchState = switchState;
    }

    @Override
    public int getCount() {
        return names.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public boolean getCheckedState(int position){
        return switchState[position];
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        view = inflter.inflate(R.layout.notif_list_row, null);

        final TextView notifTextView = (TextView) view.findViewById(R.id.notif_row_text);
        final Switch notifSwitch = (Switch) view.findViewById(R.id.notif_row_switch);

        notifTextView.setText(names[position]);
        notifSwitch.setChecked(switchState[position]);

        notifSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (notifSwitch.isChecked()) {
                    switchState[position] = notifSwitch.isChecked();
                    value = "Checked";
                    Toast.makeText(context, value, Toast.LENGTH_SHORT).show();
                } else {
                    switchState[position] = notifSwitch.isChecked();
                    value = "un-Checked";
                    Toast.makeText(context, value, Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

}