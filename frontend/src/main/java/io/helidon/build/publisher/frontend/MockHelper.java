package io.helidon.build.publisher.frontend;

import io.helidon.build.publisher.frontend.model.ArtifactDirItem;
import io.helidon.build.publisher.frontend.model.ArtifactFileItem;
import io.helidon.build.publisher.frontend.model.Artifacts;
import io.helidon.build.publisher.frontend.model.Pipeline;
import io.helidon.build.publisher.frontend.model.PipelineInfos;
import io.helidon.build.publisher.frontend.model.PipelineInfo;
import io.helidon.build.publisher.frontend.model.PipelineStageItem;
import io.helidon.build.publisher.frontend.model.PipelineStepItem;
import io.helidon.build.publisher.frontend.model.TestItem;
import io.helidon.build.publisher.frontend.model.Tests;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Random;
import java.util.UUID;

/**
 * Mock helper.
 */
final class MockHelper {

    private static final Random RANDOM = new Random();
    private static final long YEAR_IN_MILLIS = 60 * 60 * 24 * 365 * 1000;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm a z");

    private static final String[] REPOS = new String[]{
        "oracle/helidon",
        "oracle/helidon-build-tools",
        "oracle/helidon-site"
    };

    private static final String[] MOCK_REPOS_URL = new String[]{
        "https://github.com/oracle/helidon",
        "https://github.com/oracle/helidon-build-tools",
        "https://github.com/oracle/helidon-site"
    };

    private static final String[] AUTHORS = new String[] {
        "bob_dylan",
        "bob_marley",
        "marlon_brando",
        "jimmy_carter",
        "john_doe",
        "calamity_jane",
        "lucky_luke",
        "darth_vader",
        "luke_skywalker",
        "bip_bip",
        "yipikaye"
    };

    private static final String[] STATUSES = new String[]{
        "RUNNING",
        "SUCCESS",
        "FAILURE",
        "UNSTABLE"
    };

    private static final String[] BRANCHES = new String[]{
        "master",
        "581-helidon-db",
        "helidon-1.x",
        "config-refactoring",
        "my-branch",};

    private static final String[] TITLE_WORDS1 = new String[]{
        "Design",
        "Implement",
        "Fix",
        "Test",
        "Document",
        "Debug",
        "Troubleshoot",
        "Revert",
        "Uptake"
    };

    private static final String[] TITLE_WORDS2 = new String[]{
        "issue #157",
        "issue #55",
        "issue #254",
        "issue #199",
        "reactive HTTP",
        "responsive design",
        "query parameters",
        "path parameters",
        "packaging plugin",
        "build plugin",
        "deployment plugin",
        "database queries",
        "reactive database driver",
        "reactive messaging",
        "transactional database queries"
    };

    private static final String[] STAGE_NAMES = new String[] {
        "Build",
        "Compile",
        "Package",
        "Deploy",
        "Tests",
        "Install",
        "Verify",
        "Validate",
        "Clean",
        "Upload",
        "Analyze",
        "Scan",
        "Test1",
        "Test2",
        "Test3",
        "Test4",
        "Checkstyle",
        "Copyright",
        "Javadoc",
        "Spotbugs"
    };

    private static final String[] STEP_NAMES = new String[] {
        "sh['bash ./exec.sh']",
        "sh['bash ./build.sh']",
        "sh['bash ./compile.sh']",
        "sh['bash ./spotbugs.sh']",
        "sh['bash ./javadocs.sh']",
        "sh['bash ./copyright.sh']",
        "sh['bash ./unittests.sh']",
        "sh['bash ./integtests.sh']",
        "sh['bash ./deploy.sh']",
        "sh['bash ./scan.sh']",
        "sh['bash ./analyze.sh']",
        "sh['bash ./upload.sh']",
        "sh['bash ./clean.sh']",
    };

    private static final String[] TEST_PACKAGES = new String[] {
        "io.helidon.badass.project",
        "io.helidon.submarine.project",
        "io.helidon.hidden.component"
    };

    private static final String[] TEST_NAMES = new String[] {
        "TestStartup",
        "TestShutDown",
        "TestLifeCycle",
        "TestCleanup",
        "TestJSON",
        "TestList",
        "TestGet",
        "TestPut",
        "TestPost",
        "TestDelete"
    };

    private static final String[] DIR_NAMES = new String[] {
        "public",
        "build",
        "target",
        "classes",
        "output",
        "stage",
        "libs"
    };

    private static final String[] FILE_NAMES = new String[] {
        "favicon.ico",
        "logo.png",
        "pom.xml",
        "README.md",
        ".gitignore",
        "Hello.java",
        "Hello.class",
        "my-app.jar",
        "application.yaml",
        "MANIFEST.MF"
    };

    private static final String[] FILE_TYPES = new String[] {
        "html",
        "js",
        "json",
        "xml",
        "md",
        "pdf",
        "png",
        "txt",
        "xls"
    };

