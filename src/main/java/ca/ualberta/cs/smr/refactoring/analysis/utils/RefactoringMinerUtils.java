package ca.ualberta.cs.smr.refactoring.analysis.utils;

import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.diff.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class RefactoringMinerUtils {

    private Git git;

    public RefactoringMinerUtils(File repoDir) throws IOException {
        git = Git.open(repoDir);
    }

    private RefactoringMinerUtils() {

    }

    public void detectAtCommit(String commitHash, List<Refactoring> refactoringsResult) throws Exception {
        new GitUtils(git).gitReset();

        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        miner.detectAtCommit(git.getRepository(), commitHash, new RefactoringHandler() {
            @Override
            public void handle(String commitData, List<Refactoring> refactorings) {
                refactoringsResult.addAll(refactorings);
            }
        });
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
                UMLClass extractedClass = ((ExtractSuperclassRefactoring) refactoring).getExtractedClass();
                destCodeRange.add(extractedClass.getLocationInfo().codeRange());

                Set<UMLClass> subClasses = ((ExtractSuperclassRefactoring) refactoring).getUMLSubclassSet();
                subClasses.forEach(umlClass -> {
                    sourceCodeRange.add(umlClass.getLocationInfo().codeRange());
                    destCodeRange.add(umlClass.getLocationInfo().codeRange());
                });
                break;
            case EXTRACT_AND_MOVE_OPERATION:
                UMLOperation extractedOperation = ((ExtractOperationRefactoring) refactoring).getExtractedOperation();
                UMLOperation sourceBeforeExtraction = ((ExtractOperationRefactoring) refactoring)
                        .getSourceOperationBeforeExtraction();

                UMLOperation sourceAfterExtraction = ((ExtractOperationRefactoring) refactoring)
                        .getSourceOperationAfterExtraction();

                sourceCodeRange.add(sourceBeforeExtraction.getLocationInfo().codeRange());
                destCodeRange.add(extractedOperation.getLocationInfo().codeRange());
                destCodeRange.add(sourceAfterExtraction.getLocationInfo().codeRange());
                break;
            case MOVE_RENAME_CLASS:
                sourceCodeRange.add(((MoveAndRenameClassRefactoring) refactoring).getOriginalClass().getLocationInfo().codeRange());
                destCodeRange.add(((MoveAndRenameClassRefactoring) refactoring).getRenamedClass().getLocationInfo().codeRange());
                break;
            case MOVE_CLASS:
                sourceCodeRange.add(((MoveClassRefactoring) refactoring).getOriginalClass().getLocationInfo().codeRange());
                destCodeRange.add(((MoveClassRefactoring) refactoring).getMovedClass().getLocationInfo().codeRange());
                break;
            case RENAME_CLASS:
                sourceCodeRange.add(((RenameClassRefactoring) refactoring).getOriginalClass().getLocationInfo().codeRange());
                destCodeRange.add(((RenameClassRefactoring) refactoring).getRenamedClass().getLocationInfo().codeRange());
                break;
            case EXTRACT_VARIABLE:
                destCodeRange.add(((ExtractVariableRefactoring) refactoring).getExtractedVariableDeclarationCodeRange());
                break;
            case RENAME_PARAMETER:
            case RENAME_VARIABLE:
            case PARAMETERIZE_VARIABLE:
                sourceCodeRange.add(((RenameVariableRefactoring) refactoring).getOriginalVariable().codeRange());
                destCodeRange.add(((RenameVariableRefactoring) refactoring).getRenamedVariable().codeRange());
                break;
            case RENAME_ATTRIBUTE:
                sourceCodeRange.add(((RenameAttributeRefactoring) refactoring).getOriginalAttribute().codeRange());
                destCodeRange.add(((RenameAttributeRefactoring) refactoring).getRenamedAttribute().codeRange());
                break;
            case MOVE_ATTRIBUTE:
            case PULL_UP_ATTRIBUTE:
            case PUSH_DOWN_ATTRIBUTE:
                sourceCodeRange.add(((MoveAttributeRefactoring) refactoring).getSourceAttributeCodeRangeBeforeMove());
                destCodeRange.add(((MoveAttributeRefactoring) refactoring).getTargetAttributeCodeRangeAfterMove());
                break;
            case RENAME_PACKAGE:
                ((RenamePackageRefactoring) refactoring).getMoveClassRefactorings().forEach(ref -> {
                    sourceCodeRange.add(ref.getOriginalClass().getLocationInfo().codeRange());
                    destCodeRange.add(ref.getMovedClass().getLocationInfo().codeRange());
                });
                break;
        }
    }

}
