package noteAAU.task_2022_09_06_CO2.EmissionModels;

import noteAAU.task_2022_09_06_CO2.CO2emission;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpeedCubic extends ModelInit{
    Map<String, Map<String, Double>> answer = new HashMap<>();

    public SpeedCubic() throws IOException {
        answer = CO2emission.robustAllLoad();
    }

    @Override
    public String emission(AISsegment segment) { // in kilograms
        Map<String, Double> currentShip = answer.get(segment.mmsi);
        if(currentShip != null) {
            double rpm = (currentShip.get(attrRPM));

            double emissionFactor = calcEF_CO2(currentShip.get(attrRPM), FUEL); //in g/kWh

            double maxSpeed = currentShip.get(attrMaxSpeed);
            double maxPower = currentShip.get(attrMaxPower);
            double speedCorrected = Math.min(maxSpeed, segment.speed);
            segment.speedCorrected = speedCorrected;
            return "" + (maxPower * Math.pow(speedCorrected / maxSpeed, 3) * segment.timeGap / 3600 * emissionFactor / 1000);
        }
        return super.emission(segment);
    }

    private double calcEF_CO2(double rpm, String fuelType){// no option for HSD and HFO
        String engineType = rpmLEVEL(rpm);
        if (engineType.equals(SSD) && fuelType.equals(DIESEL)) {
            return 590;
        }else if (engineType.equals(MSD) && fuelType.equals(DIESEL)) {
            return 654;
        }else if (engineType.equals(HSD) && fuelType.equals(DIESEL)) {
            return 654;
        }else if (engineType.equals(SSD) && fuelType.equals(HFO)) {
            return 622;
        }else if (engineType.equals(MSD) && fuelType.equals(HFO)) {
            return 686;
        }
        return -1;
    }
}
