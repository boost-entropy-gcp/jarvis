package ai.aliz.talendtestrunner.talend;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Environment {

    private String id;
    private String name;
    @JsonProperty("default")
    private boolean _default;
}
