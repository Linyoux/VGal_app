package client.vgal.script;

import androidx.annotation.Nullable;

public class ScriptErrorException extends Exception{

    private final int line;

    public ScriptErrorException(String message, int line) {
        super(message);
        this.line = line;
    }

    public int getLine() {
        return line;
    }
}
