package com.sps.bullhorn;

import com.bullhornsdk.data.exception.RestApiException;
import com.bullhornsdk.data.model.entity.core.standard.Candidate;
import com.bullhornsdk.data.model.entity.core.standard.Placement;
import com.sps.util.Units;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.apache.poi.ss.usermodel.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class EmployeeImportGenerator extends BullhornUtility{
    // private final String payroll = "C:/Southern Point Staffing/Southern Point Staffing Team Site - Finance/Payroll/Vensure";
    private final String payroll = System.getProperty("user.home") +
            "\\Southern Point Staffing\\Southern Point Staffing Team Site - Finance\\Payroll\\Vensure";

    // private final String res = "./src/main/resources";

    // Initialize all used sets
    private final Set<String> placementFields = fieldSet("candidate", "jobOrder", "dateBegin", "id", "payRate", "customText20",
            "workersCompensationRate");
    private final  Set<String> candidateFields = fieldSet("id", "name", "ssn", "firstName", "lastName", "middleName",
            "dateOfBirth", "gender", "ethnicity", "address", "email", "federalFilingStatus", "customText5",
            "customText6", "customText7", "customText8", "customText9", "customText10", "customText11", "customText12");

    private Path employeePath;
    private Path depositPath;
    private int employeesParsed;

    private Label status;

    public EmployeeImportGenerator(BullhornAPI bullhorn) {
        super(bullhorn);
        System.out.println(payroll);

        // Develop UI components
        Label label = new Label("Enter new employee Placement IDs:");
        Label label2 = new Label("Enter Placement ID to start with:");
        TextField placementIdsField = new TextField();
        TextField startingIdField = new TextField();
        Button generateButton = new Button("Generate Report");
        status = new Label("Status: Ready to parse.");

        placementIdsField.setPromptText("All Placement IDs. . .");
        placementIdsField.setStyle("-fx-prompt-text-fill: derive(-fx-control-inner-background,-30%); }");

        startingIdField.setPromptText("Starting Placement ID. . .");
        startingIdField.setStyle("-fx-prompt-text-fill: derive(-fx-control-inner-background,-30%); }");

        generateButton.setOnAction(e -> {
            updateStats("Generating Imports. . .");


//            new Thread() {
//                @Override
//                public void run() {
//                    super.run();
//                    int currentPlacement = -1;
//                    beginProcess();
//                    if (!startingIdField.getText().isBlank()) {
//                        try {
//                            currentPlacement = Integer.parseInt(startingIdField.getText());
//                        } catch (NumberFormatException nfe) {
//                            status.setText("");
//                        }
//
//                        generateReport(currentPlacement);
//                    } else if (!placementIdsField.getText().isBlank()) {
//                        generateReport(Arrays.stream(placementIdsField.getText().split("[, ]"))
//                                .map(Integer::parseInt)
//                                .collect(Collectors.toList()));
//                    } else {
//
//                    }
//                    // processEntries(placementIdsField.getText());
//                }
//            }.start();

            new Thread(() -> {
                int currentPlacement = -1;
                beginProcess();
                if (!startingIdField.getText().isBlank()) {
                    try {
                        currentPlacement = Integer.parseInt(startingIdField.getText());
                    } catch (NumberFormatException nfe) {
                        status.setText("");
                    }

                    generateReport(currentPlacement);
                } else if (!placementIdsField.getText().isBlank()) {
                    generateReport(Arrays.stream(placementIdsField.getText().split("[, ]"))
                            .map(Integer::parseInt)
                            .collect(Collectors.toList()));
                }
                // processEntries(placementIdsField.getText());
            }).start();
        });

        setPadding(new Insets(16, 64, 16, 64));

        // Add first TextField entry
        GridPane.setHgrow(label, Priority.ALWAYS);
        GridPane.setConstraints(label, 0, 1);
        getChildren().add(label);

        GridPane.setConstraints(placementIdsField, 0, 2);
        getChildren().add(placementIdsField);

        //Add second TextField entry
        GridPane.setHgrow(label2, Priority.ALWAYS);
        GridPane.setConstraints(label2, 0, 3);
        GridPane.setMargin(label2, new Insets(32, 0, 0, 0));
        getChildren().add(label2);

        GridPane.setConstraints(startingIdField, 0, 4);
        getChildren().add(startingIdField);

        GridPane.setConstraints(generateButton, 0, 5);
        GridPane.setHalignment(generateButton, HPos.CENTER);
        GridPane.setMargin(generateButton, new Insets(32, 0, 0, 0));
        getChildren().add(generateButton);

        GridPane.setConstraints(status, 0, 6);
        GridPane.setHalignment(status, HPos.CENTER);
        GridPane.setValignment(status, VPos.BOTTOM);
        GridPane.setMargin(status, new Insets(64, 0, 8, 0));
        getChildren().add(status);
    }

    public void beginProcess() {
        employeesParsed = 0;

        // Copy the employee import templates to the Finance folder for editing
        System.out.println("Copying file");
        try {
            employeePath = Path.of(payroll).resolve(Units.today('-') + "_EmployeeImport.xlsx");
            depositPath = Path.of(payroll).resolve(Units.today('-') + "_EmployeeDeposit.xlsx");

            Files.copy(getClass().getResourceAsStream("/Employee Import Template.xlsx"), employeePath,
                    StandardCopyOption.REPLACE_EXISTING);
            Files.copy(getClass().getResourceAsStream("/Employee Deposit Template.xlsx"), depositPath,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * TASK ORDER:
     *  (if first run) receive first placement ID as dialog input
     *  Create (copy) a new Import Template into the Finance folder
     *  Read Placement IDs from list
     *  Parse ID, pull in Placement/Candidate and map data as normal
     *  Paste mapped values into the copied spreadsheet, starting at row 8
     *  Increment Placement ID, setting the next successful query as the last placement pulled
     *  Once 10 empty placements have been cycled,
     */
    public void generateReport(int currentPlacement) {
        // Loop and process Placement/Candidates until 10 IDs are empty
        int startingPlacement = currentPlacement;
        int attempts = 0;
        while (attempts < 10) {
            Placement p;
            try {
                p = getBullhorn().getPlacement(currentPlacement++, placementFields);
            } catch (RestApiException e) { // Move on
                // e.printStackTrace();
                System.out.println("No Placement found with ID=" + (currentPlacement - 1));
                attempts++;
                continue;
            }

            // Pull in and pass the values associated with this placement
            Candidate candidate = getBullhorn().getCandidate(p.getCandidate().getId(), candidateFields);

            generateEmployeeValues(p, candidate);
            updateStats("Generating Candidate " + candidate.getName());
            employeesParsed++;
            attempts = 0;
        }
        currentPlacement -= 10;
        updateStats("Created import for Placement IDs " + startingPlacement + " through " + (currentPlacement-1));
        System.out.println("Ended. 10 attempts, stopped at " + currentPlacement);
    }

    private void generateReport(List<Integer> ids) {
        ids.forEach(id -> {
            Placement p = getBullhorn().getPlacement(id, placementFields);
            Candidate c = getBullhorn().getCandidate(p.getCandidate().getId(), candidateFields);

            updateStats("Generating Candidate " + c.getName());
            generateEmployeeValues(p, c);
            employeesParsed++;
        });
        updateStats("Created import for Placement IDs in list. Import Generated at\n" + payroll);
    }

    /**
     * Routing number: customText5
     * Account number: customText6
     *
     */
    public void generateEmployeeValues(Placement p, Candidate c) {
        List<String> employee = new ArrayList<>();
        List<String> deposit = new ArrayList<>();

        System.out.println("Looping through candidate " + c.getName());

        // Correct all fields that need adjustment from Bullhorn's API
        String wcCode = p.getWorkersCompensationRate().getCompensation().getCode().substring(2);
        BigDecimal pay = p.getPayRate();
        String group = getGroup(pay);

        employee.addAll(Arrays.asList("16727", c.getSsn(), c.getFirstName(), c.getLastName(), c.getMiddleName(),
                Units.formatDate(c.getDateOfBirth()), trunc(c.getCustomText7(), 1), trunc(c.getEthnicity(), 1),
                "", c.getEmail(), c.getAddress().getAddress1(), "", c.getAddress().getCity(),
                c.getAddress().getState(), c.getAddress().getZip(), "", "", "A", "F", "", Units.formatDate(p.getDateBegin()),
                Units.formatDate(p.getDateBegin()), Units.formatDate(p.getDateBegin()), "", "", "WEEKLY", "H",
                p.getPayRate().toString(), "H", "40", "", "", "", p.getCustomText20(), wcCode, group, group, "100",
                p.getCustomText20(), "", "", "", "", "", "", c.getCustomText8(), "N", c.getCustomText9(),
                c.getCustomText10(), c.getCustomText11(), c.getCustomText12()));
        deposit.addAll(Arrays.asList("16727", c.getName(), "A", "RVS", "", "C", c.getCustomText5(), c.getCustomText6(),
                "RVS", "B", "", "", "P"));

        try {
            generateFile(employeePath, employee);
            generateFile(depositPath, deposit);

            employee.clear();
            deposit.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void generateFile(Path path, List<String> fields) throws IOException {
        // Instantiate workbook
        FileInputStream excelReader = new FileInputStream(path.toString());
        Workbook workbook = WorkbookFactory.create(excelReader);
        Sheet worksheet = workbook.getSheetAt(0);

        // Parse data into rows
        AtomicInteger col = new AtomicInteger();
        Row row = worksheet.getRow(7 + employeesParsed);
        fields.forEach(field -> {
            Cell cell = row.getCell(col.get());
            cell.setCellValue(field);
            col.getAndIncrement();
        });
        excelReader.close();

        // Write to Excel file and close connections
        FileOutputStream excelWriter = new FileOutputStream(path.toString());
        workbook.write(excelWriter);
        excelWriter.close();
        workbook.close();
    }

    public String trunc(String str, int len) {
        return str == null ? "" : str.substring(0, len);
    }

    public String getGroup(BigDecimal payRate) {
        float val = payRate.floatValue();
        if (val >= 20) {
            return "4";
        } else if (val >= 16) {
            return "3";
        } else if (val >= 13) {
            return "2";
        }
        return "1";
    }

    private Set<String> fieldSet(String... fields) {
        return new HashSet<>(Arrays.asList(fields));
    }

    public void updateStats(String str) {
        Platform.runLater(() -> status.setText("Status: " + str));
    }
}