package note.common;

public class WindowLabel {
    private final String wholeId;
    private final int partId;
    public final String id;
    public final String label;

    public WindowLabel(String id, String label) {
        this.id = id;
        this.label = label;
        int key = id.lastIndexOf("-");
        wholeId = id.substring(0, key);
        partId = Integer.parseInt(id.substring(key + 1, id.length()));
    }

    public String getWholeId() {
        return wholeId;
    }

    public int getPartId() {
        return partId;
    }

    public String getId(){return id;}

    public boolean isFishing(){
        return label.equals("fishing");
    }

    @Override
    public String toString() {
        return "WindowLabel{" +
                "wholeId='" + wholeId + '\'' +
                ", partId=" + partId +
                ", label='" + label + '\'' +
                '}';
    }
}
