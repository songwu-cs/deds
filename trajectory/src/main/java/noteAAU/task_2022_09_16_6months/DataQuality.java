package noteAAU.task_2022_09_16_6months;


import calculation.ListGeneric;
import calculation.ListString;
import datetime.TwoTimestamp;
import io.bigdata.BatchFileReader;
import org.hsqldb.lib.StringUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

public class DataQuality {
    public static String workDIR = "H:\\UpanSky\\DEDS-DataLake\\suez-canal\\six-months\\";

    //构建数据仓库，保存航行状态和船只类型列
    public static void step1() throws IOException {
        try(PrintWriter writer = new PrintWriter(workDIR + "rn-806-pos-shiptype-status.csv")){
            List<String> keys = Files.readAllLines(new File(workDIR + "rn-806-keys.csv").toPath());
            List<String> status = Files.readAllLines(new File(workDIR + "status-lookup.csv").toPath());
            List<String> POS = Files.readAllLines(new File(workDIR + "rn-806-pos.csv").toPath());

            Map<String, String> type = new HashMap<>();
            for(String s : keys.subList(1, keys.size())){
                String[] parts = s.split(",");
                type.put(parts[0], parts[1]);
            }

            Map<String, String> statusLOOKUP = new HashMap<>();
            for(String s : status.subList(1, status.size())){
                String[] parts = s.split(",");
                statusLOOKUP.put(parts[0], parts[1]);
            }

            writer.write(POS.get(0));
            writer.write(",shiptype,statusINFO\n");
            for(String s : POS.subList(1, POS.size())){
                writer.write(s);
                writer.write(",");
                String shipType = type.get(s.split(",")[0]);
                writer.write(shipType == null ? "Unknown" : shipType);
                writer.write(",");
                writer.write(statusLOOKUP.get(s.split(",")[7]));
                writer.write("\n");
            }
        }
    }

    //将相交事件转化为convoy三元组
    public static void step2() throws IOException{
        try(PrintWriter writerNorth = new PrintWriter(workDIR + "crossingNorthConvoyTriple.csv");
            PrintWriter writerSouth = new PrintWriter(workDIR + "crossingSouthConvoyTriple.csv");
            BatchFileReader reader = new BatchFileReader(workDIR + "crossingFlag4.csv", ",", true, 0)) {
            List<String[]> northConvoy = new ArrayList<>();
            List<String[]> southConvoy = new ArrayList<>();
            for(List<String> ls : reader){
                String shipID = ls.get(0).split(",")[0];
                for(int i = 0; i < ls.size() - 3; i++){
                    if(ls.get(i).contains("NorthArrival") &&
                            ls.get(i+1).contains("PortSaid") &&
                            ls.get(i+2).contains("SuezSouth") &&
                            ls.get(i).contains("Down") &&
                            ls.get(i+1).contains("Down") &&
                            ls.get(i+2).contains("Down")){
                        String t1 = ls.get(i).split(",")[3];
                        String t2 = ls.get(i+1).split(",")[3];
                        String t3 = ls.get(i+2).split(",")[3];
                        String unix1 = ls.get(i).split(",")[6];
                        String unix2 = ls.get(i+1).split(",")[6];
                        String unix3 = ls.get(i+2).split(",")[6];
                        northConvoy.add(new String[]{shipID, t1, t2, t3, unix1, unix2, unix3});
                    } else if (ls.get(i).contains("SouthArrival") &&
                            ls.get(i+1).contains("SuezSouth") &&
                            ls.get(i+2).contains("PortSaid") &&
                            ls.get(i).contains("Up") &&
                            ls.get(i+1).contains("Up") &&
                            ls.get(i+2).contains("Up")
                    ) {
                        String t1 = ls.get(i).split(",")[3];
                        String t2 = ls.get(i+1).split(",")[3];
                        String t3 = ls.get(i+2).split(",")[3];
                        String unix1 = ls.get(i).split(",")[6];
                        String unix2 = ls.get(i+1).split(",")[6];
                        String unix3 = ls.get(i+2).split(",")[6];
                        southConvoy.add(new String[]{shipID, t1, t2, t3, unix1, unix2, unix3});
                    }
                }
            }

            northConvoy.sort(Comparator.comparing(e -> e[5]));
            southConvoy.sort(Comparator.comparing(e -> e[5]));
            int counter = 1;
            writerNorth.write("ship_hash,NorthArrival,PortSaid,SuezSouth,unixNorthArrival,unixPortSaid,unixSuezSouth,rowid\n");
            for(String[] ls : northConvoy){
                writerNorth.write(String.join(",", ls));
                writerNorth.write("," + counter++ + "\n");
            }
            counter = 1;
            writerSouth.write("ship_hash,SorthArrival,SuezSouth,PortSaid,unixSouthArrival,unixSuezSouth,unixPortSaid,rowid\n");
            for(String[] ls : southConvoy){
                writerSouth.write(String.join(",", ls));
                writerSouth.write("," + counter++ + "\n");
            }
        }
    }

