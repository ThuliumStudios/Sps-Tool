package com.sps.main;

import com.sps.bullhorn.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class SPS_ATS extends Application {
	private ConfigurableApplicationContext applicationContext;
	private Scene scene;
	private Pane root;

	private Button resumeParser, newEmployeeReport;
	private Label statusLabel;

	private BullhornAPI bullhorn;

	@Override
	public void init() {
		applicationContext = new SpringApplicationBuilder(SpsAtsApplication.class).run();
	}

	@Override
	public void start(Stage stage) {
		applicationContext.publishEvent(new StageReadyEvent(stage));

		stage.setTitle("Southern Point Staffing Utility");
		stage.setAlwaysOnTop(true);
		stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon64.png")));

		root = new GridPane();
		scene = new Scene(root, 640, 480);

		scene.setFill(Color.LIGHTBLUE);

		stage.setScene(scene);
		stage.sizeToScene();
		stage.show();
		stage.centerOnScreen();

		SimpleDateFormat date = new SimpleDateFormat("E");
		System.out.println("Today is " + date.format(Calendar.getInstance().getTime()));

		initializeBullhorn(root);
	}

	public void initializeBullhorn(Pane pane) {
		bullhorn = new BullhornAPI();
		
		statusLabel = new Label("Contacting Bullhorn API Servers. . . ");

		// contentPanel.add(statusLabel, BorderLayout.NORTH);
		bullhorn.connectToServer();
		
		resumeParser = new Button("Resume Parser");
		newEmployeeReport = new Button("New Hire Export");

		resumeParser.setOnAction(event -> {
			scene.setRoot(new ResumeParser(bullhorn));
		});
		newEmployeeReport.setOnAction(event -> {
			scene.setRoot(new EmployeeImportGenerator(bullhorn));
		});

		// Create the pane layout
		HBox hbox = new HBox();
		hbox.setAlignment(Pos.BOTTOM_CENTER);
		hbox.setPadding(new Insets(8));
		hbox.setSpacing(8);

		hbox.getChildren().addAll(resumeParser, newEmployeeReport);

		pane.getChildren().add(hbox);
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		applicationContext.close();
		Platform.exit();
	}

	static class StageReadyEvent extends ApplicationEvent {
		public StageReadyEvent(Stage stage) {
			super(stage);
		}

		public Stage getStage() {
			return ((Stage) getSource());
		}
	}
}
