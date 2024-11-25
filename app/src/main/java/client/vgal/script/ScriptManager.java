package client.vgal.script;


import android.renderscript.Script;
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

    public ScriptManager(File scriptRoot) {
        this(scriptRoot,"start.vgs");
    }
    public ScriptManager(File scriptRoot,String currentScript) {
        this.scriptRoot = scriptRoot;
        this.currentScript = currentScript;
        scriptParser = new DefaultScriptParser();
    }

    public List<ScriptBlock> parse(String scriptName) throws ScriptErrorException {
        String scriptContent = FileUtil.readFileUTF8(new File(scriptRoot,scriptName).getAbsolutePath());
        return scriptParser.parse(scriptContent);
    }

    public void callCurrent() throws ScriptErrorException {
        call(this.currentScript);
    }

    public void call(String scriptName) throws ScriptErrorException{
        List<ScriptBlock> list = parse(scriptName);
        blocks.clear();
        otherBlocks.clear();

        for (ScriptBlock block : list){
            if (block instanceof NormalBlock){
                blocks.add(block);
            }else {
                otherBlocks.add(block);
            }
        }

        currentScript = scriptName;
        currentIndex.set(0);
    }

    public ScriptBlock getCurrentBlock(){
        return blocks.get(currentIndex.get());
    }

    public ScriptBlock next(int index){
        int i = currentIndex.getAndAdd(index);
        ScriptBlock scriptBlock = getBlock(i);
        return scriptBlock;
    }

    public ScriptBlock prev(int index){
        int i = currentIndex.getAndAdd(index);
        ScriptBlock scriptBlock = getBlock(i);
        return scriptBlock;
    }

    public ScriptBlock next(){
        return next(1);
    }

    public ScriptBlock prev(){
        return prev(1);
    }

    public ScriptBlock getBlock(int index){
        if (index >= blocks.size()){
            return null;
        }
        return blocks.get(index);
    }

    public int getCurrentPosition(){
        return currentIndex.get();
    }

    public String getCurrentScript() {
        return currentScript;
    }

    public void resetIndex(long currentTime){

        // 遍历JSONArray
        for (int i = 0; i < blocks.size(); i++) {
            // 获取每个JSONObject
            ScriptBlock scriptBlock = blocks.get(i);
            if (scriptBlock instanceof NormalBlock){
                NormalBlock normalBlock = (NormalBlock) scriptBlock;
                long time = (long) (normalBlock.getTime() * 1000);
                if (time > currentTime){
                    currentIndex.set(Math.max(0,i));
                    return;
                }
            }

        }

        currentIndex.set(blocks.size() - 1);
    }

    public void setCurrentPosition(int index){
        currentIndex.set(index);
    }

    public PlayBlock getPlayVideo(){
        for (ScriptBlock block : otherBlocks) {
            if (block instanceof PlayBlock){
                return (PlayBlock) block;
            }
        }
        return null;
    }

    public CallBlock getNextCall(){
        for (ScriptBlock block : otherBlocks) {
            if (block instanceof CallBlock){
                return (CallBlock) block;
            }
        }
        return null;
    }

    public int getSize(){
        return blocks.size();
    }

    public List<ScriptBlock> rangeGet(int start,int end){
        if (start<0 || end >= getSize() - 1){
            return null;
        }

        ArrayList<ScriptBlock> list = new ArrayList<>();
        for (int i = start;i <= end;i++){
            list.add(getBlock(i));
        }
        return list;
    }

}
