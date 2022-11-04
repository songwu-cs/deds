package noteAAU.task_2022_09_06_CO2;

import angle.Angle;
import calculation.Array1DString;
import com.sun.org.apache.xpath.internal.operations.Mod;
import datetime.OneTimestamp;
import datetime.TwoTimestamp;
import io.bigdata.BatchFileReader;
import noteAAU.task_2022_09_06_CO2.EmissionModels.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

public class CO2emission {

    public static void computeCO2Emission() throws IOException, ParseException {
        try(PrintWriter writer = new PrintWriter(ModelInit.workdir + "aisdk_onemonth_sorted_cargoAllColumnsSegmentsNoOutliersToCellsEmissions2.csv");
            BatchFileReader reader = new BatchFileReader(ModelInit.workdir + "aisdk_onemonth_sorted_cargoAllColumnsSegmentsNoOutliersToCellsAll2.csv", ",", true, 0,9,3)){
            writer.write("mmsi,segmentid,tileid,pieceid,heading,fromT,toT,draught,fromLon,fromLat,toLon,toLat,distance_m,time_s,speed_knots,co2_01_kg,co2_02_kg,co2_03_kg,co2_04_kg,co2_05_kg,draughtOver,speedOver,cargo_tons\n");

            BaselineCargoQuantity baselineCargoQuantity = new BaselineCargoQuantity();
            GrossTonnageAndMode grossTonnageAndMode = new GrossTonnageAndMode();
            IMO4thReport imo4thReport = new IMO4thReport();
            STEAM steam = new STEAM();
            SpeedCubic speedCubic = new SpeedCubic();

            for(List<String> ls : reader){
                String[] parts = ls.get(0).split(",");
                String mmsi = parts[0];
                String segmentid = parts[9];
                String tileid = parts[1];
                String pieceid = parts[3];
                double draught = Double.parseDouble(parts[2]);

                for(int i = 0; i < ls.size() - 1; i++){
                    String[] partsFrom = ls.get(i).split(",");
                    String[] partsTo = ls.get(i+1).split(",");
                    double fromLon = Double.parseDouble(partsFrom[7]), fromLat = Double.parseDouble(partsFrom[8]);
                    double toLon = Double.parseDouble(partsTo[7]), toLat = Double.parseDouble(partsTo[8]);
                    double distance = Math.hypot(toLon - fromLon, toLat - fromLat);
                    double timeGap = TwoTimestamp.diffInSeconds(partsTo[4], partsFrom[4], TwoTimestamp.formatter1);
                    String midTimestamp = OneTimestamp.add(partsFrom[4], 0, 0, (int)(timeGap / 2), OneTimestamp.formatter1);
                    double speed = distance / timeGap * 3600 / 1000 / 1.852;

                    double heading = Angle.heading(fromLon, fromLat, toLon, toLat);

                    AISsegment segment = new AISsegment(mmsi, distance, timeGap, speed, draught, heading, midTimestamp, (fromLon+toLon)/2, (fromLat+toLat)/2);

                    writer.write(String.join(",",
                            mmsi,
                            segmentid,
                            tileid,
                            pieceid,
                            heading+"",
                            partsFrom[4],
                            partsTo[4],
                            draught+"",
                            partsFrom[5],
                            partsFrom[6],
                            partsTo[5],
                            partsTo[6],
                            distance+"",
                            timeGap+"",
                            speed+"",
                            baselineCargoQuantity.emission(segment),
                            grossTonnageAndMode.emission(segment),
                            speedCubic.emission(segment),
                            imo4thReport.emission(segment),
                            steam.emission(segment),
                            segment.draughtCorrect(),
                            segment.speedCorrect(),
                            segment.cargoCarriedTons()));
                    writer.write("\n");
                }
            }
        }
    }

