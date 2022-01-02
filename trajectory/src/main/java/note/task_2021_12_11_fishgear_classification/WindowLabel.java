package note.task_2021_12_11_fishgear_classification;

public class WindowLabel {
    public final String id;
    private final String wholeId;
    private final int partId;
    public final String label;

    public WindowLabel(String id, String label) {
        this.id = id;
        this.label = label;

        String[] parts = id.split("-");
        wholeId = id.substring(0, id.lastIndexOf("-"));
        partId = Integer.parseInt(parts[2]);
    }

    public String getWholeId() {
        return wholeId;
    }

    public int getPartId() {
        return partId;
    }

    public boolean isFishing(){
        return label.equals("fishing");
    }

    @Override
    public String toString() {
        return "WindowLabel{" +
                "id='" + id + '\'' +
                ", wholeId='" + wholeId + '\'' +
                ", partId=" + partId +
                ", label='" + label + '\'' +
                '}';
    }
}