    //确定convoy的日期以及类型
    public static void step3(String sourcePath, String outputPath, String type) throws IOException{
        try(PrintWriter writer = new PrintWriter(workDIR + outputPath)) {
            String headerNorth = "NorthArrival,PortSaid,SuezSouth,unixNorthArrival,unixPortSaid,unixSuezSouth";
            String headerSouth = "SouthArrival,SuezSouth,PortSaid,unixSouthArrival,unixSuezSouth,unixPortSaid";
            writer.write("ship_hash," + (type.equals("South") ? headerSouth : headerNorth) + ",rowid,Type1,Type2,ConvoyDay\n");

            List<String> lines = Files.readAllLines(Paths.get(workDIR + sourcePath));
            lines = lines.subList(1, lines.size());

            List<List<String>> convoy = new ArrayList<>();
            convoy.add(new ArrayList<>()); convoy.get(0).add(lines.get(0));
            int startTime = Integer.parseInt(lines.get(0).split(",")[5]);
            int endTime = Integer.parseInt(lines.get(0).split(",")[6]);

            for(String s : lines.subList(1, lines.size())){
                String[] parts = s.split(",");

                int startTime_ = Integer.parseInt(parts[5]);
                int endTime_ = Integer.parseInt(parts[6]);

                double duration1 = endTime - startTime;
                double duration2 = endTime_ - startTime_;
                double intersection = endTime - startTime_;
                if ((intersection / Math.min(duration1, duration2) < 0.4)){
                    convoy.add(new ArrayList<>());
                }
                convoy.get(convoy.size()-1).add(s);

                startTime = startTime_;
                endTime = endTime_;
            }

            System.out.println(convoy.size());

            for(List<String> ls : convoy){
                List<String> eventTime = new ArrayList<>();
                for(String s : ls)
                    eventTime.add(s.split(",")[2].substring(0,10));
                String whichDay = ListString.mode(eventTime);
                System.out.println(whichDay + " " + ls.size());

                int pos = -1;
                for(int i = 0; i < ls.size()-1; i++){
                    String[] parts = ls.get(i).split(",");
                    int startT = Integer.parseInt(parts[5]), endT = Integer.parseInt(parts[6]);
                    String[] parts_ = ls.get(i+1).split(",");
                    int startT_ = Integer.parseInt(parts_[5]), endT_ = Integer.parseInt(parts_[6]);
                    boolean good_ = startT_ - startT >= 3600 && endT > endT_;
                    boolean good__ = false;
                    if(good_ || good__){
                        pos = i;
                        break;
                    }
                }
                for(int i = 0; i < ls.size(); i++){
                    writer.write(ls.get(i));
                    writer.write("," + type + "," + (i > pos ? type : "Early") + "," + whichDay + "\n");
                }
            }
        }
    }

