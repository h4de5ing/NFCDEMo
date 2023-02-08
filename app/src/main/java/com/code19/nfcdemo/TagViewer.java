package com.code19.nfcdemo;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class TagViewer extends AppCompatActivity {
    private TextView textView;
    private Button clean;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());
        clean = findViewById(R.id.clean);
        clean.setOnClickListener(v -> textView.setText(""));
        resolveIntent(getIntent());
    }

    void resolveIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null && rawMsgs.length > 0) {
                updateTv("读到NdefMessage");
                NdefMessage msg = (NdefMessage) rawMsgs[0];
                NdefRecord[] records = msg.getRecords();
            }
        } else {
            updateTv("Unknown intent " + intent);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        resolveIntent(intent);
    }

    private void updateTv(String message) {
        try {
            runOnUiThread(() -> {
                textView.append("\n" + message);
                int offset = textView.getLineCount() * textView.getLineHeight() - textView.getHeight();
                textView.scrollTo(0, Math.min(0, offset));
            });
        } catch (Exception ignored) {
        }
    }
}
