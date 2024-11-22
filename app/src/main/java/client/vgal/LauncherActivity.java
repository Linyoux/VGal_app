import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable; // 导入 BitmapDrawable
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import client.vgal.script.ScriptManager;
import client.vgal.script.ScriptErrorException;

public class LauncherActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 100;
    private ListView lvVgsFiles;
    private Button btnStart;
    private List<String> vgsFiles;
    private String selectedFile;
    private File scriptRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 动态创建布局
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        // 动态创建 ListView 和 Button
        lvVgsFiles = new ListView(this);
        btnStart = new Button(this);
        btnStart.setText("Start Selected File");
        btnStart.setEnabled(false);  // 初始状态为不可点击

        // 设置按钮的虚拟图标
        Bitmap icon = createCircularIcon(100);
        btnStart.setBackground(new BitmapDrawable(getResources(), icon));

        // 将组件添加到布局
        layout.addView(lvVgsFiles);
        layout.addView(btnStart);
        setContentView(layout);

        scriptRoot = new File(Environment.getExternalStorageDirectory(), "vgal");
        vgsFiles = new ArrayList<>();

        // 请求权限
        requestPermission();

        // 加载 VGS 文件
        loadVgsFiles();

        lvVgsFiles.setOnItemClickListener((parent, view, position, id) -> {
            selectedFile = vgsFiles.get(position);
            btnStart.setEnabled(true);
        });

        btnStart.setOnClickListener(v -> {
            if (selectedFile != null) {
                startVgsFile(selectedFile);
            } else {
                Toast.makeText(this, "请先选择一个文件！", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap createCircularIcon(int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(0xFF6200EE); // 紫色

        canvas.drawOval(new RectF(0, 0, size, size), paint);

        return bitmap;
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                Log.d("LauncherActivity", "MANAGE_EXTERNAL_STORAGE permission granted");
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + this.getApplicationContext().getPackageName()));
                startActivityForResult(intent, REQUEST_CODE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.d("LauncherActivity", "READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE permissions granted");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
            }
        } else {
            Log.d("LauncherActivity", "No need to request permissions");
        }
    }

    private void loadVgsFiles() {
        if (!scriptRoot.exists()) {
            Toast.makeText(this, "脚本目录不存在！", Toast.LENGTH_SHORT).show();
            return;
        }

        File[] files = scriptRoot.listFiles((dir, name) -> name.endsWith(".vgs"));
        if (files != null) {
            for (File file : files) {
                vgsFiles.add(file.getName());
            }
        }

        if (vgsFiles.isEmpty()) {
            Toast.makeText(this, "没有找到 VGS 文件！", Toast.LENGTH_SHORT).show();
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, vgsFiles);
            lvVgsFiles.setAdapter(adapter);
        }
    }

    private void startVgsFile(String fileName) {
        try {
            ScriptManager scriptManager = new ScriptManager(scriptRoot);
            scriptManager.loadAndExecuteScript(fileName);
            Toast.makeText(this, "启动脚本: " + fileName, Toast.LENGTH_SHORT).show();
        } catch (ScriptErrorException e) {
            Toast.makeText(this, "运行脚本时出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            requestPermission();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("LauncherActivity", "Permissions granted");
            } else {
                Log.d("LauncherActivity", "Permissions denied");
            }
        }
    }
}
