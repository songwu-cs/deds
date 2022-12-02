package noteAAU.task_2022_09_06_CO2.EmissionModels;

import calculation.Array1DNumber;
import com.sun.org.apache.xpath.internal.operations.Bool;
import noteAAU.task_2022_09_06_CO2.CO2emission;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IMO4thReport extends ModelInit{
    Map<String, Map<String, Double>> answer = new HashMap<>();
    Map<String, String> within5nm = new HashMap<>();
    double[] allLongitudes;
    double[] allLatitudes;

    public IMO4thReport() throws IOException {
        answer = CO2emission.robustAllLoad();
        within5nm = loadShoreDistance();

        allLatitudes = new double[160];
        allLongitudes = new double[300];
        for(int i = 0; i < 160; i++)
            allLatitudes[i] = 52.025 + 0.05 * i;
        for(int i = 0; i < 300; i++)
            allLongitudes[i] = 5.025 + 0.05 * i;
    }

    private Map<String, String> loadShoreDistance() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(ModelInit.workdir + "within5nm_20.csv"));
        Map<String, String> within5nm = new HashMap<>();

        for(String line : lines.subList(1, lines.size())){
            String[] parts = line.split(",");
            within5nm.put(parts[0], parts[1]);
        }
        return within5nm;
    }

    private boolean isCoastal(AISsegment segment){
        int lonPos = Array1DNumber.closest(allLongitudes, segment.midLongitude) + 1;
        int latPos = Array1DNumber.closest(allLatitudes, segment.midLatitude) + 1;
        int rowid = latPos + (lonPos - 1) * 160;
        return within5nm.get(rowid+"").equals("t");
    }

    @Override
    public String emission(AISsegment segment) {//in kilograms
        Map<String, Double> currentShip = answer.get(segment.mmsi);
        if(currentShip != null){
            double max_power = currentShip.get(attrMaxPower);
            double max_speed = currentShip.get(attrMaxSpeed);
            double max_draught = currentShip.get(attrDRAUGHT);
            double draughtCorrected = Math.min(max_draught, segment.draught);
            double speedCorrected = Math.min(max_speed, segment.speed);

            double delta_w = 1, eta_f = 0.917;
            double eta_w = isCoastal(segment) ? 0.909 : 0.867;
            double power_required = delta_w * max_power * Math.pow(draughtCorrected / max_draught, 0.66) * Math.pow(speedCorrected / max_speed ,3) / eta_w / eta_f;
            power_required = Math.min(max_power, power_required);

            //using MDO rather than HFO
            double SFCbase = calcSFOC(currentShip.get(attrRPM), currentShip.get(attrYear), FUEL); //可以根据建造年份进行细分, in g/kWh
            double load = power_required / max_power;
            double SFCme = SFCbase * (0.455 * load * load - 0.710 * load + 1.280);
            double CO2_emission_factor = calcEF_CO2(FUEL); //in gCO2/gFUEL

            segment.draughtCorrected = draughtCorrected;
            segment.speedCorrected = speedCorrected;
            return "" + (power_required * segment.timeGap / 3600 * SFCme * CO2_emission_factor / 1000);
        }
        return super.emission(segment);
    }

    public String emission(AISsegment segment, WeatherCurrent currentData) {//in kilograms
        Map<String, Double> currentShip = answer.get(segment.mmsi);
        if(currentShip != null){
            double max_power = currentShip.get(attrMaxPower);
            double max_speed = currentShip.get(attrMaxSpeed);
            double max_draught = currentShip.get(attrDRAUGHT);
            double draughtCorrected = Math.min(max_draught, segment.draught);
            double speedCorrected = Math.min(max_speed, currentData.calibratedSpeed(segment));

            double delta_w = 1, eta_f = 0.917;
            double eta_w = isCoastal(segment) ? 0.909 : 0.867;
            double power_required = delta_w * max_power * Math.pow(draughtCorrected / max_draught, 0.66) * Math.pow(speedCorrected / max_speed ,3) / eta_w / eta_f;
            power_required = Math.min(max_power, power_required);

            //using MDO rather than HFO
            double SFCbase = calcSFOC(currentShip.get(attrRPM), currentShip.get(attrYear), FUEL); //可以根据建造年份进行细分, in g/kWh
            double load = power_required / max_power;
            double SFCme = SFCbase * (0.455 * load * load - 0.710 * load + 1.280);
            double CO2_emission_factor = calcEF_CO2(FUEL); //in gCO2/gFUEL

            return "" + (power_required * segment.timeGap / 3600 * SFCme * CO2_emission_factor / 1000);
        }
        return super.emission(segment);
    }

    private double calcSFOC(double rpm, double year, String fuelType){
        String engineType = rpmLEVEL(rpm);

        if (engineType.equals(SSD) && fuelType.equals(HFO) && year <= 1983)
            return 205;
        if (engineType.equals(SSD) && fuelType.equals(HFO) && year <= 2000)
            return 185;
        if (engineType.equals(SSD) && fuelType.equals(HFO) && year > 2000)
            return 175;

        if (engineType.equals(SSD) && fuelType.equals(DIESEL) && year <= 1983)
            return 190;
        if (engineType.equals(SSD) && fuelType.equals(DIESEL) && year <= 2000)
            return 175;
        if (engineType.equals(SSD) && fuelType.equals(DIESEL) && year > 2000)
            return 165;

        if (engineType.equals(MSD) && fuelType.equals(HFO) && year <= 1983)
            return 215;
        if (engineType.equals(MSD) && fuelType.equals(HFO) && year <= 2000)
            return 195;
        if (engineType.equals(MSD) && fuelType.equals(HFO) && year > 2000)
            return 185;

        if (engineType.equals(MSD) && fuelType.equals(DIESEL) && year <= 1983)
            return 200;
        if (engineType.equals(MSD) && fuelType.equals(DIESEL) && year <= 2000)
            return 185;
        if (engineType.equals(MSD) && fuelType.equals(DIESEL) && year > 2000)
            return 175;

        if (engineType.equals(HSD) && fuelType.equals(HFO) && year <= 1983)
            return 225;
        if (engineType.equals(HSD) && fuelType.equals(HFO) && year <= 2000)
            return 205;
        if (engineType.equals(HSD) && fuelType.equals(HFO) && year > 2000)
            return 195;

        if (engineType.equals(HSD) && fuelType.equals(DIESEL) && year <= 1983)
            return 210;
        if (engineType.equals(HSD) && fuelType.equals(DIESEL) && year <= 2000)
            return 190;
        if (engineType.equals(HSD) && fuelType.equals(DIESEL) && year > 2000)
            return 185;
        return -1;
    }

    private double calcEF_CO2(String fuelType){
        if (fuelType.equals(HFO))
            return 3.114;
        else if (fuelType.equals(DIESEL)) {
            return 3.206;
        }
        return -1;
    }

    public static void main(String[] args) {
        int lonPos = Array1DNumber.closest(new double[]{50.25,50.75}, 50.6) + 1;
        int latPos = Array1DNumber.closest(new double[]{50.25,50.75,51.25,51.75}, 51.2) + 1;
        int rowid = latPos + (lonPos - 1) * 4;
        System.out.println(rowid);


    }
}
