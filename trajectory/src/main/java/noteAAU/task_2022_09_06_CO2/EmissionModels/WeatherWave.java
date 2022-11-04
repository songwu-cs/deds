package noteAAU.task_2022_09_06_CO2.EmissionModels;

import calculation.Array1DNumber;
import calculation.ListDouble;
import datetime.OneTimestamp;
import datetime.SimpleDateFormatExt;
import io.bigdata.BatchFileReader;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class WeatherWave {
    private Map<String, Map<String, Double>> swh = new HashMap<>();
    private Map<String, Map<String, Double>> fromDir = new HashMap<>();
    private Map<String, Set<String>> lat2lon;
    private double[] allLatitudes;
    private SimpleDateFormat simpleUnix = new SimpleDateFormatExt(SimpleDateFormatExt.UNIXSTAMP);
    private SimpleDateFormat simpleBasic = OneTimestamp.formatter1;

    public WeatherWave(String path, int heightPos, int directionPos) throws IOException, ParseException {
        try(BatchFileReader reader = new BatchFileReader(path, ",", true, 0)) {
            for(List<String> lines : reader){
                String timestamp = simpleBasic.format(simpleUnix.parse((int)(Double.parseDouble(lines.get(0).split(",")[0]) - 7200) + ""));
                Map<String, Double> swh_ = new HashMap<>();
                Map<String, Double> fromDir_ = new HashMap<>();
                swh.put(timestamp, swh_);
                fromDir.put(timestamp, fromDir_);

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
                    swh_.put(key, Double.parseDouble(parts[heightPos]));
                    fromDir_.put(key, Double.parseDouble(parts[directionPos]));
                }
            }
        }
    }

    public String wave(AISsegment s){
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

        Map<String, Double> swh_ = swh.get(t);
        Map<String, Double> fromDir_ = fromDir.get(t);

        double closestLat = allLatitudes[Array1DNumber.closest(allLatitudes, midLat)];

        Set<String> longitudeS = lat2lon.get(closestLat + "");
        List<Double> longitudeL = new ArrayList<>();
        for(String lon : longitudeS)
            longitudeL.add(Double.parseDouble(lon));
        longitudeL.sort(Comparator.naturalOrder());
        double[] allLongitudes = ListDouble.toArray(longitudeL);

        double closestLon = allLongitudes[Array1DNumber.closest(allLongitudes, midLon)];

        String key = closestLat + "," + closestLon;
        return swh_.get(key) + "," + fromDir_.get(key);
    }

    public static void main(String[] args) {
        System.out.println(Array1DNumber.closest(new double[]{1.1,1.2,1.3}, 1.12));
    }
}
