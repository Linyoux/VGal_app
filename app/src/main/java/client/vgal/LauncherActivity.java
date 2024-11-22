package client.vgal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import client.vgal.saveload.GameData;
import client.vgal.saveload.GameDataManager;
import client.vgal.script.ScriptErrorException;
import client.vgal.script.ScriptManager;
import client.vgal.script.block.CallBlock;
import client.vgal.script.block.NormalBlock;
import client.vgal.script.block.PlayBlock;
import client.vgal.utils.ToastUtils;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.material.navigation.NavigationView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class LauncherActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1024;

    private PlayerView playerView;
    private ExoPlayer player;
    private Handler handler = new Handler();
    private Handler statushandler = new Handler();
    private Runnable runnable;

    private Button autoModeButton;
    private Button skipModeButton;
    private Button saveButton;
    private Button loadButton;
    private Button backPrevButton;
    private Button exitButton;

    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private TextView gamestatusText;

    private boolean autoMode;
    private boolean skipMode;
    private boolean jumpBeforePlaying;
    private Uri videouri;
    private static GameDataManager gameDataManager;
    private ScriptManager scriptManager;
    private File root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nativevideo);
        requestPermission();
        initializeComponents();
    }

    private void initializeComponents() {
        root = new File(Environment.getExternalStorageDirectory(), "vgal/");
        gameDataManager = new GameDataManager(getApplicationContext().getFilesDir());
        scriptManager = new ScriptManager(root);
        readScripts();

        // 设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        playerView = findViewById(R.id.player_view);
        playerView.setUseController(false);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        setVideoUrl();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    CallBlock callBlock = scriptManager.getNextCall();
                    if (callBlock != null) {
                        String scriptName = callBlock.getScriptName();
                        try {
                            scriptManager.call(scriptName);
                            setVideoUrl();
                        } catch (ScriptErrorException e) {
                            e.printStackTrace();
                            handleScriptError(e);
                        }
                    }
                }
            }
        });

        player.prepare();
        player.play(); // 自动开始播放

        // 创建一个Runnable，检查视频播放位置
        runnable = new Runnable() {
            @Override
            public void run() {
                if (skipMode || autoMode) {
                    return;
                }

                double currentTime = player.getCurrentPosition() / 1000.0;
                NormalBlock obj = ((NormalBlock) scriptManager.getBlock(scriptManager.getCurrentPosition()));
                if (obj != null && currentTime >= obj.getTime()) {
                    player.pause();
                    scriptManager.next();
                    gamestatusText.setText("||");
                    gamestatusText.setVisibility(View.VISIBLE);
                }
                handler.postDelayed(this, 100);
            }
        };
        handler.postDelayed(runnable, 100);

        initRightButtons();
        setupTouchListener();
    }

    private void setVideoUrl() {
        PlayBlock playBlock = scriptManager.getPlayVideo();
        File videoFile = new File(root, playBlock.getVideoName());
        videouri = Uri.fromFile(videoFile);
        MediaItem mediaItem = MediaItem.fromUri(videouri);
        player.setMediaItem(mediaItem);
    }

    private void initRightButtons() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        gamestatusText = findViewById(R.id.gamestatusText);

        autoModeButton = findViewById(R.id.autoMode);
        skipModeButton = findViewById(R.id.skipMode);
        saveButton = findViewById(R.id.save);
        loadButton = findViewById(R.id.load);
        backPrevButton = findViewById(R.id.backPrev);
        exitButton = findViewById(R.id.exit);

        View.OnClickListener listener = (event) -> {
            switch (event.getId()) {
                case R.id.autoMode:
                    autoMode = true;
                    statushandler.post(() -> {
                        gamestatusText.setText("自动模式");
                        gamestatusText.setVisibility(View.VISIBLE);
                    });
                    player.play();
                    break;
                case R.id.skipMode:
                    player.play();
                    statushandler.post(() -> {
                        gamestatusText.setText("快进模式");
                        PlaybackParameters params = new PlaybackParameters(1000, 1);
                        player.setPlaybackParameters(params);
                        gamestatusText.setVisibility(View.VISIBLE);
                    });
                    skipMode = true;
                    break;
                case R.id.backPrev:
                    if (scriptManager.getCurrentPosition() < 2) {
                        ToastUtils.showToast(getApplicationContext(), "无法回退");
                        return;
                    }
                    handler.post(() -> {
                        NormalBlock obj = (NormalBlock) scriptManager.getBlock(scriptManager.getCurrentPosition() - 2);
                        if (obj == null) {
                            return;
                        }
                        int time = obj.getTime().intValue() * 1000 + 100;
                        player.seekTo(time);
                        scriptManager.prev(1);
                    });
                    break;
                case R.id.exit:
                    new AlertDialog.Builder(this)
                            .setTitle("确认退出")
                            .setMessage("你确定要退出吗？")
                            .setPositiveButton("确定", (dialog, which) -> LauncherActivity.this.finish())
                            .setNegativeButton("取消", null)
                            .show();
                    break;
                case R.id.save:
                case R.id.load:
                    if (player.isPlaying()) {
                        jumpBeforePlaying = true;
                        player.pause();
                    }
                    drawerLayout.closeDrawer(GravityCompat.END);
                    GameData gameData = new GameData();
                    gameData.setCurrentIndex(scriptManager.getCurrentPosition());
                    gameData.setScriptName(scriptManager.getCurrentScript());
                    gameData.setBmpPath(getCurrentFrameAsBase64());

                    Intent intent = new Intent(this, SaveLoadActivity.class);
                    boolean isSave = event.getId() == R.id.save;
                    intent.putExtra("isSave", isSave);
                    intent.putExtra("gameData", gameData);
                    startActivityForResult(intent, 1);
                    break;
                default:
                    ToastUtils.showToast(getApplicationContext(), "开发中");
                    break;
            }
            drawerLayout.closeDrawer(GravityCompat.END);
        };

        autoModeButton.setOnClickListener(listener);
        skipModeButton.setOnClickListener(listener);
        saveButton.setOnClickListener(listener);
        loadButton.setOnClickListener(listener);
        backPrevButton.setOnClickListener(listener);
        exitButton.setOnClickListener(listener);
    }

    private void setupTouchListener() {
        AtomicLong lastClick = new AtomicLong(System.currentTimeMillis());
        playerView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClick.get() <= 200) {
                    if (resetAllMode()) {
                        return true;
                    }
                } else {
                    lastClick.set(System.currentTimeMillis());
                    if (autoMode || skipMode) {
                        return true;
                    }
                    if (!player.isPlaying()) {
                        player.play();
                        gamestatusText.setVisibility(View.INVISIBLE);
                    } else {
                        NormalBlock obj = (NormalBlock) scriptManager.next();
                        if (obj != null) {
                            int time = obj.getTime().intValue() * 1000 + 10;
                            player.seekTo(time);
                        }
                    }
                }
            }
            return true;
        });
    }

    private void readScripts() {
        try {
            scriptManager.callCurrent();
        } catch (ScriptErrorException e) {
            e.printStackTrace();
            handleScriptError(e);
        }
    }

    private boolean resetAllMode() {
        if (skipMode || autoMode) {
            if (skipMode) {
                PlaybackParameters params = new PlaybackParameters(1, 1);
                player.setPlaybackParameters(params);
            }
            skipMode = false;
            autoMode = false;
            resetIndex();
            gamestatusText.setVisibility(View.INVISIBLE);
            return true;
        }
        return false;
    }

    private void resetIndex() {
        scriptManager.resetIndex(player.getCurrentPosition());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        player.release();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            drawerLayout.openDrawer(GravityCompat.END);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (jumpBeforePlaying) {
                player.play();
                jumpBeforePlaying = false;
            }
            if (data.getExtras() != null) {
                GameData gameData = (GameData) data.getExtras().get("gameData");
                if (gameData != null) {
                    if (!gameData.getScriptName().equals(scriptManager.getCurrentScript())) {
                        try {
                            scriptManager.call(gameData.getScriptName());
                            setVideoUrl();
                        } catch (ScriptErrorException e) {
                            e.printStackTrace();
                            handleScriptError(e);
                        }
                    }
                    scriptManager.setCurrentPosition(gameData.getCurrentIndex());
                    int prevIndex = Math.max(0, scriptManager.getCurrentPosition() - 1);
                    player.seekTo((long) (((NormalBlock) scriptManager.getBlock(prevIndex)).getTime() * 1000));
                    player.play();
                }
            }
        }
    }

    public String getCurrentFrameAsBase64() {
        Bitmap bitmap = getFrameFromVideo(videouri.getPath(), player.getCurrentPosition());
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 110, 62, true);
        bitmap.recycle();
        return convertBitmapToBase64(resizedBitmap);
    }

    public Bitmap getFrameFromVideo(String videoPath, long timeUs) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        Bitmap bitmap = null;
        try {
            retriever.setDataSource(videoPath);
            bitmap = retriever.getFrameAtTime(player.getCurrentPosition() * 1000, MediaMetadataRetriever.OPTION_CLOSEST);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return bitmap;
    }

    private static String convertBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
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
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 权限已被授予，可以执行相关操作
            } else {
                // 权限未被授予，提示用户
            }
        }
    }

    public void handleScriptError(ScriptErrorException e) {
        new AlertDialog.Builder(this)
                .setTitle("脚本错误")
                .setMessage(e.getMessage())
                .setPositiveButton("确定", (dialog, which) -> LauncherActivity.this.finish())
                .show();
    }

    public static GameDataManager getGameDataManager() {
        return gameDataManager;
    }
}
