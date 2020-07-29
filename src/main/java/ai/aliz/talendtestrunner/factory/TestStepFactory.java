package ai.aliz.talendtestrunner.factory;

import ai.aliz.talendtestrunner.context.Context;
import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.db.BigQueryExecutor;
import ai.aliz.talendtestrunner.db.MxSQLQueryExecutor;
import ai.aliz.talendtestrunner.service.BigQueryAssertor;
import ai.aliz.talendtestrunner.service.SftpService;
import ai.aliz.talendtestrunner.service.TalendJobStateChecker;
import ai.aliz.talendtestrunner.util.TestCollector;
import com.google.common.base.Joiner;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@Slf4j
public class TestStepFactory {

    @Autowired
    private BigQueryExecutor bigQueryExecutor;

    @Autowired
    private MxSQLQueryExecutor mxSQLTestStepExecutor;

    @Autowired
    private TalendJobStateChecker talendJobStateChecker;

    @Autowired
    private BigQueryAssertor bigQueryAssertor;

    @Autowired
    private ContextLoader contextLoader;

    @Autowired
    private SftpService sftpService;


    public Runnable createPreparationRunnable(Path preparationStepFile) {
        String filename = preparationStepFile.getFileName().toString();

        if ("BQ.bql".equals(filename)) {
            Context bigQueryContext = contextLoader.getContext("BigQuery");
            return () -> bigQueryExecutor.executeScript(readFile(preparationStepFile), bigQueryContext);
        } else if ("TalendDatabase.sql".equals(filename)) {
            Context talendDbContext = contextLoader.getContext("TalendDb");
            return () -> mxSQLTestStepExecutor.executeScript(readFile(preparationStepFile), talendDbContext);
        } else if ("navitaire.sql".equalsIgnoreCase(filename)) {
            Context navitaireContext = contextLoader.getContext("NAVITAIRE");
            return () -> mxSQLTestStepExecutor.executeStatement(readFile(preparationStepFile), navitaireContext);
        } else if (preparationStepFile.toString().contains("sftp-password")) {
            Context sftpPsswordContext = contextLoader.getContext("SFTP-password");
            return () -> sftpService.prepareFolder(preparationStepFile, sftpPsswordContext);
        } else {
            log.warn("Couldn't find executor for file {}. Skipping.", preparationStepFile);
            return () -> {
            };
        }
    }


    public Runnable createAssertionRunnable(TestCollector.AssertionDefition assertionDefition) {
        Path assertionStepFile = assertionDefition.getContentFile();
        String filename = assertionStepFile.getFileName().toString();

        String fileContent = readFile(assertionStepFile);
        String parentFolderName = assertionStepFile.getParent().getFileName().toString();
        if ("talendDatabase".equals(parentFolderName) ||
                "talend_database".equals(parentFolderName)) {
            return () -> talendJobStateChecker.checkJobState(fileContent, contextLoader.getContext("TalendDb"));
        } else if ("bq".equals(assertionStepFile.getParent().getParent().getFileName().toString())) {
            String qualifiedTableName = parentFolderName + "." + filename.replace(".json", "");
            bigQueryAssertor.assertTable(qualifiedTableName, fileContent, assertionDefition.getInexactMatchFields(),
                    contextLoader.getContext("BigQuery"));
        }

        return null;
    }


    @SneakyThrows
    public String readFile(Path file) {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        return Joiner.on('\n').join(lines);
    }

}
