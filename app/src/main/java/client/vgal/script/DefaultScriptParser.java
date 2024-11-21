package client.vgal.script;


import client.vgal.script.block.CallBlock;
import client.vgal.script.block.NormalBlock;
import client.vgal.script.block.PlayBlock;
import client.vgal.script.block.ScriptBlock;

import java.util.ArrayList;
import java.util.List;

public class DefaultScriptParser implements ScriptParser{
    @Override
    public List<ScriptBlock> parse(String scriptContent) throws ScriptErrorException {
        List<ScriptBlock> blocks = new ArrayList<>();

        String[] lines = scriptContent.split("\n");
        ScriptBlock block = new NormalBlock();
        NormalBlock normalBlock = (NormalBlock) block;
        int i = 1;
        for (String line : lines){
            if (line.trim().equals("")){
                continue;
            }

            int index = line.indexOf(" ");
            if (index == -1){
                index = line.length();
            }
            String prefix = line.substring(0,index).trim();

            String other = line.substring(index).trim();
            switch (prefix){
                case "play":
                    block = new PlayBlock();
                    ((PlayBlock)block).setVideoName(other);
                    break;
                case "text":
                    normalBlock.setText(other);
                    break;
                case "time":
                    normalBlock.setTime(Double.valueOf(other));
                    break;
                case "call":
                    block = new CallBlock();
                    ((CallBlock) block).setScriptName(other);
                    break;
                case "proc":
                    blocks.add(block);
                    block = new NormalBlock();
                    normalBlock = (NormalBlock) block;
                    break;
                default:
                    throw new ScriptErrorException("第" + i +"行出现无效语法：" + line,i);
            }

            i++;
        }

        return blocks;
    }

}
