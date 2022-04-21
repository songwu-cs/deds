package note.task_2022_03_26_preprint_experiments;

import calculation.ListString;
import com.vividsolutions.jts.index.strtree.SIRtree;
import io.bigdata.BatchFileReader;
import ml.TrajSegmentation;
import note.task_2022_01_21_paper_draft.GenerateTraingStrategy;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static note.task_2022_01_21_paper_draft.Task.toCritical10Plus;
import static note.task_2022_01_21_paper_draft.Task.toFeatures10Plus;

public class Task {
    public static String baseDir = "H:\\UpanSky\\DEDS-DataLake\\2022-03-26-preprint-experiments\\";

    //检查自己标注的分割点是否正确
    public static void checkIfCorrect() throws IOException {
        HashMap<String, Integer> trip2size = new HashMap<>();
        try(BatchFileReader reader = new BatchFileReader(baseDir + "likely_trips.csv", ",", true, 0)){
            for(List<String> trip : reader)
                trip2size.put(trip.get(0).split(",")[0], trip.size());
        }

        List<String> lines = Files.readAllLines(Paths.get(baseDir + "likely_trips.csv"));
        try(BatchFileReader reader = new BatchFileReader(baseDir + "ground-truth-all.csv", ",", true, 0)) {
            for(List<String> trip : reader){
                String tripID = trip.get(0).split(",")[0];
                int sum = 0;
                for(String segment : trip){
                    String startTime = segment.split(",")[1];
                    String endTime = segment.split(",")[2];
                    int startIndex = ListString.indexStartWith(lines, tripID + "," + startTime);
                    int endIndex = ListString.indexStartWith(lines, tripID + "," + endTime);
                    sum += endIndex - startIndex + 1;
                }
                if(sum != trip2size.get(tripID))
                    System.out.println(String.join(",", tripID, trip2size.get(tripID).toString(), sum+""));
            }
        }
    }

    public static void toWindows() throws IOException, ParseException {
        note.task_2022_01_21_paper_draft.Task.toTrainTestWindows(baseDir + "likely_trips.csv",
                baseDir + "ground-truth.csv",
                baseDir + "likely_trip_id.txt",
                baseDir + "window_training.csv",
                baseDir + "window_testing.csv",
                new GenerateTraingStrategy(300,3600,1.0/6));
    }

    public static void toCritical10Also() throws IOException, ParseException, InterruptedException {
        toCritical10Plus(baseDir + "likely_trips.csv",
                baseDir + "window_training.csv",
                baseDir + "window_training_intervals.csv");
        toCritical10Plus(baseDir + "likely_trips.csv",
                baseDir + "window_testing.csv",
                baseDir + "window_testing_intervals.csv");
    }

    public static void toFeatures10Also() throws IOException, ParseException {
        toFeatures10Plus(baseDir + "window_training_intervals.csv", baseDir + "window_training_intervals_features.csv");
        toFeatures10Plus(baseDir + "window_testing_intervals.csv", baseDir + "window_testing_intervals_features.csv");
    }

    public static void buildFishing() throws IOException {
        note.task_2022_01_21_paper_draft.Task.buildFishingParts(baseDir + "window_testing_intervals_features.csv",//预测的标签
                baseDir + "build_labels.csv",//保存路径
                baseDir + "build_encoding.csv",//变长编码
                baseDir + "likely_trips.csv",//ais数据集
                baseDir + "window_testing.csv", //窗口的开始结束时间
                39);
    }

    public static void evaluateStep1() throws IOException {
        List<String> testTripsID = Files.readAllLines(Paths.get(baseDir + "97_test_trips.txt"));
        List<String> testTripsData = Files.readAllLines(Paths.get(baseDir + "build_labels.csv"));
        List<String> testTripsGroundTruth = Files.readAllLines(Paths.get(baseDir + "ground-truth-all.csv"));
        int counter = 0;
        try(PrintWriter writer = new PrintWriter(baseDir + "testTripsGroundTruth.csv")) {
            writer.write("id,segmentID\n");
            for(String trip : testTripsID){
                int start = ListString.indexStartWith(testTripsGroundTruth, trip);
                int end = ListString.indexStartWithFromRight(testTripsGroundTruth, trip);
                for(int i = start; i <= end; i++){
                    String[] parts = testTripsGroundTruth.get(i).split(",");
                    int start2 = ListString.indexStartWith(testTripsData, parts[0] + "," + parts[1]);
                    int end2 = ListString.indexStartWith(testTripsData, parts[0] + "," + parts[2]);
                    counter++;
                    for(int j = 0; j <= end2 - start2; j++){
                        writer.write(trip + "," + String.format("%03d", counter) + "-" + parts[3] + "\n");
                    }
                }
            }
        }
    }

    public static void evaluateStep2() throws IOException {
        List<List<String>> lls = new ArrayList<>();
        String[] methods = {"sandwich.csv", "cbsmot_1knot_1km.csv", "wkmeans3_delta0.csv", "wkmeans6_delta0.csv", "sws_LR_7_99.9.csv"};
        String[] methodNames = {"sandwich", "cbsmot", "wkmeans3", "wkmeans6", "sws"};
        for(String method : methods)
            lls.add(Files.readAllLines(Paths.get(baseDir + method)));
        List<String> labelsTruth = Files.readAllLines(Paths.get(baseDir + "testTripsGroundTruth0.csv"));

        try(PrintWriter writer = new PrintWriter(baseDir + "purityCoverageHarmonicMean.csv");
            BatchFileReader reader = new BatchFileReader(baseDir + "testTripsGroundTruth.csv", ",", true, 0)){
            writer.write("id,measure,method,value\n");
            int pos = 1;
            for (List<String> ls : reader){
                String id = ls.get(0).split(",")[0];
                for(int i = 0; i < methods.length; i++){
                    double purity = TrajSegmentation.purity(labelsTruth.subList(pos, pos + ls.size()),
                            lls.get(i).subList(pos, pos + ls.size()), false);
                    double coverage = TrajSegmentation.coverageFromTruth(labelsTruth.subList(pos, pos + ls.size()),
                            lls.get(i).subList(pos, pos + ls.size()));
                    double hmean = 2 * purity * coverage / (purity + coverage);
                    int numSegs = new HashSet<>(lls.get(i).subList(pos, pos + ls.size())).size();
                    writer.write(String.join(",", id, "purity", methodNames[i], purity+"") + "\n");
                    writer.write(String.join(",", id, "coverage", methodNames[i], coverage+"") + "\n");
                    writer.write(String.join(",", id, "hmean", methodNames[i], hmean+"") + "\n");
                    writer.write(String.join(",", id, "numberSegments", methodNames[i], numSegs+"") + "\n");
                }
                pos += ls.size();
                System.out.println(id);
            }
        }
    }

    public static void main(String[] args) throws IOException, ParseException, InterruptedException {
//        checkIfCorrect(); //step1

//        toWindows(); // step2

//        toCritical10Also(); //step3

//        toFeatures10Also(); //step4

//        buildFishing(); //step5

//        evaluateStep1();

//        evaluateStep2();


    }
}
