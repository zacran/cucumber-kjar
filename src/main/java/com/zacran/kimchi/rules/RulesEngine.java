package com.zacran.kimchi.rules;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.zacran.kimchi.model.Kimchi;

import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.ReleaseId;
import org.kie.api.command.BatchExecutionCommand;
import org.kie.api.command.Command;
import org.kie.api.command.KieCommands;
import org.kie.api.io.Resource;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.scanner.KieMavenRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RulesEngine {

	private static final String GROUP = "com.zacran";
	private static final String ARTIFACT = "kimchi-rules";

	private StatelessKieSession currentKieSession;
	private KieBuilder kieBuilder;

	private ReleaseId currentReleaseId;
	private KieServices kieServices = KieServices.Factory.get();

	// Provides RulesEngine as a lazy-loading singleton
	private static class InstanceHolder {
		static final RulesEngine instance = new RulesEngine();
	}

	public static RulesEngine getInstance() {
		return InstanceHolder.instance;
	}

	public void compileAndLoadRules(File... files) {
		String version = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
		currentReleaseId = kieServices.newReleaseId(GROUP, ARTIFACT, version);

		compileRules(files);
		loadRules();
	}

	void loadRules() {
		log.debug("Loading rules, version: {}", currentReleaseId.getVersion());
		currentKieSession = kieServices.newKieContainer(currentReleaseId).newStatelessKieSession();
	}

	void compileRules(File... files) {
		log.debug("Compiling rules, version: {}", currentReleaseId.getVersion());
		log.debug("Rules content: {}", Arrays.toString(files));

		KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
		kieFileSystem.generateAndWritePomXML(currentReleaseId);

		Resource[] resources = RulesResourceAdaptor.convertFilesToResources(files);
		for (Resource resource : resources) {
			kieFileSystem.write(resource);
		}

		kieBuilder = kieServices.newKieBuilder(kieFileSystem);
		kieBuilder.getKieModule();
	}

	public String installAsKjar() {
		final InternalKieModule kieModule = (InternalKieModule) kieBuilder.getKieModule();

		File pom = PomGenerator.getPomFile(currentReleaseId);
		KieMavenRepository.getKieMavenRepository().installArtifact(currentReleaseId, kieModule, pom);

		pom.delete();

		return currentReleaseId.getVersion();
	}

	public void runRules(Kimchi kimchi) {
		if (currentKieSession == null) {
			log.info("Attempted to run rules but StatelessKieSession is not loaded. Loading now...");
			loadRules();
		}

		currentKieSession.execute(createBatchExecutionCommand(kimchi));
	}

	private BatchExecutionCommand createBatchExecutionCommand(Kimchi kimchi) {
		KieCommands kieCommands = kieServices.getCommands();

		List<Command<?>> commands = new ArrayList<>();
		commands.add(kieCommands.newInsert(kimchi));
		commands.add(kieCommands.newFireAllRules());

		return kieCommands.newBatchExecution(commands);
	}
}