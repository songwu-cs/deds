package songwu.deds.trajectory.io;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.List;

public class ToCSV<T>{
    private String output_path;
    private String header;
    private String splitter;
    private List<T> lines;

    public ToCSV<T> setLines(List<T> lines){
        this.lines = lines;
        return this;
    }

    public ToCSV<T> setOutputPath(String output_path){
        this.output_path = output_path;
        return this;
    }

    public ToCSV<T> setSplitter(String splitter){
        this.splitter = splitter;
        return this;
    }

    public ToCSV<T> setHeader(String header){
        this.header = header;
        return this;
    }

    public void go() throws IOException, NoSuchFieldException, IllegalAccessException {
        try(PrintWriter writer = new PrintWriter(output_path)){
            writer.write(header + System.lineSeparator());

            String[] parts = header.split(splitter);
            int number_of_columns = parts.length;

            int counter = 0;
            for(T line : lines){
                for(String part : parts){
                    Field f = line.getClass().getDeclaredField(part);
                    f.setAccessible(true);
                    writer.write("" + f.get(line));
                    counter++;
                    writer.write(counter == number_of_columns ? System.lineSeparator() : splitter);
                }
                counter = 0;
            }
        }
    }
}
