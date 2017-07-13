package in.iitd.assistech.smartband;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

class CustomListAdapter extends ArrayAdapter<String>{
    public CustomListAdapter(Context context, String[] items){
        super(context, R.layout.custom_first_row, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // default -  return super.getView(position, convertView, parent);
        // add the layout
        LayoutInflater myCustomInflater = LayoutInflater.from(getContext());
        View customView = myCustomInflater.inflate(R.layout.custom_first_row, parent, false);
        // get references.
        //String singleFoodItem = getItem(position);
        TextView nameText = (TextView) customView.findViewById(R.id.userNameText);
//        TextView emailText = (TextView) customView.findViewById(R.id.userEmailIdText);
        String name = getItem(position);
//        String email = getItem(1);

        //TODO: Add image dynamically from account
        ImageView userProfileImage = (ImageView) customView.findViewById(R.id.userProfileImage);

        // dynamically update the text from the array
        nameText.setText(name);
//        emailText.setText(email);
        // using the same image every time
        userProfileImage.setImageResource(R.drawable.profile_pic);
        // Now we can finally return our custom View or custom item
        return customView;
    }
}
