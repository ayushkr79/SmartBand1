package in.iitd.assistech.smartband;

import android.app.Activity;
import android.content.Context;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import static in.iitd.assistech.smartband.MainActivity.adapter;

public class HistoryNotifAdapter extends BaseAdapter {
    private Activity mContext;
    private List<String> mList;
    private LayoutInflater mLayoutInflater = null;

    public HistoryNotifAdapter(Activity context, List<String> list) {
        mContext = context;
        mList = list;
        mLayoutInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int pos) {
        return mList.get(pos);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View v = convertView;
        HistoryListViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater li = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = li.inflate(R.layout.history_list_row, null);
            viewHolder = new HistoryListViewHolder(v);
            v.setTag(viewHolder);
        } else {
            viewHolder = (HistoryListViewHolder) v.getTag();
        }
        viewHolder.historyItem.setText(mList.get(position));
        viewHolder.historyCrossButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeItem(position);
            }
        });
        viewHolder.historyCheckButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeItem(position);
            }
        });

        return v;
    }

    public void removeItem(int position){
        mList.remove(position);
    }
}

class HistoryListViewHolder {
    public TextView historyItem;
    public ImageButton historyCheckButton;
    public ImageButton historyCrossButton;
    public HistoryListViewHolder(View base) {
        historyItem = (TextView) base.findViewById(R.id.history_item);
        historyCheckButton = (ImageButton) base.findViewById(R.id.historyCheckButton);
        historyCrossButton = (ImageButton) base.findViewById(R.id.historyCrossButton);
    }
}
