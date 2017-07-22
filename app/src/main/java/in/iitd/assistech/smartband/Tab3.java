package in.iitd.assistech.smartband;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

//import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class Tab3 extends Fragment implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener{

    public View view;
    private static final String TAG = "Tab3";

    private FirebaseUser user;
    private FirebaseAuth mAuth;
    private GoogleApiClient mGoogleApiClient;
    private String providerId;
    private String uid;
    private String name;
    private String email;
    private Uri photoUrl;

    Bitmap profileBM;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.tab3, container, false);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .build();
        // [END config_signin]
        if(mGoogleApiClient == null){
            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .enableAutoManage(getActivity() /* FragmentActivity */, this /* OnConnectionFailedListener */)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();
        }

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        providerId = user.getProviderId();

        view.findViewById(R.id.signOutButton).setOnClickListener(this);
        view.findViewById(R.id.revokeButton).setOnClickListener(this);

        // UID specific to the provider
        uid = user.getUid();
        updateUI();

        /**---------Custom List View -------**/
        String[] string = {name};
//        String[] string = {name, email};
        ListAdapter customListAdapter = new CustomListAdapter(getContext(),string);// Pass the food arrary to the constructor.
        ListView customListView = (ListView) view.findViewById(R.id.profileListView);
        customListView.setAdapter(customListAdapter);

        customListView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Toast.makeText(getActivity(),"Profile List Clicked", Toast.LENGTH_LONG).show();
                    }
                }
        );

        final String[] settingListItems = {"Custom Sound", "About Us"};
        ListAdapter settingsListAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, settingListItems);
        ListView settingsListView = (ListView) view.findViewById(R.id.settingsListView);
        settingsListView.setAdapter(settingsListAdapter);
        settingsListView.setOnItemClickListener(
                new AdapterView.OnItemClickListener(){
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String setting = String.valueOf(parent.getItemAtPosition(position));
                    }
                }
        );
        /**--------------------------------**/
        /**Spinner List items**/
        final String[] select_qualification = {
                "Notification Options", "Flashlight", "Vibration", "Sound", "Flash Screen"};
        Spinner spinner = (Spinner) view.findViewById(R.id.notification_spinner);

        ArrayList<StateVO> listVOs = new ArrayList<>();

        for (int i = 0; i < select_qualification.length; i++) {
            StateVO stateVO = new StateVO();
            stateVO.setTitle(select_qualification[i]);
            stateVO.setSelected(false);
            listVOs.add(stateVO);
        }
        SpinnerAdapter myAdapter = new SpinnerAdapter(getActivity(), 0, listVOs);
        spinner.setAdapter(myAdapter);
        /**------------------*/

        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.signOutButton:
                signOut();
                break;
            case R.id.revokeButton:
                revokeAccess();
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mGoogleApiClient.stopAutoManage(getActivity());
        mGoogleApiClient.disconnect();
    }

    //TODO: Call this method from Tab3
    private void signOut() {
        // Firebase sign out
        mAuth.signOut();

        // Google sign out
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        //TODO: updateUI(null);
                        Log.e(TAG, "Google SignOut1" + status.toString());
                        if(mAuth.getCurrentUser() == null){
                            Log.e(TAG, "Google SignOut258");
                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                            startActivity(intent);
                            getActivity().finish();
                        }
                    }
                });
        Log.e(TAG, "Google SignOut");
    }

    private void revokeAccess() {
        // Firebase sign out
        mAuth.signOut();

        // Google revoke access
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        //TODO: updateUI(null);
                    }
                });
    }

    private void updateUI(){
//        CircleImageView profilePic = (CircleImageView)view.findViewById(R.id.profile_image);
//        profilePic.setImageURI(photoUrl);
        //ImageView imageView = (ImageView)view.findViewById(R.id.imageView);
        //imageView.setImageURI(photoUrl);

        name = user.getDisplayName();
        email = user.getEmail();
        photoUrl = user.getPhotoUrl();
        String mUserprofileUrl = photoUrl.toString();
        Log.d(TAG, name + email);
        Log.d(TAG, photoUrl.toString());

        // Name, email address, and profile photo Url

//        CircleImageView profilePic = (CircleImageView)view.findViewById(R.id.profile_image);
//        try{
//            Glide.with(getActivity()).load(photoUrl.getPath()).into(profilePic);
//        }catch (Exception e){
//            Log.e(TAG, e.toString());
//        }

//        updateUI();
    }

    private Bitmap getImageBitmap(String url) {
        Bitmap bm = null;
        try {
            URL aURL = new URL(url);
            URLConnection conn = aURL.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(bis);
            bis.close();
            is.close();
        } catch (IOException e) {
            Log.e(TAG, "Error getting bitmap", e);
        }
        return bm;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.e(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(getActivity(), "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }
}
