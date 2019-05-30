# Project Kimchi
Building and testing rules on the fly with Drools and Cucumber

[![Build Status](https://travis-ci.org/zacran/project-kimchi.svg?branch=master)](https://travis-ci.org/zacran/project-kimchi.svg?branch=master)
[![Total alerts](https://img.shields.io/lgtm/alerts/g/zacran/project-kimchi.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/zacran/project-kimchi/alerts/)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/88daca5d5de240e89aa2011bb1b2bdc3)](https://www.codacy.com/app/zachary.cranfill/project-kimchi?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=zacran/project-kimchi&amp;utm_campaign=Badge_Grade)

## About

Tech used:
-   `Cucumber`
-   `Drools`
-   `Gradle`
-   `SpringBoot`
-   `Swagger`

Demo application showing how to:
-   Use [`Drools`](https://github.com/kiegroup/drools) to build rules at runtime
-   Use [`Cucumber`](https://github.com/cucumber/cucumber-jvm) to test these rules at runtime
-   Provide an endpoint to update rules and validate them on the fly
-   Install `MemoryKieModules` as KJARs to your m2 repository

## Getting Started

Building:
```shell
gradle clean build
```

Running as REST service:
```shell
gradle bootRun
```

Once the service is running, you can go to [`localhost:8080/swagger-ui.html`](localhost:8080/swagger-ui.html) to see available REST options

## How the rules are compiled and loaded
Rules are compiled into a MemoryKieModule:
```java
void compileRules(File... files) {
    KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
    kieFileSystem.generateAndWritePomXML(currentReleaseId);

    Resource[] resources = RulesResourceAdaptor.convertFilesToResources(files);
    for (Resource resource : resources) {
        kieFileSystem.write(resource);
    }

    kieBuilder = kieServices.newKieBuilder(kieFileSystem);
    kieBuilder.getKieModule();
}
```

From this, a `StatelessKieSession` is created:
```java
currentKieSession = kieServices.newKieContainer(currentReleaseId).newStatelessKieSession();
```

Once the `StatelessKieSession` is available, BatchExecutionCommands can be run:
```java
public void runRules(Kimchi kimchi) {
    if (currentKieSession == null) {
        loadRules();
    }

    currentKieSession.execute(createBatchExecutionCommand(kimchi));
}

BatchExecutionCommand createBatchExecutionCommand(Kimchi kimchi) {
    KieCommands kieCommands = kieServices.getCommands();

    List<Command<?>> commands = new ArrayList<>();
    commands.add(kieCommands.newInsert(kimchi));
    commands.add(kieCommands.newFireAllRules());

    return kieCommands.newBatchExecution(commands);
}
```

## How the Cucumber tests are run at runtime
This project contains a custom Cucumber plugin, `JSONFileFormatter`, based on Cucumber's `JSONFormatter`, to spool test results to a file, which is then read and available in memory. 

The custom plugin is passed in as a Cucumber argument by providing the full classpath:
```shell
--plugin com.zacran.kimchi.cucumber.JSONFileFormatter
```

To use Cucumber at runtime, classes must be loaded and converted into `cucumber.runtime.io.Resource` depending on how the service is running. 

```java
void loadResources() {
    classLoader = Thread.currentThread().getContextClassLoader();

    boolean isArtifact = CucumberEngine.class.getResource("CucumberEngine.class").toString().contains("jar:");

    if (isArtifact) {
        resourceLoader = new TestResourceLoader(classLoader); // if running from artifact
    } else {
        resourceLoader = new MultiLoader(classLoader); // if running from bootRun
    }

    classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);

    resourcesLoaded = true;
}
```

Cucumber is invoked by setting up its components and launching an `ExecutorService` to run the `PickleEvents` of loaded `CucumberFeatures`.

```java
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
```

## Updating Rules on the Fly

Say you want to update rules in your application without having to worry about syncing with development cycle or releases:

Existing rule
```java
rule "Kimchi tastes bad if it is older than 90 days old"
    when
        $kimchi : Kimchi(age > 90)
    then
        $kimchi.setRating(KimchiRating.TASTES_BAD);
end
```

New rule
```java
rule "Kimchi tastes bad if it is older than 140 days old"
    when
        $kimchi : Kimchi(age > 140)
    then
        $kimchi.setRating(KimchiRating.TASTES_BAD);
end
```

You can upload this via the `/kimchi/upload` endpoint. In order to deploy these rules, you'll have to run tests to verify your changes.

## Testing New Rules

To test your new rule, you can update the Cucumber tests and upload them through the same `/kimchi/upload` endpoint.

Existing test:
```Gherkin
Scenario: Old Kimchi
    Given I have the following kimchi:
        | Ingredients            | Age |
        | Cabbage, Spices, Water | 120 |
    When I attempt to make Kimchi
    Then The Kimchi will not taste great.
```

New tests:
```Gherkin
Scenario: Kimchi That's A Bit Old But Still Good
    Given I have the following kimchi:
        | Ingredients            | Age |
        | Cabbage, Spices, Water | 120 |
    When I attempt to make Kimchi
    Then The Kimchi will taste great!

Scenario: Old Kimchi
    Given I have the following kimchi:
        | Ingredients            | Age |
        | Cabbage, Spices, Water | 150 |
    When I attempt to make Kimchi
    Then The Kimchi will not taste great.
```

Now when you hit the `/kimchi/test` endpoint, the new rules will be loaded into the MemoryKieModule and the new tests will execute using the new `StatelessKieSession`.

## Installing New Rules As KJARs

To install your new (and tested) rules to your m2 repository, you can hit the `/kimchi/install` endpoint. The `MemoryKieModule` is converted into a jar and installed as a Maven artifact according to the group and artifact name set in the `RulesEngine`.

```java
public String installAsKjar() {
    final InternalKieModule kieModule = (InternalKieModule) kieBuilder.getKieModule();

    File pom = PomGenerator.getPomFile(currentReleaseId);
    KieMavenRepository.getKieMavenRepository().installArtifact(currentReleaseId, kieModule, pom);

    pom.delete();

    return currentReleaseId.getVersion();
}
```

## 