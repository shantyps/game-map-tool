package ps.shanty.tool.map;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.Map;

public class AreaEnum {
    private String inputType;
    private String outputType;
    @JsonAlias("default")
    private int defaultValue;
    private Map<String, String> values;

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public int getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(int defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public void setValues(Map<String, String> values) {
        this.values = values;
    }
}
