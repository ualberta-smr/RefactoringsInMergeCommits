package ca.ualberta.smr.refactoring.analysis.utils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.RevFilter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitUtils {

    private Git git;


    public GitUtils(File repoDir) throws IOException, GitAPIException {
        git = Git.open(repoDir);
        git.reset().setMode(ResetCommand.ResetType.HARD).call();
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
        git.reset().setMode(ResetCommand.ResetType.HARD).call();
        git.checkout().setName(mergeCommit.getParent(0).getName()).call();
        String[] mergeCommand = new String[mergeCommit.getParentCount() + 1];
        mergeCommand[0] = "git";
        mergeCommand[1] = "merge";
        for (int i = 2; i < mergeCommand.length; i++) {
            mergeCommand[i] = mergeCommit.getParent(i - 1).getName();
        }
        String mergeOutput = Utils.runSystemCommand(git.getRepository().getWorkTree().getAbsolutePath(), false,
                mergeCommand);

        Pattern renameRenamePattern = Pattern.compile("CONFLICT \\((.+)\\): Rename \"(\\S*)\"->\"\\S*\".*");
        Pattern renameDeletePattern = Pattern.compile("CONFLICT \\((.+)\\):\\s(\\S+).+tree.");
        Pattern contentPattern = Pattern.compile("CONFLICT \\((.+)\\): Merge conflict in (\\S+)");

        for (String line : mergeOutput.split("\n")) {
            String filePath = null, conflictType = null;
            Matcher renameRenameMatcher = renameRenamePattern.matcher(line);
            Matcher renameDeleteMatcher = renameDeletePattern.matcher(line);
            Matcher contentMatcher = contentPattern.matcher(line);

            if (renameRenameMatcher.matches()) {
                conflictType = renameRenameMatcher.group(1);
                filePath = renameRenameMatcher.group(2);
            } else if (renameDeleteMatcher.matches()) {
                conflictType = renameDeleteMatcher.group(1);
                filePath = renameDeleteMatcher.group(2);
            } else if (contentMatcher.matches()) {
                conflictType = contentMatcher.group(1);
                filePath = contentMatcher.group(2);
            } else if (line.startsWith("CONFLICT")) {
                Utils.log("Unknown git conflict: " + line);
                continue;
            }
            if (filePath == null || !filePath.toLowerCase().endsWith(".java"))
                continue;
            javaConflicts.put(filePath, conflictType);
        }
        return mergeOutput.toLowerCase().contains("automatic merge failed");
    }

    public List<int[][]> getConflictingRegions(String path) {
        List<int[][]> conflictingRegions = new ArrayList<>();
        String diffOutput = Utils.runSystemCommand(git.getRepository().getWorkTree().getAbsolutePath(), false,
                "git", "diff", "-U0", path);
        Pattern diffStatPattern = Pattern.compile("\\@\\@\\@ \\-(\\d+),(\\d+) \\-(\\d+),(\\d+) \\+(\\d+),(\\d+) \\@\\@\\@[\\s\\S]*");
        for (String line : diffOutput.split("\n")) {
            Matcher matcher = diffStatPattern.matcher(line);
            if (matcher.matches()) {
                conflictingRegions.add(new int[][]{
                        {Integer.valueOf(matcher.group(1)), Integer.valueOf(matcher.group(2))},
                        {Integer.valueOf(matcher.group(3)), Integer.valueOf(matcher.group(4))}});
            }
        }
        return conflictingRegions;
    }

    public Map<String, int[]> getConflictingRegionHistory(String commitReachableFrom, String commitNotReachableFrom,
                                                            String path, int[] conflictingRegion) {
        Map<String, int[]> conflictingRegionHistory = new HashMap<>();

        String fileRange = conflictingRegion[0] + "," +  (conflictingRegion[0] + conflictingRegion[1]) + ":" + path;
        String commitRange = commitNotReachableFrom + ".." + commitReachableFrom;
        String logOutput = Utils.runSystemCommand(git.getRepository().getWorkTree().getAbsolutePath(), false,
                "git", "log", "--topo-order", "-u", "-L", fileRange, commitRange);

        Pattern diffStatPattern = Pattern.compile("\\@\\@ \\-(\\d+),(\\d+) \\+(\\d+),(\\d+) \\@\\@[\\s\\S]*");
        Pattern commitPattern = Pattern.compile("commit ([\\dabcdef]+)");

        String currentCommit = null;
        for (String line : logOutput.split("\n")) {
            Matcher commitMatcher = commitPattern.matcher(line);
            Matcher diffMatcher = diffStatPattern.matcher(line);

            if (commitMatcher.matches()) {
                currentCommit = commitMatcher.group(1);
            }
            else if (diffMatcher.matches()) {
                conflictingRegionHistory.put(currentCommit, new int[] {
                        Integer.valueOf(diffMatcher.group(1)),
                        Integer.valueOf(diffMatcher.group(2)),
                        Integer.valueOf(diffMatcher.group(3)),
                        Integer.valueOf(diffMatcher.group(4))});
            }
        }

        return conflictingRegionHistory;
    }

}