    //timeGapAnalysis
    public static void step_1() throws IOException{
        try(PrintWriter writer = new PrintWriter(workDIR + "timeGapAnalysis.csv");
            BatchFileReader reader = new BatchFileReader(workDIR + "rn-806-pos-shiptype-status.csv", ",", true, 0)) {
            writer.write("rowid,gapInSeconds\n");
            int counter = 0;
            for(List<String> ls : reader){
                for(int i = 0; i < ls.size() - 1; i++){
                    int from = Integer.parseInt(ls.get(i).split(",")[1]);
                    int to = Integer.parseInt(ls.get(i+1).split(",")[1]);
                    writer.write(counter + "," + (to - from) + "\n");
                    counter++;
                }
            }
        }
    }

    //找出不适合线性插值的位置
    public static void step_2() throws IOException{
        try (PrintWriter writer = new PrintWriter(workDIR + "rn-806-pos-shiptype-status-flag.csv");
             BatchFileReader reader = new BatchFileReader(workDIR + "rn-806-pos-shiptype-status.csv", ",", true, 0)){
            writer.write("ship_hash,t,lon,lat,heading,course,speed,status,shiptype,statusinfo,x,y,flag1,flag2,flag4,flag8,flag12,flag24\n");
            for (List<String> ls : reader){
                int flag1 = 0, flag2 = 0, flag4 = 0, flag8 = 0, flag12 = 0, flag24 = 0;
                for(int i = 0; i < ls.size(); i++){
                    if(i == 0){
                        writer.write(ls.get(0) + ",0,0,0,0,0,0\n");
                    }else {
                        int prev = Integer.parseInt(ls.get(i-1).split(",")[1]);
                        int cur = Integer.parseInt(ls.get(i).split(",")[1]);
                        if (cur - prev >= 3600)
                            flag1++;
                        if (cur - prev >= 3600 * 2)
                            flag2++;
                        if (cur - prev >= 3600 * 4)
                            flag4++;
                        if (cur - prev >= 3600 * 8)
                            flag8++;
                        if (cur - prev >= 3600 * 12)
                            flag12++;
                        if (cur - prev >= 3600 * 24)
                            flag24++;
                        writer.write(String.join(",", ls.get(i), flag1+"", flag2+"", flag4+"", flag8+"", flag12+"", flag24+""));
                        writer.write("\n");
                    }
                }
            }
        }
    }

    //为crossing加上unix时间戳以及Up还是Down
    public static void step_3() throws IOException, ParseException {
        try (PrintWriter writer = new PrintWriter(workDIR + "crossingFlag4_new.csv");
             BatchFileReader readerCrossing = new BatchFileReader(workDIR+"crossingFlag4.csv", ",", true, 0);
            BatchFileReader readerDirection = new BatchFileReader(workDIR+"rn-806-pos-shiptype-status-flag.csv", ",", true, 0)
            )
        {
            writer.write("ship_hash,description,rowid,t,longitude,latitude,unixstamp,direction\n");

            HashMap<String, List<String>> help = new HashMap<>();
            for (List<String> ls : readerDirection){
                help.put(ls.get(0).split(",")[0], ls);
            }

            for (List<String> ls : readerCrossing){
                String ship = ls.get(0).split(",")[0];
                List<String> records = help.get(ship);

                for (String s : ls){
                    long unixstamp = TwoTimestamp.formatter1.parse(s.split(",")[3].substring(0,19)).getTime() / 1000;

                    int larger = ListGeneric.firstIndex(records, e -> e.split(",")[1].compareTo(unixstamp+"") > 0);
                    double largerLAT = Double.parseDouble(records.get(larger).split(",")[3]);
                    double smallerLAT = Double.parseDouble(records.get(larger-1).split(",")[3]);
                    String direction = largerLAT > smallerLAT ? "Up" : "Down";

                    writer.write(String.join(",", s, unixstamp+"", direction));
                    writer.write("\n");
                }
            }
        }
    }

    public static void main(String[] args) throws IOException, ParseException {
//        step1();

//        step2();

        step3("crossingSouthConvoyTriple.csv", "crossingSouthConvoyAnswer.csv", "South");
        step3("crossingNorthConvoyTriple.csv", "crossingNorthConvoyAnswer.csv", "North");

//        step_1();

//        step_2();

//        step_3();

    }
}
