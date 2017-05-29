package in.iitd.assistech.smartband;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.Tracker;

public class Tab2 extends Fragment {

    private Tracker mTracker;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Obtain the shared Tracker instance.
        //AnalyticsApplication application = (AnalyticsApplication) getActivity().getApplication();
        //mTracker = application.getDefaultTracker();
        return inflater.inflate(R.layout.tab2, container, false);
    }
}
