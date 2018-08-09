package ca.ualberta.smr.refactoring.analysis.utils;

import gr.uom.java.xmi.diff.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RefactoringMinerUtils {

    private Git git;
    private String url;

    public RefactoringMinerUtils(File repoDir, String url) throws IOException {
        this.url = url;
        git = Git.open(repoDir);
    }

    public List<org.refactoringminer.api.Refactoring> detectAtCommit(String commitHash) throws GitAPIException {
        git.reset().setMode(ResetCommand.ResetType.HARD).call();

        List<org.refactoringminer.api.Refactoring> allRefactorings = new ArrayList<>();
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        miner.detectAtCommit(git.getRepository(), url, commitHash, new RefactoringHandler() {
            @Override
            public void handle(RevCommit commitData, List<org.refactoringminer.api.Refactoring> refactorings) {
                allRefactorings.addAll(refactorings);
            }
        });

        return allRefactorings;
    }

    public void getRefactoringCodeRanges(org.refactoringminer.api.Refactoring refactoring, List<CodeRange> sourceCodeRange,
                                         List<CodeRange> destCodeRange) {
        switch (refactoring.getRefactoringType()) {
            case MOVE_OPERATION:
                sourceCodeRange.add(((MoveOperationRefactoring) refactoring).getSourceOperationCodeRangeBeforeMove());
                destCodeRange.add(((MoveOperationRefactoring) refactoring).getTargetOperationCodeRangeAfterMove());
                break;
            case PULL_UP_OPERATION:
                sourceCodeRange.add(((PullUpOperationRefactoring) refactoring).getSourceOperationCodeRangeBeforeMove());
                destCodeRange.add(((PullUpOperationRefactoring) refactoring).getTargetOperationCodeRangeAfterMove());
                break;
            case PUSH_DOWN_OPERATION:
                sourceCodeRange.add(((PushDownOperationRefactoring) refactoring).getSourceOperationCodeRangeBeforeMove());
                destCodeRange.add(((PushDownOperationRefactoring) refactoring).getTargetOperationCodeRangeAfterMove());
                break;
            case RENAME_METHOD:
                sourceCodeRange.add(((RenameOperationRefactoring) refactoring).getSourceOperationCodeRangeBeforeRename());
                destCodeRange.add(((RenameOperationRefactoring) refactoring).getTargetOperationCodeRangeAfterRename());
                break;
            case INLINE_OPERATION:
                sourceCodeRange.add(((InlineOperationRefactoring) refactoring).getTargetOperationCodeRangeBeforeInline());
                sourceCodeRange.add(((InlineOperationRefactoring) refactoring).getInlinedOperationCodeRange());
                destCodeRange.add(((InlineOperationRefactoring) refactoring).getTargetOperationCodeRangeAfterInline());
                break;
            case EXTRACT_OPERATION:
                sourceCodeRange.add(((ExtractOperationRefactoring) refactoring).getSourceOperationCodeRangeBeforeExtraction());
                destCodeRange.add(((ExtractOperationRefactoring) refactoring).getExtractedOperationCodeRange());
                destCodeRange.add(((ExtractOperationRefactoring) refactoring).getSourceOperationCodeRangeAfterExtraction());
                break;
            case EXTRACT_INTERFACE:
            case EXTRACT_SUPERCLASS:
                destCodeRange.add(new CodeRange(((ExtractSuperclassRefactoring)refactoring).getExtractedClass()
                        .getSourceFile(), -1, -1, -1, -1));
                // TODO: Nikos should implement access to the set of subclasses.
                break;
            case EXTRACT_AND_MOVE_OPERATION:
            case MOVE_RENAME_CLASS:
            case MOVE_CLASS:
            case RENAME_CLASS:
                // TODO: CodeRange not implemented.
                break;
        }
    }

}
