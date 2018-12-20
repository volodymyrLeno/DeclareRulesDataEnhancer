package data;

public class Rule {
    String rule;
    Double support;
    Double confidence;
    Double ruleLength;

    public Rule(String rule, Double support, Double confidence, Double ruleLength){
        this.rule = rule.replace(" => Class=", " => ").replaceAll("\\s\\([\\d\\.]+/[\\d\\.]+\\)", "");
        this.support = support;
        this.confidence = confidence;
        this.ruleLength = ruleLength;
    }

    public String toString(){
        return this.rule + "   (sup = " + String.format("%.2f", this.support) + ", conf = " + String.format("%.2f", this.confidence) + ", ruleLength = " + String.format("%.2f", this.ruleLength) + ")";
    }

    public Double getSupport(){
        return this.support;
    }

    public Double getConfidence(){
        return this.confidence;
    }

    public Double getRuleLength(){
        return this.ruleLength;
    }
}
