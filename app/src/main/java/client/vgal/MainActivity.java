package client.vgal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.webkit.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import client.vgal.game.Game;
import client.vgal.game.GameData;
import client.vgal.game.GameDataManager;
import com.alibaba.fastjson.JSON;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private VideoServer videoServer;
    private static final int REQUEST_CODE = 1024;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermission();

        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_main);


//        startVideoServer();

        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true); // 如果你的HTML需要JavaScript支持
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);

        webView.addJavascriptInterface(new WebAppInterface(this), "VGAL");

        WebSettings webSettings = webView.getSettings();
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setDomStorageEnabled(true);

        webView.setWebChromeClient(new WebChromeClient() {
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d("WebConsole", cm.message() + " -- From line "
                        + cm.lineNumber() + " of "
                        + cm.sourceId() );
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

        webView.loadUrl("file:///android_asset/homepage.html");

//        webView.loadUrl("https://v.jstv.com/xw360/");
    }


    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public String getGames() {
            File rootPath = new File(Environment.getExternalStorageDirectory(), "vgal");
            List<Game> games = new ArrayList<>();

            for (File file : Objects.requireNonNull(rootPath.listFiles())){
                if(file.isDirectory()){
                    File startFile = new File(file,"start.vgs");
                    if (startFile.exists() && startFile.isFile()){
                        Game game = new Game();
                        game.setName(file.getName());

                        File iconFile = new File(file,"icon.png");
                        if(iconFile.exists() && iconFile.isFile()){
                            game.setIcon(iconFile.getAbsolutePath());
                        }

                        games.add(game);
                    }
                }
            }

            return JSON.toJSONString(games);
        }

        @JavascriptInterface
        public boolean startGame(String game) throws IOException {
            Intent intent = new Intent(MainActivity.this, NativeVideoActivity.class);
            // 启动新的Activity
            File file = new File(Environment.getExternalStorageDirectory(), "vgal/" + game);
            if (!file.exists()) {
                return false;
            }
            intent.putExtra("rootPath",file);
            startActivity(intent);

            return true;
        }

        @JavascriptInterface
        public boolean openLoadData(String game) throws IOException {
            Intent intent = new Intent(MainActivity.this, SaveLoadActivity.class);
            // 启动新的Activity
            File file = new File(Environment.getExternalStorageDirectory(), "vgal/" + game);
            if (!file.exists()) {
                return false;
            }

            GameDataManager gameDataManager = new GameDataManager(new File(getApplicationContext().getFilesDir(),game));
            intent.putExtra("rootPath",file.getAbsolutePath());
            intent.putExtra("game",game);
            intent.putExtra("isSave",false);
            intent.putExtra("gameDataManager",gameDataManager);
            startActivityForResult(intent,1);

            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopVideoServer();
    }

    private void startVideoServer(){
        try {
            videoServer = new VideoServer(6867);
        } catch (IOException ioe) {
            Log.w("Httpd", "The server could not start.");
        }
    }

    private void stopVideoServer(){
        if (videoServer != null) {
            videoServer.stop();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if (data.getExtras() != null){
                //处理读档

                if (data.getExtras() != null){
                    //处理读档
                    GameData gameData = (GameData) data.getExtras().get("gameData");
                    String game = data.getExtras().getString("game");
                    File file = new File(Environment.getExternalStorageDirectory(), "vgal/" + game);
                    if (!file.exists()) {
                        return;
                    }
                    Intent intent = new Intent(MainActivity.this, NativeVideoActivity.class);
                    intent.putExtra("rootPath",file);
                    intent.putExtra("gameData",gameData);
                    startActivity(intent);
                }
            }
        }
    }

//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        if (hasFocus) {
//            hideSystemUI();
//        }
//    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
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
            // 先判断有没有权限
        if (Environment.isExternalStorageManager()) {
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + this.getApplicationContext().getPackageName()));
                startActivityForResult(intent, REQUEST_CODE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 先判断有没有权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
              
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
            }
        } else {
          
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

            } else {
            }
        }
    }


}