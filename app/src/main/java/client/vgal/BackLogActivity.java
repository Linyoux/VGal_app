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
import client.vgal.game.BackLog;
import client.vgal.game.LogText;
import client.vgal.script.block.NormalBlock;
import client.vgal.script.block.ScriptBlock;
import com.alibaba.fastjson.JSON;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class BackLogActivity extends AppCompatActivity {
    private WebView webView;

    private String rootPath;
    private boolean playing;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        rootPath = extras.getString("rootPath");
        playing = extras.getBoolean("playing");

        // 设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 隐藏ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_saveload);


        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true); // 如果你的HTML需要JavaScript支持
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);

        webView.addJavascriptInterface(new BackLogActivity.WebAppInterface(this), "VGAL");

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

        File backLogFile = new File(rootPath, "gui/backlog.html");
        if (backLogFile.exists() && backLogFile.isFile()){
            webView.loadUrl(backLogFile.getAbsolutePath());
        }else {
            webView.loadUrl("file:///android_asset/backlog.html");
        }
    }

    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public String getBackLog(int page){
            if(page <= 0){
                return null;
            }

            int pageSize = 10;

            BackLog backLog = new BackLog();
            backLog.setPage(page);

            int currentPosition = NativeVideoActivity.getScriptManager().getCurrentPosition();
            currentPosition = playing ? currentPosition : currentPosition - 1;
            int index = currentPosition - (pageSize * (page - 1));
            int startIndex = Math.max(0,index - pageSize);

            List<ScriptBlock> list = NativeVideoActivity.getScriptManager().rangeGet(startIndex,index);
            if (list == null){
                return null;
            }
            AtomicInteger num = new AtomicInteger(startIndex);
            List<LogText> texts = list.stream().map(scriptBlock -> {
                NormalBlock block = (NormalBlock) scriptBlock;
                LogText logText = new LogText();
                logText.setText(block.getText());
                logText.setPos(num.getAndIncrement());
                return logText;
            }).collect(Collectors.toList());
//            Collections.reverse(texts);
            int maxPage = (currentPosition + 1) / pageSize;
            maxPage = (currentPosition + 1) % pageSize == 0 ? maxPage : maxPage + 1;
            backLog.setMaxPage(maxPage);
            backLog.setTexts(texts);
            return JSON.toJSONString(backLog);
        }

        @JavascriptInterface
        public boolean backTo(int pos){
            Intent resultIntent = new Intent();
            resultIntent.putExtra("backPos",pos);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
            return true;
        }
    }
}