    private static String mockTitle() {
        String title = "Pull Request #";
        title += RANDOM.nextInt(2000);
        title += ": ";
        if (RANDOM.nextBoolean()) {
            title += "WIP: ";
        }
        title += TITLE_WORDS1[RANDOM.nextInt(TITLE_WORDS1.length)];
        title += " " + TITLE_WORDS2[RANDOM.nextInt(TITLE_WORDS2.length)];
        return title;
    }

    static enum TimeUnit {
        MINUTE,
        HOUR,
        DAY,
        MONTH,
        YEAR
    }

    static String mockDuration(TimeUnit maxUnit) {
        TimeUnit unit = TimeUnit.values()[RANDOM.nextInt(maxUnit.ordinal())];
        String res;
        int num;
        switch(unit) {
            case MINUTE:
                num = RANDOM.nextInt(59) + 1; // [1,59]
                res = num + " minute";
                if (num > 1) {
                    res += "s";
                }
                break;
            case HOUR:
                num = RANDOM.nextInt(23) + 1; // [1,23]
                res = num + " hour";
                if (num > 1) {
                    res += "s";
                }
                int minutes = RANDOM.nextInt(59) + 1; // [1,59]
                res += " " + minutes + " minute";
                if (minutes > 1) {
                    res += "s";
                }
                break;
            case DAY:
                num = RANDOM.nextInt(31) + 1; // [1,31]
                res = num + " day";
                if (num > 1) {
                    res += "s";
                }
                break;
            case MONTH:
                num = RANDOM.nextInt(12) + 1; // [1,11]
                res = num + " month";
                break;
            case YEAR:
                num = RANDOM.nextInt(4) + 1; // [1-4]
                res = num + " year";
                if (num > 1) {
                    res += "s";
                }
                break;
            default:
                throw new IllegalStateException("Unknown time unit: " + unit);
        }
        return res;
    }

    static PipelineInfos mockPipelines(int page, int numitems) {
        PipelineInfos.Builder builder = PipelineInfos.builder()
                .page(page)
                .numpages(6);
        for (int i=0 ; i < 20 ; i++) {
            int repoId =  RANDOM.nextInt(REPOS.length);
            int branchId = RANDOM.nextInt(BRANCHES.length);
            builder.pipeline(PipelineInfo.builder()
                    .id(UUID.randomUUID().toString())
                    .title(mockTitle())
                    .when(mockDuration(TimeUnit.YEAR) + " ago")
                    .status(STATUSES[RANDOM.nextInt(STATUSES.length)])
                    .branch(BRANCHES[branchId])
                    .branchUrl(MOCK_REPOS_URL[repoId] + "/tree/" + BRANCHES[branchId])
                    .repository(REPOS[repoId])
                    .repositoryUrl(MOCK_REPOS_URL[repoId])
                    .build());
        }
        return builder.build();
    }

    static String mockDate() {
        // up to a year before
        long epoch = System.currentTimeMillis() - (long)RANDOM.nextDouble() * YEAR_IN_MILLIS;
        ZonedDateTime date = Instant.ofEpochMilli(epoch).atZone(ZoneId.of("America/Los_Angeles"));
        return DATE_FORMATTER.format(date);
    }

    private static String TEST_OUTPUT;

    static String mockTestOutput() {
        if (TEST_OUTPUT == null) {
            StringWriter sw = new StringWriter();
            new Exception().printStackTrace(new PrintWriter(sw));
            StringBuilder sb = new StringBuilder();
            for (String line : sw.getBuffer().toString().split("\\r?\\n")) {
                sb.append("<div class=\"line\">")
                  .append(line)
                  .append("</div>");
            }
            TEST_OUTPUT = sb.toString();
        }
        return TEST_OUTPUT;
    }

    static Tests mockTests() {
        Tests.Builder builder = Tests.builder();
        int passed=0, failed = 0, skipped = 0;
        int size = RANDOM.nextInt(50) + 1;
        String pkg = TEST_PACKAGES[RANDOM.nextInt(TEST_PACKAGES.length)];
        for (int i=0 ; i < size ; i++) {
            int statusId = RANDOM.nextInt(3); // 0=passed, 1=failed, 2=skipped
            String status;
            switch (statusId) {
                case 2:
                    skipped++;
                    status = "SKIPPED";
                    break;
                case 1:
                    failed++;
                    status = "FAILURE";
                    break;
                default:
                    passed++;
                    status = "SUCCESS";
                    break;
            }
            TestItem.Builder itemBuilder = TestItem.builder()
                    .status(status)
                    .name(pkg + "." + TEST_NAMES[RANDOM.nextInt(TEST_NAMES.length)]);
            if (statusId == 1) {
                itemBuilder.output(mockTestOutput());
            }
            builder.item(itemBuilder.build());
        }
        return builder
                .passed(passed)
                .failed(failed)
                .skipped(skipped)
                .build();
    }

