package com.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class Controller {

    @FXML
    private TextField wordInput;
    @FXML
    private ListView<String> wordList;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private TextArea reportArea;

    // Backend state
    private final List<String> forbiddenWords = Collections.synchronizedList(new ArrayList<>());
    private final List<String> reportLines = Collections.synchronizedList(new ArrayList<>());
    private File inputFolder;
    private File outputFolder;
    private ExecutorService executor;
    private volatile boolean paused = false;
    private volatile boolean stopped = false;

    @FXML
    public void initialize() {
        System.out.println("Controller loaded successfully!");
    }

    @FXML
    private void handleAddWord() {
        if (!wordInput.getText().isEmpty()) {
            forbiddenWords.add(wordInput.getText());
            wordList.getItems().add(wordInput.getText());
            wordInput.clear();
        }
    }

    @FXML
    private void handleChooseInput() {
        DirectoryChooser chooser = new DirectoryChooser();
        inputFolder = chooser.showDialog(new Stage());
        if (inputFolder != null) {
            reportArea.appendText("Input folder set: " + inputFolder.getAbsolutePath() + "\n");
        }
    }

    @FXML
    private void handleChooseOutput() {
        DirectoryChooser chooser = new DirectoryChooser();
        outputFolder = chooser.showDialog(new Stage());
        if (outputFolder != null) {
            reportArea.appendText("Output folder set: " + outputFolder.getAbsolutePath() + "\n");
        }
    }

    @FXML
    private void handleStartSearch() {
        if (inputFolder != null && outputFolder != null && !forbiddenWords.isEmpty()) {
            reportArea.appendText("Search started...\n");

            executor = Executors.newFixedThreadPool(4);
            stopped = false;
            paused = false;

            try {
                List<Path> files = Files.walk(inputFolder.toPath())
                        .filter(Files::isRegularFile)
                        .toList();

                int totalFiles = files.size();
                AtomicInteger processed = new AtomicInteger(0);

                for (Path file : files) {
                    executor.submit(() -> {
                        if (stopped) {
                            return;
                        }

                        // Pause loop
                        while (paused && !stopped) {
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException ignored) {
                            }
                        }

                        processFile(file, outputFolder.toPath());
                        int done = processed.incrementAndGet();
                        double progress = (double) done / totalFiles;

                        Platform.runLater(() -> progressBar.setProgress(progress));

                        if (done == totalFiles && !stopped) {
                            Platform.runLater(() -> writeReport(outputFolder.toPath()));
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            reportArea.appendText("Please set folders and forbidden words before starting.\n");
        }
    }

    private void processFile(Path file, Path outputFolder) {
        try {
            String content = Files.readString(file);
            boolean flagged = false;
            int replacements = 0;
            List<String> replacedWords = new ArrayList<>();

            for (String word : forbiddenWords) {
                if (content.contains(word)) {
                    flagged = true;
                    int count = content.split(word, -1).length - 1;
                    replacements += count;
                    content = content.replaceAll(word, "*******");
                    replacedWords.add(word);
                }
            }

            if (flagged) {
                Path copied = outputFolder.resolve(file.getFileName());
                Files.writeString(copied, content);

                synchronized (reportLines) {
                    reportLines.add("File: " + file
                            + " | Size: " + Files.size(file)
                            + " | Replacements: " + replacements
                            + " | Words replaced: " + replacedWords);
                }
            } else {
                synchronized (reportLines) {
                    reportLines.add("File: " + file + " | No forbidden words found");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeReport(Path outputFolder) {
        Path reportFile = outputFolder.resolve("report.txt");
        try {
            Files.write(reportFile, reportLines);
            reportArea.appendText("Report written to: " + reportFile + "\n");
            for (String line : reportLines) {
                reportArea.appendText(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePause() {
        paused = true;
        reportArea.appendText("Search paused...\n");
    }

    @FXML
    private void handleResume() {
        paused = false;
        reportArea.appendText("Search resumed...\n");
    }

    @FXML
    private void handleStop() {
        stopped = true;
        if (executor != null) {
            executor.shutdownNow();
        }
        reportArea.appendText("Search stopped.\n");
    }
}
