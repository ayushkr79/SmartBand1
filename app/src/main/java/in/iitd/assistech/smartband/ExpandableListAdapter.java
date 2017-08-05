package in.iitd.assistech.smartband;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;

public class ExpandableListAdapter extends BaseExpandableListAdapter {

//    private Context _context;
//    private List<String> _listDataHeader; // header titles
    // child data in format of header title, child title
//    private HashMap<String, List<String>> _listDataChild;

    String[] names;
    String header;
    Context context;
    LayoutInflater inflter;
    String value;
    boolean[] switchState;

//    public ExpandableListAdapter(Context context, List<String> listDataHeader,
//                                 HashMap<String, List<String>> listChildData) {
//        this._context = context;
//        this._listDataHeader = listDataHeader;
//        this._listDataChild = listChildData;
//    }

    public ExpandableListAdapter(Context context, String[] names, String header, boolean[] switchState) {
        this.context = context;
        this.names = names;
        this.header = header;
        inflter = (LayoutInflater.from(context));
        this.switchState = switchState;
        this.switchState = switchState;
    }

    public boolean getCheckedState(int position){
        return switchState[position];
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View view, ViewGroup parent)  {
        view = inflter.inflate(R.layout.notif_list_row, null);
        final TextView notifTextView = (TextView) view.findViewById(R.id.notif_row_text);
        final Switch notifSwitch = (Switch) view.findViewById(R.id.notif_row_switch);
        notifTextView.setText(names[childPosition]);
        notifSwitch.setChecked(switchState[childPosition]);

        notifSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (notifSwitch.isChecked()) {
                    switchState[childPosition] = notifSwitch.isChecked();
                    value = "Checked";
                    Toast.makeText(context, value, Toast.LENGTH_SHORT).show();
                } else {
                    switchState[childPosition] = notifSwitch.isChecked();
                    value = "un-Checked";
                    Toast.makeText(context, value, Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return null;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }
    @Override
    public int getChildrenCount(int groupPosition) {
        return names.length;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return null;
    }

    @Override
    public int getGroupCount() {
        return 1;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.collapsible_list_group, null);
        }

        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(header);

        return convertView;
    }
}
