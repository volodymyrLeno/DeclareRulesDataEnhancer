import data.*;
import weka.classifiers.Evaluation;

import java.util.*;
import java.util.stream.Collectors;

public final class correlationMiner {

    public static void findCorrelations(String file, List<DeclareConstraint> declareConstraints, boolean considerViolations, int k, double minNodeSize, Boolean pruning){

        List<Evaluation> evaluationResults = new ArrayList<>();

        long totalStartTime = System.currentTimeMillis();
        HashMap<String, List<Event>> cases = new HashMap<>();
        String format = file.substring(file.lastIndexOf(".") + 1);

        if(format.equals("csv"))
            cases = logReader.readCSV(file);
        else if(format.equals("xes"))
            cases = logReader.readXES(file);

        for (DeclareConstraint constraint: declareConstraints) {
            List<FeatureVector> fulfillments = rulesExtractor.extractFulfillments(cases, constraint);

            //logWriter.writeLog("C:\\Volodymyr\\PhD\\JOURNAL EXTENSION\\Real Experiments\\BPIC2017\\" + rule + "(" + itemset.items.get(0) + "," + itemset.items.get(1) + ").csv", featureVectors);

            long startTime = System.currentTimeMillis();

            List<Cluster> clusters;

            KMedoidsClusterer kMedoids = new KMedoidsClusterer(k, 10000);
            clusters = kMedoids.clustering(fulfillments);

            String ruleType = constraint.ruleType;
            String relationName = constraint.toString();

            for (Cluster cluster : clusters) {
                if (cluster.getElements().size() > 0 && cluster.getLabel() == null) {
                    String summary = getSummary(cluster, fulfillments, relationName, minNodeSize);
                    cluster.setLabel(summary);
                    cluster.giveLabels();
                }
            }

            List<FeatureVector> featureVectorsList = new ArrayList<>();

            if(considerViolations){
                List<FeatureVector> violations = rulesExtractor.extractViolations(cases, constraint);
                List<String> violationRule = new ArrayList<>(Collections.singletonList("-"));
                clusters.add(new Cluster("-", violationRule, violations, "Closed Leaf"));

                featureVectorsList = new ArrayList<>() {
                    {
                        addAll(fulfillments.stream().filter(fv -> fv.label != null).collect(Collectors.toList()));
                        addAll(violations);
                    }
                };
            }
            else
                featureVectorsList = fulfillments.stream().filter(fv -> fv.label != null).collect(Collectors.toList());

            for (FeatureVector fv : featureVectorsList) {
                if (fv.label == null)
                    fv.label = "-";
            }
            JRipClassifier classifier = new JRipClassifier();
            evaluationResults.add(classifier.classify(cases, featureVectorsList, relationName, constraint, minNodeSize, pruning));
            long stopTime = System.currentTimeMillis();
            System.out.println("Execution time: " + (stopTime - startTime) / 1000.0 + " sec");
        }
        long totalStopTime = System.currentTimeMillis();
        System.out.println("\n\nTotal Execution Time: " + (totalStopTime - totalStartTime) / 1000.0 + " sec");
    }

    private static String getSummary(Cluster cluster, List<FeatureVector> featureVectors, String relationName, Double minNodeSize){
        clusterDescriptor classifier = new clusterDescriptor();
        return classifier.describeCluster(cluster, featureVectors, relationName, minNodeSize);
    }

    static boolean tryParseDouble(String value){
        try{
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e){
            return false;
        }
    }
}