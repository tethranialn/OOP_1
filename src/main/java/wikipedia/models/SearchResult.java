package wikipedia.models;

public class SearchResult {
    private String title;
    private String snippet;
    private int pageid;

    public String getTitle() {
        if (title == null) {
            return "";
        }
        return title;
    }

    public String getSnippet() {
        if (snippet == null) {
            return "";
        }
        return snippet;
    }

    public int getPageid() {
        return pageid;
    }
}