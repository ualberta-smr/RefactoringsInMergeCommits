package ca.ualberta.smr.refactoring.analysis.database;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("refactoring")
public class Refactoring extends Model {

    public Refactoring() {
    }

    public Refactoring(String commitHash, String type, String detail, String mergeCommit, String mergeParent) {
        set("commit_hash", commitHash, "refactoring_type", type, "refactoring_detail", detail,
                "merge_commit", mergeCommit, "merge_parent", mergeParent);
    }

    public int getID() {
        return getInteger("id");
    }

}
