package note.task_2021_12_18_segmentation_clustering;

import note.common.ManulLabel;
import note.common.WindowLabel;
import note.common.WindowLabelAid;
import note.common.WindowLabelHelp;
import songwu.deds.trajectory.clean.CriticalTimeStampedPointTConvex;
import songwu.deds.trajectory.clean.DenoiseFakeTimeStampedPointT;
import songwu.deds.trajectory.data.*;
import songwu.deds.trajectory.io.File2TimestampedPointT;
import songwu.deds.trajectory.utility.MyDate;
import songwu.deds.trajectory.utility.Statistics;

import java.io.*;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.*;

public class Task {
    public static void obtainFishAIS() throws IOException {
        HashSet<String> fishMMSI = new HashSet<>(Files.readAllLines(new File("H:\\UpanSky\\DEDS-DataLake\\2021-12-18-segmentation-clustering\\fishing-mmsi-oneweek.txt").toPath()));
        try(PrintWriter writer = new PrintWriter("H:\\UpanSky\\DEDS-DataLake\\2021-12-18-segmentation-clustering\\fishing-ais-oneweek.txt");
            BufferedReader bufferedReader = new BufferedReader(new FileReader("H:\\UpanSky\\DEDS_DenmarkAIS\\aisdk_oneweek.csv"))){
            writer.write("mmsi,t,longitude,latitude\n");
            String line = "";
            while((line = bufferedReader.readLine()) != null){
                if(line.startsWith("mmsi"))
                    continue;
                String[] parts = line.split(",");
                if(fishMMSI.contains(parts[0])){
                    writer.write(parts[0] + "," + parts[1] + "," + parts[2] + "," + parts[3] + "\n");
                }
            }
        }
    }

    public static void denoise() throws IOException, ParseException, InterruptedException {
        String baseDir = "H:\\UpanSky\\DEDS-DataLake\\2021-12-18-segmentation-clustering\\";
//        String pathRaw = baseDir + "fishing-ais-oneweek-filtered-ge1000.csv";
//        String pathDenoised = baseDir + "fishing-ais-oneweek-filtered-ge1000-denoisedFake.csv";
//        String pathRaw = baseDir + "testing-manual-nolabels.csv";
//        String pathDenoised = baseDir + "testing-manual-nolabels-denoisedFake.csv";
//        String pathRaw = baseDir + "training-manual-labels.csv";
//        String pathDenoised = baseDir + "training-manual-labels-denoisedFake.csv";
        String pathRaw = baseDir + "build-testing-sandwich.csv";
        String pathDenoised = baseDir + "build-testing-sandwich-denoisedFake.csv";

        File2TimestampedPointT input = new File2TimestampedPointT();
        input.filePath(pathRaw)
                .splitter(",")
                .withHeader(true)
                .trajId(0)
                .timestamp(1)
                .longitude(2)
                .latitude(3)
                .x(4)
                .y(5);
        List<TimeStampedPointT> trajs = input.go();

//        DenoiseTimeStampedPointT denoiser = new DenoiseTimeStampedPointT();
        DenoiseFakeTimeStampedPointT denoiser = new DenoiseFakeTimeStampedPointT();
        denoiser.setAngleThreshold(160).setHistory(7).setNumberThreads(4)
                .setSpeedMax(50).setSpeenMin(1).setTrajs(trajs)
                .setTurnThreshold(5).setSpeedAlpha(0.25);
        List<TimeStampedPointT> denoised = denoiser.go();

        try(PrintWriter writer = new PrintWriter(pathDenoised);){
            writer.write("id,t,longitude,latitude,x,y,time_gap,distance_gap,euc_speed,signed_turn,bearing,pause,speed_change,turn\n");
            for(TimeStampedPointT traj : denoised){
                for(TimeStampedPoint p : traj.getAllUnits()){
                    writer.write(traj.trajId() + ",");
                    writer.write(p.getTimestamp() + ",");
                    writer.write(p.getLongitude() + ",");
                    writer.write(p.getLatitude() + ",");
                    writer.write(p.getX() + ",");
                    writer.write(p.getY() + ",");
                    writer.write(p.getTimeGap() + ",");
                    writer.write(p.getDistanceGap() + ",");
                    writer.write(p.getEucSpeed() + ",");
                    writer.write(p.getSignedTurn() + ",");
                    writer.write(p.getBearing() + ",");
                    writer.write(p.isPaused() + ",");
                    writer.write(p.isSpeedchanged() + ",");
                    writer.write(p.isTurned() + "\n");
                }
            }
        }
    }

    public static int indexStartWith(List<String> ss, String prefix){
        for(int i = 0; i < ss.size(); i++){
            if(ss.get(i).startsWith(prefix))
                return i;
        }
        return -1;
    }

