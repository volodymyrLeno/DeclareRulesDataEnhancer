import data.*;
import weka.classifiers.Evaluation;
import weka.classifiers.rules.JRip;
import weka.classifiers.rules.RuleStats;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class JRipClassifier {
    public JRipClassifier() {

    }

    public Evaluation classify(HashMap<String, List<Event>> cases, List<FeatureVector> featureVectors, String relationName, DeclareConstraint constraint, Double minNodeSize, Boolean pruning) {
        try {
            List<String> keys = new ArrayList<>(featureVectors.get(0).from.keySet());
            ArrayList<Attribute> attributes = new ArrayList<>();
            for (String attr : keys) {
                String value = featureVectors.get(0).from.get(attr);
                if (correlationMiner.tryParseDouble(value))
                    attributes.add(new Attribute(attr));
                else {
                    List<String> attVals = featureVectors.stream().map(obj -> obj.from.get(attr)).distinct().collect(Collectors.toList());
                    attributes.add(new Attribute(attr, attVals));
                }
            }
            List<String> labels = featureVectors.stream().map(obj -> obj.label).distinct().collect(Collectors.toList());
            //System.out.println(labels);
            attributes.add(new Attribute("Class", labels));

            Instances data = new Instances(relationName, attributes, featureVectors.size());

            for (int i = 0; i < featureVectors.size(); i++) {
                Instance inst = new DenseInstance(attributes.size());
                for (Attribute attr : attributes) {
                    if (!attr.name().equals("Class")) {
                        String value = featureVectors.get(i).from.get(attr.name());
                        if (attr.isNumeric())
                            inst.setValue(attr, Double.parseDouble(value));
                        else
                            inst.setValue(attr, value);
                    } else {
                        String value = featureVectors.get(i).label;
                        if (attr.isNumeric())
                            inst.setValue(attr, Double.parseDouble(value));
                        else
                            inst.setValue(attr, value);
                    }
                }
                data.add(inst);
            }

            if (data.classIndex() == -1)
                data.setClassIndex(data.numAttributes() - 1);

            System.out.println("\n" + relationName + "\n");

            if(labels.size() == 1){
                String rule = " => Class=" + labels.get(0) + " (" + (double)data.size() + "/" + (double)data.size() + ")";
                Correlation correlation = new Correlation(constraint, getAntecedent(rule), getConsequent(rule));
                System.out.println(new Rule(rule, Summary.getRelativeSupport(featureVectors, correlation), Summary.getConfidence(featureVectors, correlation),
                        (double)rule.split("[)] and [(]|[)] or [(]|\\s&&\\s|\\s\\|\\|\\s|\\s=>").length));
                return new Evaluation(data);
            }
            else{
                JRip classifier = new JRip();
                String[] options;

                if(!pruning){
                    options = new String[3];
                    options[0] = "-N";
                    options[1] = "1.0";
                    options[2] = "-P";
                }
                else{
                    options = new String[2];
                    options[0] = "-N";
                    options[1] = String.valueOf((double) Math.round(minNodeSize * featureVectors.size()));
                }
                classifier.setOptions(options);
                classifier.buildClassifier(data);

                RuleStats ruleStats = new RuleStats();
                ruleStats.setData(data);
                ruleStats.setRuleset(classifier.getRuleset());
                ruleStats.countData();

                String[] classifierRules = classifier.toString().split("\n");

                List<Double> confidences = new ArrayList<>();
                List<Double> supports = new ArrayList<>();
                List<Double> ruleLengths = new ArrayList<>();
                String ruleType = relationName.substring(0, relationName.indexOf("(") - 1);
                List<Rule> rules = new ArrayList<>();
                for (int i = 3; i < classifierRules.length - 2; i++) {
                    if(!classifierRules[i].substring(0, classifierRules[i].indexOf(" =>")).equals("")) {
                        Correlation correlation = new Correlation(constraint, getAntecedent(classifierRules[i]), getConsequent(classifierRules[i]));
                        Rule rule = new Rule(classifierRules[i], Summary.getRelativeSupport(featureVectors, correlation), Summary.getConfidence(featureVectors,
                                correlation), (double)classifierRules[i].split("[)] and [(]|[)] or [(]|\\s&&\\s|\\s\\|\\|\\s|\\s=>").length);
                        rules.add(rule);
                        confidences.add(rule.getConfidence());
                        supports.add(rule.getSupport());
                        ruleLengths.add(rule.getRuleLength());
                    }
                    else{
                        String defaultRule = "";
                        for(int j = 3; j < classifierRules.length - 3; j++){
                            if(j != i){
                                String value = classifierRules[j].substring(0, classifierRules[j].indexOf(" =>"));
                                value = value.replaceAll("[)] and [(]",") || (");
                                value = value.replaceAll("[)(]", "");
                                defaultRule += "(" + makeNegative(value) + ")" + " and ";
                            }
                        }
                        if(defaultRule.length() > 0)
                            defaultRule = defaultRule.substring(0, defaultRule.length() - 5);
                        defaultRule += classifierRules[i].substring(classifierRules[i].indexOf(" =>"), classifierRules[i].length() - classifierRules[i].indexOf(" =>"));
                        Correlation correlation = new Correlation(constraint, getAntecedent(defaultRule), getConsequent(defaultRule));
                        Rule rule = new Rule(classifierRules[i], Summary.getRelativeSupport(featureVectors, correlation), Summary.getConfidence(featureVectors,
                                correlation), (double)classifierRules[i].split("[)] and [(]|[)] or [(]|\\s&&\\s|\\s\\|\\|\\s|\\s=>").length);
                        rules.add(rule);
                        confidences.add(rule.getConfidence());
                        supports.add(rule.getSupport());
                        ruleLengths.add(rule.getRuleLength());
                    }
                }
                for(Rule rule: rules)
                    System.out.println(rule);

                System.out.println("\nAverage support: " + String.format("%.3f", supports.stream().mapToDouble(e -> e).average().getAsDouble()));
                System.out.println("Average confidence: " + String.format("%.3f", confidences.stream().mapToDouble(e -> e).average().getAsDouble()));
                System.out.println("Average rule length: " + String.format("%.2f", ruleLengths.stream().mapToDouble(e -> e).average().getAsDouble()));

                Evaluation eval = new Evaluation(data);
                eval.evaluateModel(classifier, data);

                System.out.println("Rules in total: " + classifier.getRuleset().size());

                return eval;
            }
        } catch (Exception e) {
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

    public static HashMap<String, List<String>> getAntecedent(String rule) {
        HashMap<String, List<String>> antecedent = new HashMap<>();

        String[] q = rule.split(" => Class=");
        String q1 = q[0];
        String joinType = "";
        if (q1.contains(") and ("))
            joinType = "AND";
        else
            joinType = "OR";
        String[] ant = q1.split("[)] and [(]|[)] or [(]");
        for (int i = 0; i < ant.length; i++)
            ant[i] = ant[i].replaceAll("[)]|[(]", "");
        antecedent.put(joinType, Arrays.asList(ant));
        return antecedent;
    }

    public static HashMap<String, List<String>> getConsequent(String rule) {
        HashMap<String, List<String>> consequent = new HashMap<>();

        rule = rule.replaceAll("\\s\\([\\d\\.]+/[\\d\\.]+\\)", "");
        String[] q = rule.split(" => Class=");
        String q2 = q[1];
        String joinType = "";
        if (q2.contains(") and ("))
            joinType = "AND";
        else
            joinType = "OR";
        String[] csq = q2.split("[)] and [(]|[)] or [(]");
        for (int i = 0; i < csq.length; i++)
            csq[i] = csq[i].replaceAll("[)]|[(]", "");
        consequent.put(joinType, Arrays.asList(csq));
        return consequent;
    }
}