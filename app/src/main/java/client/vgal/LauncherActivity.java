package client.vgal;

import client.vgal.script.ScriptManager;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LauncherActivity extends Activity {

    private ListView lvVgsFiles;
    private Button btnStart;
    private ProgressBar progressLoader;

    private File scriptRoot;
    private List<String> vgsFiles;
    private String selectedFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        // 初始化视图
        lvVgsFiles = findViewById(R.id.lv_vgs_files);
        btnStart = findViewById(R.id.btn_start);
        progressLoader = findViewById(R.id.progress_loader);

        scriptRoot = new File(Environment.getExternalStorageDirectory(), "vgal");
        vgsFiles = new ArrayList<>();

        // 加载文件
        loadVgsFiles();

        // 文件列表点击事件
        lvVgsFiles.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            selectedFile = vgsFiles.get(position);
            btnStart.setEnabled(true);
        });

        // 启动按钮点击事件
        btnStart.setOnClickListener(v -> {
            if (selectedFile != null) {
                startVgsFile(selectedFile);
            } else {
                Toast.makeText(this, "Please select a file first!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 加载 .vgs 文件
    private void loadVgsFiles() {
        progressLoader.setVisibility(View.VISIBLE);
        new Thread(() -> {
            File[] files = scriptRoot.listFiles((dir, name) -> name.endsWith(".vgs"));
            if (files != null) {
                for (File file : files) {
                    vgsFiles.add(file.getName());
                }
            }

            // 更新UI
            runOnUiThread(() -> {
                progressLoader.setVisibility(View.GONE);
                if (vgsFiles.isEmpty()) {
                    Toast.makeText(this, "No VGS files found!", Toast.LENGTH_SHORT).show();
                } else {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, vgsFiles);
                    lvVgsFiles.setAdapter(adapter);
                }
            });
        }).start();
    }

    // 启动选中的 .vgs 文件
    private void startVgsFile(String fileName) {
        File vgsFile = new File(scriptRoot, fileName);
        if (vgsFile.exists()) {
            // 替代 ScriptManager 的启动逻辑
            try {
                ScriptManager scriptManager = new ScriptManager(scriptRoot, fileName);
                scriptManager.callCurrent();
                Toast.makeText(this, "Started: " + fileName, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Error starting file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "File not found!", Toast.LENGTH_SHORT).show();
        }
    }
}
