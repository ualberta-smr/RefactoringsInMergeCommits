package ca.ualberta.cs.smr.refactoring.analysis.utils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.RevFilter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitUtils {

    private static final Pattern PATTERN_CONFLICT_RENAME_RENAME = Pattern.compile("CONFLICT \\((.+)\\): Rename \"(\\S*)\"->\"\\S*\".*");
    private static final Pattern PATTERN_CONFLICT_DELETE = Pattern.compile("CONFLICT \\((.+)\\):\\s(\\S+).+tree.");
    private static final Pattern PATTERN_CONFLICT_CONTENT = Pattern.compile("CONFLICT \\((.+)\\): Merge conflict in (\\S+)");
    private static final Pattern PATTERN_DIFF_PATH = Pattern.compile("(?:\\-\\-\\-|\\+\\+\\+) (?:a\\/|b\\/)?([\\s\\S]+)");
    private static final Pattern PATTERN_DIFF_RANGE = Pattern.compile("\\@\\@ \\-(\\d+),(\\d+) \\+(\\d+),(\\d+) \\@\\@[\\s\\S]*");
    private static final Pattern PATTERN_COMBINED_DIFF_RANGE = Pattern.compile("\\@\\@\\@ \\-(\\d+),(\\d+) \\-(\\d+),(\\d+) \\+(\\d+),(\\d+) \\@\\@\\@[\\s\\S]*");
    private static final Pattern PATTERN_LOG_COMMIT = Pattern.compile("commit ([\\dabcdef]{40})");

    private Git git;

    public GitUtils(File repoDir) throws IOException, GitAPIException {
        git = Git.open(repoDir);
        Utils.runSystemCommand(git.getRepository().getWorkTree().getAbsolutePath(), false,
                "git", "reset", "--hard");
    }

    private GitUtils() {

    }

    static GitUtils getMockInstance() {
        return new GitUtils();
    }

    public Iterable<RevCommit> getMergeCommits() {
        try {
            return git.log().all().setRevFilter(RevFilter.ONLY_MERGES).call();
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public boolean isConflicting(RevCommit mergeCommit, Map<String, String> javaConflicts) throws GitAPIException {
        Utils.runSystemCommand(git.getRepository().getWorkTree().getAbsolutePath(), false,
                "git", "reset", "--hard");
        Utils.runSystemCommand(git.getRepository().getWorkTree().getAbsolutePath(), false,
                "git", "checkout", mergeCommit.getParent(0).getName());
//        git.checkout().setName(mergeCommit.getParent(0).getName()).call();
        String[] mergeCommand = new String[mergeCommit.getParentCount() + 1];
        mergeCommand[0] = "git";
        mergeCommand[1] = "merge";
        for (int i = 2; i < mergeCommand.length; i++) {
            mergeCommand[i] = mergeCommit.getParent(i - 1).getName();
        }
        String mergeOutput = Utils.runSystemCommand(git.getRepository().getWorkTree().getAbsolutePath(), false,
                mergeCommand);

        return isConflictingFromMergeOutput(mergeOutput, javaConflicts);
    }

    public boolean isConflictingFromMergeOutput(String mergeOutput, Map<String, String> javaConflicts) {
        for (String line : mergeOutput.split("\n")) {
            if (!line.startsWith("CONFLICT")) continue;

            String filePath, conflictType;
            Matcher renameRenameMatcher = PATTERN_CONFLICT_RENAME_RENAME.matcher(line);
            Matcher renameDeleteMatcher = PATTERN_CONFLICT_DELETE.matcher(line);
            Matcher contentMatcher = PATTERN_CONFLICT_CONTENT.matcher(line);

            if (renameRenameMatcher.matches()) {
                conflictType = renameRenameMatcher.group(1);
                filePath = renameRenameMatcher.group(2);
            } else if (renameDeleteMatcher.matches()) {
                conflictType = renameDeleteMatcher.group(1);
                filePath = renameDeleteMatcher.group(2);
            } else if (contentMatcher.matches()) {
                conflictType = contentMatcher.group(1);
                filePath = contentMatcher.group(2);
            } else if (line.toLowerCase().contains(".java")) {
                conflictType = "Undetected";
                filePath = line;
            } else {
                Utils.log("Unknown git conflict: " + line);
                continue;
            }

            if (filePath.toLowerCase().endsWith(".java") || conflictType.equals("Undetected"))
                javaConflicts.put(filePath, conflictType);
        }
        return mergeOutput.toLowerCase().contains("automatic merge failed");
    }

    public void getConflictingRegions(String path, String[] conflictingRegionPaths,
                                      List<int[][]> conflictingRegions) {
        String diffOutput = Utils.runSystemCommand(git.getRepository().getWorkTree().getAbsolutePath(), false,
                "git", "diff", "-U0", path);
        getConflictingRegionsFromDiffOutput(diffOutput, conflictingRegionPaths, conflictingRegions);
    }

    public void getConflictingRegionsFromDiffOutput(String diffOutput, String[] conflictingRegionPaths,
                                                     List<int[][]> conflictingRegions) {
        for (String line : diffOutput.split("\n")) {
            Matcher conflictLocMatcher = PATTERN_COMBINED_DIFF_RANGE.matcher(line);
            Matcher conflictPathMatcher = PATTERN_DIFF_PATH.matcher(line);

            if (conflictPathMatcher.matches()) {
                if (conflictingRegionPaths[0] == null) {
                    conflictingRegionPaths[0] = conflictPathMatcher.group(1);
                } else if (conflictingRegionPaths[1] == null) {
                    conflictingRegionPaths[1] = conflictPathMatcher.group(1);
                }
            } else if (conflictLocMatcher.matches()) {
                conflictingRegions.add(new int[][]{
                        {Integer.valueOf(conflictLocMatcher.group(1)), Integer.valueOf(conflictLocMatcher.group(2))},
                        {Integer.valueOf(conflictLocMatcher.group(3)), Integer.valueOf(conflictLocMatcher.group(4))}});
            }
        }
    }

    public List<CodeRegionChange> getConflictingRegionHistory(String commitReachableFrom, String commitNotReachableFrom,
                                                              String path, int[] conflictingRegion) {
        String fileRange = conflictingRegion[0] + "," + (conflictingRegion[0] + conflictingRegion[1]) + ":" + path;
        String commitRange = commitNotReachableFrom + ".." + commitReachableFrom;
        String logOutput = Utils.runSystemCommand(git.getRepository().getWorkTree().getAbsolutePath(), false,
                "git", "log", "--topo-order", "-u", "-L", fileRange, commitRange);
        return getConflictingRegionHistoryFromGitOutput(logOutput);
    }

    public List<CodeRegionChange> getConflictingRegionHistoryFromGitOutput(String logOutput) {
        List<CodeRegionChange> codeRegionChanges = new ArrayList<>();
        String currentCommit = null;
        String[] currentPaths = new String[2];
        for (String line : logOutput.split("\n")) {
            Matcher commitMatcher = PATTERN_LOG_COMMIT.matcher(line);
            Matcher pathMatcher = PATTERN_DIFF_PATH.matcher(line);
            Matcher diffMatcher = PATTERN_DIFF_RANGE.matcher(line);

            if (commitMatcher.matches()) {
                currentCommit = commitMatcher.group(1);
            } else if (pathMatcher.matches()) {
                if (currentPaths[0] == null) {
                    currentPaths[0] = pathMatcher.group(1);
                } else if (currentPaths[1] == null) {
                    currentPaths[1] = pathMatcher.group(1);
                } else {

                }
            } else if (diffMatcher.matches()) {
                codeRegionChanges.add(new CodeRegionChange(
                        currentCommit, currentPaths[0], currentPaths[1],
                        Integer.valueOf(diffMatcher.group(1)),
                        Integer.valueOf(diffMatcher.group(2)),
                        Integer.valueOf(diffMatcher.group(3)),
                        Integer.valueOf(diffMatcher.group(4))));
                currentPaths = new String[2];
            }
        }

        return codeRegionChanges;
    }

    public static class CodeRegionChange {
        public String commitHash, oldPath, newPath;
        public int oldStartLine, oldLength, newStartLine, newLength;

        CodeRegionChange(String commitHash, String oldPath, String newPath, int oldStartLine, int oldLength,
                         int newStartLine, int newLength) {
            this.commitHash = commitHash;
            this.oldPath = oldPath;
            this.newPath = newPath;
            this.oldStartLine = oldStartLine;
            this.oldLength = oldLength;
            this.newStartLine = newStartLine;
            this.newLength = newLength;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CodeRegionChange that = (CodeRegionChange) o;
            return oldStartLine == that.oldStartLine &&
                    oldLength == that.oldLength &&
                    newStartLine == that.newStartLine &&
                    newLength == that.newLength &&
                    Objects.equals(commitHash, that.commitHash) &&
                    Objects.equals(oldPath, that.oldPath) &&
                    Objects.equals(newPath, that.newPath);
        }
    }

}
