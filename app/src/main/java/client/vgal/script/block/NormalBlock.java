package client.vgal.script.block;

public class NormalBlock implements ScriptBlock {

    private String bgmName;
    private String bgmPlayType;

    private Double time;
    private String text;
    private String extraText;

    public String getBgmName() {
        return bgmName;
    }

    public void setBgmName(String bgmName) {
        this.bgmName = bgmName;
    }

    public String getBgmPlayType() {
        return bgmPlayType;
    }

    public void setBgmPlayType(String bgmPlayType) {
        this.bgmPlayType = bgmPlayType;
    }

    public Double getTime() {
        return time;
    }

    public void setTime(Double time) {
        this.time = time;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getExtraText() {
        return extraText;
    }

    public void setExtraText(String extraText) {
        this.extraText = extraText;
    }
}
