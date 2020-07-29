package ai.aliz.talendtestrunner.talend;

import com.google.common.collect.ImmutableList;
import lombok.Data;

import java.util.List;

@Data
public class Execution {

    public static final List<String> RUNNING_STATES =
            ImmutableList.of("DISPATCHING_FLOW", "STARTING_FLOW_EXECUTION", "EXECUTION_EVENT_RECEIVED");

    public static final String SUCCESS_STATE = "EXECUTION_SUCCESS";

    private String executionId;
    private String executionStatus;
}
