package com.example.webview.cpostal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    String[] PERMISSOES = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    private int goBackHome = 0;
    private boolean pressEnterButton = false;

    private Button btnAddPermission, btnError;
    private Group group;
    ConstraintLayout layout;
    private WebView myWebView;
    private LinearProgressIndicator progressIndicator;
    private LottieAnimationView lottieAnimationView;
    private TextView tvWelcome, tvErrorInfo;
    private MaterialToolbar toolbar;

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
                Bitmap bitmapPhoto = null;
                String stringData = null;

                if (result.getData() != null) {
                    if (result.getData().getExtras() != null) {
                        bitmapPhoto = (Bitmap) result.getData().getExtras().get("data");
                        clipData = new Uri[]{getImageUri(getApplicationContext(), bitmapPhoto)};
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
    private ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), (ActivityResultCallback<Map<String, Boolean>>) result -> {
                if (!result.containsValue(false)) {
                    loadWebView();
                } else {
                    Snackbar.make(
                            findViewById(R.id.activity_main),
                            getResources().getString(R.string.label_permission_toast),
                            Snackbar.LENGTH_LONG
                    ).show();
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

        instanciarLayout();


        btnAddPermission.setOnClickListener(v -> {
            if (checarPermissoesAuto()) {
                loadWebView();
            } else {
                checarPermissoes();
            }
        });

        toolbar.setNavigationOnClickListener(v -> {
            onBackPressed();
        });

        btnError.setOnClickListener(v -> {
            loadWebView();
        });

        startSplashAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loading(checarPermissoesAuto());
    }

    @Override
    public void onBackPressed() {
        myWebView.clearAnimation();
        if (myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
            toolbar.setVisibility(View.GONE);
            goBackHome++;
            if (goBackHome == 1) {

                Animation animationMove = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.move_center_to_right);
                myWebView.setVisibility(View.GONE);
                myWebView.startAnimation(animationMove);
                animationMove.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        myWebView.clearAnimation();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
            }
            if (goBackHome > 1 || !pressEnterButton) {
                super.onBackPressed();
            }
        }
    }

    private void instanciarLayout() {
        btnAddPermission = findViewById(R.id.btn_add_permission);
        btnError = findViewById(R.id.btn_error);
        group = findViewById(R.id.group);
        layout = findViewById(R.id.layout);
        myWebView = findViewById(R.id.web_view);
        progressIndicator = findViewById(R.id.progress_horizontal);
        lottieAnimationView = findViewById(R.id.ic_lottie);
        tvWelcome = findViewById(R.id.tv_welcome);
        tvErrorInfo = findViewById(R.id.tv_error_info);

        toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

    }

    private void startSplashAnimation() {
        Animation animationFade = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade);
        findViewById(R.id.ic_logo).startAnimation(animationFade);

        Animation animationUpAlpha = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.move_up_alpha);
        findViewById(R.id.tv_welcome).startAnimation(animationUpAlpha);

        Animation animationBlink = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoom_in_out);
        findViewById(R.id.ic_letter).startAnimation(animationBlink);

        Animation animationBounce = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bounce);
        btnAddPermission.startAnimation(animationBounce);
    }

    private void loading(boolean permissionsGranted) {
        if (permissionsGranted) {
            btnAddPermission.setText(getResources().getString(R.string.label_enter));
        } else {
            btnAddPermission.setText(getResources().getString(R.string.label_request_permission_text_button));
        }
    }

    private void checarPermissoes() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            ) {
                loadWebView();
            } else if (shouldShowRequestPermissionRationale(PERMISSOES[0])) {
                dialogPermission("Câmera");
            } else if (shouldShowRequestPermissionRationale(PERMISSOES[1])) {
                dialogPermission("Arquivos e mídia");
            } else {
                requestPermissionLauncher.launch(PERMISSOES);
            }
        }
    }

    private boolean checarPermissoesAuto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void loadWebView() {
        //Controle de visualização no onBackPressed()
        goBackHome = 0;
        pressEnterButton = true;

        myWebView.clearAnimation();
        Animation animationMove = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.move_right_to_left);
        myWebView.setVisibility(View.VISIBLE);
        myWebView.startAnimation(animationMove);

        WebSettings settings = myWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setUseWideViewPort(false);
        settings.setDomStorageEnabled(true);
        myWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.d("TAG", "onReceivedError: " + error.getDescription().toString());
                    setErrorUi(error.getDescription().toString());
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                MainMenu();
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

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);

                if (newProgress == 100) {
                    progressIndicator.setVisibility(View.GONE);
                } else {
                    progressIndicator.setVisibility(View.VISIBLE);
                    progressIndicator.setProgress(newProgress);
                }
            }
        });
    }

    private void setErrorUi(String errorDescription) {
        myWebView.clearAnimation();
        myWebView.setVisibility(View.GONE);
        layout.setElevation(4);
        lottieAnimationView.setAnimation(R.raw.animation_error);
        lottieAnimationView.setRepeatCount(0);
        lottieAnimationView.playAnimation();
        tvWelcome.setText(getResources().getString(R.string.label_somehting_wrong));
        tvErrorInfo.setText(errorDescription);
        btnAddPermission.clearAnimation();
        btnAddPermission.setVisibility(View.GONE);
        btnError.setVisibility(View.VISIBLE);
        toolbar.setVisibility(View.GONE);
    }

    private void MainMenu() {
        layout.setElevation(1);
        group.setVisibility(View.VISIBLE);
        lottieAnimationView.setAnimation(R.raw.animation_package);
        lottieAnimationView.playAnimation();
        tvWelcome.setText(getResources().getString(R.string.label_welcome));
        tvWelcome.setTextColor(getResources().getColor(R.color.black));
        tvErrorInfo.setText("");
        btnAddPermission.setVisibility(View.VISIBLE);
        btnError.setVisibility(View.GONE);
        toolbar.setVisibility(View.VISIBLE);
    }

    private void dialogPermission(String permissaoNegada) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getResources().getString(R.string.label_permission_title))
                .setMessage(getResources().getString(R.string.label_permission_description, permissaoNegada))
                .setPositiveButton("CONFIGURAÇÕES DO APLICATIVO", (dialogInterface, i) -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts(
                            "package",
                            BuildConfig.APPLICATION_ID,
                            null
                    );
                    intent.setData(uri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }).setNegativeButton("AGORA NÃO", ((dialogInterface, i) -> dialogInterface.dismiss()))
                .show();
    }
}