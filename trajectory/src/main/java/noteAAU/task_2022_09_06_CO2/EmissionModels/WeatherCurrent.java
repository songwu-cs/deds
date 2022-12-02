package noteAAU.task_2022_09_06_CO2.EmissionModels;

import calculation.Array1DNumber;
import calculation.ListDouble;
import datetime.OneTimestamp;
import datetime.SimpleDateFormatExt;
import datetime.TwoTimestamp;
import io.bigdata.BatchFileReader;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class WeatherCurrent {
    private Map<String, Map<String, Double>> uo = new HashMap<>();
    private Map<String, Map<String, Double>> vo = new HashMap<>();
    private Map<String, Set<String>> lat2lon;
    private double[] allLatitudes;
    private SimpleDateFormat simpleUnix = new SimpleDateFormatExt(SimpleDateFormatExt.UNIXSTAMP);
    private SimpleDateFormat simpleBasic = OneTimestamp.formatter1;

    public WeatherCurrent(String path, int eastPos, int northPos) throws IOException, ParseException {
        try(BatchFileReader reader = new BatchFileReader(path, ",", true, 0)) {
            for(List<String> lines : reader){
                String timestamp = OneTimestamp.add("1950-01-01 00:00:00", (int)(Double.parseDouble(lines.get(0).split(",")[0]))-1, 0, 0, OneTimestamp.formatter1);
                Map<String, Double> uo_ = new HashMap<>();
                Map<String, Double> vo_ = new HashMap<>();
                uo.put(timestamp, uo_);
                vo.put(timestamp, vo_);

                if(lat2lon == null){
                    lat2lon = new HashMap<>();
                    for(String line : lines){
                        String[] parts = line.split(",");
                        if (! lat2lon.containsKey(parts[1]))
                            lat2lon.put(parts[1], new HashSet<>());
                        lat2lon.get(parts[1]).add(parts[2]);
                    }

                    List<Double> latitudes_= new ArrayList<>();
                    for(String lat : lat2lon.keySet())
                        latitudes_.add(Double.parseDouble(lat));
                    latitudes_.sort(Comparator.naturalOrder());
                    allLatitudes = ListDouble.toArray(latitudes_);
                }

                for(String line : lines){
                    String[] parts = line.split(",");
                    String key = parts[1] + "," + parts[2];
                    uo_.put(key, Double.parseDouble(parts[eastPos]));
                    vo_.put(key, Double.parseDouble(parts[northPos]));
                }
            }
        }
    }

    public double calibratedSpeed(AISsegment segment){
        String currentSpeed = current(segment);
        String[] parts = currentSpeed.split(",");
        double eastSpeed = Double.parseDouble(parts[0]) * 3.6 / 1.852;
        double northSpeed = Double.parseDouble(parts[1]) * 3.6 / 1.852;
        double eastAIS = segment.speed * Math.sin(segment.heading);
        double northAIS = segment.speed * Math.cos(segment.heading);

        double speedCalibrated = Math.hypot(eastAIS - eastSpeed, northAIS - northSpeed);
        return speedCalibrated;
    }

    public String current(AISsegment s){
        String t = s.midTimestamp;
        double midLat = s.midLatitude;
        double midLon = s.midLongitude;

        int minute = Integer.parseInt(t.substring(14,16));
        int second = Integer.parseInt(t.substring(17,19));
        int total = minute * 60 + second;
        if(total >= 1800)
            t = OneTimestamp.add(t, 0, 0, 3600 - total, OneTimestamp.formatter1);
        else
            t = OneTimestamp.add(t, 0, 0, -total, OneTimestamp.formatter1);

        if (t.startsWith("2022-06-01"))
            t = "2022-05-31 23:00:00";

        Map<String, Double> uo_ = uo.get(t);
        Map<String, Double> vo_ = vo.get(t);

        double closestLat = allLatitudes[Array1DNumber.closest(allLatitudes, midLat)];

        Set<String> longitudeS = lat2lon.get(closestLat + "");
        List<Double> longitudeL = new ArrayList<>();
        for(String lon : longitudeS)
            longitudeL.add(Double.parseDouble(lon));
        longitudeL.sort(Comparator.naturalOrder());
        double[] allLongitudes = ListDouble.toArray(longitudeL);

        double closestLon = allLongitudes[Array1DNumber.closest(allLongitudes, midLon)];

        String key = closestLat + "," + closestLon;
        return uo_.get(key) + "," + vo_.get(key);
    }

    public static void main(String[] args) throws ParseException {
        System.out.println(Array1DNumber.closest(new double[]{1.1,1.2,1.3}, 1.12));

        SimpleDateFormat simpleUnix = new SimpleDateFormatExt(SimpleDateFormatExt.UNIXSTAMP);
        System.out.println(OneTimestamp.add("1950-01-01 00:00:00", 634032, 0, 0, OneTimestamp.formatter1));
    }
}
