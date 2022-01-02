package note.task_2021_12_11_fishgear_classification;

public class WindowLabelAid {
    public final String whole;
    public final int start;
    public final int end;

    public WindowLabelAid(String whole, int start, int end) {
        this.whole = whole;
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return "WindowLabelAid{" +
                "whole='" + whole + '\'' +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
