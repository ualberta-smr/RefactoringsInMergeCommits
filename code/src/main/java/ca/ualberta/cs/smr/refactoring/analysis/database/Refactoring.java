package ca.ualberta.cs.smr.refactoring.analysis.database;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("refactoring")
public class Refactoring extends Model {

    private final static int MAX_DETAIL_LENGTH = 2000;

    public Refactoring() {
    }

    public Refactoring(String commitHash, int mergeParent, String type, String detail, int mergeCommitId, int projectId) {
        set("commit_hash", commitHash, "merge_parent", mergeParent, "refactoring_type", type, "refactoring_detail",
                detail.substring(0, Math.min(detail.length(), MAX_DETAIL_LENGTH)),
                "merge_commit_id", mergeCommitId, "project_id", projectId);
    }

    public int getMergeCommitId() {
        return getInteger("merge_commit_id");
    }


    public int getProjectId() {
        return getInteger("project_id");
    }

}
