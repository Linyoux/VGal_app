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

public class NativeVideoActivity extends AppCompatActivity {

    PlayerView playerView;
    private Handler handler = new Handler();
    private Handler statushandler = new Handler();
    private Runnable runnable;
    private static final int REQUEST_CODE = 1024;
    private ExoPlayer player;
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
    Uri videouri;
    static GameDataManager gameDataManager;
    private ScriptManager scriptManager;
    File root;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nativevideo);

        // Get the VGS file name from the intent
        String vgsFileName = getIntent().getStringExtra("vgs_file_name");
        if (vgsFileName != null) {
            root = new File(Environment.getExternalStorageDirectory(), "vgal/");
            gameDataManager = new GameDataManager(getApplicationContext().getFilesDir());
            scriptManager = new ScriptManager(root, vgsFileName);
            try {
                scriptManager.callCurrent();
            } catch (ScriptErrorException e) {
                e.printStackTrace();
                handleScriptError(e);
            }
            readScripts();
        } else {
            Toast.makeText(this, "No VGS file name provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Remaining initialization code...
        requestPermission();

        // Set full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Hide ActionBar
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
                Player.Listener.super.onPlaybackStateChanged(playbackState);
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
        player.play(); // Automatically start playing

        // Create a Runnable to check video playback position
        runnable = new Runnable() {
            @Override
            public void run() {
                try {
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
                } finally {
                    handler.postDelayed(this, 100);
                }
            }
        };
        handler.postDelayed(runnable, 100);

        AtomicLong lastClick = new AtomicLong(System.currentTimeMillis());
        View.OnTouchListener listener = (v, event) -> {
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
                    if (player.isPlaying()) {
                        player.play();
                        gamestatusText.setVisibility(View.INVISIBLE);
                    } else {
                        NormalBlock obj = (NormalBlock) scriptManager.next();
                        if (obj == null) {
                            return true;
                        }
                        int time = obj.getTime().intValue() * 1000 + 10;
                        player.seekTo(time);
                    }
                }
            }
            return true;
        };
        // Set touch listener
        playerView.setOnTouchListener(listener);

        initRightButtons();
    }

    private void setVideoUrl() {
        PlayBlock playBlock = scriptManager.getPlayVideo();
        // Set video URL
        File videoFile = new File(root, playBlock.getVideoName());
        videouri = Uri.fromFile(videoFile);
        MediaItem mediaItem = MediaItem.fromUri(videouri); // Replace with your video URL
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
        @SuppressLint("NonConstantResourceId") View.OnClickListener listener = (event) -> {
            switch (event.getId()) {
                case R.id.autoMode:
                    autoMode = true;
                    statushandler.post(() -> {
                        gamestatusText.setText("Auto Mode");
                        gamestatusText.setVisibility(View.VISIBLE);
                    });
                    player.play();
                    break;
                case R.id.skipMode:
                    player.play();
                    statushandler.post(() -> {
                        gamestatusText.setText("Skip Mode");
                        PlaybackParameters params = new PlaybackParameters(1000, 1);
                        player.setPlaybackParameters(params);
                        gamestatusText.setVisibility(View.VISIBLE);
                    });
                    skipMode = true;
                    break;
                case R.id.backPrev:
                    if (scriptManager.getCurrentPosition() < 2) {
                        ToastUtils.showToast(getApplicationContext(), "Cannot go back");
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
                            .setTitle("Confirm Exit")
                            .setMessage("Are you sure you want to exit?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Destroy the current layout, for example, end the current activity
                                    NativeVideoActivity.this.finish();
                                }
                            })
                            .setNegativeButton("Cancel", null)
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
                    // Start new activity
                    startActivityForResult(intent, 1);
                    break;
                default:
                    ToastUtils.showToast(getApplicationContext(), "In development");
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

    public void readScripts() {
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
        }
        return false;
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // First check if permission is granted
            if (Environment.isExternalStorageManager()) {
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + this.getApplicationContext().getPackageName()));
                startActivityForResult(intent, REQUEST_CODE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // First check if permission is granted
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            } else {
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
            } else {
                Toast.makeText(this, "Permissions not granted!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getCurrentFrameAsBase64() {
        // Implement logic to get the current frame as Base64 string
        return "";
    }

    private void handleScriptError(ScriptErrorException e) {
        // Implement error handling logic
    }

    private void resetIndex() {
        // Implement logic to reset the index if needed
    }
}
