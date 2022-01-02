package note.task_2021_12_18_segmentation_clustering;

public class WindowLabelHelp {
    public final String whole;
    public final String label;
    public final int start;
    public final int end;
    public final int size;

    public WindowLabelHelp(String whole, String label, int start, int end) {
        this.whole = whole;
        this.label = label;
        this.start = start;
        this.end = end;
        size = end - start + 1;
    }

    public int magicNumber(){
        return size * (label.equals("fishing") ? 1 : -1);
    }

    @Override
    public String toString() {
        return "WindowLabelHelp{" +
                "whole='" + whole + '\'' +
                ", label='" + label + '\'' +
                ", start=" + start +
                ", end=" + end +
                ", size=" + size +
                '}';
    }
}
