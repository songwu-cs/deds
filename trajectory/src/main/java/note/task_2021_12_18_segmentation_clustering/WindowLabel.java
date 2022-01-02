package note.task_2021_12_18_segmentation_clustering;

public class WindowLabel {
    private final String wholeId;
    private final int partId;
    public final String label;

    public WindowLabel(String id, String label) {
        this.label = label;
        String[] parts = id.split("-");
        wholeId = parts[0];
        partId = Integer.parseInt(parts[1]);
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
                "wholeId='" + wholeId + '\'' +
                ", partId=" + partId +
                ", label='" + label + '\'' +
                '}';
    }
}
