package ca.ualberta.smr.refactoring.analysis.utils;

import ca.ualberta.smr.refactoring.analysis.database.RefactoringRegion;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.diff.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
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

    public List<Refactoring> detectAtCommit(String commitHash) throws GitAPIException {
        git.reset().setMode(ResetCommand.ResetType.HARD).call();

        List<Refactoring> allRefactorings = new ArrayList<>();
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        miner.detectAtCommit(git.getRepository(), url, commitHash, new RefactoringHandler() {
            @Override
            public void handle(RevCommit commitData, List<Refactoring> refactorings) {
                allRefactorings.addAll(refactorings);
            }
        });

        return allRefactorings;
    }

    public void getRefactoringCodeRanges(Refactoring refactoring, List<CodeRange> sourceCodeRanges,
                                         List<CodeRange> destCodeRanges) {
        List<CodeRange> tempSourceCodeRange = new ArrayList<>();
        List<CodeRange> tempDestCodeRange = new ArrayList<>();
        switch (refactoring.getRefactoringType()) {
            case MOVE_OPERATION:
                tempSourceCodeRange.add(((MoveOperationRefactoring) refactoring).getSourceOperationCodeRangeBeforeMove());
                tempDestCodeRange.add(((MoveOperationRefactoring) refactoring).getTargetOperationCodeRangeAfterMove());
                break;
            case PULL_UP_OPERATION:
                tempSourceCodeRange.add(((PullUpOperationRefactoring) refactoring).getSourceOperationCodeRangeBeforeMove());
                tempDestCodeRange.add(((PullUpOperationRefactoring) refactoring).getTargetOperationCodeRangeAfterMove());
                break;
            case PUSH_DOWN_OPERATION:
                tempSourceCodeRange.add(((PushDownOperationRefactoring) refactoring).getSourceOperationCodeRangeBeforeMove());
                tempDestCodeRange.add(((PushDownOperationRefactoring) refactoring).getTargetOperationCodeRangeAfterMove());
                break;
            case RENAME_METHOD:
                tempSourceCodeRange.add(((RenameOperationRefactoring) refactoring).getSourceOperationCodeRangeBeforeRename());
                tempDestCodeRange.add(((RenameOperationRefactoring) refactoring).getTargetOperationCodeRangeAfterRename());
                break;
            case INLINE_OPERATION:
                tempSourceCodeRange.add(((InlineOperationRefactoring) refactoring).getTargetOperationCodeRangeBeforeInline());
                tempSourceCodeRange.add(((InlineOperationRefactoring) refactoring).getInlinedOperationCodeRange());
                tempDestCodeRange.add(((InlineOperationRefactoring) refactoring).getTargetOperationCodeRangeAfterInline());
                break;
            case EXTRACT_OPERATION:
                tempSourceCodeRange.add(((ExtractOperationRefactoring) refactoring).getSourceOperationCodeRangeBeforeExtraction());
                tempDestCodeRange.add(((ExtractOperationRefactoring) refactoring).getExtractedOperationCodeRange());
                tempDestCodeRange.add(((ExtractOperationRefactoring) refactoring).getSourceOperationCodeRangeAfterExtraction());
                break;
            case EXTRACT_INTERFACE:
            case EXTRACT_SUPERCLASS:
                tempDestCodeRange.add(new CodeRange(((ExtractSuperclassRefactoring)refactoring).getExtractedClass()
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

        // Slice all file paths so they become relative to the repository.
        tempSourceCodeRange.forEach(cr -> sourceCodeRanges.add(new CodeRange(cr.getFilePath().substring(
                git.getRepository().getWorkTree().getAbsolutePath().length()), cr.getStartLine(), cr.getEndLine(),
                cr.getStartColumn(), cr.getEndColumn())));
        tempDestCodeRange.forEach(cr -> destCodeRanges.add(new CodeRange(cr.getFilePath().substring(
                git.getRepository().getWorkTree().getAbsolutePath().length()), cr.getStartLine(), cr.getEndLine(),
                cr.getStartColumn(), cr.getEndColumn())));
    }

}
