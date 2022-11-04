package note.task_2022_03_26_preprint_experiments;

import calculation.Array1DBoolean;
import calculation.ListString;
import com.vividsolutions.jts.index.strtree.SIRtree;
import datetime.TwoTimestamp;
import io.bigdata.BatchFileReader;
import ml.TrajSegmentation;
import note.task_2022_01_21_paper_draft.GenerateTraingStrategy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

import static note.task_2022_01_21_paper_draft.Task.toCritical10Plus;
import static note.task_2022_01_21_paper_draft.Task.toFeatures10Plus;

public class Task {
    public static String baseDir = "H:\\UpanSky\\DEDS-DataLake\\2022-03-26-preprint-experiments\\";
    public static String marceauDIR = "H:\\UpanSky\\DEDS-DataLake\\Marceau\\";

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

    public static void archive() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(baseDir + "likely_trips.csv"));

        try(PrintWriter writer = new PrintWriter(baseDir + "128_fishing_trajs.csv")) {
            writer.write("id,t,longitude,latitude,x,y,signed_turn,bearing,time_gap,distance_gap,euc_speed,distanceToShore,label\n");

            try(BatchFileReader reader = new BatchFileReader(baseDir + "ground-truth-all.csv", ",", true, 0)) {
                for(List<String> trip : reader){
                    int counter = 1;
                    String tripID = trip.get(0).split(",")[0];

                    for(String segment : trip){
                        String activity = segment.split(",")[3];

                        String startTime = segment.split(",")[1];
                        String endTime = segment.split(",")[2];
                        int startIndex = ListString.indexStartWith(lines, tripID + "," + startTime);
                        int endIndex = ListString.indexStartWith(lines, tripID + "," + endTime);

                        for(int i = startIndex; i <= endIndex; i++){
                            writer.write(lines.get(i) + "," + String.format("%02d", counter) + "-" + activity + "\n");
                        }

                        counter++;
                    }
                }
            }
        }
    }

    //查看两个数据集的差别
    public static void marceau1() throws IOException {
        List<String> marceau = Files.readAllLines(Paths.get(marceauDIR+ "Marcea-result.csv"));
        List<Integer> li = new ArrayList<>();
        int old = -1;
        int counter = 0;
        for(String s : marceau){
            counter++;
            if(s.endsWith("00")){
                if(old >= 0){
                    li.add(counter - old);
                    old = counter;
                }else
                    old = counter;
            }
        }
        li.add(counter - old + 1);

        System.out.println(li);

        Map<Integer, String> lss = new HashMap<>();
        BatchFileReader batchFileReader = new BatchFileReader(marceauDIR + "128_fishing_trajs.csv", ",", true, 0);
        for(List<String> ls : batchFileReader){
            lss.put(ls.size()-1, ls.get(0).split(",")[0]);
        }

        for(Integer i : lss.keySet()){
            if(! li.contains(i))
                System.out.println(lss.get(i));
            else
                li.remove(i);
        }
        System.out.println(li);
    }

    //确保两个数据集的record order一样
    public static void marceau2() throws IOException {
        List<String> marceau = Files.readAllLines(Paths.get(marceauDIR + "Marcea-result.csv"));
        List<Integer> li = new ArrayList<>();
        int old = -1;
        int counter = 0;
        for(String s : marceau){
            counter++;
            if(s.endsWith("00")){
                if(old >= 0){
                    li.add(counter - old);
                    old = counter;
                }else
                    old = counter;
            }
        }
        li.add(counter - old + 1);

        System.out.println(li);

        li.clear();
        BatchFileReader batchFileReader = new BatchFileReader(marceauDIR+"128_fishing_trajs.csv", ",", true, 0);
        for(List<String> ls : batchFileReader){
            li.add(ls.size()-1);
        }
        System.out.println(li);
    }

    //合并Marceau的标签预测结果到128个轨迹中
    public static void marceau3() throws IOException {
        try(PrintWriter writer = new PrintWriter(marceauDIR+"128_fishing_trajsNEW.csv")) {
            writer.write("id,t,longitude,latitude,x,y,signed_turn,bearing,time_gap,distance_gap,euc_speed,distanceToShore,label\n");
            BatchFileReader batchFileReader = new BatchFileReader(marceauDIR+"128_fishing_trajs.csv", ",", true, 0);
            for(List<String> ls : batchFileReader){
                for(String s : ls.subList(0, ls.size() - 1)){
                    writer.write(s + "\n");
                }
            }
        }
    }

    //合并Marceau的标签预测结果到97个轨迹中
    private static void marceau4() throws IOException {
        try(PrintWriter writer = new PrintWriter(marceauDIR+"build_labelsNEW.csv")){
            writer.write("id,t,longitude,latitude,x,y,signed_turn,bearing,time_gap,distance_gap,euc_speed,Distance To Shore,sandwich,kernel,cbsmot,wkmeans3,wkmeans6,sws,Truth,Marceau\n");
            List<String> marceauResult = Files.readAllLines(Paths.get(marceauDIR+"128_fishing_trajsNEW.csv"));
            Map<String,String> helper = new HashMap<>();
            for(String s : marceauResult){
                String[] ss = s.split(",");
                helper.put(ss[0]+ss[1], ss[13]);
            }

            List<String> myResult = Files.readAllLines(Paths.get(marceauDIR+"build_labels.csv"));
            for(String s : myResult.subList(1, myResult.size())){
                String[] ss = s.split(",");
                if(helper.containsKey(ss[0]+ss[1])){
                    writer.write(s+","+helper.get(ss[0]+ss[1]));
                    writer.write("\n");
                }
            }
        }
    }


    public static void marceau5() throws IOException {
        try(PrintWriter writer = new PrintWriter(marceauDIR+"build_labelsNEWsmooth.csv")){
            writer.write("id,t,longitude,latitude,x,y,signed_turn,bearing,time_gap,distance_gap,euc_speed,Distance To Shore,sandwich,kernel,cbsmot,wkmeans3,wkmeans6,sws,Truth,Marceau,MarceauSmooth\n");
            BatchFileReader batchFileReader = new BatchFileReader(marceauDIR+"build_labelsNEW.csv", ",", true, 0);
            for(List<String> ls : batchFileReader){
                boolean[] bools = new boolean[ls.size()];
                String[] times = new String[ls.size()];
                for(int i = 0; i < ls.size(); i++){
                    String[] ss = ls.get(i).split(",");
                    bools[i] = ss[19].contains("fish");
                    times[i] = ss[1];
                }

                List<Integer> runlength = Array1DBoolean.runLengthEncoding(bools);
                int[] runlengthArray = new int[runlength.size()];
                runlengthArray[0] = runlength.get(0); runlengthArray[runlength.size()-1] = runlength.get(runlength.size()-1);
                for(int i = 1;  i < runlength.size() - 1; i++){
                    if(runlength.get(i) < 0){
                        if(runlength.get(i-1) >= -1*runlength.get(i) && runlength.get(i+1) >= -1*runlength.get(i)){
                            runlengthArray[i] = -1 * runlength.get(i);
                        }else
                            runlengthArray[i] = runlength.get(i);
                    }else
                        runlengthArray[i] = runlength.get(i);
                }
//                System.out.println(runlength);
//                System.out.println(Arrays.toString(runlengthArray));

                int start = 0;
                boolean[] bools2 = new boolean[ls.size()];
                for(int i : runlengthArray){
                    Arrays.fill(bools2, start, start+Math.abs(i), i > 0);
                    start = start + Math.abs(i);
                }
                List<Integer> runlength2 = Array1DBoolean.runLengthEncoding(bools2);
                int[] runlengthArray2 = new int[runlength2.size()];
//                System.out.println(runlength2);

                start = 0;
                int pos = 0;
                for(int i : runlength2){
                    if(i > 0){
                        if(TwoTimestamp.diffInSeconds(times[start+i-1], times[start], TwoTimestamp.formatter1) >= 7200)
                            runlengthArray2[pos] = i;
                        else
                            runlengthArray2[pos] = -i;
                    }
                    else
                        runlengthArray2[pos] = i;

                    pos++;
                    start += Math.abs(i);
                }
//                System.out.println(Arrays.toString(runlengthArray2));

                start = 0;
                boolean[] bools3 = new boolean[ls.size()];
                for(int i : runlengthArray2){
                    Arrays.fill(bools3, start, start+Math.abs(i), i > 0);
                    start = start + Math.abs(i);
                }
                List<Integer> runlength3 = Array1DBoolean.runLengthEncoding(bools3);
//                System.out.println(runlength3);

                String[] smoothLabels = new String[ls.size()];
                start = 0;
                int count = 1;
                for(int i : runlength3){
                    Arrays.fill(smoothLabels, start, start+Math.abs(i), String.format("%02d", count)+"-"+(i > 0 ? "fishing" : "sailing"));
                    start += Math.abs(i);
                    count++;
                }
//                System.out.println(Arrays.toString(smoothLabels));

                for(int i = 0; i < ls.size(); i++){
                    writer.write(ls.get(i) + "," + smoothLabels[i] + "\n");
                }
            }
        }
    }

    public static void marceau6() throws IOException{
        BatchFileReader batchFileReader = new BatchFileReader(marceauDIR+"build_labelsNEWsmooth.csv", ",", true, 0);
        double puritySumMarceau = 0;
        double puritySumSong = 0;
        double coverageSumMarceau = 0;
        double coverageSumSong = 0;
        double meanSumMarceau = 0;
        double meanSumSong = 0;
        double numberSumMarceau = 0;
        double numberSumSong = 0;

        for(List<String> ls : batchFileReader){
            List<String> truth = new ArrayList<>();
            List<String> rle = new ArrayList<>();
            List<String> marceau = new ArrayList<>();
            for(String s : ls){
                String[] ss = s.split(",");
                truth.add(ss[18]);
                rle.add(ss[12]);
                marceau.add(ss[20]);
            }
            double a = TrajSegmentation.coverageFromTruth(truth, marceau);
            coverageSumMarceau += a;
            double b = TrajSegmentation.coverageFromTruth(truth, rle);
            coverageSumSong += b;
            double c = TrajSegmentation.purity(truth, marceau, false);
            puritySumMarceau += c;
            double d = TrajSegmentation.purity(truth, rle, false);
            puritySumSong += d;
            numberSumMarceau += new HashSet<>(marceau).size();
            numberSumSong += new HashSet<>(rle).size();
            meanSumMarceau += (2 * a * c / (a + c));
            meanSumSong += (2 * b * d / (b + d));
        }

        System.out.println(String.join(",", puritySumMarceau/97+"",coverageSumMarceau/97+"", meanSumMarceau/97+"", numberSumMarceau/97+""));
        System.out.println(String.join(",", puritySumSong/97+"",coverageSumSong/97+"", meanSumSong/97+"", numberSumSong/97+""));

    }

    public static void main(String[] args) throws IOException, ParseException, InterruptedException {
//        checkIfCorrect(); //step1

//        toWindows(); // step2

//        toCritical10Also(); //step3

//        toFeatures10Also(); //step4

//        buildFishing(); //step5

//        evaluateStep1();

//        evaluateStep2();

//        archive();

//        marceau1();
//        marceau2();
//        marceau3();
//        marceau4();
//        marceau5();
        marceau6();
    }


}