    public static int indexStartWithFromRight(List<String> ss, String prefix){
        for(int i = ss.size() - 1; i >= 0; i--){
            if(ss.get(i).startsWith(prefix))
                return i;
        }
        return -1;
    }

    public static void toWindowTraining() throws IOException, ParseException {
        String baseDir = "H:\\UpanSky\\DEDS-DataLake\\2021-12-18-segmentation-clustering\\";

        File file = new File(baseDir + "manual_label.csv");
        List<ManulLabel> labels = new ArrayList<>();
        List<String> lines = Files.readAllLines(file.toPath());
        for(String line : lines.subList(1, lines.size())){
            labels.add(new ManulLabel(line));
        }

        File dataFile = new File(baseDir + "fishing-ais-oneweek-filtered-ge1000.csv");
        List<String> data = Files.readAllLines(dataFile.toPath());

        try(PrintWriter writer = new PrintWriter(baseDir + "training-manual-labels.csv")){
            writer.write("mmsi,t,longitude,latitude,x,y,label\n");
            Map<String, Integer> mmsi2identifier = new HashMap<>();
            for(ManulLabel ml : labels){
                int start = indexStartWith(data, ml.mmsi + "," + ml.startTime);
                int end = indexStartWith(data, ml.mmsi + "," + ml.endTime);

                int duration = 0;
                int numPoints = 1;
                int currentStart = start;
                int currentEnd = start;
                while (currentEnd <= end){
                    if(numPoints >= 300 && duration >= 3600){
                        int counter = mmsi2identifier.getOrDefault(ml.mmsi, 0);
                        System.out.println(ml.mmsi + " " + currentStart + " " + currentEnd);
                        for(String line : data.subList(currentStart, currentEnd + 1)){
                            writer.write(line.replace(ml.mmsi, ml.mmsi + "-" + counter) + "," + ml.label + "\n");
                        }

                        currentStart = currentEnd - (currentEnd - currentStart) / 3;
                        currentEnd = currentStart;

                        duration = 0;
                        numPoints = 1;
                        mmsi2identifier.put(ml.mmsi, ++counter);
                        continue;
                    }
                    currentEnd++;
                    if(currentEnd == data.size())
                        break;
                    numPoints++;
                    duration += MyDate.timestampDiff(data.get(currentEnd).split(",")[1],
                            data.get(currentEnd-1).split(",")[1]);
                }
            }
        }
    }

    public static void toWindowTesting() throws IOException, ParseException {
        String baseDir = "H:\\UpanSky\\DEDS-DataLake\\2021-12-18-segmentation-clustering\\";
        Set<String> mmsiTrain = new HashSet<>();
        Set<String> mmsiAll = new HashSet<>();

        List<String> lines = Files.readAllLines(new File(baseDir + "manual_label.csv").toPath());
        for(String line : lines.subList(1, lines.size())){
            mmsiTrain.add(line.split(",")[0]);
        }
        mmsiAll.addAll(Files.readAllLines(new File(baseDir + "fishing-mmsi-oneweek.csv").toPath()));
        File dataFile = new File(baseDir + "fishing-ais-oneweek-filtered-ge1000.csv");
        List<String> data = Files.readAllLines(dataFile.toPath());

        try(PrintWriter writer = new PrintWriter(baseDir + "testing-manual-nolabels.csv")){
            writer.write("mmsi,t,longitude,latitude,x,y\n");
            for(String mmsi : mmsiAll){
                if(mmsiTrain.contains(mmsi))
                    continue;
                int start = indexStartWith(data, mmsi);
                int end = indexStartWithFromRight(data, mmsi);
                int counter = 0;
                int duration = 0;
                int numPoints = 1;
                int currentStart = start;
                int currentEnd = start;
                while (currentEnd <= end){
                    if(numPoints >= 300 && duration >= 3600){
                        for(String line : data.subList(currentStart, currentEnd + 1)){
                            writer.write(line.replace(mmsi, mmsi + "-" + counter) + "\n");
                        }
                        currentStart = currentEnd - (currentEnd - currentStart) / 3;
                        currentEnd = currentStart;
                        duration = 0;
                        numPoints = 1;
                        counter++;
                        continue;
                    }
                    currentEnd++;
                    numPoints++;
                    if(currentEnd > end)
                        break;
                    duration += MyDate.timestampDiff(data.get(currentEnd).split(",")[1],
                            data.get(currentEnd-1).split(",")[1]);
                }
            }
        }
    }

