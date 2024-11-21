package client.vgal.script;

import client.vgal.script.block.ScriptBlock;

import java.util.List;

public interface ScriptParser {

    List<ScriptBlock> parse(String scriptContent) throws ScriptErrorException;

}
