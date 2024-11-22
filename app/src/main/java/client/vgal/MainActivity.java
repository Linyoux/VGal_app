package client.vgal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.webkit.*;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import client.vgal.utils.FileUtil;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private VideoServer videoServer;
    private static final int REQUEST_CODE = 1024;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 创建一个LinearLayout以代码方式设置布局
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // 创建启动VgsLauncherActivity的按钮
        Button launchVgsButton = new Button(this);
        launchVgsButton.setText("Launch VGS Activity");
        launchVgsButton.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // 添加按钮点击监听器
        launchVgsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 启动VgsLauncherActivity
                Intent intent = new Intent(MainActivity.this, VgsLauncherActivity.class);
                startActivity(intent);
            }
        });

        // 将按钮添加到布局
        layout.addView(launchVgsButton);

        // 设置布局为活动的内容视图
        setContentView(layout);

        // 继续初始化其他组件
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        requestPermission();
        startVideoServer();

        // 如果你的应用中需要WebView，可以在代码中动态添加，或使用其他方式
        webView = new WebView(this);
        layout.addView(webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");

        WebSettings webSettings = webView.getSettings();
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebChromeClient(new WebChromeClient() {
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d("WebConsole", cm.message() + " -- From line "
                        + cm.lineNumber() + " of "
                        + cm.sourceId());
                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.loadUrl("file:///android_asset/index.html");
    }

    // 其他方法...

    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public String getDataPath() {
            return new File(Environment.getExternalStorageDirectory(), "vgal").getAbsolutePath();
        }

        @JavascriptInterface
        public String getScript() throws IOException {
            String text = FileUtil.readFileUTF8(new File(Environment.getExternalStorageDirectory(), "vgal/script.json").getAbsolutePath());
            return text;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopVideoServer();
    }

    private void startVideoServer() {
        try {
            videoServer = new VideoServer(6867);
        } catch (IOException ioe) {
            Log.w("Httpd", "The server could not start.");
        }
    }

    private void stopVideoServer() {
        if (videoServer != null) {
            videoServer.stop();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + this.getApplicationContext().getPackageName()));
                startActivityForResult(intent, REQUEST_CODE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // 权限请求被拒绝
            }
        }
    }
}
