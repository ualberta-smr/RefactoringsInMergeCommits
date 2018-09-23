package ca.ualberta.cs.smr.refactoring.analysis.database;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("merge_commit")
public class MergeCommit extends Model {

    public MergeCommit(){}

    public MergeCommit(String commitHash, boolean isConflicting, String parent1, String parent2, Project project,
                       String authorName, String authorEmail, int timestamp) {
        set("commit_hash", commitHash, "project_id", project.getId(), "is_conflicting", isConflicting, "parent_1", parent1,
                "parent_2", parent2, "is_done", false, "author_name", authorName, "author_email", authorEmail,
                "timestamp", timestamp);
    }

    public int getProjectId() {
        return getInteger("project_id");
    }

    public String getCommitHash() {
        return getString("commit_hash");
    }

    public String getParent1() {
        return getString("parent_1");
    }


    public String getParent2() {
        return getString("parent_2");
    }

    public boolean isDone() {
        return getBoolean("is_done");
    }

    public void setDone() {
        setBoolean("is_done", true);
    }

    public void setCommitDetails(String authorName, String authorEmail, int timestamp) {
        set("author_name", authorName, "author_email", authorEmail,
                "timestamp", timestamp);
    }
}
