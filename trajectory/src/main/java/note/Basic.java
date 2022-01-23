package note;

import gis.TransformCoordinates;
import org.opengis.referencing.operation.TransformException;

public class Basic {
    public static void main(String[] args) throws TransformException {
        TransformCoordinates transformCoordinates = new TransformCoordinates(4326,25832);
        System.out.println(transformCoordinates.go(7.4929,56.809537));
    }
}
