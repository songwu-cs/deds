package noteAAU.task_2022_09_06_CO2.EmissionModels;

import noteAAU.task_2022_09_06_CO2.CO2emission;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaselineCargoQuantity extends ModelInit{
    public static final double SIMPLE_CONSTANT_perTonKm_GRAM = 3;

    Map<String, Map<String, Double>> answer = new HashMap<>();
    public BaselineCargoQuantity() throws IOException {
        answer = CO2emission.robustAllLoad();
    }

    @Override
    public String emission(AISsegment segment) {//in kilograms
        Map<String, Double> currentShip = answer.get(segment.mmsi);
        if(currentShip != null){
            double draughtCorrected = Math.min(currentShip.get(attrDRAUGHT), segment.draught);
            double cargo_tons = currentShip.get(attrDWT) - currentShip.get(attrTPC) * 100 * (currentShip.get(attrDRAUGHT) - draughtCorrected);

            segment.cargoTons = cargo_tons;
            segment.draughtCorrected = draughtCorrected;

            return "" + (cargo_tons * segment.distance / 1000 * SIMPLE_CONSTANT_perTonKm_GRAM / 1000);
        }
        return super.emission(segment);
    }
}
