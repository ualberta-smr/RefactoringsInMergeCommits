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

public class RefactoringAnalysis {

    public final static String PROJECTS_DIRECTORY = "../projects";
    public final static String PROJECTS_LIST_FILE = "../reposList.txt";

    public static void main(String[] args) throws Exception {
        Base.open();
        new RefactoringAnalysis().run();
        Base.close();
    }


    private void run() throws Exception {
        List<String> projectURLs = Files.readAllLines(Paths.get(PROJECTS_LIST_FILE));
        for (String projectURL : projectURLs) {
            cloneProject(projectURL);
            Project project = Project.findFirst("url = ?", projectURL);
            if (project == null) {
                project = new Project(projectURL, projectURL.substring(projectURL.lastIndexOf('/') + 1));
                project.saveIt();
            }
            analyzeProject(project);
            removeProject(project.getName());
        }
    }

    private void cloneProject(String url) {
        try {
            String projectName = url.substring(url.lastIndexOf('/') + 1);
            Utils.log(String.format("Cloning %s...", projectName));
            Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(new File(PROJECTS_DIRECTORY, projectName))
                    .call();
        } catch (JGitInternalException e) {
            System.err.println(e.getMessage());
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }

    private void removeProject(String projectName) {
        try {
            Files.walk(Paths.get(new File(PROJECTS_DIRECTORY, projectName).getAbsolutePath()))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void analyzeProject(Project project) {
        Utils.log(String.format("Analyzing %s's commits...", project.getName()));
        analyzeProjectCommits(project);

        Utils.log(String.format("Analyzing %s with RefMiner...", project.getName()));
        analyzeProjectWithRefMiner(project);
    }

    private void analyzeProjectCommits(Project project) {
        try {
            GitUtils gitUtils = new GitUtils(new File(PROJECTS_DIRECTORY, project.getName()));
            List<RevCommit> mergeCommits = gitUtils.getMergeCommits();
            for (int i = 0; i < mergeCommits.size(); i++) {
                RevCommit mergeCommit = mergeCommits.get(i);
                Utils.log(String.format("Analyzing commit %.7s... (%d/%d)", mergeCommit.getName(), i + 1,
                        mergeCommits.size()));

                // Skip this commit if it already exists in the database.
                if (MergeCommit.where("commit_hash = ?", mergeCommit.getName()).size() > 0) continue;

                try {
                    Map<String, String> conflictingJavaFiles = new HashMap<>();
                    boolean isConflicting = gitUtils.isConflicting(mergeCommit, conflictingJavaFiles);

                    MergeCommit mergeCommitModel = new MergeCommit(mergeCommit.getName(), isConflicting,
                            mergeCommit.getParent(0).getName(), mergeCommit.getParent(1).getName(), project);
                    mergeCommitModel.saveIt();

                    extractConflictingRegions(gitUtils, mergeCommitModel, conflictingJavaFiles);
                } catch (GitAPIException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
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

                for (int i = 0; i < conflictingRegions.size(); i++) {
                    int[][] conflictingLines = conflictingRegions.get(i);
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
            RefactoringMinerUtils refMinerUtils = new RefactoringMinerUtils(new File(PROJECTS_DIRECTORY, project.getName()),
                    project.getURL());
            for (ConflictingRegionHistory conflictingRegionHistory : historyConfRegions) {
                if (ca.ualberta.cs.smr.refactoring.analysis.database.Refactoring.where("commit_hash = ?",
                        conflictingRegionHistory.getCommitHash()).size() > 0)
                    continue;

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
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }
    }
}
