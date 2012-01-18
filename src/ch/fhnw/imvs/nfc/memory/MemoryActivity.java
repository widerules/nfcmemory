/*
 * Copyright (c) 2012 Fachhochschule Nordwestschweiz (FHNW)
 * All Rights Reserved. 
 */

package ch.fhnw.imvs.nfc.memory;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.TextView;

public class MemoryActivity extends Activity {

    private TextView text;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);
        //mText = (TextView) findViewById(R.id.text);

        // NFC
        String activity = getCodeFromNdefMessages(getIntent());
        if (activity != null)
        {
            // invokeActivity(activity); ???
        }
    }

    private void invokeActivity(String id)
    {
        this.text.setText(id);
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        String item = getCodeFromNdefMessages(intent);
        if (item != null)
        {
            invokeActivity(item);
        }
    }

    String getCodeFromNdefMessages(Intent intent)
    {
        String code = null;

        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
            || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action))
        {
            Parcelable[] rawMsgs =
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null && rawMsgs.length > 0)
            {
                NdefMessage msg = (NdefMessage) rawMsgs[0];
                NdefRecord[] recs = msg.getRecords();
                if (recs.length > 0)
                {
                    NdefRecord rec = recs[0];
                    byte[] payload = rec.getPayload();
                    code = new String(payload);
                }
            }
        }
        return code;
    }

}
