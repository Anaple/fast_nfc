package org.anaple.nfc_;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import org.anaple.nfc_.databinding.ActivityMainBinding;

import java.io.IOException;

public class MainActivity extends AppCompatActivity  implements NfcAdapter.ReaderCallback {

    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter == null) {
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC is disabled.", Toast.LENGTH_SHORT).show();
        }
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button backButton = findViewById(R.id.button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, WriteNfcTagActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Set up a listener for any NFC tag or card detected by the device
        mNfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Disable reader mode when the app is in the background
        mNfcAdapter.disableReaderMode(this);
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        // This method is called when an NFC tag or card is detected by the device

        // Get the ID of the tag
        byte[] tagId = tag.getId();

        // Read the first sector of the tag
        byte[] sector0 = null;

        try {
            sector0 = readSector(tag, 0);

        } catch (IOException e) {
            e.printStackTrace();
        }


        // Convert the sector data to a string
        String sector0Data = "empty value";
      if (sector0[0] != 0x00){
            sector0Data = String.valueOf(sector0[0] & 0xFF);
        }

        // Display the sector data in a text view
        TextView textView = findViewById(R.id.text_view);
        textView.setText(sector0Data);
    }

    private byte[] readSector(Tag tag, int sectorIndex) throws IOException {
        // Get an instance of the MifareClassic class for the tag
        MifareClassic mifare = MifareClassic.get(tag);

        // Connect to the tag
        mifare.connect();

        // Get the number of sectors in the tag
        int sectorCount = mifare.getSectorCount();

        if (sectorIndex < 0 || sectorIndex >= sectorCount) {
            throw new IllegalArgumentException("Invalid sector index.");
        }

        // Authenticate the sector with key A
        boolean auth = mifare.authenticateSectorWithKeyA(sectorIndex, MifareClassic.KEY_DEFAULT);

        if (!auth) {
            throw new IOException("Authentication failed for sector " + sectorIndex);
        }

        // Read the data blocks in the sector
        byte[] sectorData = mifare.readBlock(1);

        // Disconnect from the tag
        mifare.close();

        return sectorData;
    }

    private static String ByteArrayToHexString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; ++i) {
            sb.append(String.format("%02X", data[i] & 0xFF));
        }
        return sb.toString();
    }

    private void writeBlockToSectorZero(byte[] data ,Tag tag) {
        try {
            MifareClassic mifare = MifareClassic.get(tag);
            mifare.connect();

            // Authenticate with the default key
            boolean auth = mifare.authenticateSectorWithKeyA(0, MifareClassic.KEY_DEFAULT);
            if (!auth) {
                // Authentication failed
                return;
            }

            // Write the data to block 0 in sector 0
            mifare.writeBlock(0, data);

            mifare.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}