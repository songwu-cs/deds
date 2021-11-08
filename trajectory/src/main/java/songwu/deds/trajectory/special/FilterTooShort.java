package songwu.deds.trajectory.special;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class FilterTooShort {
    private String sourceFile;
    private String destFile;
    private int idIndex;
    private int threshold;
    private boolean withHeader = true;
    private String splitter = ",";

    public FilterTooShort() {
    }

    public FilterTooShort setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile; return this;
    }

    public FilterTooShort setDestFile(String destFile) {
        this.destFile = destFile; return this;
    }

    public FilterTooShort setIdIndex(int idIndex) {
        this.idIndex = idIndex; return this;
    }

    public FilterTooShort setThreshold(int threshold) {
        this.threshold = threshold; return this;
    }

    public FilterTooShort setWithHeader(boolean withHeader){
        this.withHeader = withHeader; return this;
    }

    public FilterTooShort setSplitter(String splitter){
        this.splitter = splitter; return this;
    }

    public void go() throws IOException{
        try(PrintWriter writer = new PrintWriter(destFile)){
            List<String> lines = Files.readAllLines(Paths.get(sourceFile));
            if(withHeader)
                writer.write(lines.get(0) + "\n");
            int index = withHeader ? 1 : 0;

            String currentObj = "-1";
            int start = 0, end = 0;
            for(; index < lines.size(); index++){
                String line = lines.get(index);
                String[] parts = line.split(splitter);
                if(currentObj.equals(parts[idIndex])){
                    end++;
                }else {
                    currentObj = parts[idIndex];
                    if(end - start + 1 >= threshold){
                        for(String _ : lines.subList(start, end+1))
                            writer.write(_ + "\n");
                    }
                    start = end = index;
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        FilterTooShort filterTooShort = new FilterTooShort()
                .setSourceFile("C:\\Users\\TJUer\\Desktop\\dk_csv_apr2021_coastline\\ais_20210926_deanchorage_10km_subtrajectory.csv")
                .setDestFile("C:\\Users\\TJUer\\Desktop\\dk_csv_apr2021_coastline\\ais_20210926_deanchorage_10km_subtrajectory_ge1000.csv")
                .setIdIndex(0)
                .setThreshold(1000);
        filterTooShort.go();
    }
}
