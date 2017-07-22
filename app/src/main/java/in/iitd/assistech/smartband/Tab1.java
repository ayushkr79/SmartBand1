package in.iitd.assistech.smartband;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;

public class Tab1 extends Fragment implements View.OnClickListener {

    public ListView sttListView;
    private ImageButton speakButton;
//    private ImageButton ttsSendButton;
    private View view;

    private EditText messageET;
    private ListView messagesContainer;
    private Button sendBtn;
    private TextToSpeech msgTTS;
    private ChatAdapter adapter;
    private ArrayList<ChatMessage> chatHistory;

    public static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.tab1, container, false);
        //Returning the layout file after inflating
        //Change R.layout.tab1 in you classes

        speakButton = (ImageButton) view.findViewById(R.id.speakButton);
        speakButton.setOnClickListener(this);


        // Check to see if a recognition activity is present
// if running on AVD virtual device it will give this message. The mic
// required only works on an actual android device
        PackageManager pm = getContext().getPackageManager();
        List activities = pm.queryIntentActivities(new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() != 0) {
            speakButton.setOnClickListener(this);
        } else {
            speakButton.setEnabled(false);
            Toast.makeText(getActivity(), "Recognizer not present", Toast.LENGTH_SHORT).show();
//            speakButton.setText("Recognizer not present");
        }


        initControls();

        msgTTS = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener(){
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    msgTTS.setLanguage(Locale.US);
                }
            }
        });

        return view;
    }

    public void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak Now");
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000);
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.speakButton) {
            startVoiceRecognitionActivity();
        }

        if (view.getId() == R.id.chatSendButton){
            String messageText = messageET.getText().toString();
            if (TextUtils.isEmpty(messageText)) {
                return;
            }

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setId(122);//dummy
            chatMessage.setMessage(messageText);
            chatMessage.setDate(DateFormat.getDateTimeInstance().format(new Date()));
            chatMessage.setMe(true);

            messageET.setText("");

            displayMessage(chatMessage);
            msgTTS.speak(messageText, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private void initControls() {
        messagesContainer = (ListView) view.findViewById(R.id.messagesContainer);
        messageET = (EditText) view.findViewById(R.id.messageEdit);
        sendBtn = (Button) view.findViewById(R.id.chatSendButton);

        adapter = new ChatAdapter(getActivity(), new ArrayList<ChatMessage>());
        messagesContainer.setAdapter(adapter);
        messagesContainer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ChatMessage clickedMsg = adapter.getItem(i);
                String toSpeak = clickedMsg.getMessage();
                msgTTS.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            }
        });

        TextView meLabel = (TextView) view.findViewById(R.id.meLbl);
        TextView companionLabel = (TextView) view.findViewById(R.id.friendLabel);
        RelativeLayout container = (RelativeLayout) view.findViewById(R.id.container);
        companionLabel.setText("My Buddy");// Hard Coded

        sendBtn.setOnClickListener(this);
    }

    public void displayMessage(ChatMessage message) {
        adapter.add(message);
        adapter.notifyDataSetChanged();
        scroll();
    }

    private void scroll() {
        messagesContainer.setSelection(messagesContainer.getCount() - 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {

            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String voiceText = matches.get(0);
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setId(122);//dummy
            chatMessage.setMessage(voiceText);
            chatMessage.setDate(DateFormat.getDateTimeInstance().format(new Date()));
            chatMessage.setMe(false);

            displayMessage(chatMessage);
        }
    }

    @Override
    public void onDestroy() {
        if(msgTTS !=null){
            msgTTS.stop();
            msgTTS.shutdown();
        }
        super.onDestroy();
    }
}
