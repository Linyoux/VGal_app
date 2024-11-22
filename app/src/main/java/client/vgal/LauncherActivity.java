package client.vgal;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import client.vgal.script.ScriptManager;
import client.vgal.script.ScriptErrorException;

public class LauncherActivity extends Activity {

    private ListView lvVgsFiles;
    private Button btnStart;
    private List<String> vgsFiles;
    private String selectedFile;
    private File scriptRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        lvVgsFiles = findViewById(R.id.lv_vgs_files);
        btnStart = findViewById(R.id.btn_start);
        btnStart.setEnabled(false);  // 初始状态为不可点击

        scriptRoot = new File(Environment.getExternalStorageDirectory(), "vgal");
        vgsFiles = new ArrayList<>();

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
}
