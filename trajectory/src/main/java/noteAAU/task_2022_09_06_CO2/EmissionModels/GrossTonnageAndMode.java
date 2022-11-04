package noteAAU.task_2022_09_06_CO2.EmissionModels;

import noteAAU.task_2022_09_06_CO2.CO2emission;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GrossTonnageAndMode extends ModelInit{
    Map<String, Map<String, Double>> answer = new HashMap<>();
    public GrossTonnageAndMode() throws IOException {
        answer = CO2emission.robustAllLoad();
    }

    @Override
    public String emission(AISsegment segment) {//in kilograms
        Map<String, Double> currentShip = answer.get(segment.mmsi);

        if(currentShip != null){
            double max_speed = currentShip.get(attrMaxSpeed);
            double grossTon = currentShip.get(attrGrossTonnage);
            double Cjk = 9.8197 + 0.00143 * grossTon; //tons fuel per day
            double maxFuel = 3173; //kg of CO2 per ton of fuel

            double speedCorrected = Math.min(max_speed, segment.speed);
            double fraction = 0;
            if(speedCorrected / max_speed > 0.8)
                fraction = 1;
            else if (speedCorrected / max_speed > 0.2) {
                fraction = 0.48;
            }else
                fraction = 0.03;

            segment.speedCorrected = speedCorrected;
            return "" + (Cjk * segment.timeGap / 3600 / 24 * fraction * maxFuel);
        }
        return super.emission(segment);
    }
}
