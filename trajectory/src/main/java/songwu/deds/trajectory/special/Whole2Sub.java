package songwu.deds.trajectory.special;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Whole2Sub {
    private String sourceFile;
    private String destFile;
    private int majorIndex;
    private int minorIndex;
    private String goodValue;
    private String id = "id";
    private boolean withHeader = true;
    private String splitter = ",";

    public Whole2Sub() {
    }

    public Whole2Sub setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile; return this;
    }

    public Whole2Sub setDestFile(String destFile) {
        this.destFile = destFile; return this;
    }

    public Whole2Sub setMajorIndex(int majorIndex) {
        this.majorIndex = majorIndex; return this;
    }

    public Whole2Sub setMinorIndex(int minorIndex) {
        this.minorIndex = minorIndex; return this;
    }

    public Whole2Sub setGoodValue(String goodValue) {
        this.goodValue = goodValue; return this;
    }

    public Whole2Sub setId(String id){
        this.id = id; return this;
    }

    public Whole2Sub setWithHeader(boolean withHeader){
        this.withHeader = withHeader; return this;
    }

    public Whole2Sub setSplitter(String splitter){
        this.splitter = splitter; return this;
    }

    //C:\Users\TJUer\Desktop\dk_csv_apr2021_coastline\ais_20210926_deanchorage_1km_subtrajectory.csv
    //C:\Users\TJUer\Desktop\dk_csv_apr2021_coastline\ais_20210926_deanchorage_1km.csv
    public void go() throws IOException{
        try(PrintWriter writer = new PrintWriter(destFile)){
            List<String> lines = Files.readAllLines(Paths.get(sourceFile));
            if(withHeader)
                writer.write(id + splitter + lines.get(0) + "\n");
            int index = withHeader ? 1 : 0;

            String currentObj = "-1";
            int currentSub = 0;
            boolean previousIsGood = true;
            for(String line : lines.subList(index, lines.size())){
                String[] parts = line.split(splitter);
                if(parts[majorIndex].equals(currentObj)){
                    if(parts[minorIndex].equals(goodValue)){
                        writer.write(parts[majorIndex] + "-" + currentSub + splitter + line.replace(goodValue,"_") + "\n");
                        previousIsGood = true;
                    }else {
                        if(previousIsGood)
                            currentSub++;
                        previousIsGood = false;
                    }
                }else {
                    currentObj = parts[majorIndex];
                    previousIsGood = parts[minorIndex].equals(goodValue);
                    currentSub = 0;
                    if(parts[minorIndex].equals(goodValue)){
                        writer.write(parts[majorIndex] + "-" + currentSub + splitter + line.replace(goodValue, "_") + "\n");
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Whole2Sub whole2Sub = new Whole2Sub()
                .setSourceFile("C:\\Users\\TJUer\\Desktop\\1.txt")
                .setDestFile("C:\\Users\\TJUer\\Desktop\\2.txt")
                .setMajorIndex(0)
                .setMinorIndex(1)
                .setGoodValue("good");
        whole2Sub.go();
    }
}
