package ai.aliz.talendtestrunner.talend;

import lombok.Data;

@Data
public class Executable {

    private String executable;
    private String name;
    private Workspace workspace;
}
