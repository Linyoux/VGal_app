package client.vgal;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import client.vgal.script.ScriptManager;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class VgsLauncherActivity extends AppCompatActivity {

    private ListView vgsListView;
    private List<String> vgsFiles;
    private File scriptRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher); // 使用新的布局文件名

        vgsListView = findViewById(R.id.vgsListView);
        vgsFiles = new ArrayList<>();
        scriptRoot = new File(getExternalFilesDir(null), "vgal"); // 获取vgal目录

        // 读取所有vgs文件
        loadVgsFiles();

        // 设置ListView的适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, vgsFiles);
        vgsListView.setAdapter(adapter);

        // 设置点击事件
        vgsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedVgs = vgsFiles.get(position);
                launchVgsScript(selectedVgs);
            }
        });
    }

    private void loadVgsFiles() {
        // 列出所有以.vgs结尾的文件
        File[] files = scriptRoot.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".vgs");
            }
        });

        if (files != null) {
            for (File file : files) {
                vgsFiles.add(file.getName());
            }
        }
    }

    private void launchVgsScript(String vgsFileName) {
        Intent intent = new Intent(this, NativeVideoActivity.class);
        intent.putExtra("vgs_file_name", vgsFileName); // 传递文件名
        startActivity(intent);
    }
}
