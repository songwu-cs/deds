package songwu.deds.trajectory.data;

import songwu.deds.trajectory.algo.UnionFind;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnchoragePoint {
    private int unionId;
    private double x;
    private double y;
    private String line;

    public AnchoragePoint(String line, int xIndex, int yIndex, int unionId){
        this.line = line;
        String[] parts = line.split(",");
        this.x = Double.parseDouble(parts[xIndex]);
        this.y = Double.parseDouble(parts[yIndex]);
        this.unionId = unionId;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getUnionId() {
        return unionId;
    }

    public String getLine() {
        return line;
    }

    public static void main(String[] args) throws IOException {
        String sourcePath = "C:\\Users\\TJUer\\Desktop\\GlobalFishingWatch\\anchorageDenmark.csv";
        String outputPath = "C:\\Users\\TJUer\\Desktop\\GlobalFishingWatch\\anchorageDenmark_UnionFind_4km.csv";
        try(PrintWriter writer = new PrintWriter(outputPath)){
            List<String> lines = Files.readAllLines(Paths.get(sourcePath));
            List<AnchoragePoint> points = new ArrayList<>();
            int counter = 0;
            writer.write(lines.get(0) + ",cluster\n");
            for(String line: lines.subList(1, lines.size())){
                points.add(new AnchoragePoint(line, 6, 7, counter++));
            }

            UnionFind unionFind = new UnionFind(points.size());
            for(AnchoragePoint p1 : points){
                for(AnchoragePoint p2 : points){
                    if(Math.hypot(p1.getX() - p2.getX(), p1.getY() - p2.getY()) < 4000){
                        unionFind.union(p1.getUnionId(), p2.getUnionId());
                    }
                }
            }

            int[] status = unionFind.status();
            for(AnchoragePoint point : points){
                writer.write(point.getLine() + "," + status[point.getUnionId()] + "\n");
            }
        }
    }
}
