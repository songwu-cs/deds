package note.task_2022_01_21_paper_draft;

import calculation.ListGeneric;
import calculation.ListString;
import io.bigdata.BatchFileReader;
import note.common.ManulLabel;
import note.common.WindowLabel;
import note.common.WindowLabelAid;
import note.common.WindowLabelHelp;
import songwu.deds.trajectory.clean.CriticalTimeStampedPointTConvexFinerSpeed;
import songwu.deds.trajectory.clean.CriticalTimeStampedPointTConvexFinerSpeed10Plus;
import songwu.deds.trajectory.clean.DenoiseFakeTimeStampedPointT;
import songwu.deds.trajectory.data.CriticalPointInterval;
import songwu.deds.trajectory.data.CriticalPointT;
import songwu.deds.trajectory.data.TimeStampedPointT;
import songwu.deds.trajectory.io.File2TimestampedPointT;
import songwu.deds.trajectory.utility.MyDate;
import songwu.deds.trajectory.utility.Statistics;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

public class Task {
    public static final String baseDir = "H:\\UpanSky\\DEDS-DataLake\\2022-01-21-paper-draft\\";
    public static List<TimeStampedPointT> preloaded;

    public static void t1() throws IOException, ParseException {
        toTrainTestWindows(baseDir + "deanchorage_noise2.csv",
                baseDir + "ground-truth.csv",
                baseDir + "deanchorage_noise2_allTrips.csv",
                baseDir + "window_training.csv",
                baseDir + "window_testing.csv",
                new GenerateTraingStrategy(300,3600,1.0/6));
    }

    public static void t2() throws IOException, ParseException, InterruptedException {
        toCritical(baseDir + "deanchorage_noise2.csv",
                baseDir + "window_training.csv",
                baseDir + "window_training_intervals.csv");
        toCritical(baseDir + "deanchorage_noise2.csv",
                baseDir + "window_testing.csv",
                baseDir + "window_testing_intervals.csv");
    }

    public static void t3() throws IOException, ParseException {
        toFeatures(baseDir + "window_training_intervals.csv", baseDir + "window_training_intervals_features.csv");
        toFeatures(baseDir + "window_testing_intervals.csv", baseDir + "window_testing_intervals_features.csv");
    }

    public static void t4() throws IOException {
        buildFishingParts(baseDir + "window_testing_intervals_features.csv",//预测的标签
                baseDir + "build_labels.csv",//保存路径
                baseDir + "build_encoding.csv",//变长编码
                baseDir + "deanchorage_noise2_mini.csv",//ais数据集
                baseDir + "window_testing.csv",
                35);//窗口的开始结束时间
    }

