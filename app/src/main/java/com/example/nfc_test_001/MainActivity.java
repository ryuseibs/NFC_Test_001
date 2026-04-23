package com.example.nfc_test_001;

import android.media.MediaParser;
import android.media.MediaPlayer;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {
    private NfcAdapter nfcAdapter;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // 音を鳴らす準備（MediaPlayer）
        mediaPlayer = MediaPlayer.create(this, R.raw.id_sound);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(nfcAdapter != null) {
            // Reader Mode 有効化
            Bundle options = new Bundle();
            nfcAdapter.enableReaderMode(this,this,
//                    NfcAdapter.FLAG_READER_NFC_A |
//                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F|
                            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options
                    );
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableReaderMode(this);
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        // ID取得（iPhoneのUID、IDm）
        byte[] id = tag.getId();
        String idString = bytesToHexString(id);

        // ログ出力
        Log.d("NFC_CHECK", "iPhone detected! ID:" + idString);

        //NFC規格
        String[] techList = tag.getTechList();
        Log.d("NFC_CHECK", "Tech List:" + Arrays.toString(techList));

        // 音を鳴らす
        if(mediaPlayer != null) {
            // 再生中の場合は停止して最初に戻す
            if(mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                try {
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mediaPlayer.start();
        }
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}