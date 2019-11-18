import data.Correlation;
import data.Event;
import data.FeatureVector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by volodymyr leno on 13.02.2018.
 */
public final class Summary {

    public static void getCoverage(HashMap<String, List<Event>> cases, List<FeatureVector> featureVectors, String ruleType){
        Integer coveredNumber = featureVectors.size();
        Integer totalNumber = 0;
        for(String caseID: cases.keySet()){
            List<String> activities = new ArrayList<>();
            cases.get(caseID).stream().filter(event -> !activities.contains(event.activityName)).
                    forEach(event -> activities.add(event.activityName));
            for(String activity1: activities)
                for(String activity2: activities)
                    totalNumber += getFrequency(cases.get(caseID), activity1, activity2, ruleType);
        }
        System.out.println("Total number: " + totalNumber);
        System.out.println("Covered number: " + coveredNumber);
    }

    static Integer getFrequency(List<Event> eventList, String from, String to, String ruleType) {
        Integer frequency = 0;
        List<Integer> id1 = new ArrayList<>();
        List<Integer> id2 = new ArrayList<>();
        for (int i = 0; i < eventList.size(); i++) {
            if (eventList.get(i).activityName.equals(from)) id1.add(i);
            if (eventList.get(i).activityName.equals(to)) id2.add(i);
        }
        if(id1.size() > 0 && id2.size() > 0){
            switch (ruleType) {
                case "precedence":
                case "chain precedence":
                    for (Integer i2 : id2)
                        for (Integer i1 : id1)
                            if ((ruleType.equals("precedence") && i2 > i1) || (ruleType.equals("chain precedence") && (i2 - i1 == 1))) {
                                frequency++;
                                break;
                            }
                    break;
                case "response":
                case "chain response":
                    for (Integer i1 : id1)
                        for (Integer i2 : id2)
                            if ((ruleType.equals("response") && i2 > i1) || (ruleType.equals("chain response") && (i2 - i1 == 1))) {
                                frequency++;
                                break;
                            }
                    break;
                case "responded existence":
                    for (Integer i1 : id1)
                        for (Integer i2 : id2) {
                            frequency++;
                            break;
                        }
                    break;
                case "alternate response":
                    for (Integer i1 : id1)
                        for (Integer i2 : id2)
                            if (i2 > i1 && id1.stream().noneMatch(el -> el > i1 && el < i2)) {
                                frequency++;
                                break;
                            }
                    break;
                case "alternate precedence":
                    for (Integer i2 : id2)
                        for (Integer i1 : id1)
                            if (i2 > i1 && id2.stream().noneMatch(el -> el > i1 && el < i2)) {
                                frequency++;
                                break;
                            }
                    break;
            }
        }
        return frequency;
    }

    static double getRelativeSupport(List<FeatureVector> featureVectorList, Correlation correlation){
        if(correlation.consequent.containsKey("OR") && correlation.consequent.get("OR").contains("-"))
            return(double)featureVectorList.stream().filter(fv -> ruleSatisfaction(fv.from, correlation.antecedent) && fv.to == null).collect(Collectors.toList()).size()/featureVectorList.size();
        else
            return(double)featureVectorList.stream().filter(fv -> fv.from != null && fv.to != null).filter(fv -> ruleSatisfaction(fv.from, correlation.antecedent) &&
                    ruleSatisfaction(fv.to, correlation.consequent)).collect(Collectors.toList()).size()/featureVectorList.size();
    }

    static double getConfidence(List<FeatureVector> featureVectorList, Correlation correlation){
        Integer coverage = featureVectorList.stream().filter(fv -> ruleSatisfaction(fv.from, correlation.antecedent)).collect(Collectors.toList()).size();
        Integer ruleFrequency;
        if(correlation.consequent.containsKey("OR") && correlation.consequent.get("OR").contains("-"))
            ruleFrequency = featureVectorList.stream().filter(fv -> ruleSatisfaction(fv.from, correlation.antecedent) &&
                    fv.to == null).collect(Collectors.toList()).size();
        else
            ruleFrequency = featureVectorList.stream().filter(fv -> fv.from != null && fv.to != null).filter(fv -> ruleSatisfaction(fv.from, correlation.antecedent) &&
                    ruleSatisfaction(fv.to, correlation.consequent)).collect(Collectors.toList()).size();

        return (double)ruleFrequency/coverage;
    }

