import data.FeatureVector;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public final class logWriter {
    public static void writeLog(String path, List<FeatureVector> featureVectors) {
        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(path);

            String header = "";
            for (String attribute : featureVectors.get(0).from.keySet())
                header += attribute + ",";
            for (String attribute : featureVectors.get(0).to.keySet())
                header += attribute + ",";
            header = header.substring(0, header.length() - 1);
            fileWriter.append(header + "\n");

            for (FeatureVector featureVector : featureVectors) {
                String row = "";
                for (String attribute : featureVector.from.keySet())
                    row += featureVector.from.get(attribute) + ",";
                for (String attribute : featureVector.to.keySet())
                    row += featureVector.to.get(attribute) + ",";
                row = row.substring(0, row.length() - 1) + "\n";
                fileWriter.append(row);
            }
        } catch (Exception e) {
            System.out.println("Error in CsvFileWriter !!!");
            e.printStackTrace();
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }
        }
    }
}
