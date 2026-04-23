package com.example.nfc_test_001;

import android.media.MediaParser;
import android.media.MediaPlayer;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
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

        //NFC-Fの中身
        NfcF nfcF = NfcF.get(tag);
        if (nfcF != null) {
            // IDm
            byte[] idm = tag.getId();
            Log.d("NFC_CHECK", "IDm:" + bytesToHexString(idm));

            //システムコード
            byte[] systemCode = nfcF.getSystemCode();
            Log.d("NFC_CHECK", "System Code:" + bytesToHexString(systemCode));
        }

        try {
            // 接続開始
            nfcF.connect();

            // コマンド（バイナリ）を組み立てる
            // Pollingコマンド（システムコードFE00を検索）
            byte[] command = new byte[] {
                    (byte) 0x06,   // データ長（6バイト）
                    (byte) 0x00,   // Pollingコマンドコード
                    (byte) 0xFE, (byte) 0x00, // System Code (iD)
                    (byte) 0x01,   // タイムスロット
                    (byte) 0x00    // おまじない
            };

            // 送信してレスポンスを受け取る
            byte[] response = nfcF.transceive(command);

            // 5. ログに出力（16進数に変換して表示）
            Log.d("NFC_CHECK", "Response: " + bytesToHexString(response));

            // レスポンスからIDm（2〜9バイト目）を抽出
            if (response != null && response.length >= 10) {
                byte[] idm = new byte[8];
                System.arraycopy(response, 2, idm, 0, 8);

                // --- 2. 抽出したIDmを使って、中身を読みに行く (Read Without Encryption) ---
                byte[] readCommand = new byte[] {
                        (byte) 0x10, // データ長
                        (byte) 0x06, // Read Without Encryption
                        idm[0], idm[1], idm[2], idm[3], idm[4], idm[5], idm[6], idm[7], // 動的なIDm
                        (byte) 0x01, // サービス数
                        (byte) 0x0B, (byte) 0x10, // iDのサービスコード (100Bのリトルエンディアン)
                        (byte) 0x01, // ブロック数
                        (byte) 0x80, (byte) 0x00  // 0番目のブロック
                };

                // ここが運命の分かれ道！
                byte[] readRes = nfcF.transceive(readCommand);
                Log.d("NFC_CHECK", "Read Response: " + bytesToHexString(readRes));
            }

            nfcF.close();
        } catch (IOException e) {
            Log.e("NFC_CHECK", "通信失敗", e);
        }

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