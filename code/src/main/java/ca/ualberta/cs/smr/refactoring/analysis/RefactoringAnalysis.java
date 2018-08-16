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
            if (Project.where("url = ?", projectURL).size() == 0)
                new Project(projectURL, projectURL.substring(projectURL.lastIndexOf('/') + 1)).save();
            analyzeProject(Project.findFirst("url = ?", projectURL));
            removeProject(projectURL);
        }


//        projectURLs.forEach(this::cloneProject);
//        for (String projectURL : projectURLs) {
//            if (Project.where("url = ?", projectURL).size() == 0)
//                new Project(projectURL, projectURL.substring(projectURL.lastIndexOf('/') + 1)).save();
//        }
//        Project.findAll().forEach(model -> analyzeProject((Project) model));
    }

    private void cloneProject(String url) {
        try {
            String projectName = url.substring(url.lastIndexOf('/') + 1);
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

    private void removeProject(String url) {
        try {
            String projectName = url.substring(url.lastIndexOf('/') + 1);
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
            List<RevCommit> mergeCommits = new ArrayList<>();
            gitUtils.getMergeCommits().forEach(mergeCommits::add);
            for (int i = 0; i < mergeCommits.size(); i++) {
                RevCommit mergeCommit = mergeCommits.get(i);
                Utils.log(String.format("Analyzing commit %.7s... (%d/%d)", mergeCommit.getName(), i + 1,
                        mergeCommits.size()));

                // Skip this commit if it already exists in the database.
                if (MergeCommit.where("commit_hash = ?", mergeCommit.getName()).size() > 0) continue;

                try {
                    Map<String, String> conflictingJavaFiles = new HashMap<>();
                    boolean isConflicting = gitUtils.isConflicting(mergeCommit, conflictingJavaFiles);

                    new MergeCommit(mergeCommit.getName(), project.getID(), isConflicting).saveIt();

                    MergeParent mergeParentLeft = new MergeParent(mergeCommit.getName(), mergeCommit.getParent(0).getName());
                    MergeParent mergeParentRight = new MergeParent(mergeCommit.getName(), mergeCommit.getParent(1).getName());
                    mergeParentLeft.saveIt();
                    mergeParentRight.saveIt();

                    extractConflictingRegions(gitUtils, mergeCommit, conflictingJavaFiles, mergeParentLeft, mergeParentRight);
                } catch (GitAPIException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }
    }

    private void extractConflictingRegions(GitUtils gitUtils, RevCommit mergeCommit,
                                           Map<String, String> conflictingJavaFiles, MergeParent mergeParentLeft,
                                           MergeParent mergeParentRight) {
        for (String path : conflictingJavaFiles.keySet()) {
            String conflictType = conflictingJavaFiles.get(path);
            ConflictingFile conflictingFile = new ConflictingFile(mergeCommit.getName(), path, conflictType);
            conflictingFile.saveIt();

            if (conflictType.equalsIgnoreCase("content") ||
                    conflictType.equalsIgnoreCase("add/add")) {
                String[] conflictingRegionPaths = new String[2];
                List<int[][]> conflictingRegions = new ArrayList<>();
                gitUtils.getConflictingRegions(path, conflictingRegionPaths, conflictingRegions);

                for (int i = 0; i < conflictingRegions.size(); i++) {
                    int[][] conflictingLines = conflictingRegions.get(i);
                    ConflictingRegion leftConflictingRegion = new ConflictingRegion(conflictingLines[0][0],
                            conflictingLines[0][1], conflictingRegionPaths[0],
                            mergeParentLeft.getID(), conflictingFile.getID());
                    ConflictingRegion rightConflictingRegion = new ConflictingRegion(conflictingLines[1][0],
                            conflictingLines[1][1], conflictingRegionPaths[1],
                            mergeParentRight.getID(), conflictingFile.getID());
                    leftConflictingRegion.saveIt();
                    rightConflictingRegion.setId(leftConflictingRegion.getID());
                    // Using Model#insert() instead of Model#saveIt() to enforce insertion, because ActiveJDBC will
                    // attempt to update when the same id already exists.
                    rightConflictingRegion.insert();

                    List<GitUtils.CodeRegionChange> leftConflictingRegionHistory = gitUtils.getConflictingRegionHistory(
                            mergeCommit.getParent(0).getName(), mergeCommit.getParent(1).getName(),
                            path, conflictingLines[0]);
                    List<GitUtils.CodeRegionChange> rightConflictingRegionHistory = gitUtils.getConflictingRegionHistory(
                            mergeCommit.getParent(1).getName(), mergeCommit.getParent(0).getName(),
                            path, conflictingLines[1]);

                    leftConflictingRegionHistory.forEach(codeRegionChange -> new ConflictingRegionHistory(
                            codeRegionChange.commitHash,
                            codeRegionChange.oldStartLine, codeRegionChange.oldLength, codeRegionChange.oldPath,
                            codeRegionChange.newStartLine, codeRegionChange.newLength, codeRegionChange.newPath,
                            leftConflictingRegion.getID(), leftConflictingRegion.getMergeParentId()).saveIt());
                    rightConflictingRegionHistory.forEach(codeRegionChange -> new ConflictingRegionHistory(
                            codeRegionChange.commitHash,
                            codeRegionChange.oldStartLine, codeRegionChange.oldLength, codeRegionChange.oldPath,
                            codeRegionChange.newStartLine, codeRegionChange.newLength, codeRegionChange.newPath,
                            rightConflictingRegion.getID(), rightConflictingRegion.getMergeParentId()).saveIt());
                }
            }
        }
    }


    private void analyzeProjectWithRefMiner(Project project) {
        List<ConflictingRegionHistory> historyConfRegions = ConflictingRegionHistory.findBySQL(
                "select conflicting_region_history.* from " +
                        "conflicting_region_history, conflicting_region, conflicting_file, merge_commit where " +
                        "conflicting_region_history.conflicting_region_id = conflicting_region.id and " +
                        "conflicting_region_history.merge_parent_id = conflicting_region.merge_parent_id and " +
                        "conflicting_region.conflicting_file_id = conflicting_file.id and " +
                        "conflicting_file.merge_commit = merge_commit.commit_hash and " +
                        "merge_commit.project_id = ?", project.getID());

        try {
            RefactoringMinerUtils refMiner = new RefactoringMinerUtils(new File(PROJECTS_DIRECTORY, project.getName()),
                    project.getURL());
            for (ConflictingRegionHistory conflictingRegionHistory : historyConfRegions) {
                if (ca.ualberta.cs.smr.refactoring.analysis.database.Refactoring.where("commit_hash = ?",
                        conflictingRegionHistory.getCommitHash()).size() > 0)
                    continue;

                List<Refactoring> refactorings = refMiner.detectAtCommit(conflictingRegionHistory.getCommitHash());
                for (Refactoring refactoring : refactorings) {
                    ConflictingRegion conflictingRegion = ConflictingRegion.findFirst("id = ?",
                            conflictingRegionHistory.getConflictingRegionId());
                    ca.ualberta.cs.smr.refactoring.analysis.database.Refactoring refactoringModel =
                            new ca.ualberta.cs.smr.refactoring.analysis.database.Refactoring(
                                    conflictingRegionHistory.getCommitHash(),
                                    refactoring.getRefactoringType().getDisplayName(),
                                    refactoring.toString(), conflictingRegion.getMergeParentId());
                    refactoringModel.saveIt();
                    List<CodeRange> sourceCodeRanges = new ArrayList<>();
                    List<CodeRange> destCodeRanges = new ArrayList<>();
                    refMiner.getRefactoringCodeRanges(refactoring, sourceCodeRanges, destCodeRanges);
                    sourceCodeRanges.forEach(cr -> new RefactoringRegion('s', cr.getFilePath(), cr.getStartLine(),
                            cr.getEndLine() - cr.getStartLine(), refactoringModel.getID()).saveIt());
                    destCodeRanges.forEach(cr -> new RefactoringRegion('d', cr.getFilePath(), cr.getStartLine(),
                            cr.getEndLine() - cr.getStartLine(), refactoringModel.getID()).saveIt());
                }

            }
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }
    }
}