    public static void EEDIprocessing() throws IOException{
        try(BatchFileReader reader = new BatchFileReader(ModelInit.workdir + "EIVvalues.csv", ",", true, 0);
            PrintWriter writer = new PrintWriter(ModelInit.workdir + "EIVvalues_02_unique.csv")) {
            writer.write("Imo,shiptype,reporting_period,index,emissionFactor(gCO2/t·nm)\n");
            for(List<String> ls : reader){
                String IMO = ls.get(0).split(",")[0];
                String shiptype = ls.get(0).split(",")[2];
                List<String[]> candidates = new ArrayList<>();
                for(String s : ls)
                    candidates.add(s.split(","));

                candidates.sort(Comparator.comparing(e -> ((String[])e)[4].substring(0,3)).thenComparing(Comparator.comparing(e -> ((String[])e)[3]).reversed()));

                for(String[] as : candidates){
                    if(as[4].startsWith("EEDI") || as[4].startsWith("EIV")){
                        writer.write(String.join(",", IMO, shiptype, as[3],as[4].contains("EEDI") ? "EEDI" : (as[4].contains("EIV") ? "EIV" : "Unknown"),
                                ""+Double.parseDouble(as[4].replace("EIV (", "")
                                        .replace("EEDI (", "")
                                        .replace(" gCO₂/t·nm)", "")
                                        .replace(" gCO₂/t·nm", ""))));
                        writer.write("\n");
                        break;
                    }
                }
            }
        }
    }

