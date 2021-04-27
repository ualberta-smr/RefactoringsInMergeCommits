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
import java.util.concurrent.*;

public class RefactoringAnalysis {

    private String repoListFile;
    private String clonePath;

    public RefactoringAnalysis(String repoListFile, String clonePath) {
        this.repoListFile = repoListFile;
        this.clonePath = clonePath;
    }

    public void start(int parallelism) {
        try {
            DatabaseUtils.createDatabase();
            runParallel(parallelism);
        } catch (Throwable e) {
            Utils.log(null, e);
            e.printStackTrace();
        }
    }

    private void runParallel(int parallelism) throws Exception {
        List<String> projectURLs = Files.readAllLines(Paths.get(repoListFile));
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

    private void cloneAndAnalyzeProject(String projectURL) {
        String projectName = projectURL.substring(projectURL.lastIndexOf('/') + 1);

        Project project = Project.findFirst("url = ?", projectURL);
        if (project == null) {
            project = new Project(projectURL, projectName);
            project.saveIt();
        } else if (project.isDone()) {
            Utils.log(projectName, String.format("%s has been already analyzed, skipping...", projectName));
            return;
        }
        try {
            removeProject(projectName);
            cloneProject(projectURL);
            analyzeProject(project);
            project.setDone();
            project.saveIt();
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
                .setDirectory(new File(clonePath, projectName))
                .call();
    }

    private void removeProject(String projectName) {
        try {
            Files.walk(Paths.get(new File(clonePath, projectName).getAbsolutePath()))
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
        GitUtils gitUtils = new GitUtils(new File(clonePath, project.getName()));
        Iterable<RevCommit> mergeCommits = gitUtils.getMergeCommits();
        int mergeCommitIndex = 0;
        Map<String, String> conflictingJavaFiles = new HashMap<>();
        for (RevCommit mergeCommit : mergeCommits) {
            mergeCommitIndex++;
            Utils.log(project.getName(), String.format("Analyzing commit %.7s... (%d/?)", mergeCommit.getName(),
                    mergeCommitIndex));

            // Skip this commit if it already exists in the database.
            MergeCommit mergeCommitModel = MergeCommit.findFirst("commit_hash = ?", mergeCommit.getName());
            if (mergeCommitModel != null) {
                if (mergeCommitModel.isDone()) {
                    Utils.log(project.getName(), "Already analyzed, skipping...");
                    continue;
                }
                // Will cascade to dependent records because of foreign key constraints
                mergeCommitModel.delete();
            }

            try {
                conflictingJavaFiles.clear();
                boolean isConflicting = gitUtils.isConflicting(mergeCommit, conflictingJavaFiles);

                mergeCommitModel = new MergeCommit(mergeCommit.getName(), isConflicting,
                        mergeCommit.getParent(0).getName(), mergeCommit.getParent(1).getName(), project,
                        mergeCommit.getAuthorIdent().getName(), mergeCommit.getAuthorIdent().getEmailAddress(),
                        mergeCommit.getCommitTime());
                mergeCommitModel.saveIt();
                extractConflictingRegions(gitUtils, mergeCommitModel, conflictingJavaFiles);
                mergeCommitModel.setDone();
                mergeCommitModel.saveIt();
            } catch (GitAPIException e) {
                Utils.log(project.getName(), e);
                e.printStackTrace();
            }
            conflictingJavaFiles.clear();
        }
    }

    private void extractConflictingRegions(GitUtils gitUtils, MergeCommit mergeCommit,
                                           Map<String, String> conflictingJavaFiles) {
        List<int[][]> conflictingRegions = new ArrayList<>();
        List<GitUtils.CodeRegionChange> leftConflictingRegionHistory = new ArrayList<>();
        List<GitUtils.CodeRegionChange> rightConflictingRegionHistory = new ArrayList<>();

        for (String path : conflictingJavaFiles.keySet()) {
            String conflictType = conflictingJavaFiles.get(path);
            ConflictingJavaFile conflictingJavaFile = new ConflictingJavaFile(path, conflictType, mergeCommit);
            conflictingJavaFile.saveIt();

            if (conflictType.equalsIgnoreCase("content") ||
                    conflictType.equalsIgnoreCase("add/add")) {
                String[] conflictingRegionPaths = new String[2];
                conflictingRegions.clear();
                gitUtils.getConflictingRegions(path, conflictingRegionPaths, conflictingRegions);

                for (int[][] conflictingLines : conflictingRegions) {
                    ConflictingRegion conflictingRegion = new ConflictingRegion(
                            conflictingLines[0][0], conflictingLines[0][1], conflictingRegionPaths[0],
                            conflictingLines[1][0], conflictingLines[1][1], conflictingRegionPaths[1],
                            conflictingJavaFile);
                    conflictingRegion.saveIt();

                    leftConflictingRegionHistory.clear();
                    rightConflictingRegionHistory.clear();
                    gitUtils.getConflictingRegionHistory(mergeCommit.getParent1(), mergeCommit.getParent2(),
                            path, conflictingLines[0], leftConflictingRegionHistory);
                    gitUtils.getConflictingRegionHistory(mergeCommit.getParent2(), mergeCommit.getParent1(),
                            path, conflictingLines[1], rightConflictingRegionHistory);

                    leftConflictingRegionHistory.forEach(codeRegionChange -> {
                        RevCommit commit = gitUtils.populateCommit(codeRegionChange.commitHash);
                        String authorName = commit == null ? null : commit.getAuthorIdent().getName();
                        String authorEmail = commit == null ? null : commit.getAuthorIdent().getEmailAddress();
                        int timestamp = commit == null ? 0 : commit.getCommitTime();
                        new ConflictingRegionHistory(
                                codeRegionChange.commitHash, 1,
                                codeRegionChange.oldStartLine, codeRegionChange.oldLength, codeRegionChange.oldPath,
                                codeRegionChange.newStartLine, codeRegionChange.newLength, codeRegionChange.newPath,
                                conflictingRegion, authorName, authorEmail, timestamp).saveIt();
                    });
                    rightConflictingRegionHistory.forEach(codeRegionChange -> {
                        RevCommit commit = gitUtils.populateCommit(codeRegionChange.commitHash);
                        String authorName = commit == null ? null : commit.getAuthorIdent().getName();
                        String authorEmail = commit == null ? null : commit.getAuthorIdent().getEmailAddress();
                        int timestamp = commit == null ? 0 : commit.getCommitTime();
                        new ConflictingRegionHistory(
                                codeRegionChange.commitHash, 2,
                                codeRegionChange.oldStartLine, codeRegionChange.oldLength, codeRegionChange.oldPath,
                                codeRegionChange.newStartLine, codeRegionChange.newLength, codeRegionChange.newPath,
                                conflictingRegion, authorName, authorEmail, timestamp).saveIt();
                    });
                }
            }
        }
        conflictingRegions.clear();
        leftConflictingRegionHistory.clear();
        rightConflictingRegionHistory.clear();
    }

    private void analyzeProjectWithRefMiner(Project project) {
        List<ConflictingRegionHistory> historyConfRegions =
                ConflictingRegionHistory.where("project_id = ?", project.getId());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            File projectFile = new File(clonePath, project.getName());
            RefactoringMinerUtils refMinerUtils = new RefactoringMinerUtils(projectFile);

            for (int i = 0; i < historyConfRegions.size(); i++) {
                ConflictingRegionHistory conflictingRegionHistory = historyConfRegions.get(i);
                Utils.log(project.getName(), String.format("Analyzing commit %.7s with RefMiner... (%d/%d)",
                        conflictingRegionHistory.getCommitHash(), i + 1, historyConfRegions.size()));

                RefactoringCommit refactoringCommit = populateRefactoringCommit(conflictingRegionHistory);
                if (refactoringCommit == null) {
                    Utils.log(project.getName(), String.format("Already analyzed %.7s with RefMiner. Skipping...",
                            conflictingRegionHistory.getCommitHash()));
                    continue;
                }

                asyncRunRefMiner(executor, refMinerUtils, project, conflictingRegionHistory, refactoringCommit);
            }
        } catch (IOException | InterruptedException | ExecutionException e) {
            Utils.log(project.getName(), e);
            e.printStackTrace();
        }
        historyConfRegions.clear();
        executor.shutdownNow();
    }

    private RefactoringCommit populateRefactoringCommit(ConflictingRegionHistory conflictingRegionHistory) {
        RefactoringCommit refactoringCommit = RefactoringCommit.findFirst("commit_hash = ?",
                conflictingRegionHistory.getCommitHash());

        if (refactoringCommit == null) {
            refactoringCommit = new RefactoringCommit(conflictingRegionHistory.getCommitHash(),
                    conflictingRegionHistory.getProjectId());
            refactoringCommit.saveIt();
        } else if (refactoringCommit.isProcessed()) {
            return null;
        } else {
            ca.ualberta.cs.smr.refactoring.analysis.database.Refactoring.delete(
                    "refactoring_commit_id = ?", refactoringCommit.getID());
        }
        return refactoringCommit;
    }

    private void asyncRunRefMiner(ExecutorService executor, RefactoringMinerUtils refMinerUtils, Project project,
                                  ConflictingRegionHistory conflictingRegionHistory, RefactoringCommit refactoringCommit)
            throws InterruptedException, ExecutionException {
        List<Refactoring> refactorings = new ArrayList<>();
        Future futureRefMiner = executor.submit(() -> {
            try {
                refMinerUtils.detectAtCommit(conflictingRegionHistory.getCommitHash(), refactorings);
            } catch (Exception e) {
                Utils.log(project.getName(), e);
                e.printStackTrace();
            }
        });

        try {
            // Wait up to 4 minutes for RefactoringMiner to finish its analysis.
            futureRefMiner.get(4, TimeUnit.MINUTES);
            processRefactorings(refactorings, refactoringCommit, refMinerUtils);
            refactoringCommit.setDone();
            refactoringCommit.saveIt();

        } catch (TimeoutException e) {
            Utils.log(project.getName(), String.format("Commit %.7s timed out. Skipping...",
                    refactoringCommit.getCommitHash()));
            refactoringCommit.setTimedOut();
            refactoringCommit.saveIt();
        }
    }

    private void processRefactorings(List<Refactoring> refactorings, RefactoringCommit refactoringCommit,
                                     RefactoringMinerUtils refMinerUtils) {
        for (Refactoring refactoring : refactorings) {
            ca.ualberta.cs.smr.refactoring.analysis.database.Refactoring refactoringModel =
                    new ca.ualberta.cs.smr.refactoring.analysis.database.Refactoring(
                            refactoring.getRefactoringType().getDisplayName(),
                            refactoring.toString(),
                            refactoringCommit);
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
}