    static boolean ruleSatisfaction(HashMap<String, String> payload, HashMap<String, List<String>> rules) {
        String joinType = rules.containsKey("OR") ? "OR" : "AND";
        List<Boolean> localOutcomes = new ArrayList<>();
        List<Boolean> globalOutcomes = new ArrayList<>();
        if (joinType.equals("AND")) {
            for (String rule : rules.get(joinType)) {
                if(rule.equals(""))
                    return true;
                String localJointType = "";
                localOutcomes.clear();
                String[] conditions;
                if(rule.contains(" || ")){
                    localJointType = "OR";
                    conditions = rule.split("\\s\\|\\|\\s");
                }
                else {
                    localJointType = "AND";
                    conditions = rule.split("\\s&&\\s");
                }
                for (String condition : conditions) {
                    Pattern pattern = Pattern.compile("[<!>=]+");
                    Matcher matcher = pattern.matcher(condition);
                    String operator = "";
                    if (matcher.find())
                        operator = matcher.group();

                    String[] params = condition.split("\\s[<!>=]+\\s");
                    String attribute = params[0];
                    String value = params[1];
                    Boolean out = false;
                    switch (operator) {
                        case "=":
                            if (payload.get(attribute).equals(value)) out = true;
                            break;
                        case "!=":
                            if (!payload.get(attribute).equals(value)) out = true;
                            break;
                        case ">":
                            if (Double.parseDouble(payload.get(attribute)) > Double.parseDouble(value)) out = true;
                            break;
                        case ">=":
                            if (Double.parseDouble(payload.get(attribute)) >= Double.parseDouble(value)) out = true;
                            break;
                        case "<":
                            if (Double.parseDouble(payload.get(attribute)) < Double.parseDouble(value)) out = true;
                            break;
                        case "<=":
                            if (Double.parseDouble(payload.get(attribute)) <= Double.parseDouble((value))) out = true;
                            break;
                        default:
                            break;
                    }
                    localOutcomes.add(out);
                }
                if(localJointType.equals("AND")){
                    if(localOutcomes.contains(false))
                        globalOutcomes.add(false);
                    else
                        globalOutcomes.add(true);
                }
                else{
                    if(localOutcomes.contains(true))
                        globalOutcomes.add(true);
                    else
                        globalOutcomes.add(false);
                }
            }
            if (globalOutcomes.contains(false))
                return false;
            else
                return true;
        } else {
            for (String rule : rules.get(joinType)) {
                if(rule.equals(""))
                    return true;
                localOutcomes.clear();
                String localJointType = "";
                String[] conditions;
                if(rule.contains(" && ")){
                    localJointType = "AND";
                    conditions = rule.split("\\s&&\\s");
                }
                else{
                    localJointType = "OR";
                    conditions = rule.split("\\s\\|\\|\\s");
                }
                for (String condition : conditions) {
                    Pattern pattern = Pattern.compile("[<!>=]+");
                    Matcher matcher = pattern.matcher(condition);
                    String operator = "";
                    if (matcher.find())
                        operator = matcher.group();

                    String[] params = condition.split("\\s[<!>=]+\\s");
                    String attribute = params[0];
                    String value = params[1];
                    Boolean out = true;
                    switch (operator) {
                        case "=":
                            if (!payload.get(attribute).equals(value)) out = false;
                            break;
                        case "!=":
                            if (payload.get(attribute).equals(value)) out = false;
                            break;
                        case ">=":
                            if (Double.parseDouble(payload.get(attribute)) < Double.parseDouble(value)) out = false;
                            break;
                        case ">":
                            if (Double.parseDouble(payload.get(attribute)) <= Double.parseDouble(value)) out = false;
                            break;
                        case "<":
                            if (Double.parseDouble(payload.get(attribute)) >= Double.parseDouble(value)) out = false;
                            break;
                        case "<=":
                            if (Double.parseDouble(payload.get(attribute)) > Double.parseDouble((value))) out = false;
                            break;
                        default:
                            break;
                    }
                    localOutcomes.add(out);
                }
                if(localJointType.equals("AND")){
                    if(localOutcomes.contains(false))
                        globalOutcomes.add(false);
                    else
                        globalOutcomes.add(true);
                }
                else{
                    if(localOutcomes.contains(true))
                        globalOutcomes.add(true);
                    else
                        globalOutcomes.add(false);
                }
            }
            if (globalOutcomes.contains(true))
                return true;
            else
                return false;
        }
    }
}