package note.task_2022_01_29_gfw;

import calculation.*;
import datetime.TwoTimestamp;
import db.pg.DenmarkCoast;
import io.bigdata.BatchFileReader;
import note.common.CommonIO;
import note.task_2021_12_25_fine_tune.IsDense;
import songwu.deds.trajectory.clean.DenoiseFakeTimeStampedPointT;
import songwu.deds.trajectory.data.TimeStampedPoint;
import songwu.deds.trajectory.data.TimeStampedPointT;
import songwu.deds.trajectory.io.File2TimestampedPointT;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Task {
    public static final String baseDir = "H:\\UpanSky\\DEDS-DataLake\\2022-01-25-gfw\\";

    public static void anchorageDetect(String dataFile, String outFile) throws IOException, ParseException, InterruptedException {
        List<TimeStampedPointT> denoised = CommonIO.denoiseTask(dataFile);

        IsDense isDense = new IsDense(50, 10, 20, 3600);
        try(PrintWriter writer = new PrintWriter(outFile)){
            writer.write("id,t,labelNoiseFalse,labelNoiseTrue,cluster\n");
            for(TimeStampedPointT traj : denoised){
                boolean[] lnf = isDense.labelNoiseTrueFalse(traj, false);
                boolean[] lnt = isDense.labelNoiseTrueFalse(traj, true);
                int[] clusters = Array1DBoolean.falseNegativeTruePositive(lnt);
                for(int i = 0; i < traj.getAllUnits().size(); i++){
                    TimeStampedPoint point = traj.getUnit(i);
                    writer.write(String.join(",",
                            traj.trajId(),
                            point.getTimestamp(),
                            lnf[i] ? "anchor" : "sailing",
                            lnt[i] ? "anchor" : "sailing",
                            (clusters[i] > 0 ? "anchor-" : "sailing-") + Math.abs(clusters[i])) + "\n");
                }
            }
        }
    }

    public static void toDenmarkCoast(String path, int step, int idIndex, int tIndex, int xIndex, int yIndex) throws IOException, InterruptedException {
        List<String> lines = Files.readAllLines(Paths.get(path));
        List<ToDenmarkCoastDistance> toDenmarkCoastDistances = new ArrayList<>();
        for(int i = 0; i < step; i++){
            toDenmarkCoastDistances.add(new ToDenmarkCoastDistance(lines.subList(1, lines.size()), step, i,0, 1, 4,5));
        }
        List<Thread> threads = new ArrayList<>();
        for(int i = 0; i < step; i++){
            threads.add(new Thread(toDenmarkCoastDistances.get(i)::go));
            threads.get(i).start();
        }
        for(Thread thread : threads)
            thread.join();
    }

    public static void anchorageConnect(String dataFile, String outFile, int timeThreshold, int distanceThreshold, int idIndex, int tIndex, int xIndex, int yIndex, int distIndex, int labelIndex) throws IOException, ParseException, SQLException {
        DenmarkCoast denmarkCoast = new DenmarkCoast("172.29.129.234",
                "bmda22",
                "postgres",
                "wusong",
                25832,
                distanceThreshold,
                "denmark_administrative_national_boundary",
                "geom25832");
        try(BatchFileReader reader = new BatchFileReader(dataFile, ",", true, idIndex);
            PrintWriter writer = new PrintWriter(outFile)
        ){
            writer.write("id,t,label\n");
            for (List<String> ls : reader){
                List<Integer> trueAnchorage = new ArrayList<>();

                List<List<String>> lls = ListGeneric.groupString(ls, s -> s.split(",")[labelIndex]);
                for(int i = 0; i < lls.size(); i++){
                    List<String> ls2 = lls.get(i);
                    String[] firstLine = ls2.get(0).split(",");
                    String[] lastLine = ls2.get(ls2.size()-1).split(",");
                    if(firstLine[labelIndex].startsWith("anchor")){
                        String startTime = firstLine[tIndex];
                        String endTime = lastLine[tIndex];
                        if(TwoTimestamp.diffInSeconds(endTime, startTime, TwoTimestamp.formatter1) >= timeThreshold){
                            double avgX = ListString.average(ls2, e -> Double.parseDouble(e.split(",")[xIndex]));
                            double avgY = ListString.average(ls2, e -> Double.parseDouble(e.split(",")[yIndex]));
                            if(denmarkCoast.within(avgX, avgY)){
                                trueAnchorage.add(i);
                            }
                        }
                    }
                }
                List<Integer> check = ListInteger.splitInterval(trueAnchorage, lls.size());
                for(int i = 0; i + 1 < check.size(); i += 2){
                    List<String> checkTrip = ListListGeneric.flat(lls, check.get(i), check.get(i+1)+1);
                    double avgDistance = ListString.average(checkTrip, e -> Double.parseDouble(e.split(",")[distIndex]));
                    if(avgDistance <= distanceThreshold) {
                        for(int j = check.get(i); j <= check.get(i+1); j++)
                            trueAnchorage.add(j);
                    }
                }
                boolean[] mark = ListInteger.toBoolean(trueAnchorage, lls.size());
                int[] mark2 = Array1DBoolean.falseNegativeTruePositive(mark);
                for(int i = 0; i < lls.size(); i++){
                    String id = lls.get(i).get(0).split(",")[idIndex];
                    String label = (mark2[i] > 0 ? "anchor-" : "sailing-") + Math.abs(mark2[i]);
                    for(String s : lls.get(i)){
                        writer.write(String.join(",", id, s.split(",")[tIndex], label) + "\n");
                    }
                }
            }
        }
    }


    public static void main(String[] args) throws IOException, ParseException, InterruptedException, SQLException {
//        anchorageDetect(baseDir + "fishing-ais-oneweek-filtered-ge1000.csv",
//                baseDir + "density-50.csv");

//        toDenmarkCoast(baseDir + "fishing-ais-oneweek-filtered-ge1000.csv", 8, 0, 1, 4, 5);

        anchorageConnect(baseDir + "fishing-ais-oneweek-filtered-ge1000-denoisedFake-anchorage-withDistanceXY.csv",
                baseDir + "density-50-finalLabel.csv",
                3600,
                100,
                0, 1,13,14,12,11);


    }
}
