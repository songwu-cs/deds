package note.task_2022_02_13_suez_canal;

import io.bigdata.BatchFileReader;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class Task {
    public static String baseDIR = "H:\\UpanSky\\DEDS-DataLake\\suez-canal\\SuezCanal\\";

    public static void toPairs() throws IOException {
        String path = baseDIR + "crossingAll.csv";
        String outputPath = baseDIR + "crossingAll_pairs.csv";

        try(PrintWriter writer = new PrintWriter(outputPath)){
            BatchFileReader fileReader = new BatchFileReader(path, ",", true, 0);
            writer.write("ship,fromGate,toGate,fromTime,toTime,rowid\n");
            int counter = 0;
            for(List<String> ls : fileReader){
                ls.removeIf(e -> e.endsWith(","));
                Comparator<String> comp = Comparator.comparing((String e) -> e.split(",")[2])
                                .thenComparing(Comparator.comparing((String e) -> e.split(",")[1]).reversed());
                ls.sort(comp);
                for(int i = 0; i < ls.size() - 1; i++){
                    String[] parts1 = ls.get(i).split(",");
                    String[] parts2 = ls.get(i+1).split(",");
                    writer.write(String.join(",", parts1[0], parts1[1], parts2[1], parts1[2], parts2[2], "" + (++counter)) + "\n");
                }
            }
        }
    }

    public static void directionInEachDay() throws IOException {
        for(int day : new int[]{15,16,17,18,19,20,21,22}){
            try(BatchFileReader reader = new BatchFileReader("H:\\UpanSky\\DEDS-DataLake\\suez-canal\\SuezCanal\\March-@.csv".replace("@", day+""), ",", true, 0);
                PrintWriter writer = new PrintWriter("H:\\UpanSky\\DEDS-DataLake\\suez-canal\\SuezCanal\\March-@-direction.csv".replace("@", day+""))){
                writer.write("ship,direction\n");
                for(List<String> ss : reader){
                    String id = ss.get(0).split(",")[0];
                    ss.sort(Comparator.naturalOrder());
                    double startLAT = Double.parseDouble(ss.get(0).split(",")[2]);
                    double endLAT = Double.parseDouble(ss.get(ss.size()-1).split(",")[2]);
                    writer.write(String.join(",", id, startLAT > endLAT ? "2South" : "2North") + "\n");
                }
            }
        }
    }

    public static void pivot16() throws IOException{
        try(BatchFileReader reader = new BatchFileReader("H:\\UpanSky\\DEDS-DataLake\\suez-canal\\SuezCanal\\March-16-convoy-time.csv", ",", true, 1);
            PrintWriter writer = new PrintWriter("H:\\UpanSky\\DEDS-DataLake\\suez-canal\\SuezCanal\\March-16-convoy-time2.csv")) {
            writer.write("ship,start time,port said,suez south\n");
            for(List<String> ss : reader){
                String ship = ss.get(0).split(",")[1];
                String t1 = ss.get(0).split(",")[2];
                String t2 = ss.get(1).split(",")[2];
                String t3 = ss.get(2).split(",")[2];
                writer.write(String.join(",", ship, t1, t2, t3) + "\n");
            }
        }
    }

    public static void pivot17to22() throws IOException {
        List<String> lines = Files.readAllLines(new File("H:\\UpanSky\\DEDS-DataLake\\suez-canal\\SuezCanal\\March-17to22-convoy-eventtime.csv").toPath());
        HashMap<String, HashMap<String, String>> mapHashMap = new HashMap<>();
        for(String s : lines.subList(1, lines.size())){
            String[] parts = s.split(",");
            if(! mapHashMap.containsKey(parts[0]))
                mapHashMap.put(parts[0], new HashMap());
            mapHashMap.get(parts[0]).put(parts[1], parts[2]);
        }

        try(PrintWriter writer = new PrintWriter("H:\\UpanSky\\DEDS-DataLake\\suez-canal\\SuezCanal\\March-17to22-convoy-time2.csv")){
            writer.write("ship,start time,port said,suez south,whichDay,role\n");
            lines = Files.readAllLines(new File("H:\\UpanSky\\DEDS-DataLake\\suez-canal\\SuezCanal\\March-17to22-convoy-time.csv").toPath());
            for(String line : lines.subList(1, lines.size())){
                String[] parts = line.split(",");
                HashMap<String, String> map = mapHashMap.get(parts[0]);
                if(map.size() < 3)
                    continue;
                parts[1] = map.get("start time");
                parts[2] = map.get("port said");
                parts[3] = map.get("suez south");
                writer.write(String.join(",", parts) + "\n");
            }
        }
    }

    public static void main(String[] args) throws IOException {

        List<String> ss = new ArrayList<>();
    }
}
