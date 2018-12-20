import data.Cluster;
import data.FeatureVector;
import weka.classifiers.rules.JRip;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.*;
import java.util.stream.Collectors;

public class clusterDescriptor {
    public String describeCluster(Cluster cluster, List<FeatureVector> featureVectors, String relationName, Double minNodeSize){
        try {
            List<String> keys = new ArrayList<>(featureVectors.get(0).to.keySet());
            ArrayList<Attribute> attributes = new ArrayList<>();
            for(String attr: keys)
            {
                String value = featureVectors.get(0).to.get(attr);
                if(correlationMiner.tryParseDouble(value))
                    attributes.add(new Attribute(attr));
                else{
                    List<String> attVals = featureVectors.stream().map(obj -> obj.to.get(attr)).distinct().collect(Collectors.toList());
                    attributes.add(new Attribute(attr, attVals));
                }
            }
            List<String> labels = Arrays.asList("positive", "negative");
            if(labels.size() == 1)
                return(labels.get(0));

            attributes.add(new Attribute("Class", labels));

            Instances data = new Instances(relationName, attributes, featureVectors.size());

            for(int i = 0; i < featureVectors.size(); i++)
            {
                Instance inst = new DenseInstance(attributes.size());
                for(Attribute attr: attributes){
                    if(!attr.name().equals("Class")){
                        String value = featureVectors.get(i).to.get(attr.name());
                        if(attr.isNumeric())
                            inst.setValue(attr, Double.parseDouble(value));
                        else
                            inst.setValue(attr, value);
                    }
                    else{
                        String value = "";
                        if(cluster.getElements().contains(featureVectors.get(i)))
                            value = "positive";
                        else
                            value = "negative";
                        inst.setValue(attr, value);
                    }
                }
                data.add(inst);
            }

            if (data.classIndex() == -1)
                data.setClassIndex(data.numAttributes() - 1);

            JRip classifier = new JRip();
            String[] options = new String[2];
            options[0] = "-N";
            options[1] = String.valueOf((double) Math.round(minNodeSize * featureVectors.size()));
            classifier.setOptions(options);
            classifier.buildClassifier(data);

            String cls = classifier.toString();
            String[] info = cls.split("\n");

            List<String> summary = new ArrayList<>();
            for(String row: info){
                if(row.contains("positive"))
                    summary.add(row.substring(0, row.indexOf(" =>")).replaceAll(" and ", " && "));
            }

            String condition = "";
            if(summary.contains(""))
            {
                for(String row: info)
                    if(row.contains("negative"))
                    {
                        String value = row.substring(0, row.indexOf(" =>"));
                        value = value.replaceAll("[)] and [(]",") || (");
                        value = value.replaceAll("[)(]", "");
                        condition += "(" + makeNegative(value) + ")" + " and ";
                    }
                if(condition.length() > 0)
                    condition = condition.substring(0, condition.length() - 5);
                else
                    condition = getSummary(cluster);
            }
            else{
                for(String rule: summary){
                    String value = rule.replaceAll("[)(]", "");
                    condition += "(" + value + ")" + " or ";
                }
                if(condition.length() > 0)
                    condition = condition.substring(0, condition.length() - 4);
                else
                    condition = getSummary(cluster);
            }
            return condition;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String makeNegative(String condition){
        HashMap<String,String> map = new HashMap<>();
        map.put("<",">=");
        map.put(">=", "<");
        map.put(">", "<=");
        map.put("<=", ">");
        map.put("=", "!=");
        map.put("!=", "=");

        StringBuilder sb = new StringBuilder();
        Scanner testScanner = new Scanner(condition);
        while (testScanner.hasNext()) {
            String text = testScanner.next();
            text = map.get(text) == null ? text : map.get(text);
            sb.append(text + " ");
        }
        return sb.toString().substring(0, sb.length() - 1);
    }

    private static String getSummary(Cluster cluster){
        String summary = "(";
        for(String attribute: cluster.getElements().get(0).to.keySet())
        {
            if(correlationMiner.tryParseDouble(cluster.getElements().get(0).to.get(attribute))){
                Double max = Collections.max(cluster.getElements().stream().map(fv -> Double.parseDouble(fv.to.get(attribute))).
                        collect(Collectors.toList()));
                Double min = Collections.min(cluster.getElements().stream().map(fv -> Double.parseDouble(fv.to.get(attribute))).
                        collect(Collectors.toList()));
                summary +=  attribute + " >= " + min + " && " + attribute + " <= " + max + " && ";
            }
            else{
                List<String> values = new ArrayList<>();
                for(int i = 0; i < cluster.getElements().size(); i++){
                    String value = cluster.getElements().get(i).to.get(attribute);
                    if(!values.contains(value))
                        values.add(value);
                }

                if(values.size() <= 3){
                    HashMap<String, Integer> frequencies = new HashMap<>();
                    for(int i = 0; i < cluster.getElements().size(); i++){
                        String value = cluster.getElements().get(i).to.get(attribute);
                        if(frequencies.containsKey(value))
                            frequencies.put(value, frequencies.get(value) + 1);
                        else
                            frequencies.put(value, 1);
                    }
                    Integer max = frequencies.get(cluster.getElements().get(0).to.get(attribute));
                    String bestLabel = cluster.getElements().get(0).to.get(attribute);
                    for(String value: frequencies.keySet())
                        if(frequencies.get(value) > max){
                            max = frequencies.get(value);
                            bestLabel = value;
                        }
                    summary += attribute + " = " + bestLabel + " && ";
                }
            }
        }
        return summary.substring(0, summary.length() - 4) + ")";
    }
}
