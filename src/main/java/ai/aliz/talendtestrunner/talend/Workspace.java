package ai.aliz.talendtestrunner.talend;

import lombok.Data;

@Data
public class Workspace {

    private String id;
    private String name;
    private String owner;
    private String type;
    private Environment environment;
}
