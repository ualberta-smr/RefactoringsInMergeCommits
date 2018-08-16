package ca.ualberta.cs.smr.refactoring.analysis.database;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("refactoring")
public class Refactoring extends Model {

    private final static int MAX_DETAIL_LENGTH = 2000;

    public Refactoring() {
    }

    public Refactoring(String commitHash, String type, String detail, int mergeParentId) {
        set("commit_hash", commitHash, "refactoring_type", type, "refactoring_detail",
                detail.substring(0, Math.min(detail.length(), MAX_DETAIL_LENGTH)),
                "merge_parent_id", mergeParentId);
    }

    public int getID() {
        return getInteger("id");
    }

}
