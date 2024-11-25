package client.vgal.game;

import java.util.List;

public class BackLog {

    private int page;
    private int maxPage;
    private List<LogText> texts;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getMaxPage() {
        return maxPage;
    }

    public void setMaxPage(int maxPage) {
        this.maxPage = maxPage;
    }

    public List<LogText> getTexts() {
        return texts;
    }

    public void setTexts(List<LogText> texts) {
        this.texts = texts;
    }
}
