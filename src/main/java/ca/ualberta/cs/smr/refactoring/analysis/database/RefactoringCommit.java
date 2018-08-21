package ca.ualberta.cs.smr.refactoring.analysis.database;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("refactoring_commit")
public class RefactoringCommit extends Model {

    public RefactoringCommit() {
    }

    public RefactoringCommit(String commitHash, Object projectId) {
        set("commit_hash", commitHash, "project_id", projectId, "is_done", false);
    }

    public int getID() {
        return getInteger("id");
    }

    public int getProjectId() {
        return getInteger("project_id");
    }

    public String getCommitHash() {
        return getString("commit_hash");
    }

    public boolean isDone() {
        return getBoolean("is_done");
    }

    public void setDone() {
        setBoolean("is_done", true);
    }

}
