package ai.aliz.jarvis.service.shared;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.base.Preconditions;

import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.Context;
import ai.aliz.jarvis.context.ContextLoader;

@Service
public class ActionCreatorHelperService {
    
    public Context getContext(ContextLoader contextLoader, String fileName) {
        Context context = contextLoader.getContext(fileName);
        Preconditions.checkNotNull(context, "No context exists with name: %s", fileName);
        return context;
    }
    
    public Path getTargetFolderPath(File testCaseFolder, String folderName) {
        Path folderPath = Paths.get(testCaseFolder.getAbsolutePath(), folderName);
        Preconditions.checkArgument(Files.isDirectory(folderPath), "%s folder does not exists %s", folderName, folderPath);
        return folderPath;
    }
}