    public static void toTrainTestWindows(String dataFile, String truthFile, String tripsFile, String outputTrain, String outputTest, GenerateTraingStrategy strategy) throws IOException, ParseException {
        List<String> data = Files.readAllLines(Paths.get(dataFile));
        List<String> truth = Files.readAllLines(Paths.get(truthFile));
        List<String> trips = Files.readAllLines(Paths.get(tripsFile));

        List<ManulLabel> labels = new ArrayList<>();
        for(String line : truth.subList(1, truth.size())){
            labels.add(new ManulLabel(line));
        }
        for(String trip : trips){
            if(ListString.indexStartWith(truth, trip) < 0){
                int left = ListString.indexStartWith(data, trip);
                int right = ListString.indexStartWithFromRight(data, trip);
                labels.add(new ManulLabel(String.join(",",
                        trip,
                        data.get(left).split(",")[1],
                        data.get(right).split(",")[1],
                        "test"
                )));
            }
        }

        try(PrintWriter writeTrain = new PrintWriter(outputTrain);
            PrintWriter writeTest = new PrintWriter(outputTest)){
            Map<String, Integer> mmsi2identifier = new HashMap<>();
            writeTrain.write("id,startTime,endTime,label\n");
            writeTest.write("id,startTime,endTime\n");
            for(ManulLabel ml : labels){
                List<String[]> train = new ArrayList<>();
                List<String[]> test = new ArrayList<>();

                int start = ListString.indexStartWith(data, ml.mmsi + "," + ml.startTime);
                int end = ListString.indexStartWith(data, ml.mmsi + "," + ml.endTime);

                int duration = 0;
                int numPoints = 1;
                int currentStart = start;
                int currentEnd = start;
                while (currentEnd <= end){
                    if(numPoints >= strategy.numPoints && duration >= strategy.timeGap){
                        int counter = mmsi2identifier.getOrDefault(ml.mmsi, 0);
                        System.out.println(ml.mmsi + " " + currentStart + " " + currentEnd);

                        if(ml.label.equals("test")){
                            test.add(new String[]{
                                    ml.mmsi + "-" + counter,
                                    data.get(currentStart).split(",")[1],
                                    data.get(currentEnd).split(",")[1]});
                        }else {
                            train.add(new String[]{
                                    ml.mmsi + "-" + counter,
                                    data.get(currentStart).split(",")[1],
                                    data.get(currentEnd).split(",")[1],
                                    ml.label
                            });
                        }

                        currentStart = currentStart + (int)((currentEnd - currentStart) * strategy.stepsize);
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

                if(ml.label.equals("test")){
                    test.get(test.size() - 1)[2] = data.get(end).split(",")[1];
                }else {
                    if(train.size() > 0)
                        train.get(train.size() - 1)[2] = data.get(end).split(",")[1];
                    else {
                        train.add(new String[]{ml.mmsi + "-0", data.get(start).split(",")[1],
                                                data.get(end).split(",")[1], ml.label});
                        mmsi2identifier.put(ml.mmsi, 1);
                    }
                }
                for(String[] sa : train){
                    writeTrain.write(String.join(",", sa) + "\n");
                }
                for(String[] sa : test){
                    writeTest.write(String.join(",", sa) + "\n");
                }
            }
        }
    }

    public static void toCritical(String dataFile, String windowFile, String intervalFile) throws IOException, ParseException, InterruptedException {
        List<TimeStampedPointT> denoised;
        if(preloaded != null)
            denoised = preloaded;
        else {
            File2TimestampedPointT input = new File2TimestampedPointT();
            input.filePath(dataFile)
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
            denoised = denoiser.go();
            preloaded = denoised;
        }

        List<String> lines = Files.readAllLines(Paths.get(windowFile));
        List<TimeStampedPointT> windowed = new ArrayList<>();
        try(PrintWriter writer = new PrintWriter(intervalFile)){
            writer.write("rank,type,startTime,endTime,id" + "\n");
            for(String line : lines.subList(1, lines.size())){
                String[] parts = line.split(",");
                String id = parts[0];
                String sTime = parts[1];
                String eTime = parts[2];
                String magic = id.contains("-") ? id.substring(0,id.lastIndexOf("-")) : id;
                TimeStampedPointT traj = denoised.get(ListGeneric.firstIndex(denoised, e -> e.trajId().equals(magic)));
                int start = ListGeneric.firstIndex(traj.getAllUnits(), e -> e.getTimestamp().equals(sTime));
                int end = ListGeneric.firstIndex(traj.getAllUnits(), e -> e.getTimestamp().equals(eTime));
                System.out.println(start + " " + end);
                windowed.add(traj.subTraj(start, end + 1).setId(id));
            }
        }

        CriticalTimeStampedPointTConvexFinerSpeed criticaler = new CriticalTimeStampedPointTConvexFinerSpeed();
        criticaler.setHistory(7).setGap(1800)
                .setSmoothThreshold(10).setSpeedAlpha(0.25)
                .setSpeedMin(1).setSpeedSlowMotion(5).setTrajs(windowed).setNumberThreads(4);
        criticaler.go();

        CriticalPointInterval.saveIntervals(intervalFile, criticaler.getCriticalIntervals());
    }

    public static void toCritical10Plus(String dataFile, String windowFile, String intervalFile) throws IOException, ParseException, InterruptedException {
        List<TimeStampedPointT> denoised;
        if(preloaded != null)
            denoised = preloaded;
        else {
            File2TimestampedPointT input = new File2TimestampedPointT();
            input.filePath(dataFile)
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
            denoised = denoiser.go();
            preloaded = denoised;
        }

        List<String> lines = Files.readAllLines(Paths.get(windowFile));
        List<TimeStampedPointT> windowed = new ArrayList<>();
        try(PrintWriter writer = new PrintWriter(intervalFile)){
            writer.write("rank,type,startTime,endTime,id" + "\n");
            for(String line : lines.subList(1, lines.size())){
                String[] parts = line.split(",");
                String id = parts[0];
                String sTime = parts[1];
                String eTime = parts[2];
                String magic = id.contains("-") ? id.substring(0,id.lastIndexOf("-")) : id;
                TimeStampedPointT traj = denoised.get(ListGeneric.firstIndex(denoised, e -> e.trajId().equals(magic)));
                int start = ListGeneric.firstIndex(traj.getAllUnits(), e -> e.getTimestamp().equals(sTime));
                int end = ListGeneric.firstIndex(traj.getAllUnits(), e -> e.getTimestamp().equals(eTime));
                System.out.println(start + " " + end);
                windowed.add(traj.subTraj(start, end + 1).setId(id));
            }
        }

        CriticalTimeStampedPointTConvexFinerSpeed10Plus criticaler = new CriticalTimeStampedPointTConvexFinerSpeed10Plus();
        criticaler.setHistory(7).setGap(1800)
                .setSmoothThreshold(10).setSpeedAlpha(0.25)
                .setSpeedMin(1).setSpeedSlowMotion(5).setTrajs(windowed).setNumberThreads(1);
        criticaler.go();

        CriticalPointInterval.saveIntervals(intervalFile, criticaler.getCriticalIntervals());
    }

    public static void criticalConvex() throws InterruptedException, IOException, ParseException{
        String pathRaw = baseDir + "paper-220051000-ais.csv";
        String pathCritical = baseDir + "paper-220051000-ais-critical.csv";
        String pathCriticalInterval = baseDir + "paper-220051000-ais-criticalInterval.csv";

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

        CriticalTimeStampedPointTConvexFinerSpeed criticaler = new CriticalTimeStampedPointTConvexFinerSpeed();
        criticaler.setHistory(7).setGap(1800)
                .setSmoothThreshold(10).setSpeedAlpha(0.25)
                .setSpeedMin(1).setSpeedSlowMotion(5).setTrajs(denoised).setNumberThreads(4);
        List<CriticalPointT> criticaled = criticaler.go();

        CriticalPointT.saveCriticalPointT(pathCritical, criticaled);
        CriticalPointInterval.saveIntervals(pathCriticalInterval, criticaler.getCriticalIntervals());
    }

    public static void toFeatures(String intervalFile, String outFile) throws IOException, ParseException {
        String[] events =  new String[]{"gap","stop","slowMotion","speed5to6","speed6to7","speed7to8","speed8to9","speed9to10"};
        String[] attrs = new String[]{"NumberAvg","TimeAvg","TimeTotalAvg","TimeSpanAvg"};
        try(PrintWriter writer = new PrintWriter(outFile);
            BatchFileReader batchFileReader = new BatchFileReader(intervalFile, ",", true, 4)) {
            String header = "id,speedchangeavg,turnavg";
            for(String _ : events) {
                for(String __ : attrs){
                    header += ("," + _ + __);
                }
            }
            writer.write(header + "\n");

            for(List<String> lines : batchFileReader){
                String key = lines.get(0).split(",")[4];
                Map<String, List<CriticalPointInterval>> value = new HashMap<>();
                for(String line : lines){
                    String[] parts = line.split(",");
                    if(! value.containsKey(parts[1]))
                        value.put(parts[1], new ArrayList<>());
                    value.get(parts[1]).add(new CriticalPointInterval()
                            .setId(parts[4])
                            .setType(parts[1])
                            .setRank(parts[0])
                            .setStartTime(parts[2])
                            .setEndTime(parts[3])
                    );
                }

                double windowSize = CriticalPointInterval.spanDuration(value.get("trip"));
                writer.write(key + ",");
                writer.write(value.getOrDefault("speedChange", new ArrayList<>()).size() / windowSize + ",");
                writer.write(value.getOrDefault("smooth_turn", new ArrayList<>()).size() / windowSize + "");
                for(String _ : events){
                    List<CriticalPointInterval> lc = value.getOrDefault(_, new ArrayList<>());
                    writer.write("," + lc.size() / windowSize);
                    writer.write("," + CriticalPointInterval.avgDuration(lc));
                    writer.write("," + CriticalPointInterval.totalDuration(lc) / windowSize);
                    writer.write("," + CriticalPointInterval.spanDuration(lc) / windowSize);
                }
                writer.write("\n");
            }
        }

    }

    public static void toFeatures10Plus(String intervalFile, String outFile) throws IOException, ParseException {
        String[] events =  new String[]{"gap","stop","slowMotion","speed5to6","speed6to7","speed7to8","speed8to9","speed9to10","speed10Plus"};
        String[] attrs = new String[]{"NumberAvg","TimeAvg","TimeTotalAvg","TimeSpanAvg"};
        try(PrintWriter writer = new PrintWriter(outFile);
            BatchFileReader batchFileReader = new BatchFileReader(intervalFile, ",", true, 4)) {
            String header = "id,speedchangeavg,turnavg";
            for(String _ : events) {
                for(String __ : attrs){
                    header += ("," + _ + __);
                }
            }
            writer.write(header + "\n");

            for(List<String> lines : batchFileReader){
                String key = lines.get(0).split(",")[4];
                Map<String, List<CriticalPointInterval>> value = new HashMap<>();
                for(String line : lines){
                    String[] parts = line.split(",");
                    if(! value.containsKey(parts[1]))
                        value.put(parts[1], new ArrayList<>());
                    value.get(parts[1]).add(new CriticalPointInterval()
                            .setId(parts[4])
                            .setType(parts[1])
                            .setRank(parts[0])
                            .setStartTime(parts[2])
                            .setEndTime(parts[3])
                    );
                }

                double windowSize = CriticalPointInterval.spanDuration(value.get("trip"));
                writer.write(key + ",");
                writer.write(value.getOrDefault("speedChange", new ArrayList<>()).size() / windowSize + ",");
                writer.write(value.getOrDefault("smooth_turn", new ArrayList<>()).size() / windowSize + "");
                for(String _ : events){
                    List<CriticalPointInterval> lc = value.getOrDefault(_, new ArrayList<>());
                    writer.write("," + lc.size() / windowSize);
                    writer.write("," + CriticalPointInterval.avgDuration(lc));
                    writer.write("," + CriticalPointInterval.totalDuration(lc) / windowSize);
                    writer.write("," + CriticalPointInterval.spanDuration(lc) / windowSize);
                }
                writer.write("\n");
            }
        }

    }

    public static void buildFishingParts(String testPath, String buildPath, String runlengthFile, String dataFile, String testWindows, int pythonLabelPos) throws IOException {
        List<WindowLabel> windowLabels = new ArrayList<>();
        List<String> lines = Files.readAllLines(new File(testPath).toPath());
        for(String line : lines.subList(1, lines.size())){
            String parts[] = line.split(",");
            windowLabels.add(new WindowLabel(parts[0], parts[pythonLabelPos]));
        }
        windowLabels.sort(Comparator.comparing(WindowLabel::getWholeId).thenComparing(WindowLabel::getPartId));


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
        try(PrintWriter writer = new PrintWriter(runlengthFile)){
            writer.write("mmsi,label,size,order\n");
            int counter = 0;
            for(WindowLabelHelp help : windowLabelHelps){
                counter++;
                writer.write(help.whole + "," + help.label + "," + help.size + "," + counter + "\n");
            }
        }

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


        List<WindowLabelAid> windowLabelAidsMovingAvg = new ArrayList<>();
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
                windowLabelAidsMovingAvg.add(new WindowLabelAid(currentWhole, helps.get(ints[0]).start, helps.get(ints[1]).end));
            }
        }
//        System.out.println(windowLabelAidsMovingAvg.size());
//        for(WindowLabelAid aid : windowLabelAidsMovingAvg)
//            System.out.println(aid);


        List<WindowLabelAid> windowLabelAidsSandwich = new ArrayList<>();
        int quantity = 17;
        int currentPos = 0;
        while (currentPos < windowLabelHelps.size() && (windowLabelHelps.get(currentPos).label.equals("sailing")))
            currentPos++;
        if(currentPos == windowLabelHelps.size())
            return;
        currentWhole = windowLabelHelps.get(currentPos).whole;
        int endPos = currentPos;
        while (currentPos < windowLabelHelps.size()){
            while (endPos + 2 < windowLabelHelps.size()){
                WindowLabelHelp help__ = windowLabelHelps.get(endPos);
                WindowLabelHelp help_ = windowLabelHelps.get(endPos + 1);
                WindowLabelHelp help = windowLabelHelps.get(endPos + 2);
                if(help.whole.equals(currentWhole) && help.label.equals("fishing") && help.size >= help_.size && help__.size >= help_.size){
                    endPos+=2;
                }else {
                    break;
                }
            }
            int total = 0;
            for(int i = currentPos; i <= endPos; i++)
                total += windowLabelHelps.get(i).size;
            if(total >= quantity){
                windowLabelAidsSandwich.add(new WindowLabelAid(currentWhole,
                        windowLabelHelps.get(currentPos).start,
                        windowLabelHelps.get(endPos).end));
            }

            currentPos = endPos + 1;
            while (currentPos < windowLabelHelps.size() && (windowLabelHelps.get(currentPos).label.equals("sailing")))
                currentPos++;
            if(currentPos == windowLabelHelps.size())
                break;
            currentWhole = windowLabelHelps.get(currentPos).whole;
            endPos = currentPos;
        }
//        System.out.println(windowLabelAidsSandwich.size());
//        for(WindowLabelAid aid : windowLabelAidsSandwich)
//            System.out.println(aid.whole + " " + windowLabels.get(aid.start).getPartId() + " " + windowLabels.get(aid.end).getPartId());


        List<String> truth = Files.readAllLines(Paths.get(testWindows));
        List<ManulLabel> labels = new ArrayList<>();
        for(String line : truth.subList(1, truth.size())){
            labels.add(new ManulLabel(line + ",dummy"));
        }
        List<ManulLabel> forMovingAverage = new ArrayList<>();
        List<ManulLabel> forSandwich = new ArrayList<>();
        for(WindowLabelAid aid : windowLabelAidsMovingAvg){
            WindowLabel sl = windowLabels.get(aid.start);
            WindowLabel el = windowLabels.get(aid.end);
            boolean sRight = aid.start > 0 && sl.getWholeId().equals(windowLabels.get(aid.start-1).getWholeId());
            boolean eLeft = aid.end < windowLabels.size() - 1 && el.getWholeId().equals(windowLabels.get(aid.end+1).getWholeId());

            WindowLabel sl2 = sRight ? windowLabels.get(aid.start - 1) : sl;
            ManulLabel ssl = labels.get(ListGeneric.firstIndex(labels, e -> e.mmsi.equals(sl2.id)));
            String sTime = sRight ? ssl.endTime : ssl.startTime;
            WindowLabel el2 = eLeft ? windowLabels.get(aid.end + 1) : el;
            ManulLabel eel = labels.get(ListGeneric.firstIndex(labels, e -> e.mmsi.equals(el2.id)));
            String eTime = eLeft ? eel.startTime : eel.endTime;

            forMovingAverage.add(new ManulLabel(String.join(",", aid.whole, sTime, eTime, "dummy")));
        }
        for(WindowLabelAid aid : windowLabelAidsSandwich){
            WindowLabel sl = windowLabels.get(aid.start);
            WindowLabel el = windowLabels.get(aid.end);
            boolean sRight = aid.start > 0 && sl.getWholeId().equals(windowLabels.get(aid.start-1).getWholeId());
            boolean eLeft = aid.end < windowLabels.size() - 1 && el.getWholeId().equals(windowLabels.get(aid.end+1).getWholeId());

            WindowLabel sl2 = sRight ? windowLabels.get(aid.start - 1) : sl;
            ManulLabel ssl = labels.get(ListGeneric.firstIndex(labels, e -> e.mmsi.equals(sl2.id)));
            String sTime = sRight ? ssl.endTime : ssl.startTime;
            WindowLabel el2 = eLeft ? windowLabels.get(aid.end + 1) : el;
            ManulLabel eel = labels.get(ListGeneric.firstIndex(labels, e -> e.mmsi.equals(el2.id)));
            String eTime = eLeft ? eel.startTime : eel.endTime;

            forSandwich.add(new ManulLabel(String.join(",", aid.whole, sTime, eTime, "dummy")));
        }
//        try(PrintWriter writer = new PrintWriter(baseDir + "build_fishing_start_end_time_kernel.csv")){
//            writer.write("id,startTime,endTime,order\n");
//            int counter = 0;
//            for(ManulLabel label : forMovingAverage){
//                writer.write(String.join(",", label.mmsi, label.startTime, label.endTime, counter+"") + "\n");
//                counter++;
//            }
//        }
//        try(PrintWriter writer = new PrintWriter(baseDir + "build_fishing_start_end_time_sandwich.csv")){
//            writer.write("id,startTime,endTime,order\n");
//            int counter = 0;
//            for(ManulLabel label : forSandwich){
//                writer.write(String.join(",", label.mmsi, label.startTime, label.endTime, counter+"") + "\n");
//                counter++;
//            }
//        }
        for(int i = 0; i < forMovingAverage.size() - 1; i++){
            ManulLabel m1 = forMovingAverage.get(i);
            ManulLabel m2 = forMovingAverage.get(i+1);
            if(m1.mmsi.equals(m2.mmsi) && m1.endTime.compareTo(m2.startTime) >= 0)
                System.out.println(m1 + "\n" + m2);
        }
        for(int i = 0; i < forSandwich.size() - 1; i++){
            ManulLabel m1 = forSandwich.get(i);
            ManulLabel m2 = forSandwich.get(i+1);
            if(m1.mmsi.equals(m2.mmsi) && m1.endTime.compareTo(m2.startTime) >= 0)
                System.out.println(m1 + "\n" + m2);
        }


        Set<String> tests = new HashSet<>();
        for(WindowLabel label : windowLabels)
            tests.add(label.getWholeId());
        try(PrintWriter writer = new PrintWriter(buildPath)){
            List<String> aiss = Files.readAllLines(Paths.get(dataFile));
            writer.write(aiss.get(0) + ",sandwich,kernel\n");
            int lastSandwich = 0;
            int lastKernel = 0;
            for(String ais : aiss.subList(1, aiss.size())){
                String[] parts = ais.split(",");
                if(! tests.contains(parts[0]))
                    continue;

                int indiceSandwich = ListGeneric.firstIndex(forSandwich, e -> e.mmsi.equals(parts[0]) &&
                        e.startTime.compareTo(parts[1]) <= 0 && e.endTime.compareTo(parts[1]) >= 0);
                lastSandwich = indiceSandwich >= 0 ? indiceSandwich + 1: lastSandwich;
                int indiceKernel = ListGeneric.firstIndex(forMovingAverage, e -> e.mmsi.equals(parts[0]) &&
                        e.startTime.compareTo(parts[1]) <= 0 && e.endTime.compareTo(parts[1]) >= 0);
                lastKernel = indiceKernel >= 0 ? indiceKernel + 1: lastKernel;

                int countSandwich = indiceSandwich < 0 ? 2 * lastSandwich + 1 : 2 * lastSandwich;
                int countKernel = indiceKernel < 0 ? 2 * lastKernel + 1 : 2 * lastKernel;
                writer.write(String.join(",",
                        ais,
                        String.format("%03d", countSandwich) + "-" + (indiceSandwich == -1 ? ("sailing-" + lastSandwich) : ("fishing-" + indiceSandwich)),
                        String.format("%03d", countKernel) + "-" + (indiceKernel == -1 ? ("sailing-" + lastKernel) : ("fishing-" + indiceKernel))) + "\n");
            }
        }
    }

    public static void main(String[] args) throws IOException, ParseException, InterruptedException {
//        criticalConvex();

//        toCritical(baseDir + "paper-220051000-ais.csv",
//                baseDir + "paper-220051000-window.csv",
//                baseDir + "paper-220051000-windowIntervals.csv");

//        toCritical10Plus(baseDir + "paper-220051000-ais.csv",
//                baseDir + "paper-220051000-window.csv",
//                baseDir + "paper-220051000-windowIntervals10Plus.csv");

    }
}