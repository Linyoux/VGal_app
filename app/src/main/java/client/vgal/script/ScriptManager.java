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

    // Default constructor that initializes with "start.vgs"
    public ScriptManager(File scriptRoot) {
        this(scriptRoot, "start.vgs");
    }

    // Constructor that allows the specification of a script name
    public ScriptManager(File scriptRoot, String currentScript) {
        this.scriptRoot = scriptRoot;
        this.currentScript = currentScript;
        scriptParser = new DefaultScriptParser();
        
        // Automatically call current script when initializing
        try {
            call(currentScript); // Load the script when the manager is created
        } catch (ScriptErrorException e) {
            e.printStackTrace(); // Handle exceptions appropriately
        }
    }

    // Parse the given script and return a list of script blocks
    public List<ScriptBlock> parse(String scriptName) throws ScriptErrorException {
        String scriptContent = FileUtil.readFileUTF8(new File(scriptRoot, scriptName).getAbsolutePath());
        return scriptParser.parse(scriptContent);
    }

    // Call the current script to load its blocks
    public void callCurrent() throws ScriptErrorException {
        call(this.currentScript);
    }

    // Call a specified script by name
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

    // Get the current block of the script
    public ScriptBlock getCurrentBlock() {
        return blocks.get(currentIndex.get());
    }

    // Move to the next block by index
    public ScriptBlock next(int index) {
        int i = currentIndex.getAndAdd(index);
        return getBlock(i);
    }

    // Move to the previous block by index
    public ScriptBlock prev(int index) {
        int i = currentIndex.getAndAdd(-index); // decrementing
        return getBlock(i);
    }

    // Move to the next block
    public ScriptBlock next() {
        return next(1);
    }

    // Move to the previous block
    public ScriptBlock prev() {
        return prev(1);
    }

    // Get a block at a specific index
    public ScriptBlock getBlock(int index) {
        if (index >= blocks.size() || index < 0) {
            return null; // Make sure index is valid
        }
        return blocks.get(index);
    }

    // Get the current position in the script
    public int getCurrentPosition() {
        return currentIndex.get();
    }

    // Get the name of the current script
    public String getCurrentScript() {
        return currentScript;
    }

    // Reset the current index based on the current time
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

    // Set the current position manually
    public void setCurrentPosition(int index) {
        currentIndex.set(index);
    }

    // Get the PlayBlock from the script
    public PlayBlock getPlayVideo() {
        for (ScriptBlock block : otherBlocks) {
            if (block instanceof PlayBlock) {
                return (PlayBlock) block;
            }
        }
        return null;
    }

    // Get the next CallBlock from the script
    public CallBlock getNextCall() {
        for (ScriptBlock block : otherBlocks) {
            if (block instanceof CallBlock) {
                return (CallBlock) block;
            }
        }
        return null;
    }
}
