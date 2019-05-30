package com.zacran.kimchi.cucumber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.zacran.kimchi.config.KimchiConstants;
import com.zacran.kimchi.cucumber.copied.CucumberResourceLoader;

import cucumber.api.StepDefinitionReporter;
import cucumber.api.event.TestRunFinished;
import cucumber.api.event.TestRunStarted;
import cucumber.runner.EventBus;
import cucumber.runner.ThreadLocalRunnerSupplier;
import cucumber.runner.TimeService;
import cucumber.runner.TimeServiceEventBus;
import cucumber.runtime.BackendModuleBackendSupplier;
import cucumber.runtime.BackendSupplier;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.CucumberException;
import cucumber.runtime.ExitStatus;
import cucumber.runtime.FeatureCompiler;
import cucumber.runtime.FeaturePathFeatureSupplier;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.filter.Filters;
import cucumber.runtime.filter.RerunFilters;
import cucumber.runtime.formatter.PluginFactory;
import cucumber.runtime.formatter.Plugins;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.FeatureLoader;
import gherkin.events.PickleEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CucumberEngine {

	private ClassLoader classLoader;
	private ResourceLoader resourceLoader;
	private ClassFinder classFinder;

	private boolean resourcesLoaded = false;

	// Provides CucumberEngine as a lazy-loading singleton
	private static class InstanceHolder {
		static final CucumberEngine instance = new CucumberEngine();
	}

	public static CucumberEngine getInstance() {
		return InstanceHolder.instance;
	}

	private void loadResources() {
		classLoader = Thread.currentThread().getContextClassLoader();

		boolean isArtifact = CucumberEngine.class.getResource("CucumberEngine.class").toString().contains("jar:");

		if (isArtifact) {
			resourceLoader = new CucumberResourceLoader(classLoader); // if running from artifact
		} else {
			resourceLoader = new MultiLoader(classLoader); // if running from bootRun
		}

		classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);

		resourcesLoaded = true;
	}

	public int startTests(String... cucumberArgs) {
		if (!resourcesLoaded) {
			loadResources();
		}

		RuntimeOptions runtimeOptions = new RuntimeOptions(new ArrayList<String>(Arrays.asList(cucumberArgs)));

		final FeatureLoader featureLoader = new FeatureLoader(resourceLoader);
		FeaturePathFeatureSupplier featureSupplier = new FeaturePathFeatureSupplier(featureLoader, runtimeOptions);

		final List<CucumberFeature> loadedCucumberFeatures = featureSupplier.get();

		final ExecutorService executorService = Executors.newFixedThreadPool(KimchiConstants.CUCUMBER_EXECUTOR_THREADS);
		FeatureCompiler featureCompiler = new FeatureCompiler();

		RerunFilters rerunFilters = new RerunFilters(runtimeOptions, featureLoader);
		Filters filters = new Filters(runtimeOptions, rerunFilters);
		EventBus eventBus = new TimeServiceEventBus(TimeService.SYSTEM);
		BackendSupplier backendSupplier = new BackendModuleBackendSupplier(resourceLoader, classFinder, runtimeOptions);
		ThreadLocalRunnerSupplier runnerSupplier = new ThreadLocalRunnerSupplier(runtimeOptions, eventBus,
				backendSupplier);

		ExitStatus exitStatus = new ExitStatus(runtimeOptions);
		exitStatus.setEventPublisher(eventBus);

		final Plugins plugins = new Plugins(this.classLoader, new PluginFactory(), eventBus, runtimeOptions);

		eventBus.send(new TestRunStarted(eventBus.getTime()));
		for (CucumberFeature feature : loadedCucumberFeatures) {
			feature.sendTestSourceRead(eventBus);
		}

		final StepDefinitionReporter stepDefinitionReporter = plugins.stepDefinitionReporter();
		runnerSupplier.get().reportStepDefinitions(stepDefinitionReporter);

		for (CucumberFeature feature : loadedCucumberFeatures) {
			for (final PickleEvent pickleEvent : featureCompiler.compileFeature(feature)) {
				if (filters.matchesFilters(pickleEvent)) {
					executorService.execute(new Runnable() {

						@Override
						public void run() {
							runnerSupplier.get().runPickle(pickleEvent);
						}
					});
				}
			}
		}

		executorService.shutdown();

		try {
			// gracefully await termination
			while (!executorService.awaitTermination(1, TimeUnit.DAYS))
				;
		} catch (InterruptedException e) {
			throw new CucumberException(e);
		}

		eventBus.send(new TestRunFinished(eventBus.getTime()));

		return exitStatus.exitStatus();
	}

}