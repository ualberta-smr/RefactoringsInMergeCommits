package ca.ualberta.cs.smr.refactoring.analysis;

import ca.ualberta.cs.smr.refactoring.analysis.database.*;
import ca.ualberta.cs.smr.refactoring.analysis.utils.GitUtils;
import ca.ualberta.cs.smr.refactoring.analysis.utils.RefactoringMinerUtils;
import ca.ualberta.cs.smr.refactoring.analysis.utils.Utils;
import gr.uom.java.xmi.diff.CodeRange;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.javalite.activejdbc.Base;
import org.refactoringminer.api.Refactoring;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

public class RefactoringAnalysis {

    private String projectsDirectory = "../projects";
    private String repoListFile = "../reposList.txt";

    public static void main(String[] args) {
        RefactoringAnalysis refAnalysis;
        if (args.length == 1) {
            refAnalysis = new RefactoringAnalysis(args[0]);
        } else if (args.length == 2) {
            refAnalysis = new RefactoringAnalysis(args[0], args[1]);
        } else {
            refAnalysis = new RefactoringAnalysis();
        }

        try {
            DatabaseUtils.createDatabase();
            refAnalysis.runParallel();
        } catch (Throwable e) {
            Utils.log(null, e);
            e.printStackTrace();
        }
    }

    public RefactoringAnalysis() {
    }

    public RefactoringAnalysis(String repoListFile) {
        this.repoListFile = repoListFile;
        this.projectsDirectory = "projects";
    }

    public RefactoringAnalysis(String repoListFile, String projectsDirectory) {
        this.repoListFile = repoListFile;
        this.projectsDirectory = projectsDirectory;
    }

