package com.sps.bullhorn;

import com.bullhornsdk.data.model.entity.core.standard.Candidate;
import com.bullhornsdk.data.model.entity.core.standard.JobOrder;
import com.sps.util.Units;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ResumeParser extends BullhornUtility {
	private final List<Label> newCandidates = new ArrayList<>();

	private Label status;
	private final TextField jobTitleField;
	private final ComboBox<String> dateField;
	private final ComboBox<String> sourceField;

	private final HBox fieldBox;
	private final VBox statusBox;

	public ResumeParser(BullhornAPI bh) {
		super(bh);
		Insets insets = new Insets(16, 64, 16, 64);

		fieldBox = new HBox();
		statusBox = new VBox();

		jobTitleField = new TextField();

		dateField = new ComboBox<>();
		sourceField = new ComboBox<>();

		jobTitleField.setPromptText("Job Title/Job ID");
		dateField.setPromptText("Application Date");
		sourceField.setPromptText("Applicant Source");

		sourceField.setEditable(true);

		for (int i = 0; i <= 30; i++)
			dateField.getItems().add(Units.todayMinus(i));

		for (String str : new String[] {"Facebook", "Indeed"})
			sourceField.getItems().add(str);

		setOnDragOver(event -> {
			if (event.getDragboard().hasFiles()) {
				// allow for both copying and moving, whatever user chooses
				event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
			}
			event.consume();
		});
		setOnDragDropped(event -> {
			List<File> files = event.getDragboard().getFiles();
			new Thread(() -> {
				files.forEach(f -> parse(f));
				Platform.runLater(() -> {
					statusBox.getChildren().clear();
					statusBox.getChildren().add(status);
					statusBox.getChildren().addAll(newCandidates);
				});
//				statusBox.getChildren().clear();
//				statusBox.getChildren().add(status);
//				statusBox.getChildren().addAll(newCandidates);
				event.setDropCompleted(true);
				event.consume();
			}).start();
		});

		status = new Label("Status: Ready to parse.");

		setPadding(insets);
		fieldBox.setPadding(new Insets(0, 8, 0, 8));

		// Add UI elements
		statusBox.getChildren().add(status);
		fieldBox.getChildren().addAll(jobTitleField, sourceField, dateField);

		GridPane.setConstraints(fieldBox, 0, 1);
		GridPane.setHalignment(fieldBox, HPos.CENTER);
		GridPane.setMargin(fieldBox, new Insets(32, 8, 0, 8));
		getChildren().add(fieldBox);

		GridPane.setHgrow(statusBox, Priority.ALWAYS);
		GridPane.setConstraints(statusBox, 0, 2);
		GridPane.setMargin(statusBox, new Insets(32, 8, 0, 8));
		getChildren().add(statusBox);
	}

	public void parse(File file) {
		try {
			// Create Job Order
			int jobID = Integer.parseInt(jobTitleField.getText());
			updateStats("Parsing to Job ID " + jobID + ". . .");
			parseResume(file, jobID);
		} catch (NumberFormatException nfe) {
			// Create and parse Candidate without a Job Submission
			updateStats("Parsing Candidate(s) with Note items. . .");
			parseResume(file);

		}
	}

	public void parseResume(File file) {
		String parsedText = "";

		// Parse pdf to String
		try {
			PDDocument doc = Loader.loadPDF(file);
			parsedText = new PDFTextStripper().getText(doc);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Candidate c = getBullhorn().addNewCandidate(parsedText, jobTitleField.getText(), sourceField.getValue(),
				dateField.getValue());
		updateStats(getBullhorn().getCreatedStatus() + " candidate " + c.getName() + " with ID: " + c.getId());

		getBullhorn().uploadResume(c, file);
		newCandidates.add(new Label(getBullhorn().getCreatedStatus() + " candidate " + c.getName() + " (" + c.getId() + ")"));

		System.out.println(parsedText);
	}

	public void parseResume(File file, int jobID) {
		String parsedText;

		try {
			PDDocument doc = Loader.loadPDF(file);
			parsedText = new PDFTextStripper().getText(doc);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		// Create Job and Note info
		JobOrder job = getBullhorn().get(JobOrder.class, jobID, getFieldSet("id", "title"));

		// Create Candidate
		Candidate c = getBullhorn().addNewCandidate(parsedText, job.getTitle(), sourceField.getValue(),
				dateField.getValue());
		updateStats(getBullhorn().getCreatedStatus() + " candidate " + c.getName() + " with ID: " + c.getId());

		// Upload resume
		getBullhorn().uploadResume(c, file);
		newCandidates.add(new Label(getBullhorn().getCreatedStatus() + " candidate " + c.getName() + " (" + c.getId() + ")"));

		// Create Job Submission
		getBullhorn().addJobSubmission(sourceField.getValue(), job, c);

		System.out.println(parsedText);
	}

	public void updateStats(String str) {
		System.out.println("Status: " + str);
		Platform.runLater(() -> status.setText("Status: " + str));
	}
}
