package client.vgal.saveload;

import java.io.Serializable;
import java.time.LocalDateTime;

public class GameData implements Serializable {

    private int slot;
    private int currentIndex;
    private String scriptName;

    private String bmpPath;
    private LocalDateTime saveTime;

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public String getScriptName() {
        return scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    public LocalDateTime getSaveTime() {
        return saveTime;
    }

    public void setSaveTime(LocalDateTime saveTime) {
        this.saveTime = saveTime;
    }

    public String getBmpPath() {
        return bmpPath;
    }

    public void setBmpPath(String bmpPath) {
        this.bmpPath = bmpPath;
    }
}
