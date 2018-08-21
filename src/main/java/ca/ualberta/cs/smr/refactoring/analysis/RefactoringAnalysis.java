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
                        mergeCommit.getParent(0).getName(), mergeCommit.getParent(1).getName(), project);
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
        conflictingRegions.clear();
        leftConflictingRegionHistory.clear();
        rightConflictingRegionHistory.clear();
    }


    private void analyzeProjectWithRefMiner(Project project) {
        List<ConflictingRegionHistory> historyConfRegions =
                ConflictingRegionHistory.where("project_id = ?", project.getId());

        List<Refactoring> refactorings = new ArrayList<>();
        List<CodeRange> sourceCodeRanges = new ArrayList<>();
        List<CodeRange> destCodeRanges = new ArrayList<>();
        try {
            RefactoringMinerUtils refMinerUtils = new RefactoringMinerUtils(new File(clonePath, project.getName()),
                    project.getURL());
            for (int i = 0; i < historyConfRegions.size(); i++) {
                ConflictingRegionHistory conflictingRegionHistory = historyConfRegions.get(i);
                if (ca.ualberta.cs.smr.refactoring.analysis.database.Refactoring.where("commit_hash = ?",
                        conflictingRegionHistory.getCommitHash()).size() > 0)
                    continue;

                Utils.log(project.getName(), String.format("Analyzing commit %.7s with RefMiner... (%d/%d)",
                        conflictingRegionHistory.getCommitHash(), i + 1, historyConfRegions.size()));
                refactorings.clear();
                refMinerUtils.detectAtCommit(conflictingRegionHistory.getCommitHash(), refactorings);
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

                    sourceCodeRanges.clear();
                    destCodeRanges.clear();
                    refMinerUtils.getRefactoringCodeRanges(refactoring, sourceCodeRanges, destCodeRanges);
                    sourceCodeRanges.forEach(cr -> new RefactoringRegion('s', cr.getFilePath(), cr.getStartLine(),
                            cr.getEndLine() - cr.getStartLine(), refactoringModel).saveIt());
                    destCodeRanges.forEach(cr -> new RefactoringRegion('d', cr.getFilePath(), cr.getStartLine(),
                            cr.getEndLine() - cr.getStartLine(), refactoringModel).saveIt());
                }
                refactorings.clear();
            }
            historyConfRegions.clear();
        } catch (GitAPIException | IOException e) {
            Utils.log(project.getName(), e);
            e.printStackTrace();
        }
        refactorings.clear();
        sourceCodeRanges.clear();
        destCodeRanges.clear();
    }
}
