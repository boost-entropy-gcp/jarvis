package ai.aliz.talendtestrunner.helper;

import lombok.experimental.UtilityClass;

import java.io.File;

@UtilityClass
public class TestHelper {

    public String addSeparator(String path) {
        return path.replace('\\', File.separatorChar);
    }
}