    public static void criticalConvex() throws InterruptedException, IOException, ParseException{
        String baseDir = "H:\\UpanSky\\DEDS-DataLake\\2021-12-18-segmentation-clustering\\";
//        String pathRaw = baseDir + "testing-manual-nolabels.csv";
//        String pathCritical = baseDir + "testing-manual-nolabels-critical.csv";
//        String pathCriticalInterval = baseDir + "testing-manual-nolabels-criticalInterval.csv";
        String pathRaw = baseDir + "build-testing-sandwich.csv";
        String pathCritical = baseDir + "build-testing-sandwich-critical.csv";
        String pathCriticalInterval = baseDir + "build-testing-sandwich-criticalInterval.csv";

        File2TimestampedPointT input = new File2TimestampedPointT();
        input.filePath(pathRaw)
                .splitter(",")
                .withHeader(true)
                .trajId(0)
                .timestamp(1)
                .longitude(2)
                .latitude(3)
                .x(4)
                .y(5);
        List<TimeStampedPointT> trajs = input.go();

        DenoiseFakeTimeStampedPointT denoiser = new DenoiseFakeTimeStampedPointT();
        denoiser.setAngleThreshold(160).setHistory(7).setNumberThreads(4)
                .setSpeedMax(50).setSpeenMin(1).setTrajs(trajs)
                .setTurnThreshold(5).setSpeedAlpha(0.25);
        List<TimeStampedPointT> denoised = denoiser.go();

        CriticalTimeStampedPointTConvex criticaler = new CriticalTimeStampedPointTConvex();
        criticaler.setHistory(7).setGap(1800)
                .setSmoothThreshold(10).setSpeedAlpha(0.25)
                .setSpeedMin(1).setSpeedSlowMotion(5).setTrajs(denoised).setNumberThreads(4);
        List<CriticalPointT> criticaled = criticaler.go();

        CriticalPointT.saveCriticalPointT(pathCritical, criticaled);
        CriticalPointInterval.saveIntervals(pathCriticalInterval, criticaler.getCriticalIntervals());
    }

