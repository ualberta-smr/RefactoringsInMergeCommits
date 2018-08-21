package ca.ualberta.cs.smr.refactoring.analysis.utils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.StopWalkException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitUtils {

    private static final Pattern PATTERN_CONFLICT_RENAME_RENAME = Pattern.compile("CONFLICT \\((rename\\/rename)\\): Rename \"?[^\"]+\"?->\"?([^\"]+)\"? in .+ [Rr]ename \"?[^\"]+\"?->\"?[^\"]+\"? in .+");
    private static final Pattern PATTERN_CONFLICT_RENAME_ADD = Pattern.compile("CONFLICT \\((rename\\/add)\\): Rename \"?[^\"]+\"?->\"?([^\"]+)\"? in \\S+ \"?[^\"]+\"? added in .+");
    private static final Pattern PATTERN_CONFLICT_DELETE = Pattern.compile("CONFLICT \\(((?:rename|modify)\\/delete)\\): \"?([^\"]+)\"? deleted in .+ and (?:renamed|modified) .+");
    private static final Pattern PATTERN_CONFLICT_CONTENT = Pattern.compile("CONFLICT \\(((?:content|add\\/add))\\): Merge conflict in \"?([^\"]+)\"?");
    private static final Pattern PATTERN_DIFF_PATH = Pattern.compile("(?:\\-\\-\\-|\\+\\+\\+) (?:a\\/|b\\/)?([\\s\\S]+)");
    private static final Pattern PATTERN_DIFF_RANGE = Pattern.compile("\\@\\@ \\-(\\d+),(\\d+) \\+(\\d+),(\\d+) \\@\\@[\\s\\S]*");
    private static final Pattern PATTERN_COMBINED_DIFF_RANGE = Pattern.compile("\\@\\@\\@ \\-(\\d+),(\\d+) \\-(\\d+),(\\d+) \\+(\\d+),(\\d+) \\@\\@\\@[\\s\\S]*");
    private static final Pattern PATTERN_LOG_COMMIT = Pattern.compile("commit ([\\dabcdef]{40})");

    private Git git;

    public GitUtils(File repoDir) throws IOException, GitAPIException {
        git = Git.open(repoDir);
        Utils.runSystemCommand(git.getRepository().getWorkTree().getAbsolutePath(),
                "git", "reset", "--hard");
    }

    private GitUtils() {

    }

    static GitUtils getMockInstance() {
        return new GitUtils();
    }

    public Iterable<RevCommit> getMergeCommits() {
        try {
            return git.log().all().setRevFilter(new RevFilter() {
                @Override
                public boolean include(RevWalk revWalk, RevCommit revCommit) throws StopWalkException {
                    return revCommit.getParentCount() == 2;
                }

                @Override
                public RevFilter clone() {
                    return this;
                }
            }).call();
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isConflicting(RevCommit mergeCommit, Map<String, String> javaConflicts) throws GitAPIException {
        Utils.runSystemCommand(git.getRepository().getWorkTree().getAbsolutePath(),
                "git", "reset", "--hard");
        Utils.runSystemCommand(git.getRepository().getWorkTree().getAbsolutePath(),
                "git", "checkout", mergeCommit.getParent(0).getName());
        String mergeOutput = Utils.runSystemCommand(git.getRepository().getWorkTree().getAbsolutePath(),
                "git", "merge", "--no-commit", mergeCommit.getParent(1).getName());

        return isConflictingFromMergeOutput(mergeOutput, javaConflicts);
    }

    public boolean isConflictingFromMergeOutput(String mergeOutput, Map<String, String> javaConflicts) throws
            GitAPIException {
        if (mergeOutput.toLowerCase().startsWith("fatal:")) {
            throw new UnsupportedGitConflict("There was a problem while checking for merge conflict:\n" + mergeOutput);
        }
        for (String line : mergeOutput.split("\n")) {
            if (!line.startsWith("CONFLICT")) continue;

            String filePath, conflictType;
            Matcher renameRenameMatcher = PATTERN_CONFLICT_RENAME_RENAME.matcher(line);
            Matcher renameAddMatcher = PATTERN_CONFLICT_RENAME_ADD.matcher(line);
            Matcher deleteMatcher = PATTERN_CONFLICT_DELETE.matcher(line);
            Matcher contentMatcher = PATTERN_CONFLICT_CONTENT.matcher(line);

            if (renameRenameMatcher.matches()) {
                conflictType = renameRenameMatcher.group(1);
                filePath = renameRenameMatcher.group(2);
            } else if (renameAddMatcher.matches()) {
                conflictType = renameAddMatcher.group(1);
                filePath = renameAddMatcher.group(2);
            } else if (deleteMatcher.matches()) {
                conflictType = deleteMatcher.group(1);
                filePath = deleteMatcher.group(2);
            } else if (contentMatcher.matches()) {
                conflictType = contentMatcher.group(1);
                filePath = contentMatcher.group(2);
            } else if (line.toLowerCase().contains(".java")) {
                conflictType = "Undetected";
                filePath = line;
            } else {
                if (git != null)
                    Utils.log(git.getRepository().getWorkTree().getName(), "Unknown git conflict: " + line);
                continue;
            }

            if (filePath.toLowerCase().endsWith(".java") || conflictType.equals("Undetected"))
                javaConflicts.put(filePath, conflictType);
        }
        return mergeOutput.toLowerCase().contains("automatic merge failed");
    }

    public void getConflictingRegions(String path, String[] conflictingRegionPaths,
                                      List<int[][]> conflictingRegions) {
        String diffOutput = Utils.runSystemCommand(git.getRepository().getWorkTree().getAbsolutePath(),
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

    public void getConflictingRegionHistory(String commitReachableFrom, String commitNotReachableFrom,
                                            String path, int[] conflictingRegion,
                                            List<CodeRegionChange> conflictingRegionHistory) {
        String fileRange = conflictingRegion[0] + "," + (conflictingRegion[0] + conflictingRegion[1]) + ":" + path;
        String commitRange = commitNotReachableFrom + ".." + commitReachableFrom;
        String logOutput = Utils.runSystemCommand(git.getRepository().getWorkTree().getAbsolutePath(),
                "git", "log", "--topo-order", "-u", "-L", fileRange, commitRange);
        getConflictingRegionHistoryFromGitOutput(logOutput, conflictingRegionHistory);
    }

    public void getConflictingRegionHistoryFromGitOutput(String logOutput, List<CodeRegionChange> conflictingRegionHistory) {
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
                conflictingRegionHistory.add(new CodeRegionChange(
                        currentCommit, currentPaths[0], currentPaths[1],
                        Integer.valueOf(diffMatcher.group(1)),
                        Integer.valueOf(diffMatcher.group(2)),
                        Integer.valueOf(diffMatcher.group(3)),
                        Integer.valueOf(diffMatcher.group(4))));
                currentPaths = new String[2];
            }
        }
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

    public static class UnsupportedGitConflict extends GitAPIException {

        protected UnsupportedGitConflict(String message) {
            super(message);
        }
    }

}
