package com.example.webview.cpostal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    public int REQUEST_CODE = 1;

    private ValueCallback<Uri[]> f_string;
    private String cam_path;
    private ActivityResultLauncher<Intent> myARL = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            Uri[] results = null;

            if (result.getResultCode() == Activity.RESULT_CANCELED) {
                f_string.onReceiveValue(null);
                return;
            }
            if (result.getResultCode() == Activity.RESULT_OK) {
                if (null == f_string) {
                    return;
                }

                Uri[] clipData = null;
                Bitmap bitmapPhoto;
                String stringData = null;

                if (result.getData() != null) {
                    if (result.getData().getExtras() != null) {
                        bitmapPhoto = (Bitmap) result.getData().getExtras().get("data");
                        clipData = new Uri[] {getImageUri(getApplicationContext(), bitmapPhoto)};
                    } else {
                        stringData = result.getData().getDataString();
                    }

                }


                if (clipData == null && stringData == null && cam_path != null) {
                    results = new Uri[]{Uri.parse(cam_path)};
                } else {
                    if (null != clipData) {
                        final int numSelectedFiles = clipData.length;
                        results = new Uri[numSelectedFiles];
                        for (int i = 0; i < clipData.length; i++) {
                            results[i] = clipData[i];
                        }
                    } else {
                        try {
                            assert result.getData() != null;
                            Bitmap cam_photo = (Bitmap) result.getData().getExtras().get("data");
                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                            cam_photo.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                            stringData = MediaStore.Images.Media.insertImage(getContentResolver(), cam_photo, null, null);
                        } catch (Exception ignored) {
                        }

                        results = new Uri[]{Uri.parse(stringData)};
                    }
                }
            }

            f_string.onReceiveValue(results);
            f_string = null;
        }
    });

    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadWebView();

    }


    @SuppressLint("SetJavaScriptEnabled")
    private void loadWebView() {
        WebView myWebView = findViewById(R.id.web_view);

        WebSettings settings = myWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setUseWideViewPort(false);
        settings.setDomStorageEnabled(true);
        myWebView.setWebViewClient(new WebViewClient() {
            private WebView view;

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        myWebView.loadUrl("MY_URL");

        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                f_string = filePathCallback;
                Intent takePictureIntent;
                takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                Intent contentSelectionInent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionInent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionInent.setType("image/*");
                Intent[] intentArray;
                intentArray = new Intent[]{takePictureIntent};
                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionInent);
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                myARL.launch(chooserIntent);
                return true;
            }
        });
    }
}