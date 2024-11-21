package client.vgal;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.*;
import androidx.appcompat.app.AppCompatActivity;
import client.vgal.saveload.GameData;
import com.alibaba.fastjson.JSON;

public class SaveLoadActivity extends AppCompatActivity {

    private WebView webView;
    private boolean save;
    private GameData gameData;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();

        save = extras.getBoolean("isSave");
        gameData = (GameData) getIntent().getSerializableExtra("gameData");

        // 设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 隐藏ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_saveload);


        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true); // 如果你的HTML需要JavaScript支持
        webView.getSettings().setDomStorageEnabled(true); // 如果你的HTML需要JavaScript支持


        webView.addJavascriptInterface(new SaveLoadActivity.WebAppInterface(this), "VGAL");

        WebSettings webSettings = webView.getSettings();
        webSettings.setMediaPlaybackRequiresUserGesture(false);

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

        if (save){
            webView.loadUrl("file:///android_asset/save.html");
        }else {
            webView.loadUrl("file:///android_asset/load.html");
        }
    }
    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        setResult(Activity.RESULT_OK, resultIntent); // 设置结果码
        finish();
    }

    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public boolean SaveData(int slot){
            return NativeVideoActivity.getGameDataManager().SaveData(slot,gameData);
        }


        @JavascriptInterface
        public void Close(){
            onBackPressed();
        }

        @JavascriptInterface
        public boolean LoadData(int slot){
            try {
                GameData gameData = NativeVideoActivity.getGameDataManager().LoadData(slot);

                Intent resultIntent = new Intent();
                resultIntent.putExtra("gameData",gameData);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
                return true;
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }

        @JavascriptInterface
        public String getSaveData(){
            return JSON.toJSONString(NativeVideoActivity.getGameDataManager().getSaveData2());
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
