package ca.ualberta.cs.smr.refactoring.analysis.database;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("refactoring")
public class Refactoring extends Model {

    private final static int MAX_DETAIL_LENGTH = 2000;

    public Refactoring() {
    }

    public Refactoring(String type, String detail, RefactoringCommit refactoringCommit) {
        set("refactoring_type", type, "refactoring_detail",
                detail.substring(0, Math.min(detail.length(), MAX_DETAIL_LENGTH)),
                "refactoring_commit_id", refactoringCommit.getID(), "commit_hash", refactoringCommit.getCommitHash(),
                "project_id", refactoringCommit.getProjectId());
    }

    public int getProjectId() {
        return getInteger("project_id");
    }

    public String getCommitHash() {
        return getString("commit_hash");
    }

    public int getRefactoringCommitId() {
        return getInteger("refactoring_commit_id");
    }

}