    static Artifacts mockArtifacts() {
        Artifacts.Builder builder = Artifacts.builder();
        int count=0;
        LinkedList<ArtifactDirItem.Builder> dirs = new LinkedList<>();
        LinkedList<Integer> depths = new LinkedList<>();
        for (int i=0 ; i < RANDOM.nextInt(5) + 1 ; i++) {
            ArtifactDirItem.Builder dirBuilder = (ArtifactDirItem.Builder)ArtifactDirItem.builder()
                    .name(DIR_NAMES[RANDOM.nextInt(DIR_NAMES.length)]);
            builder.item(dirBuilder);
            dirs.push(dirBuilder);
            depths.push(RANDOM.nextInt(2) + 1);
        }
        while(!dirs.isEmpty()) {
            ArtifactDirItem.Builder dirBuilder = dirs.pop();
            int depth = depths.pop();
            if (depth == 1) {
                for (int nfiles = RANDOM.nextInt(8) + 1; nfiles > 0 ; nfiles--) {
                    count++;
                    dirBuilder.child(ArtifactFileItem.builder()
                            .file(FILE_TYPES[RANDOM.nextInt(FILE_TYPES.length)])
                            .name(FILE_NAMES[RANDOM.nextInt(FILE_NAMES.length)]));
                }
            } else {
                for (int i=0 ; i < RANDOM.nextInt(3) + 1 ; i++) {
                    ArtifactDirItem.Builder childDirBuilder = (ArtifactDirItem.Builder) ArtifactDirItem.builder()
                            .name(DIR_NAMES[RANDOM.nextInt(DIR_NAMES.length)]);
                    dirBuilder.child(childDirBuilder);
                    dirs.push(childDirBuilder);
                    depths.push(depth - 1);
                }
            }
        }
        return builder
                .count(count)
                .build();
    }

    static Pipeline mockPipeline(String pipelineId) {
        Pipeline.Builder builder = Pipeline.builder()
                .id(pipelineId);
        int repoId =  RANDOM.nextInt(REPOS.length);
        int branchId = RANDOM.nextInt(BRANCHES.length);
        int authorId = RANDOM.nextInt(AUTHORS.length);
        String commit = UUID.randomUUID().toString().replace("-", "");
        builder
            .title(mockTitle())
            .duration(mockDuration(TimeUnit.HOUR))
            .status(STATUSES[RANDOM.nextInt(STATUSES.length)])
            .startTime(mockDate())
            .branch(BRANCHES[branchId])
            .branchUrl(MOCK_REPOS_URL[repoId] + "/tree/" + BRANCHES[branchId])
            .repository(REPOS[repoId])
            .repositoryUrl(MOCK_REPOS_URL[repoId])
            .commit(commit)
            .commitUrl(MOCK_REPOS_URL[repoId] + "/commit/" + commit)
            .author(AUTHORS[authorId])
            .authorUrl("https://github.com/" + AUTHORS[authorId]);

        LinkedList<PipelineStageItem.Builder> stages = new LinkedList<>();
        LinkedList<Integer> depths = new LinkedList<>();
        int nodeId = 1;
        for (int i=0 ; i < RANDOM.nextInt(4) + 1 ; i++) {
            PipelineStageItem.Builder stageBuilder = (PipelineStageItem.Builder) PipelineStageItem.builder()
                    .tests(mockTests())
                    .artifacts(mockArtifacts())
                    .parallel(RANDOM.nextBoolean())
                    .name(STAGE_NAMES[RANDOM.nextInt(STAGE_NAMES.length)])
                    .id(nodeId++);
            builder.item(stageBuilder);
            stages.push(stageBuilder);
            depths.push(RANDOM.nextInt(2) + 1);
        }
        while(!stages.isEmpty()) {
            PipelineStageItem.Builder stageBuilder = stages.pop();
            int depth = depths.pop();
            if (depth == 1) {
                for (int nfiles = RANDOM.nextInt(5) + 1; nfiles > 0 ; nfiles--) {
                    stageBuilder.child(PipelineStepItem.builder()
                            .status(STATUSES[RANDOM.nextInt(STATUSES.length)])
                            .name(STEP_NAMES[RANDOM.nextInt(STEP_NAMES.length)])
                            .id(nodeId++));
                }
            } else {
                for (int i=0 ; i < RANDOM.nextInt(5) + 1 ; i++) {
                    PipelineStageItem.Builder childStageBuilder = (PipelineStageItem.Builder) PipelineStageItem.builder()
                            .tests(mockTests())
                            .artifacts(mockArtifacts())
                            .parallel(RANDOM.nextBoolean())
                            .name(STAGE_NAMES[RANDOM.nextInt(STAGE_NAMES.length)])
                            .id(nodeId++);
                    stageBuilder.child(childStageBuilder);
                    stages.push(childStageBuilder);
                    depths.push(depth - 1);
                }
            }
        }
        return builder.build();
    }
}
