package com.dji.FPVDemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;



import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import dji.common.product.Model;
import dji.common.remotecontroller.HardwareState;
import dji.keysdk.KeyManager;
import dji.keysdk.RemoteControllerKey;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.thirdparty.okhttp3.MediaType;
import dji.thirdparty.okhttp3.OkHttpClient;
import dji.thirdparty.okhttp3.Request;
import dji.thirdparty.okhttp3.RequestBody;
import dji.thirdparty.okhttp3.Response;

import static com.google.mlkit.vision.barcode.Barcode.*;

public class CameraActivity extends Activity implements SurfaceTextureListener,OnClickListener{

    private static final String TAG = CameraActivity.class.getName();
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

    protected TextureView mVideoSurface = null;
    protected TextView TV = null;

    private Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");

    private Handler handler;

    private BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                    FORMAT_QR_CODE
            )
            .build();

    private BarcodeScanner scanner = BarcodeScanning.getClient(options);
    private List<String> codes = null;
    private Camera camera;
    private String Mu;
    private String Loc;
    private Boolean Read = false;
    private List<String> CorrectLabels = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler();
        initUI();

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };
        camera = FPVDemoApplication.getCameraInstance();
    }

    protected void onProductChange() {
        initPreviewer();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
        onProductChange();

        if(mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view){
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initUI() {

        // init mVideoSurface
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);
        TV = findViewById(R.id.textview_qrcode);

        RemoteControllerKey keyC2 = RemoteControllerKey.create(RemoteControllerKey.CUSTOM_BUTTON_2);

        RemoteControllerKey keyC1 = RemoteControllerKey.create(RemoteControllerKey.CUSTOM_BUTTON_1);

        KeyManager.getInstance().addListener(keyC1, (oldValue, newValue) -> {
            if( newValue != null ) {
                if(((HardwareState.Button)newValue).isClicked()){
                    QrSend(Loc, Mu, Read);
                }
            }
        });

        KeyManager.getInstance().addListener(keyC2, (oldValue, newValue) -> {
            if( newValue != null ) {
                if(((HardwareState.Button)newValue).isClicked()){
                    QrRead();
                }
            }
        });
        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }
    }

    private void QrRead() {
        Read = false;
        Mu = null;
        Loc = null;
        CorrectLabels.clear();

        Bitmap bm = mVideoSurface.getBitmap();
        InputImage image = InputImage.fromBitmap(bm, 0);
        Task<List<Barcode>> result = scanner.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> barcodes) {
                        if (barcodes.size() > 0){
                            for (Integer Index =0 ; Index < barcodes.size(); Index++) {
                                if (!(TextUtils.isEmpty(barcodes.get(Index).getRawValue())) || ((barcodes.get(Index).getRawValue().contains("MU")) || (barcodes.get(Index).getRawValue().contains("LOC")))){
                                    if (!CorrectLabels.contains(barcodes.get(Index).getRawValue())){
                                        CorrectLabels.add(barcodes.get(Index).getRawValue());
                                    }
                                }
                            }
                            for (Integer Index =0 ; Index < CorrectLabels.size(); Index++) {
                                if (CorrectLabels.get(Index).contains("MU")){
                                    Mu = CorrectLabels.get(Index);
                                }
                                else{
                                     if(CorrectLabels.get(Index).contains("LOC")){

                                         Loc = CorrectLabels.get(Index);
                                     }
                                }
                            }

                            if (TextUtils.isEmpty(Mu) && TextUtils.isEmpty(Loc)) {
                                TV.setText("Sem Leitura.");
                                ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 1000);
                                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,1000);

                            }else{
                                if (TextUtils.isEmpty(Mu)){
                                    TV.setText(Loc + " and Mu is empty.");
                                    ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 1000);
                                    toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,1000);
                                }else{
                                    if (TextUtils.isEmpty(Loc)){
                                        TV.setText("Loc is empty and " + Mu);
                                        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 1000);
                                        toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,1000);
                                    }else{
                                            TV.setText(Loc + " and " + Mu);
                                            Read = true;
                                            ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 1000);
                                            toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,100);
                                    }
                                }
                            }
                        }else{
                            TV.setText("Sem Leitura.");
                            ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 1000);
                            toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,1000);
                        }
                    }
                });
    }

    private void QrSend(String Location, String MaterialUnit, Boolean Read) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if(Read){
                    try {
                        URL url = new URL("https://cyclic-inventory-api.lomi.devtest.aws.scania.com/api/Validation/SendValidation");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                        conn.setRequestProperty("Accept","application/json");
                        conn.setDoOutput(true);
                        conn.setDoInput(true);
                        JSONObject jsonParam = new JSONObject();
                        jsonParam.put("PositionId", Location);
                        jsonParam.put("PartId", MaterialUnit);
                        jsonParam.put("RegisteredBy", "Collector");
                        DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                        os.writeBytes(jsonParam.toString());
                        os.flush();
                        os.close();
                        showToast(conn.getResponseMessage());
                        if (conn.getResponseCode() == 200){
                            ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 1000);
                            toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);
                            TV.setText("Success!");
                        }else{
                            TV.setText("System Failure!");
                            ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 1000);
                            toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,1000);
                        }
                        conn.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else{
                    TV.setText("System Malfunction!");
                    ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 1000);
                    toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,1000);
                }
            }
        });
        thread.start();
    }



    private void initPreviewer() {
        BaseProduct product = FPVDemoApplication.getProductInstance();
        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
            }
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG,"onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(CameraActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
    }

    private void Alert(){
        AlertDialog.Builder builder  = new AlertDialog.Builder(this);
        builder.setView(R.layout.alert_view);
        builder.show();
    }

    public boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        return pattern.matcher(strNum).matches();
    }
}