    public static void buildFishingParts() throws IOException {
        List<WindowLabel> windowLabels = new ArrayList<>();
        List<String> lines = Files.readAllLines(new File("H:\\UpanSky\\DEDS-DataLake\\2021-12-18-segmentation-clustering\\testing-features.csv").toPath());
        for(String line : lines.subList(1, lines.size())){
            String parts[] = line.split(",");
            windowLabels.add(new WindowLabel(parts[0], parts[22]));
        }
        windowLabels.sort(Comparator.comparing(WindowLabel::getWholeId).thenComparing(WindowLabel::getPartId));
//        for(WindowLabel windowLabel : windowLabels)
//            System.out.println(windowLabel);


        List<WindowLabelHelp> windowLabelHelps = new ArrayList<>();
        String currentWhole = windowLabels.get(0).getWholeId();
        String currentLabel = windowLabels.get(0).label;
        int start = 0;
        int end = 0;
        for(int i = 1; i < windowLabels.size(); i++){
            String whole = windowLabels.get(i).getWholeId();
            String label = windowLabels.get(i).label;
            if((! whole.equals(currentWhole)) ||
                    (! label.equals(currentLabel))){
                windowLabelHelps.add(new WindowLabelHelp(currentWhole, currentLabel, start, end));
                currentWhole = whole;
                currentLabel = label;
                start = end = i;
            }else{
                end = i;
            }
        }
        windowLabelHelps.add(new WindowLabelHelp(currentWhole, currentLabel, start, end));
//        for(WindowLabelHelp help : windowLabelHelps)
//            System.out.println(help);
//        try(PrintWriter writer = new PrintWriter("H:\\UpanSky\\DEDS-DataLake\\2021-12-18-segmentation-clustering\\testing-run-length-encoding.csv")){
//            writer.write("mmsi,label,size,order\n");
//            int counter = 0;
//            for(WindowLabelHelp help : windowLabelHelps){
//                counter++;
//                writer.write(help.whole + "," + help.label + "," + help.size + "," + counter + "\n");
//            }
//        }

        List<List<WindowLabelHelp>> helpss = new ArrayList<>();
        helpss.add(new ArrayList<>());
        helpss.get(helpss.size() - 1).add(windowLabelHelps.get(0));
        currentWhole = windowLabelHelps.get(0).whole;
        for(WindowLabelHelp help : windowLabelHelps.subList(1, windowLabelHelps.size())){
            if(! help.whole.equals(currentWhole)){
                helpss.add(new ArrayList<>());
                currentWhole = help.whole;
            }
            helpss.get(helpss.size() - 1).add(help);
        }
//        for(List<WindowLabelHelp> helps : helpss)
//            System.out.println(helps.get(0).whole + " " + helps.size());
        List<WindowLabelAid> windowLabelAids = new ArrayList<>();
        for(List<WindowLabelHelp> helps : helpss){
            currentWhole = helps.get(0).whole;
            boolean[] kernelAvg = new boolean[helps.size()];
            for(int i = 0; i < kernelAvg.length; i++){
                int total = 0;
                for(int ii = Math.max(i - 2, 0); ii <= Math.min(i + 2, kernelAvg.length - 1); ii++){
                    total += helps.get(ii).magicNumber();
                }
                kernelAvg[i] = total > 0;
            }

            List<int[]> fishing = Statistics.select(kernelAvg);
            for(int[] ints : fishing){
                windowLabelAids.add(new WindowLabelAid(currentWhole, helps.get(ints[0]).start, helps.get(ints[1]).end));
            }
        }
        System.out.println(windowLabelAids.size());
        for(WindowLabelAid aid : windowLabelAids)
            System.out.println(aid);

//        List<WindowLabelAid> windowLabelAids = new ArrayList<>();
//        int quantity = 2;
//        int currentPos = 0;
//        while (currentPos < windowLabelHelps.size() && (windowLabelHelps.get(currentPos).label.equals("sailing")))
//            currentPos++;
//        if(currentPos == windowLabelHelps.size())
//            return;
//        currentWhole = windowLabelHelps.get(currentPos).whole;
//        int endPos = currentPos;
//        while (currentPos < windowLabelHelps.size()){
//            while (endPos + 2 < windowLabelHelps.size()){
//                WindowLabelHelp help__ = windowLabelHelps.get(endPos);
//                WindowLabelHelp help_ = windowLabelHelps.get(endPos + 1);
//                WindowLabelHelp help = windowLabelHelps.get(endPos + 2);
//                if(help.whole.equals(currentWhole) && help.label.equals("fishing") && help.size >= help_.size && help__.size >= help_.size){
//                    endPos+=2;
//                }else {
//                    break;
//                }
//            }
//            int total = 0;
//            for(int i = currentPos; i <= endPos; i++)
//                total += windowLabelHelps.get(i).size;
//            if(total >= quantity){
//                windowLabelAids.add(new WindowLabelAid(currentWhole,
//                        windowLabelHelps.get(currentPos).start,
//                        windowLabelHelps.get(endPos).end));
//            }
//
//            currentPos = endPos + 1;
//            while (currentPos < windowLabelHelps.size() && (windowLabelHelps.get(currentPos).label.equals("sailing")))
//                currentPos++;
//            if(currentPos == windowLabelHelps.size())
//                break;
//            currentWhole = windowLabelHelps.get(currentPos).whole;
//            endPos = currentPos;
//        }
//        System.out.println(windowLabelAids.size());
//        for(WindowLabelAid aid : windowLabelAids)
//            System.out.println(aid.whole + " " + windowLabels.get(aid.start).getPartId() + " " + windowLabels.get(aid.end).getPartId());


        try(PrintWriter writer = new PrintWriter("H:\\UpanSky\\DEDS-DataLake\\2021-12-18-segmentation-clustering\\build-testing-kernel-average.csv")){
            writer.write("mmsi,t,longitude,latitude,x,y\n");
            lines = Files.readAllLines(new File("H:\\UpanSky\\DEDS-DataLake\\2021-12-18-segmentation-clustering\\testing-manual-nolabels.csv").toPath());
            Map<String, Integer> windowStart = new HashMap<>();
            Map<String, Integer> windowEnd = new HashMap<>();
            String line = lines.get(1);
            String key = line.substring(0, line.indexOf(","));
            windowStart.put(key, 1);
            for(int i = 2; i < lines.size(); i++){
                line = lines.get(i);
                String magic = line.substring(0, line.indexOf(","));
                if(! magic.equals(key)){
                    windowEnd.put(key, i - 1);
                    windowStart.put(magic, i);
                    key = magic;
                }
            }
            windowEnd.put(key, lines.size() - 1);
            int counter = 0;
            for(WindowLabelAid aid : windowLabelAids){
                start = windowStart.get(aid.whole + "-" + windowLabels.get(aid.start).getPartId());
                end = windowEnd.get(aid.whole + "-" + windowLabels.get(aid.end).getPartId());
                System.out.println(aid.whole + " " + start + " " + end);
                for(int i = start; i <= end; i++){
                    line = lines.get(i);
                    writer.write(line.replace(line.substring(0,line.indexOf(",")), aid.whole + "-" + counter) + "\n");
                }
                counter++;
            }
        }
    }

    public static void main(String[] args) throws IOException, ParseException, InterruptedException {
//        obtainFishAIS();
//        denoise();
//        criticalConvex();
//        toWindowTraining();
//        toWindowTesting();
        buildFishingParts();

    }
}
