package client.vgal;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import vgal.script.ScriptManager; // 确保正确导入

public class VgsLauncherActivity extends Activity {

    private ListView listView;
    private ArrayList<String> vgsFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 创建ListView程序性地
        listView = new ListView(this);
        setContentView(listView); // 设置ListView为活动的内容视图

        vgsFiles = new ArrayList<>();

        // 获取vgal目录
        File vgalDirectory = new File(Environment.getExternalStorageDirectory(), "vgal");
        if (vgalDirectory.exists() && vgalDirectory.isDirectory()) {
            File[] files = vgalDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".vgs")) {
                        vgsFiles.add(file.getName());
                    }
                }
            }
        }

        // 设置适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, vgsFiles);
        listView.setAdapter(adapter);

        // 设置点击事件
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedFile = vgsFiles.get(position);
            launchVgsFile(selectedFile);
        });
    }

    private void launchVgsFile(String fileName) {
        File scriptRoot = new File(Environment.getExternalStorageDirectory(), "vgal");
        ScriptManager scriptManager = new ScriptManager(scriptRoot, fileName);
        try {
            scriptManager.callCurrent();
            Toast.makeText(this, "Launched " + fileName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error launching " + fileName, Toast.LENGTH_SHORT).show();
        }
    }
}
