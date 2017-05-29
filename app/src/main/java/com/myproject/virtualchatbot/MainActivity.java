package com.myproject.virtualchatbot;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.github.bassaer.chatmessageview.models.Message;
import com.github.bassaer.chatmessageview.models.User;
import com.github.bassaer.chatmessageview.utils.ChatBot;
import com.github.bassaer.chatmessageview.views.ChatView;

import org.alicebot.ab.AIMLProcessor;
import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;
import org.alicebot.ab.MagicStrings;
import org.alicebot.ab.PCAIMLProcessorExtension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private ChatView mChatView;
    public Bot virtualBot;
    public static Chat chat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //User id
        int myId = 0;
        //User icon
        Bitmap myIcon = BitmapFactory.decodeResource(getResources(), R.drawable.user);
        //User name
        String userName = getResources().getString(R.string.user_name);

        int yourId = 1;
        Bitmap yourIcon = BitmapFactory.decodeResource(getResources(), R.drawable.virtual);
        String botName = getResources().getString(R.string.bot_name);

        final User user = new User(myId, userName, myIcon);
        final User bot = new User(yourId, botName, yourIcon);

        mChatView = (ChatView)findViewById(R.id.chat_view);

        //Set UI parameters if you need
        mChatView.setRightBubbleColor(ContextCompat.getColor(this, R.color.green500));
        mChatView.setLeftBubbleColor(Color.WHITE);
        mChatView.setBackgroundColor(ContextCompat.getColor(this, R.color.blueGray500));
        mChatView.setSendButtonColor(ContextCompat.getColor(this, R.color.cyan900));
        mChatView.setSendIcon(R.drawable.ic_action_send);
        mChatView.setRightMessageTextColor(Color.WHITE);
        mChatView.setLeftMessageTextColor(Color.BLACK);
        mChatView.setUsernameTextColor(Color.WHITE);
        mChatView.setSendTimeTextColor(Color.WHITE);
        mChatView.setDateSeparatorColor(Color.WHITE);
        mChatView.setInputTextHint("new message...");
        mChatView.setMessageMarginTop(5);
        mChatView.setMessageMarginBottom(5);

        //copy aiml files from assets to external files directory
        //check if SD card is available to prevent crash
        boolean isSDCardAvailable = Environment.getExternalStorageState().
                equals(Environment.MEDIA_MOUNTED)? true :false;

        File storageFile = new File(getExternalFilesDir(null).toString() + "/bots/EDI");
        boolean dirs = storageFile.mkdirs();
        if(storageFile.exists()) {
            try {
                AssetManager assetManager = getApplicationContext().getAssets();
                for (String dir : assetManager.list("EDI")) {
                    File subdir = new File(storageFile.getPath() + "/" + dir);
                    boolean subDirs = subdir.mkdirs();
                    for (String file : assetManager.list("EDI/" + dir)) {
                        File f = new File(storageFile.getPath() + "/" + dir + "/" + file);
                        if (f.exists()) {
                            continue;
                        }
                        InputStream in = null;
                        OutputStream out = null;
                        in = assetManager.open("EDI/" + dir + "/" + file);
                        out = new FileOutputStream(storageFile.getPath() + "/" + dir + "/" + file);
                        //copy file from assets to the mobile's SD card or any secondary memory
                        byte[] buffer = new byte[1024];
                        int read;
                        while((read = in.read(buffer)) != -1){
                            out.write(buffer, 0, read);
                        }
                        in.close();
                        out.flush();
                        out.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //initialize chatbot
        MagicStrings.root_path = getExternalFilesDir(null).toString() + "/";
        AIMLProcessor.extension = new PCAIMLProcessorExtension();
        virtualBot = new Bot("EDI", MagicStrings.root_path, "chat");
        chat = new Chat(virtualBot);

        //Click Send Button
        mChatView.setOnClickSendButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //new message
                Message message = new Message.Builder()
                        .setUser(user)
                        .setRightMessage(true)
                        .setMessageText(mChatView.getInputText())
                        .hideIcon(true)
                        .build();
                //Set to chat view
                mChatView.send(message);
                //Reset edit text
                mChatView.setInputText("");

                //Receive message
                final Message receivedMessage = new Message.Builder()
                        .setUser(bot)
                        .setRightMessage(false)
                        .setMessageText(ChatBot.talk(user.getName(), message.getMessageText()))
                        .build();

                // This is a demo bot
                // Return within 3 seconds
                int sendDelay = (new Random().nextInt(4) + 1) * 1000;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mChatView.receive(receivedMessage);
                    }
                }, sendDelay);
            }

        });

    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
