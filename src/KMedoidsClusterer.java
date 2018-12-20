import data.Cluster;
import data.FeatureVector;
import data.RangesSummary;
import data.StringPair;

import java.util.*;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static java.util.stream.Collectors.*;
import static java.util.Map.Entry.*;


public class KMedoidsClusterer {
    private int maxIterations;
    private int numberOfClusters;
    private Random rg;
    private Cluster[] output;

    private RangesSummary rangesSummary;
    private HashMap<StringPair, Double> editDistances;
    private HashMap<String, HashMap<StringPair, Double>> rangesDistances;

    public KMedoidsClusterer(){
        this(4, 100);
    }

    public KMedoidsClusterer(int numberOfClusters, int maxIterations){
        this.numberOfClusters = numberOfClusters;
        this.maxIterations = maxIterations;
        this.rg = new Random(System.currentTimeMillis());

        this.rangesSummary = new RangesSummary();
        this.editDistances = new HashMap<>();
        this.rangesDistances = new HashMap<>();

        this.output = new Cluster[numberOfClusters];
        for(int i = 0; i < numberOfClusters; i++)
            output[i] = new Cluster();
    }

    public List<Cluster> clustering(List<FeatureVector> points){
        rangesSummary = Distance.computeRanges(points);
        editDistances = Distance.computeEditDistances(points);
        rangesDistances = Distance.computeRangesDistances(points);

        FeatureVector[] medoids = new FeatureVector[numberOfClusters];

        for (int i = 0; i < numberOfClusters; i++) {
            int random = rg.nextInt(points.size());
            medoids[i] = points.get(i);
        }

        boolean changed = true;
        int count = 0;

        while(changed && count < maxIterations) {
            changed = false;
            count++;
            int[] assignment = assign(medoids, points);
            changed = recalculateMedoids(assignment, medoids, points);
        }

        return new LinkedList<>(Arrays.asList(output));
    }

    private int[] assign(FeatureVector[] medoids, List<FeatureVector> points) {
        int[] out = new int[points.size()];
        for (int i = 0; i < points.size(); i++) {
            double bestDistance = Distance.computeDistance(points.get(i), medoids[0], rangesSummary.getAttrMax(), rangesSummary.getAttrMin(), editDistances, rangesDistances);
            int bestIndex = 0;
            for(int j = 1; j < medoids.length; j++){
                double tmpDistance = Distance.computeDistance(points.get(i), medoids[j], rangesSummary.getAttrMax(), rangesSummary.getAttrMin(), editDistances, rangesDistances);
                if(tmpDistance < bestDistance){
                    bestDistance = tmpDistance;
                    bestIndex = j;
                }
            }

            out[i] = bestIndex;
        }
        return out;
    }

    private boolean recalculateMedoids(int[] assignment, FeatureVector[] medoids, List<FeatureVector> points) {
        boolean changed = false;

        for (int i = 0; i < numberOfClusters; i++) {
            output[i].setElements(new ArrayList<>());
            for (int j = 0; j < assignment.length; j++) {
                if (assignment[j] == i) {
                    output[i].getElements().add(points.get(j));
                }
            }
            if (output[i].getElements().size() == 0) {
                medoids[i] = points.get(rg.nextInt(points.size()));
                changed = true;
            } else {
                FeatureVector centroid = getCentroid(output[i]);
                FeatureVector oldMedoid = medoids[i];
                medoids[i] = kNearest(1, centroid, points).iterator().next();
                if (!medoids[i].equals(oldMedoid))
                    changed = true;
            }
        }
        return changed;
    }

    private FeatureVector getCentroid(Cluster cluster){
        HashMap<String, String> from = new HashMap<>();
        HashMap<String, String> to = new HashMap<>();

        for(String attribute: cluster.getElements().get(0).from.keySet()){
            if(correlationMiner.tryParseDouble(cluster.getElements().get(0).from.get(attribute))){
                double sum = 0.0;
                for(FeatureVector element: cluster.getElements())
                    sum += Double.parseDouble(element.from.get(attribute));
                from.put(attribute, String.valueOf(sum/cluster.getElements().size()));
            }
            else{
                HashMap<String, Integer> categories = new HashMap<>();
                for(FeatureVector element: cluster.getElements()) {
                    if (categories.containsKey(element.from.get(attribute)))
                        categories.put(element.from.get(attribute), categories.get(element.from.get(attribute)) + 1);
                    else
                        categories.put(element.from.get(attribute), 1);
                }
                from.put(attribute, Collections.max(categories.entrySet(), comparingByValue()).getKey());
            }
        }

        for(String attribute: cluster.getElements().get(0).to.keySet()){
            if(correlationMiner.tryParseDouble(cluster.getElements().get(0).to.get(attribute))){
                double sum = 0.0;
                for(FeatureVector element: cluster.getElements())
                    sum += Double.parseDouble(element.to.get(attribute));
                to.put(attribute, String.valueOf(sum/cluster.getElements().size()));
            }
            else{
                HashMap<String, Integer> categories = new HashMap<>();
                for(FeatureVector element: cluster.getElements()) {
                    if (categories.containsKey(element.to.get(attribute)))
                        categories.put(element.to.get(attribute), categories.get(element.to.get(attribute)) + 1);
                    else
                        categories.put(element.to.get(attribute), 1);
                }
                to.put(attribute, Collections.max(categories.entrySet(), comparingByValue()).getKey());
            }
        }
        FeatureVector centroid = new FeatureVector(from, to);
        return centroid;
    }

    List<FeatureVector> kNearest(int k, FeatureVector centroid, List<FeatureVector> points){
        List<FeatureVector> knearest = new ArrayList<>();
        HashMap<FeatureVector, Double> distances = new HashMap<>();
        for(FeatureVector fv: points)
            distances.put(fv, Math.abs(Distance.computeDistance(fv, centroid, rangesSummary.getAttrMax(), rangesSummary.getAttrMin(), editDistances, rangesDistances)));

        distances = distances.entrySet().stream().sorted(comparingByValue()).collect(toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e2, LinkedHashMap::new));

        for(int i = 0; i < k; i++){
            knearest.add(distances.keySet().toArray(new FeatureVector[distances.size()])[i]);
        }

        return knearest;
    }
}
