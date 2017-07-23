package in.iitd.assistech.smartband;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

public class NotifListAdapter extends ArrayAdapter<String> implements CompoundButton.OnCheckedChangeListener{

    private SparseBooleanArray mCheckStates;
    LayoutInflater myCustomInflater;
    TextView notifTypeText;
    CheckBox cb;
    String[] gen;

    public NotifListAdapter(Context context, String[] items){
        super(context, R.layout.notif_list_row, items);

        mCheckStates = new SparseBooleanArray(items.length);
        myCustomInflater = LayoutInflater.from(getContext());
        gen= items;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View customView = myCustomInflater.inflate(R.layout.notif_list_row, parent, false);

        String notifType = getItem(position);
        notifTypeText = (TextView) customView.findViewById(R.id.notif_type);
        notifTypeText.setText(notifType);

        cb = (CheckBox) customView.findViewById(R.id.notif_check_box);
        cb.setChecked(mCheckStates.get(position, false)); // cb is checkbox
        cb.setOnCheckedChangeListener(this);


        return customView;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
//        mCheckStates.put((Integer) buttonView.getTag(), isChecked);
    }

    public boolean isChecked(int position) {
        return mCheckStates.get(position, false);
    }

    public void setChecked(int position, boolean isChecked) {
        mCheckStates.put(position, isChecked);

    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return gen.length;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub

        return 0;
    }

    public void toggle(int position) {
        setChecked(position, !isChecked(position));
    }
}
