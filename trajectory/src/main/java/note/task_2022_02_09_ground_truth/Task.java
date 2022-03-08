package note.task_2022_02_09_ground_truth;

import calculation.UnitString;
import io.bigdata.BatchFileReader;
import note.task_2022_01_21_paper_draft.GenerateTraingStrategy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Task {
    public static final String path220125 = "H:\\UpanSky\\DEDS-DataLake\\2022-01-25-gfw\\";
    public static final String path220209 = "H:\\UpanSky\\DEDS-DataLake\\2022-02-09-ground-truth-using-old-model-on-cluster50\\";
    public static final String pathGermanNorway = "德国及瑞典船只.txt";
    public static final String pathTripsCluster50 = "trips_cluster50.csv";

    //调用完之后，已经重命名文件
    public static void changeColumeOrder() throws IOException {
        try(PrintWriter writer = new PrintWriter(path220209 + "likely_trips2.csv");
            BatchFileReader reader = new BatchFileReader(path220209 + "likely_trips.csv", ",", true, 0)){
            writer.write("Id,T,Longitude,Latitude,X,Y,Signed Turn,Bearing,Time Gap,Distance Gap,Speed,Distance To Shore\n");
            for(List<String> ls : reader){
                for(String line : ls){
                    writer.write(UnitString.subset(line, ",", 0,1,7,6,10,11,8,2,9,3,5,4) + "\n");
                }
            }
        }
    }

    //step 1：将簇50转换为trips，去掉德国以及挪威船只
    public static void getTripsByCluster50() throws IOException {
        Set<String> germannorway = new HashSet<>(Files.readAllLines(Paths.get(path220125 + pathGermanNorway)));

        try(BatchFileReader batchFileReader = new BatchFileReader(path220125 + "fishing-ais-oneweek-filtered-ge1000-denoisedFake-anchorage-withDistanceXYFinal.csv", ",", true, 0, 15);
            PrintWriter printWriter = new PrintWriter(path220209 + pathTripsCluster50)){
            printWriter.write("id,t,longitude,latitude,time_gap,distance_gap,euc_speed,signed_turn,bearing,distanceToShore,x,y\n");

            for(List<String> ls : batchFileReader){
                String[] parts = ls.get(0).split(",");
                if(germannorway.contains(parts[0]) || parts[parts.length-1].startsWith("anchor"))
                    continue;

                for(String line : ls){
                    String newLine = UnitString.subset(line, ",", 0,15,1,2,3,4,5,6,7,8,12,13,14);
                    printWriter.write(newLine.replace(",sailing", "") + "\n");
                }
            }

        }
    }

    //step 2: 转化为窗口
    public static void toWindows() throws IOException, ParseException {
        note.task_2022_01_21_paper_draft.Task.toTrainTestWindows(path220209 + "likely_trips.csv",
                path220209 + "ground-truth.csv",
                path220209 + "likely_trip_id.txt",
                path220209 + "window_training.csv",
                path220209 + "window_testing.csv",
                new GenerateTraingStrategy(300,3600,1.0/6));
    }

    //step 3: 转换为critical events
    public static void toCriticalEvents() throws IOException, ParseException, InterruptedException {
        note.task_2022_01_21_paper_draft.Task.toCritical(path220209 + "likely_trips.csv",
                path220209 + "window_testing.csv",
                path220209 + "window_testing_intervals.csv");
    }

    //step 4：计算features
    public static void toFeatures() throws IOException, ParseException {
        note.task_2022_01_21_paper_draft.Task.toFeatures(path220209 + "window_testing_intervals.csv", path220209 + "window_testing_intervals_features.csv");
    }

    //step 5：计算捕鱼活动
    public static void buildFishing() throws IOException {
        note.task_2022_01_21_paper_draft.Task.buildFishingParts(path220209 + "window_testing_intervals_features.csv",//预测的标签
                path220209 + "build_labels.csv",//保存路径
                path220209 + "build_encoding.csv",//变长编码
                path220209 + "likely_trips.csv",//ais数据集
                path220209 + "window_testing.csv");//窗口的开始结束时间
    }


    public static void main(String[] args) throws IOException, ParseException, InterruptedException {
//        buildFishing();
        getTripsByCluster50();
    }
}
