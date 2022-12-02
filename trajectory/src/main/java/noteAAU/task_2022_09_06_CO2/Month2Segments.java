package noteAAU.task_2022_09_06_CO2;

import calculation.ListGeneric;
import calculation.UnitString;
import datetime.TwoTimestamp;
import io.bigdata.BatchFileReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Month2Segments {
    public static String workdir = "H:\\UpanSky\\DEDS_DenmarkAIS_May_2022\\";


    //空间过滤, 时间过滤, 修正draught, 根据draught划分, 在emeditor中手动去重
    public static void step1() throws IOException {
        int mmsiINDEX = 2;
        int timeINDEX = 0;
        int timeThreshold = 3600; // seconds
        int statusINDEX = 5;
        int draughtINDEX = 18;
        int latINDEX = 3;
        int lonINDEX = 4;
        int sogINDEX = 7;

        try(PrintWriter writer = new PrintWriter(workdir+ "aisdk_onemonth_sorted_cargoAllColumnsSegments.csv")) {
            writer.write("mmsi,timestamp,latitude,longitude,sog,navigationStatus,draughtCorrected,segmentID\n");
            BatchFileReader batchFileReader = new BatchFileReader(workdir + "aisdk_onemonth_sorted_cargoAllColumns.csv", ",", true, mmsiINDEX);
            for(List<String> ss : batchFileReader){
                List<String[]> aoi = new ArrayList<>();

                //空间过滤
                for(String s : ss){
                    String line = UnitString.replaceCommaInQuote(s, ',', '@');
                    String[] parts = line.split(",");
                    double lat = Double.parseDouble(parts[latINDEX]);
                    double lon = Double.parseDouble(parts[lonINDEX]);
                    if(lat >= 52 && lat <= 60 && lon >= 5 && lon <= 20){
                        aoi.add(parts);
                    }
                }

                if(aoi.size() < 2)
                    continue;
                //时间分割
                String previousTimestamp = aoi.get(0)[timeINDEX];
                List<List<String[]>> initialSegments = new ArrayList<>();
                initialSegments.add(new ArrayList<>());
                initialSegments.get(initialSegments.size()-1).add(aoi.get(0));
                for(String[] parts : aoi.subList(1, aoi.size())){
                    String currentTimestamp = parts[timeINDEX];
                    if(TwoTimestamp.diffInSeconds(currentTimestamp, previousTimestamp, TwoTimestamp.formatter2) > timeThreshold){
                        initialSegments.add(new ArrayList<>());
                    }
                    initialSegments.get(initialSegments.size()-1).add(parts);
                    previousTimestamp = currentTimestamp;
                }

                //修正draught
                List<List<String[]>> initialValidSegments = new ArrayList<>();
                for(List<String[]> seg : initialSegments){
                    for(String[] parts : seg){
                        if(! parts[draughtINDEX].equals("")){
                            initialValidSegments.add(seg);
                            break;
                        }
                    }
                }
                for(List<String[]> seg : initialValidSegments){
                    HashSet<Integer> estimated = new HashSet<>();
                    for(int i = 0; i < seg.size(); i++){
                        String[] parts = seg.get(i);
                        if(parts[draughtINDEX].equals("")){
                            estimated.add(i);
                            String[] leftNeighbor = null;
                            String[] rightNeighbor = null;
                            for(int ii = i-1; ii >= 0 ; ii--){
                                String[] leftParts = seg.get(ii);
                                if((! leftParts[draughtINDEX].equals("")) && (!estimated.contains(ii))){
                                    leftNeighbor = leftParts;
                                    break;
                                }
                            }
                            for(int ii = i+1; ii < seg.size(); ii++){
                                String[] rightParts = seg.get(ii);
                                if((! rightParts[draughtINDEX].equals("")) && (!estimated.contains(ii))){
                                    rightNeighbor = rightParts;
                                    break;
                                }
                            }
                            if(leftNeighbor == null){
                                parts[draughtINDEX] = rightNeighbor[draughtINDEX];
                            } else if (rightNeighbor == null) {
                                parts[draughtINDEX] = leftNeighbor[draughtINDEX];
                            }else {
                                double leftSeconds = TwoTimestamp.diffInSeconds(parts[timeINDEX], leftNeighbor[timeINDEX], TwoTimestamp.formatter2);
                                double rightSeconds = TwoTimestamp.diffInSeconds(rightNeighbor[timeINDEX], parts[timeINDEX], TwoTimestamp.formatter2);
                                parts[draughtINDEX] = (leftSeconds >= rightSeconds) ? rightNeighbor[draughtINDEX] : leftNeighbor[draughtINDEX];
                            }
                        }
                    }
                }


                //根据draught生成最终的segments, 忽略只有一个点的segment
                int segmentID = 1;
                for(List<String[]> seg : initialValidSegments){
                    for(List<String[]> finalSeg : ListGeneric.groupString(seg, e -> ((String[])e)[18])){
                        if(finalSeg.size() < 2)
                            continue;

                        for(String[] line : finalSeg){
                            writer.write(String.join(",", line[mmsiINDEX], line[timeINDEX], line[latINDEX], line[lonINDEX], line[sogINDEX], line[statusINDEX], line[draughtINDEX], String.format("%03d", segmentID)));
                            writer.write("\n");
                        }
                        segmentID++;
                    }
                }
            }
        }
    }

    //去除异常点
    public static void step2() throws IOException{
        try(BatchFileReader batchFileReader = new BatchFileReader(workdir+"aisdk_onemonth_sorted_cargoAllColumnsSegments.csv", ",", true, 0,7);
            PrintWriter writer = new PrintWriter(workdir+"aisdk_onemonth_sorted_cargoAllColumnsSegmentsNoOutliers.csv")) {
            writer.write("mmsi,timestamp,latitude,longitude,sog,navigationStatus,draughtCorrected,segmentID,x,y\n");
            //假定异常点不会出现在开头
            for (List<String> lines : batchFileReader){
                List<String> goodLines = new ArrayList<>();
                goodLines.add(lines.get(0));
                String[] parts = lines.get(0).split(",");
                for(String line : lines.subList(1, lines.size())){
                    String[] parts2 = line.split(",");
                    double timeGap = TwoTimestamp.diffInSeconds(parts2[1], parts[1], TwoTimestamp.formatter2);
                    double diffX = Math.abs(Double.parseDouble(parts2[8]) - Double.parseDouble(parts[8]));
                    double diffY = Math.abs(Double.parseDouble(parts2[9]) - Double.parseDouble(parts[9]));
                    if (Math.hypot(diffX, diffY) / timeGap * 3.6 / 1.852 >= 50)
                        continue;
                    else {
                        goodLines.add(line);
                        parts = parts2;
                    }
                }

                if (goodLines.size() >= 2){
                    for (String line : goodLines) {
                        writer.write(line);
                        writer.write("\n");
                    }
                }
            }
        }
    }

    //处理由于去掉异常点造成的time gap大于1小时的情况
    public static void step2Extra() throws Exception{
        try(BatchFileReader batchFileReader = new BatchFileReader(workdir+"aisdk_onemonth_sorted_cargoAllColumnsSegmentsNoOutliers.csv", ",", true, 0);
            PrintWriter writer = new PrintWriter(workdir+"aisdk_onemonth_sorted_cargoAllColumnsSegmentsNoOutliers2.csv")) {
            writer.write("mmsi,timestamp,latitude,longitude,sog,navigationStatus,draughtCorrected,segmentID,x,y\n");

            for (List<String> mmsi : batchFileReader){
                List<List<String>> segments = ListGeneric.groupString(mmsi, e -> e.split(",")[7]);
                List<List<String>> realSegments = new ArrayList<>();
                for(List<String> segment : segments){
                    List<String> head = new ArrayList<>();
                    String previousT = segment.get(0).split(",")[1];
                    head.add(segment.get(0));
                    realSegments.add(head);

                    for(String s : segment.subList(1, segment.size())){
                        String nowT = s.split(",")[1];
                        if (TwoTimestamp.diffInSeconds(nowT, previousT, TwoTimestamp.formatter2) >= 3600){
                            realSegments.add(new ArrayList<>());
                        }
                        realSegments.get(realSegments.size()-1).add(s);
                        previousT = nowT;
                    }
                }

                int segmentID = 1;
                for(List<String> segment : realSegments){
                    for(String line : segment){
                        String[] parts = line.split(",");
                        parts[7] = String.format("%03d", segmentID);
                        writer.write(String.join(",", parts));
                        writer.write("\n");
                    }

                    segmentID++;
                }
            }
        }
    }

    //获取数据集中MMSI和IMO的对应关系
    public static void step4() throws IOException{
        try(BatchFileReader reader = new BatchFileReader(workdir + "aisdk_onemonth_sorted_cargoAllColumns.csv", ",", true, 2);
            PrintWriter writer = new PrintWriter(workdir + "aisdk_onemonth_sorted_cargo_MMSI_IMO.csv")){
            writer.write("mmsi,imo\n");

            HashSet<String> pairs = new HashSet<>();
            for(List<String> ls : reader){
                for(String s : ls){
                    String[] parts = s.split(",");
                    if(parts[10] != null)
                        pairs.add(parts[2] + "," + parts[10]);
                }
            }

            List<String> ls = new ArrayList<>(pairs);
            ls.sort(Comparator.naturalOrder());
            for(String s : ls) {
                writer.write(s + "\n");
            }
        }
    }


    public static void main(String[] args) throws Exception {
//        step1();
//        System.out.println("q,b,,c".split(",")[2].equals(""));

//        step2();
        step2Extra();

//        step3();

//        step4();

//        System.out.println(TwoTimestamp.diffInSeconds("27/05/2022 07:37:49", "27/05/2022 05:30:23", TwoTimestamp.formatter2));

    }
}
