package com.sps.main;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude={DataSourceAutoConfiguration.class})
public class SpsAtsApplication {
	public static void main(String[] args) {
		Application.launch(SPS_ATS.class, args);
	}
}
