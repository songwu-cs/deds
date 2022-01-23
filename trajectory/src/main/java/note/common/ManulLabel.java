package note.common;

public class ManulLabel {
    public final String mmsi;
    public final String startTime;
    public final String endTime;
    public final String label;

    int mmsi_index = 0;
    int start_index = 1;
    int end_index = 2;
    int label_index = 3;

    public ManulLabel(String line){
        String parts[] = line.split(",");
        mmsi = parts[mmsi_index];
        startTime = parts[start_index];
        endTime = parts[end_index];
        label = parts[label_index];
    }

    @Override
    public String toString() {
        return "ManulLabel{" +
                "mmsi='" + mmsi + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", label='" + label + '\'' +
                ", mmsi_index=" + mmsi_index +
                ", start_index=" + start_index +
                ", end_index=" + end_index +
                ", label_index=" + label_index +
                '}';
    }
}
