package com.sps.bullhorn;

import com.bullhornsdk.data.api.BullhornData;
import com.bullhornsdk.data.api.BullhornRestCredentials;
import com.bullhornsdk.data.api.StandardBullhornData;
import com.bullhornsdk.data.model.entity.core.standard.*;
import com.bullhornsdk.data.model.entity.core.type.BullhornEntity;
import com.bullhornsdk.data.model.entity.embedded.OneToMany;
import com.bullhornsdk.data.model.parameter.standard.ParamFactory;
import com.bullhornsdk.data.model.response.resume.ParsedResume;
import javafx.scene.control.Label;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.joda.time.DateTime;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class BullhornAPI {
	public static final String username = "SouthernPoint.API";
	public static final String password = "$p$1501!";
	public static final String authUrl = "https://auth-east.bullhornstaffing.com/oauth/authorize";
	public static final String clientID = "6039f6b6-c91a-42be-bd7c-b61f69b4dea6";
	public static final String clientSecret = "1ocsoDf4JNPLnTYFOWcd96Ol";
	public static final String loginUrl = "https://rest.bullhornstaffing.com/rest-services/login";
	public static final String tokenUrl = "https://auth-east.bullhornstaffing.com/oauth/token";
	public static final String minutesToLive = "60";

	private String createdStatus = "Created";

	private Set<String> fieldSet = new HashSet<>();

	private BullhornRestCredentials creds;
	private BullhornData bullhornData;

	private Properties bullhornProps;
	private Logger logger;
	private Path filePath;

	public BullhornAPI() {
		System.out.println("Connecting to Bullhorn API");
		bullhornProps = new Properties();

		logger = LogManager.getLogger(com.sps.bullhorn.BullhornAPI.class);//.getRootLogger();
		logger.trace("Configuration File :: " + System.getProperty("log4j.configurationFile"));

		filePath = Path.of(System.getProperty("user.home")).resolve("placementFile.dat");

		try {
			// TEST!
			InputStream in = getClass().getResourceAsStream("/bullhorn.properties");
			bullhornProps.load(in);

			// bullhornProps.load(new FileInputStream("./src/main/resources/bullhorn.properties"));
			// bullhornProps.load(new FileInputStream("bullhorn.properties"));
			System.out.println(bullhornProps.getProperty("username"));

			if (!Files.notExists(filePath)) {
				Path path = Files.createFile(filePath);
				String latestID = JOptionPane.showInputDialog("");
				Files.writeString(path, latestID);
				logger.warn("File " + filePath + " was created. Began with Placement ID #" + latestID);
			}
		} catch (IOException e) {
			logger.error("File not created. \n" + e);
		}

		connectToServer();
	}

	public void connectToServer() {
		creds = new BullhornRestCredentials();
		creds.setUsername(bullhornProps.getProperty("username"));
		creds.setPassword(bullhornProps.getProperty("password"));
		logger.info("Credentials verified. Authorizing URL. . .");
		creds.setRestAuthorizeUrl(bullhornProps.getProperty("authUrl"));
		logger.info("URL authorized. Verifying client ID & secret. . .");
		creds.setRestClientId(bullhornProps.getProperty("clientID"));
		creds.setRestClientSecret(bullhornProps.getProperty("clientSecret"));
		logger.info("Client verified. Logging in. . .");
		creds.setRestLoginUrl(bullhornProps.getProperty("loginUrl"));
		logger.info("Logged in. Obtaining REST token. . .");
		creds.setRestSessionMinutesToLive(bullhornProps.getProperty("minutesToLive"));
		creds.setRestTokenUrl(bullhornProps.getProperty("tokenUrl"));
		bullhornData = new StandardBullhornData(creds);
		logger.info("Successfully connected.");
	}

	public Candidate getCandidate(int id) {
		return bullhornData.findEntity(Candidate.class, id, fieldSet);
	}

	public Candidate getCandidate(int id, Set<String> fieldSet) {
		return bullhornData.findEntity(Candidate.class, id, fieldSet);
	}

	public Placement getPlacement(int id, Set<String> fieldSet) {
		return bullhornData.findEntity(Placement.class, id, fieldSet);
	}

	public <T extends BullhornEntity> T get(Class<T> entity, int id, Set<String> fieldSet) {
		return bullhornData.findEntity(entity, id, fieldSet);
	}

	public Candidate addJobNote(String jobName, String source, int id, String date) {
		Candidate c = bullhornData.findEntity(Candidate.class, id, fieldSet);
		OneToMany<Candidate> otmc = new OneToMany<>(c);
		Note note = new Note();

		// Create note and associate with employee/candidate
		note.setAction(source + " Application");
		note.setComments(date + " - Applied for " + jobName + " on " + source);

		// note.setAction("Indeed Application");
		// note.setComments(Units.todayMinus(daysAgo) + " - Applied for " + jobName + " on Indeed");

		note.setPersonReference(new Person(id));
		note.setCandidates(otmc);

		bullhornData.insertEntity(note);
		return c;
	}

	public Candidate addNewCandidate(String resumeText, String jobName, String source, String date) {
		ParsedResume resume = bullhornData.parseResumeText(resumeText, ParamFactory.resumeTextParseParams());
		createdStatus = "Created";

		Candidate c = bullhornData.saveParsedResumeDataToBullhorn(resume).getCandidate();

		// This checks if a candidate exists, for some reason
		if (c.getName() == null && c.getId() != null) {
			createdStatus = "Updated existing";

			c = bullhornData.findEntity(Candidate.class, c.getId(), fieldSet);
			c.setDescription(resumeText);

			// resume.setCandidate(c);
		}

		c.setSource(source);
		if (updateStatus(c.getStatus()))
			c.setStatus("New Lead");

		addJobNote(jobName, source, c.getId(), date);
		bullhornData.updateEntity(c);
		return c;
	}

	public JobSubmission addJobSubmission(String source, JobOrder job, Candidate candidate) {
		JobSubmission submission = new JobSubmission();

		submission.setCandidate(candidate);
		submission.setDateAdded(DateTime.now());
		submission.setSource(source.toLowerCase());
		submission.setJobOrder(job);
		bullhornData.insertEntity(submission);

		return submission;
	}

	public boolean updateStatus(String status) {
		String[] exemptStatuses = { "On-Assignment", "Interview Pending", "Not Eligible for Re-hire" };
		for (String s : exemptStatuses)
			if (status != null && status.equalsIgnoreCase(s))
				return false;
		return true;
	}

	public String getCreatedStatus() {
		return createdStatus;
	}

	public void uploadResume(Candidate c, File file) {
		// Process file
		try {
			MultipartFile resume = new MockMultipartFile(file.getName(), file.getName(), "text/plain",
					Files.readAllBytes(file.toPath()));

			bullhornData.parseResumeThenAddfile(Candidate.class, c.getId(), resume, "Resume",
					ParamFactory.fileParams(), ParamFactory.resumeFileParseParams());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
