package com.example.copyclient;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.os.MessageQueue;
import android.util.Log;
import android.widget.Toast;

public class CopyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.copy_activity);

        listenToMessageQueue();



        finish();
    }

    private void listenToMessageQueue(){
        final MessageQueue.IdleHandler handler = () -> {
            ClipboardService service = CustomServiceConnection.myService;
            // Get the ClipboardManager instance
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

            // Check if there is any data on the clipboard
            if (clipboardManager.hasPrimaryClip()) {
                // Get the primary clip from the clipboard
                ClipData clipData = clipboardManager.getPrimaryClip();

                // Check if the clip contains data
                if (clipData != null && clipData.getItemCount() > 0) {
                    // Get the text from the first item in the clip
                    CharSequence copiedText = clipData.getItemAt(0).getText();
                    service.sendMessage(copiedText.toString());
                }
            }
            return false;
        };
        Looper.myQueue().addIdleHandler(handler);
    }
}