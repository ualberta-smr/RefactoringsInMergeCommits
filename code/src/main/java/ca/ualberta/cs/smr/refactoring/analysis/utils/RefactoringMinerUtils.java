package ca.ualberta.cs.smr.refactoring.analysis.utils;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.diff.*;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.refactoringminer.RefactoringMiner;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RefactoringMinerUtils {

    private Git git;
    private String url;

    public RefactoringMinerUtils(File repoDir, String url) throws IOException {
        this.url = url;
        git = Git.open(repoDir);
    }

    private RefactoringMinerUtils () {

    }

    public static RefactoringMinerUtils getMockRefactoringMinerUtils() {
        return new RefactoringMinerUtils();
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

    public void getRefactoringCodeRanges(org.refactoringminer.api.Refactoring refactoring,
                                         List<CodeRange> sourceCodeRange, List<CodeRange> destCodeRange) {
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
                // TODO: Ask Nikos to implement changes made to ExtractSuperclassRefactoring.
                UMLClass extractedClass = ((ExtractSuperclassRefactoring)refactoring).getExtractedClass();
                destCodeRange.add(extractedClass.getLocationInfo().codeRange());

                Set<UMLClass> subClasses = ((ExtractSuperclassRefactoring)refactoring).getSubclassUMLSet();
                subClasses.forEach(umlClass -> {
                    // TODO: This doesn't look very accurate.
                    sourceCodeRange.add(umlClass.getLocationInfo().codeRange());
                    destCodeRange.add(umlClass.getLocationInfo().codeRange());
                });
                break;
            case EXTRACT_AND_MOVE_OPERATION:
                UMLOperation extractedOperation = ((ExtractAndMoveOperationRefactoring) refactoring)
                        .getExtractedOperation();
                UMLOperation sourceBeforeExtraction = ((ExtractAndMoveOperationRefactoring) refactoring)
                        .getSourceOperationBeforeExtraction();
                UMLOperation sourceAfterExtraction = ((ExtractAndMoveOperationRefactoring) refactoring)
                        .getSourceOperationAfterExtraction();

                sourceCodeRange.add(sourceBeforeExtraction.getLocationInfo().codeRange());
                destCodeRange.add(extractedOperation.getLocationInfo().codeRange());
                destCodeRange.add(sourceAfterExtraction.getLocationInfo().codeRange());
                break;
            // TODO: Ask Nikos to implement changes made to the following classes.
            case MOVE_RENAME_CLASS:
                sourceCodeRange.add(((MoveAndRenameClassRefactoring)refactoring).getOriginalClass().getLocationInfo().codeRange());
                destCodeRange.add(((MoveAndRenameClassRefactoring)refactoring).getRenamedClass().getLocationInfo().codeRange());
                break;
            case MOVE_CLASS:
                sourceCodeRange.add(((MoveClassRefactoring)refactoring).getOriginalClass().getLocationInfo().codeRange());
                destCodeRange.add(((MoveClassRefactoring)refactoring).getMovedClass().getLocationInfo().codeRange());
                break;
            case RENAME_CLASS:
                sourceCodeRange.add(((RenameClassRefactoring)refactoring).getOriginalClass().getLocationInfo().codeRange());
                destCodeRange.add(((RenameClassRefactoring)refactoring).getRenamedClass().getLocationInfo().codeRange());
                break;
        }
    }

}
