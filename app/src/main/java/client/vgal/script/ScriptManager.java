package client.vgal.script;

import client.vgal.script.block.CallBlock;
import client.vgal.script.block.NormalBlock;
import client.vgal.script.block.PlayBlock;
import client.vgal.script.block.ScriptBlock;
import client.vgal.utils.FileUtil;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ScriptManager {

    private AtomicInteger currentIndex = new AtomicInteger();
    private String currentScript;
    private List<ScriptBlock> blocks = new ArrayList<>();
    private List<ScriptBlock> otherBlocks = new ArrayList<>();
    private File scriptRoot;

    private ScriptParser scriptParser;

    // 构造函数初始化，但不自动执行脚本
    public ScriptManager(File scriptRoot) {
        this(scriptRoot, "start.vgs");
    }

    public ScriptManager(File scriptRoot, String currentScript) {
        this.scriptRoot = scriptRoot;
        this.currentScript = currentScript;
        this.scriptParser = new DefaultScriptParser();
    }

    // 解析并执行指定的脚本
    public void loadAndExecuteScript(String scriptName) throws ScriptErrorException {
        call(scriptName);
    }

    // 解析给定脚本并返回脚本块列表
    public List<ScriptBlock> parse(String scriptName) throws ScriptErrorException {
        String scriptContent = FileUtil.readFileUTF8(new File(scriptRoot, scriptName).getAbsolutePath());
        return scriptParser.parse(scriptContent);
    }

    // 加载当前脚本的块
    public void callCurrent() throws ScriptErrorException {
        call(this.currentScript);
    }

    // 加载指定名称的脚本
    public void call(String scriptName) throws ScriptErrorException {
        List<ScriptBlock> list = parse(scriptName);
        blocks.clear();
        otherBlocks.clear();

        for (ScriptBlock block : list) {
            if (block instanceof NormalBlock) {
                blocks.add(block);
            } else {
                otherBlocks.add(block);
            }
        }

        currentScript = scriptName;
        currentIndex.set(0);
    }

    // 获取当前脚本块
    public ScriptBlock getCurrentBlock() {
        return blocks.get(currentIndex.get());
    }

    // 移动到下一个块（通过索引）
    public ScriptBlock next(int index) {
        int i = currentIndex.getAndAdd(index);
        return getBlock(i);
    }

    // 移动到前一个块（通过索引）
    public ScriptBlock prev(int index) {
        int i = currentIndex.getAndAdd(-index); // 减少索引
        return getBlock(i);
    }

    // 移动到下一个块
    public ScriptBlock next() {
        return next(1);
    }

    // 移动到前一个块
    public ScriptBlock prev() {
        return prev(1);
    }

    // 获取指定索引处的块
    public ScriptBlock getBlock(int index) {
        if (index >= blocks.size() || index < 0) {
            return null; // 确保索引有效
        }
        return blocks.get(index);
    }

    // 获取当前脚本位置
    public int getCurrentPosition() {
        return currentIndex.get();
    }

    // 获取当前脚本名称
    public String getCurrentScript() {
        return currentScript;
    }

    // 根据当前时间重置索引
    public void resetIndex(long currentTime) {
        for (int i = 0; i < blocks.size(); i++) {
            ScriptBlock scriptBlock = blocks.get(i);
            if (scriptBlock instanceof NormalBlock) {
                NormalBlock normalBlock = (NormalBlock) scriptBlock;
                long time = (long) (normalBlock.getTime() * 1000);
                if (time > currentTime) {
                    currentIndex.set(Math.max(0, i));
                    return;
                }
            }
        }
        currentIndex.set(blocks.size() - 1);
    }

    // 手动设置当前位置
    public void setCurrentPosition(int index) {
        currentIndex.set(index);
    }

    // 获取脚本中的 PlayBlock
    public PlayBlock getPlayVideo() {
        for (ScriptBlock block : otherBlocks) {
            if (block instanceof PlayBlock) {
                return (PlayBlock) block;
            }
        }
        return null;
    }

    // 获取脚本中的下一个 CallBlock
    public CallBlock getNextCall() {
        for (ScriptBlock block : otherBlocks) {
            if (block instanceof CallBlock) {
                return (CallBlock) block;
            }
        }
        return null;
    }
}