    private void runParallel() throws Exception {
        List<String> projectURLs = Files.readAllLines(Paths.get(repoListFile));

        int parallelism = Math.max(1, (int) (Runtime.getRuntime().availableProcessors() * .75));
        ForkJoinPool forkJoinPool = null;
        try {
            forkJoinPool = new ForkJoinPool(parallelism);
            forkJoinPool.submit(() ->
                    projectURLs.parallelStream().forEach(s -> {
                        Base.open();
                        cloneAndAnalyzeProject(s);
                        Base.close();
                    })
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            if (forkJoinPool != null) {
                forkJoinPool.shutdown();
            }
        }
    }

    private void run() throws Exception {
        Base.open();
        Files.readAllLines(Paths.get(repoListFile)).forEach(this::cloneAndAnalyzeProject);
        Base.close();
    }

    private void cloneAndAnalyzeProject(String projectURL) {
        String projectName = projectURL.substring(projectURL.lastIndexOf('/') + 1);
        try {
            cloneProject(projectURL);
        } catch (JGitInternalException | GitAPIException e) {
            Utils.log(projectName, e);
            e.printStackTrace();
        }

        try {
            Project project = Project.findFirst("url = ?", projectURL);
            if (project == null) {
                project = new Project(projectURL, projectName);
                project.saveIt();
            }
            analyzeProject(project);
            Utils.log(projectName, "Finished the analysis, removing the repository...");
            removeProject(projectName);
            Utils.log(projectName, "Done with " + projectName);
        } catch (JGitInternalException | GitAPIException | IOException e) {
            Utils.log(projectName, e);
            e.printStackTrace();
        }
    }

    private void cloneProject(String url) throws GitAPIException {
        String projectName = url.substring(url.lastIndexOf('/') + 1);
        Utils.log(projectName, String.format("Cloning %s...", projectName));
        Git.cloneRepository()
                .setURI(url)
                .setDirectory(new File(projectsDirectory, projectName))
                .call();
    }

    private void removeProject(String projectName) {
        try {
            Files.walk(Paths.get(new File(projectsDirectory, projectName).getAbsolutePath()))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void analyzeProject(Project project) throws GitAPIException, IOException {
        Utils.log(project.getName(), String.format("Analyzing %s's commits...", project.getName()));
        analyzeProjectCommits(project);

        Utils.log(project.getName(), String.format("Analyzing %s with RefMiner...", project.getName()));
        analyzeProjectWithRefMiner(project);
    }

    private void analyzeProjectCommits(Project project) throws GitAPIException, IOException {
        GitUtils gitUtils = new GitUtils(new File(projectsDirectory, project.getName()));
        List<RevCommit> mergeCommits = gitUtils.getMergeCommits();
        for (int i = 0; i < mergeCommits.size(); i++) {
            RevCommit mergeCommit = mergeCommits.get(i);
            Utils.log(project.getName(), String.format("Analyzing commit %.7s... (%d/%d)", mergeCommit.getName(),
                    i + 1, mergeCommits.size()));

            // Skip this commit if it already exists in the database.
            if (MergeCommit.where("commit_hash = ?", mergeCommit.getName()).size() > 0) {
                Utils.log(project.getName(), "Already exists in the database, skipping...");
                continue;
            }

            try {
                Map<String, String> conflictingJavaFiles = new HashMap<>();
                boolean isConflicting = gitUtils.isConflicting(mergeCommit, conflictingJavaFiles);

                MergeCommit mergeCommitModel = new MergeCommit(mergeCommit.getName(), isConflicting,
                        mergeCommit.getParent(0).getName(), mergeCommit.getParent(1).getName(), project);
                mergeCommitModel.saveIt();

                extractConflictingRegions(gitUtils, mergeCommitModel, conflictingJavaFiles);
            } catch (GitAPIException e) {
                Utils.log(project.getName(), e);
                e.printStackTrace();
            }
        }
    }

    private void extractConflictingRegions(GitUtils gitUtils, MergeCommit mergeCommit,
                                           Map<String, String> conflictingJavaFiles) {
        for (String path : conflictingJavaFiles.keySet()) {
            String conflictType = conflictingJavaFiles.get(path);
            ConflictingJavaFile conflictingJavaFile = new ConflictingJavaFile(path, conflictType, mergeCommit);
            conflictingJavaFile.saveIt();

            if (conflictType.equalsIgnoreCase("content") ||
                    conflictType.equalsIgnoreCase("add/add")) {
                String[] conflictingRegionPaths = new String[2];
                List<int[][]> conflictingRegions = new ArrayList<>();
                gitUtils.getConflictingRegions(path, conflictingRegionPaths, conflictingRegions);

                for (int[][] conflictingLines : conflictingRegions) {
                    ConflictingRegion conflictingRegion = new ConflictingRegion(
                            conflictingLines[0][0], conflictingLines[0][1], conflictingRegionPaths[0],
                            conflictingLines[1][0], conflictingLines[1][1], conflictingRegionPaths[1],
                            conflictingJavaFile);
                    conflictingRegion.saveIt();

                    List<GitUtils.CodeRegionChange> leftConflictingRegionHistory = gitUtils.getConflictingRegionHistory(
                            mergeCommit.getParent1(), mergeCommit.getParent2(),
                            path, conflictingLines[0]);
                    List<GitUtils.CodeRegionChange> rightConflictingRegionHistory = gitUtils.getConflictingRegionHistory(
                            mergeCommit.getParent2(), mergeCommit.getParent1(),
                            path, conflictingLines[1]);

                    leftConflictingRegionHistory.forEach(codeRegionChange -> new ConflictingRegionHistory(
                            codeRegionChange.commitHash, 1,
                            codeRegionChange.oldStartLine, codeRegionChange.oldLength, codeRegionChange.oldPath,
                            codeRegionChange.newStartLine, codeRegionChange.newLength, codeRegionChange.newPath,
                            conflictingRegion).saveIt());
                    rightConflictingRegionHistory.forEach(codeRegionChange -> new ConflictingRegionHistory(
                            codeRegionChange.commitHash, 2,
                            codeRegionChange.oldStartLine, codeRegionChange.oldLength, codeRegionChange.oldPath,
                            codeRegionChange.newStartLine, codeRegionChange.newLength, codeRegionChange.newPath,
                            conflictingRegion).saveIt());
                }
            }
        }
    }


    private void analyzeProjectWithRefMiner(Project project) {
        List<ConflictingRegionHistory> historyConfRegions =
                ConflictingRegionHistory.where("project_id = ?", project.getId());

        try {
            RefactoringMinerUtils refMinerUtils = new RefactoringMinerUtils(new File(projectsDirectory, project.getName()),
                    project.getURL());
            for (int i = 0; i < historyConfRegions.size(); i++) {
                ConflictingRegionHistory conflictingRegionHistory = historyConfRegions.get(i);
                if (ca.ualberta.cs.smr.refactoring.analysis.database.Refactoring.where("commit_hash = ?",
                        conflictingRegionHistory.getCommitHash()).size() > 0)
                    continue;

                Utils.log(project.getName(), String.format("Analyzing commit %.7s with RefMiner... (%d/%d)",
                        conflictingRegionHistory.getCommitHash(), i + 1, historyConfRegions.size()));
                List<Refactoring> refactorings = refMinerUtils.detectAtCommit(conflictingRegionHistory.getCommitHash());
                for (Refactoring refactoring : refactorings) {
                    ca.ualberta.cs.smr.refactoring.analysis.database.Refactoring refactoringModel =
                            new ca.ualberta.cs.smr.refactoring.analysis.database.Refactoring(
                                    conflictingRegionHistory.getCommitHash(),
                                    conflictingRegionHistory.getMergeParent(),
                                    refactoring.getRefactoringType().getDisplayName(),
                                    refactoring.toString(),
                                    conflictingRegionHistory.getMergeCommitId(),
                                    conflictingRegionHistory.getProjectId());
                    refactoringModel.saveIt();

                    List<CodeRange> sourceCodeRanges = new ArrayList<>();
                    List<CodeRange> destCodeRanges = new ArrayList<>();
                    refMinerUtils.getRefactoringCodeRanges(refactoring, sourceCodeRanges, destCodeRanges);
                    sourceCodeRanges.forEach(cr -> new RefactoringRegion('s', cr.getFilePath(), cr.getStartLine(),
                            cr.getEndLine() - cr.getStartLine(), refactoringModel).saveIt());
                    destCodeRanges.forEach(cr -> new RefactoringRegion('d', cr.getFilePath(), cr.getStartLine(),
                            cr.getEndLine() - cr.getStartLine(), refactoringModel).saveIt());
                }

            }
        } catch (GitAPIException | IOException e) {
            Utils.log(project.getName(), e);
            e.printStackTrace();
        }
    }
}
