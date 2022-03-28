package note.task_2022_02_13_suez_canal;

import io.bigdata.BatchFileReader;

import java.io.*;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.*;

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

    public static void numberOfWaitingShips() throws IOException {
        try(BatchFileReader reader = new BatchFileReader("H:\\UpanSky\\DEDS-DataLake\\2022-03-14-suez-weather\\March-17to22-convoy-time-unixStamp.csv", ",", true, 1);
            PrintWriter writer = new PrintWriter("H:\\UpanSky\\DEDS-DataLake\\2022-03-14-suez-weather\\March-17to22-convoy-time-unixStamp2.csv")) {
            writer.write("Port Said,Role,Ship,Start Time,Suez South,Which Day,stampSAID,stampSOUTH,stampSTART,timeOfDayEgypt,timeOfDayUTC,total time,transit time,waiting time,anchoraged\n");
            List<String> northConvoy = reader.readBatch();
            List<String> southConvoy = reader.readBatch();
            for(int i = 0; i < northConvoy.size(); i++){
                int counter = 0;
                int _start = Integer.parseInt(northConvoy.get(i).split(",")[8]);
                for(int j = 0; j < i; j++){
                    int __end = Integer.parseInt(northConvoy.get(j).split(",")[6]);
                    if(__end > _start)
                        counter++;
                }
                writer.write(northConvoy.get(i) + "," + counter + "\n");
            }
            for(int i = 0; i < southConvoy.size(); i++){
                int counter = 0;
                int _start = Integer.parseInt(southConvoy.get(i).split(",")[8]);
                for(int j = 0; j < i; j++){
                    int __end = Integer.parseInt(southConvoy.get(j).split(",")[7]);
                    if(__end > _start)
                        counter++;
                }
                writer.write(southConvoy.get(i) + "," + counter + "\n");
            }
        }
    }

    public static void global2021() throws IOException {
        File f = new File("H:\\UpanSky\\DEDS-DataLake\\2021");
        double latMin = 29.754839972510933, latMax = 31.56449510799119;
        double lonMin =  31.849365234374996, lonMax = 33.123779296875;
        int counter = 0;
        try(PrintWriter writer = new PrintWriter("H:\\UpanSky\\DEDS-DataLake\\2022-03-14-suez-weather\\NoaaGlobal2021.csv")){
            for(File ff : f.listFiles()){
                List<String> ss = Files.readAllLines(ff.toPath());
                double lat = Double.parseDouble(ss.get(1).replace("\"", "").split(",")[3]);
                double lon = Double.parseDouble(ss.get(1).replace("\"", "").split(",")[4]);
                if(lat > latMin && lat < latMax && lon > lonMin && lon < lonMax){
                    writer.write(String.join("\n", ss));
                    writer.write("\n");
                }
                System.out.println(++counter);
            }
        }
    }

    public static void main(String[] args) throws IOException, ParseException {
//        numberOfWaitingS hips();
        global2021();
    }

}
