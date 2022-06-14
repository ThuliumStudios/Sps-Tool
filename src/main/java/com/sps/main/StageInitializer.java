package com.sps.main;

import javafx.stage.Stage;
import org.springframework.context.ApplicationListener;

public class StageInitializer implements ApplicationListener<SPS_ATS.StageReadyEvent> {

    @Override
    public void onApplicationEvent(SPS_ATS.StageReadyEvent event) {
        Stage stage = event.getStage();
    }
}
