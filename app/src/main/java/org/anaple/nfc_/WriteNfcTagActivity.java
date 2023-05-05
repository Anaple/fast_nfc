package org.anaple.nfc_;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.Locale;

public class WriteNfcTagActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFiltersArray;
    private EditText editText;

    private static final String MIME_TEXT_PLAIN = "text/plain";
    private static final String TAG = "WriteNfcTagActivity";
    private String ed;



    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_nfc_tag);

        editText = findViewById(R.id.edit_text);

        // 获取NFC适配器
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Button backButton = findViewById(R.id.button_back_to_main);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WriteNfcTagActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        Button writeButton = findViewById(R.id.button_write_tag);
        writeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ed = editText.getText().toString();
            }
        });

    }





    @Override
    protected void onPause() {
        super.onPause();
        // 禁用前台调度以确保我们的Activity不再在前台运行时处理NFC标签
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Set up a listener for any NFC tag or card detected by the device
        nfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null);
    }


    @Override
    public void onTagDiscovered(Tag tag) {
        // 获取MifareUltralight对象

        // 连接NFC标签

        // 写入第2块数据
        Integer st1;
        byte b = 0x00;
        Log.e("Err",ed);
        try {

            st1 = Integer.parseInt(ed);
            b = (byte) (st1 & 0xff);

        }catch (Exception e){
            Log.e("Err",e.getMessage());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(WriteNfcTagActivity.this, "读取input失败", Toast.LENGTH_LONG).show();
                }
            });

        }
        byte[] data = new byte[]{(byte) b , (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x78, (byte) 0x78, (byte) 0x78, (byte) 0x78, (byte) 0x78, (byte) 0x78, (byte) 0x78, (byte) 0x78, (byte) 0x78, (byte) 0x78, (byte) 0x78, (byte) 0x78};
        writeTag(tag, data);
        // 在UI线程中更新UI
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(WriteNfcTagActivity.this, "写入成功", Toast.LENGTH_LONG).show();
            }
        });
    }




    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
    public void writeTag(Tag tag, byte[] data) {
        MifareClassic nfcA = MifareClassic.get(tag);
        try {
            nfcA.connect();
            nfcA.authenticateSectorWithKeyA(0, MifareClassic.KEY_DEFAULT);
            nfcA.writeBlock(1, data);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                nfcA.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private NdefRecord createRecord(String text) {
        byte[] languageCode = Locale.getDefault().getLanguage().getBytes(Charset.forName("US-ASCII"));
        final byte[] textBytes = text.getBytes(Charset.forName("UTF-8"));
        final int languageCodeLength = languageCode.length;
        final int textLength = textBytes.length;
        final ByteArrayOutputStream payload = new ByteArrayOutputStream(1 + languageCodeLength + textLength);

        // set status byte (see NDEF spec for actual bits)
        payload.write((byte) (languageCodeLength & 0x1F));

        // write language code
        payload.write(languageCode, 0, languageCodeLength);

        // write text
        payload.write(textBytes, 0, textLength);

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0],
                payload.toByteArray());
    }


    private void writeTag(Tag tag, String text) {
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);

        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                ndef.writeNdefMessage(message);
                Toast.makeText(this, "Tag written successfully", Toast.LENGTH_LONG).show();
                ndef.close();
            } else {
                NdefFormatable formatable = NdefFormatable.get(tag);
                if (formatable != null) {
                    formatable.connect();
                    formatable.format(message);
                    Toast.makeText(this, "Tag written successfully", Toast.LENGTH_LONG).show();
                    formatable.close();
                } else {
                    Toast.makeText(this, "Tag is not NDEF formatable", Toast.LENGTH_LONG).show();
                }
            }
        } catch (IOException | FormatException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to write tag", Toast.LENGTH_LONG).show();
        }
    }
}
