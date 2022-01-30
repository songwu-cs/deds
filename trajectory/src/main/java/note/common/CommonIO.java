package note.common;

import songwu.deds.trajectory.clean.DenoiseFakeTimeStampedPointT;
import songwu.deds.trajectory.data.TimeStampedPointT;
import songwu.deds.trajectory.io.File2TimestampedPointT;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

public class CommonIO {
    public static List<TimeStampedPointT> denoiseTask(String dataFile) throws IOException, ParseException, InterruptedException {
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
        return denoiser.go();
    }
}
