package com.infusi.waflow.flow;

import java.util.List;

/**
 * A conversational flow composed of ordered steps.
 */
public interface Flow {

    String getId();

    String getName();

    String getInitialStep();

    FlowStep getStep(String stepId);

    List<FlowStep> getSteps();
}
