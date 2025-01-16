package com.survivorsim.plugins.model.reports;

import java.util.function.Consumer;

import gov.hhs.aspr.ms.gcm.simulation.nucleus.ExperimentContext;

public class OutputConsumer implements Consumer<ExperimentContext> {

    @Override
	public void accept(ExperimentContext experimentContext) {
		experimentContext.subscribeToOutput(Object.class, this::handleOutput);
	}

	private void handleOutput(ExperimentContext experimentContext, Integer scenarioId, Object output) {
		System.out.println("scenario " + scenarioId + ": " + output);
	}

}
