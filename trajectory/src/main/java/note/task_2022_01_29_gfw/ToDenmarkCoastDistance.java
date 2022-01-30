package note.task_2022_01_29_gfw;

import db.pg.DenmarkCoast;

import java.io.PrintWriter;
import java.util.List;

public class ToDenmarkCoastDistance {
    public static final String baseDir = "H:\\UpanSky\\DEDS-DataLake\\2022-01-25-gfw\\";

    List<String> lines;
    int step;
    int offset;
    int idIndex;
    int tIndex;
    int xIndex;
    int yIndex;

    public ToDenmarkCoastDistance(List<String> lines, int step, int offset, int idIndex, int tIndex, int xIndex, int yIndex) {
        this.lines = lines;
        this.step = step;
        this.offset = offset;
        this.idIndex = idIndex;
        this.tIndex = tIndex;
        this.xIndex = xIndex;
        this.yIndex = yIndex;
    }

    public void go(){
        try(PrintWriter writer = new PrintWriter(baseDir + "toDenmarkCoast_" + step + "_" + offset + ".csv");
            DenmarkCoast denmarkCoast = new DenmarkCoast("172.29.129.234",
                    "bmda22",
                    "postgres",
                    "wusong",
                    25832,
                    100,
                    "denmark_administrative_national_boundary",
                    "geom25832");){
            writer.write("id,t,distance\n");

            for (int i = 0; i < Integer.MAX_VALUE; i+=step){
                if (i + offset >= lines.size())
                    return;
                String[] parts = lines.get(i+offset).split(",");
                String id = parts[idIndex];
                String t = parts[tIndex];
                double x = Double.parseDouble(parts[xIndex]);
                double y = Double.parseDouble(parts[yIndex]);
                double distance = denmarkCoast.getDistanceToShore(x, y);
                if(i % 1000 == 0)
                    System.out.println(i);
                writer.write(String.join(",", id, t, distance + "") + "\n");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