    public static void robustAllSave() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(ModelInit.workdir + "RobustAll.csv"));
        try (PrintWriter writer = new PrintWriter(ModelInit.workdir + "RobustAllForRealUse.csv")){
            writer.write(String.join(",", "mmsi,imo", ModelInit.attrTPC, ModelInit.attrDRAUGHT, ModelInit.attrDWT, ModelInit.attrGrossTonnage, ModelInit.attrMaxSpeed, ModelInit.attrMaxPower, ModelInit.attrRPM, ModelInit.attrYear, ModelInit.attrLength, ModelInit.attrBeam, "fleetMonFlag"));
            writer.write("\n");

            for(String line : lines.subList(1, lines.size())){
                String[] parts = line.split(",", -1);

                double tpc, draught, dwt, gt, ms, mp, rpm, year, loa, beam;

                boolean fleetMonFlag;
                int posTPC= 23, posGT = 14, posYear = 2;
                int[] maxPower = {16, 15, 18, 17}; //bureau, mt, atlas, baltic
                int[] maxSpeed = {22, 21, 19, 20, 24}; //bureau, mt, tracker, fleetmonDesign, fleetMonMax
                int[] rpmArr = {11, 13, 12}; //bureau, mt, atlas
                int[] loaArr = {3, 4}; //mt, atlas
                int[] beamArr = {5, 6}; //mt, atlas
                int[] dwtArr = {7, 8}; //mt, atlas
                int[] draughtArr = {10, 9}; //mt, atlas

                if (parts[posTPC].equals(""))
                    continue;
                else{
                    tpc = Double.parseDouble(parts[posTPC]);
                }

                if (parts[posGT].equals(""))
                    continue;
                else
                    gt = Double.parseDouble(parts[posGT]);

                if (parts[posYear].equals(""))
                    continue;
                else
                    year = Double.parseDouble(parts[posYear]);

                String power_ = Array1DString.firstNotNullProxy(maxPower, parts);
                if (power_ == null)
                    continue;
                else
                    mp = Double.parseDouble(power_);

                String speed_ = Array1DString.firstNotNullProxy(maxSpeed, parts);
                if (speed_ == null)
                    continue;
                else
                    ms = Double.parseDouble(speed_);
                fleetMonFlag = (speed_.equals(parts[maxSpeed[maxSpeed.length-1]]))
                        && (! speed_.equals(parts[maxSpeed[0]]))
                        && (! speed_.equals(parts[maxSpeed[1]]))
                        && (! speed_.equals(parts[maxSpeed[2]]))
                        && (! speed_.equals(parts[maxSpeed[3]]));

                String rpm_ = Array1DString.firstNotNullProxy(rpmArr, parts);
                if (rpm_ == null)
                    continue;
                else
                    rpm = Double.parseDouble(rpm_);

                String loa_ = Array1DString.firstNotNullProxy(loaArr, parts);
                if (loa_ == null)
                    continue;
                else
                    loa = Double.parseDouble(loa_);

                String beam_ = Array1DString.firstNotNullProxy(beamArr, parts);
                if (beam_ == null)
                    continue;
                else
                    beam = Double.parseDouble(beam_);

                String dwt_ = Array1DString.firstNotNullProxy(dwtArr, parts);
                if (dwt_ == null)
                    continue;
                else
                    dwt = Double.parseDouble(dwt_);

                String draught_ = Array1DString.firstNotNullProxy(draughtArr, parts);
                if (draught_ == null)
                    continue;
                else
                    draught = Double.parseDouble(draught_);

                if(dwt / 100 / tpc >= draught)
                    continue;

                writer.write(String.join(",", parts[0], parts[1], tpc+"", draught+"", dwt+"", gt+"", ms+"", mp+"", rpm+"", year+"", loa+"", beam+"", fleetMonFlag+""));
                writer.write("\n");
            }
        }
    }

    public static Map<String, Map<String, Double>> robustAllLoad() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(ModelInit.workdir + "RobustAllForRealUse.csv"));

        Map<String, Map<String, Double>> answer = new HashMap<>();
        String[] header = lines.get(0).split(",", -1);

        for(String line : lines.subList(1, lines.size())){
            String[] parts = line.split(",", -1);
            Map<String, Double> attrs = new HashMap<>();
            answer.put(parts[0], attrs);

            for(int k = 2; k < header.length - 1; k++){
                attrs.put(header[k], Double.parseDouble(parts[k]));
            }
        }
        return answer;
    }

    public static void robustSpeedPowerRpmRatio() throws IOException {
        for (String file : new String[]{"RobustSpeed.csv", "RobustPower.csv", "RobustRpm.csv"}){
            try (PrintWriter writer = new PrintWriter(ModelInit.workdir + file.replace("Robust", "Ratio"))){
                writer.write("attr1,attr2,ratio,count\n");

                List<String> lines = Files.readAllLines(Paths.get(ModelInit.workdir + file));
                List<String> header = Arrays.asList(lines.get(0).split(","));
                header = header.subList(2, header.size());

                int[] counter = new int[(header.size()) * (header.size() - 1) / 2];
                double[] sum = new double[(header.size()) * (header.size() - 1) / 2];

                for(String s : lines.subList(1, lines.size())){
                    List<String> parts = Arrays.asList(s.split(",", -1));
                    parts = parts.subList(2, parts.size());

                    int pos = -1;
                    for(int k = 0; k < header.size() - 1; k++){
                        for(int k_ = k + 1; k_ < header.size(); k_++){
                            pos++;
                            if(parts.get(k).equals("") || parts.get(k_).equals(""))
                                continue;
                            double num1 = Double.parseDouble(parts.get(k));
                            double num2 = Double.parseDouble(parts.get(k_));
                            double ratio = Math.min(num1, num2) / Math.max(num1, num2);
                            counter[pos]++;
                            sum[pos] += ratio;
                        }
                    }
                }

                int pos = -1;
                for(int k = 0; k < header.size() - 1; k++)
                    for(int k_ = k + 1; k_ < header.size(); k_++){
                        pos++;
                        writer.write(String.join(",", header.get(k), header.get(k_), (sum[pos] / counter[pos]) + "", "" + counter[pos]));
                        writer.write("\n");
                        writer.write(String.join(",", header.get(k_), header.get(k), (sum[pos] / counter[pos]) + "", "" + counter[pos]));
                        writer.write("\n");
                    }
            }
        }
    }

    public static void main(String[] args) throws IOException, ParseException {
        computeCO2Emission();

//        EEDIprocessing();

//        robustSpeedPowerRpmRatio();
//        robustAllSave();

    }
}
