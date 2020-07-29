package ai.aliz.talendtestrunner.util;

import ai.aliz.talendtestrunner.testcase.TestCase;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestCollector {
    private boolean TEST_FILTERING_ENABLED = true;
    private static final String BASE_TEST_FOLDER = "";

    public List<TestCase> listTestCases() {
        Stream<Path> testGroupPaths = listDirectoryContents(Paths.get(BASE_TEST_FOLDER));

        Stream<Path> jobTestFolders = testGroupPaths.flatMap(this::listDirectoryContents)
                .filter(Files::isDirectory);
        Stream<Path> testCaseFolders = jobTestFolders.flatMap(this::listCasesForTest);

        List<TestCase> testCases = testCaseFolders.map(this::testPathToTestCase).collect(Collectors.toList());
        if (TEST_FILTERING_ENABLED) {
            return filterTests(testCases);
        } else {
            return testCases;
        }

    }

    @SneakyThrows
    private List<TestCase> filterTests(List<TestCase> testCases) {
        InputStream enabledTestsInputStream = getClass().getClassLoader().getResourceAsStream("enabled_tests.properties");
        Set<String> enabledTests = readStringListInputStream(enabledTestsInputStream);
        return testCases.stream().filter(testCase -> enabledTests.contains(testCase.getJobName())).collect(Collectors.toList());
    }

    private Set<String> readStringListInputStream(InputStream inputStream) throws IOException {
        String enabledTestsString = IOUtils.toString(inputStream, "UTF-8");
        String[] lines = enabledTestsString.split("\n");
        return Arrays.stream(lines)
                .filter(line -> !line.trim().isEmpty())
                .map(this::withoutCommentedPart)
                .flatMap(line -> Arrays.stream(line.split(",")))
                .map(String::trim)
                .filter(testName -> !testName.isEmpty())
                .collect(Collectors.toSet());
    }

    private String withoutCommentedPart(String inputLine) {
        if (inputLine.contains("#")) {
            int commentMarkerIndex = inputLine.indexOf("#");
            return inputLine.substring(0, commentMarkerIndex);
        } else {
            return inputLine;
        }
    }

    private TestCase testPathToTestCase(Path testCasePath) {
        boolean testCaseFolder = isTestCaseFolder(testCasePath);

        Path jobTestPath = testCaseFolder ? testCasePath.getParent() : testCasePath;
        String caseId = testCaseFolder ? testCasePath.getFileName().toString() : "case1";
        String group = jobTestPath.getParent().getFileName().toString();
        String jobName = jobTestPath.getFileName().toString();

        TestCase testCase = new TestCase();
        testCase.setBaseFolder(jobTestPath);
        testCase.setJobName(jobName);
        testCase.setGroup(group);
        testCase.setCaseId(caseId);
        testCase.setPreparationFiles(collectNormalFilesRecursive(testCasePath.resolve("pre")));
        testCase.setAssertionDefinitions(collectAssertions(testCasePath));

        return testCase;
    }

    private List<AssertionDefition> collectAssertions(Path testCasePath) {
        List<Path> files = collectNormalFilesRecursive(testCasePath.resolve("assert"));
        return files.stream()
                .filter(file -> !isIgnoreFile(file))
                .map(this::assertFileToAssertionDefinition)
                .collect(Collectors.toList());
    }

    @SneakyThrows
    private AssertionDefition assertFileToAssertionDefinition(Path assertFile) {
        AssertionDefition result = new AssertionDefition();
        result.setContentFile(assertFile);

        Path parent = assertFile.getParent();
        Path fileName = assertFile.getFileName();


        String inexactFileName = "." + fileName + ".inexact";

        Path inexactFilePath = parent.resolve(inexactFileName);
        if (Files.exists(inexactFilePath)) {
            try (FileInputStream inexactMatchFileInputStream = new FileInputStream(inexactFilePath.toFile())) {
                Set<String> inexactMatchFields = readStringListInputStream(inexactMatchFileInputStream);
                result.setInexactMatchFields(inexactMatchFields);
            }
        }

        return result;
    }

    private boolean isIgnoreFile(Path file) {
        return file.getFileName().toString().startsWith(".");
    }

    private List<Path> collectNormalFilesRecursive(Path currentPath) {
        List<Path> directChildren = listDirectoryContentsSafe(currentPath);
        List<Path> result = Lists.newArrayList(directChildren);

        List<Path> directories = directChildren.stream().filter(Files::isDirectory).collect(Collectors.toList());
        result.removeAll(directories);

        List<Path> leafFiles = directories.stream().flatMap(directory -> collectNormalFilesRecursive(directory).stream())
                .collect(Collectors.toList());
        result.addAll(leafFiles);

        return result;
    }

    private List<Path> listDirectoryContentsSafe(Path baseDir) {
        if (Files.exists(baseDir) && Files.isDirectory(baseDir)) {
            return listDirectoryContents(baseDir).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @SneakyThrows
    private Stream<Path> listDirectoryContents(Path baseTestFolder) {
        try (Stream<Path> fileStream = Files.list(baseTestFolder)) {
            // stream -> list -> stream to avoid resource leaks
            return fileStream.collect(Collectors.toList()).stream();
        }
    }

    private Stream<Path> listCasesForTest(Path baseTestFolder) {
        // in some cases there are no "case{n}" folders...
        boolean testDirectlyUnderTestFolder = listDirectoryContents(baseTestFolder)
                .anyMatch(path -> path.toString().endsWith("pre") || path.toString().endsWith("assert"));
        if (testDirectlyUnderTestFolder) {
            return Stream.of(baseTestFolder);
        }

        boolean casesUnderTestfolder = listDirectoryContents(baseTestFolder).anyMatch(this::isTestCaseFolder);
        if (casesUnderTestfolder) {
            return listDirectoryContents(baseTestFolder);
        }

        throw new IllegalStateException(String.format("Folders found under baseTestFolder %s are neither cases, nor test definitions.", baseTestFolder));
    }

    private boolean isTestCaseFolder(Path path) {
        Path fileName = path.getFileName();
        return fileName.toString().toLowerCase().matches("case( )*[0-9]+");
    }

    @Data
    public static class AssertionDefition {
        private Path contentFile;

        Set<String> inexactMatchFields = Sets.newHashSet();
    }


}
