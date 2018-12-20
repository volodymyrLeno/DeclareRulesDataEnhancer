import data.DeclareConstraint;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String csvFile = args[0];
        String constraintsFile = args[1];
        Boolean considerViolations = Boolean.parseBoolean(args[2]);
        Integer k = Integer.parseInt(args[3]);
        Double minNodeSize = Double.parseDouble(args[4]);
        Boolean pruning = Boolean.parseBoolean(args[5]);

        List<DeclareConstraint> declareConstraints = logReader.readConstraints(constraintsFile);
        correlationMiner.findCorrelations(csvFile, declareConstraints, considerViolations, k, minNodeSize, pruning);
    }
}