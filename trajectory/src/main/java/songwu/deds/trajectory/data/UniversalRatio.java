package songwu.deds.trajectory.data;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.PriorityQueue;

public class UniversalRatio {
    private String id;
    private int previous;
    private int now;
    private double ratio;

    public UniversalRatio id(String id){
        this.id = id;
        return this;
    }

    public UniversalRatio previous(int previous){
        this.previous = previous;
        return this;
    }

    public UniversalRatio now(int now){
        this.now = now;
        return this;
    }

    public UniversalRatio ratio(){
        this.ratio = 1.0 * now / previous;
        return this;
    }

    public double getRatio() {
        return ratio;
    }

    public int getPrevious() {
        return previous;
    }

    public int getNow() {
        return now;
    }

    public String getId() {
        return id;
    }

    public static void saveRatio(String outputPath, PriorityQueue<UniversalRatio> queue) throws FileNotFoundException {
        try(PrintWriter writerRatio = new PrintWriter(outputPath)){
            writerRatio.write("id,previous,now,ratio\n");
            while (queue.size() > 0){
                UniversalRatio top = queue.poll();
                writerRatio.write(top.getId() + "," + top.getPrevious() + "," + top.getNow() + "," + top.getRatio() + "\n");
            }
        }
    }
}
