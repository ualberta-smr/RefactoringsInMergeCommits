package ca.ualberta.cs.smr.refactoring.analysis.database;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("conflicting_region_history")
public class ConflictingRegionHistory extends Model {

    public ConflictingRegionHistory() {
    }

    public ConflictingRegionHistory(String commitHash, int oldStartLine, int oldLength, String oldPath,
                                    int newStartLine, int newLength, String newPath, int conflictingRegionId,
                                    int mergeParentId) {
        set("commit_hash", commitHash, "old_start_line", oldStartLine, "old_length", oldLength, "old_path", oldPath,
                "new_start_line", newStartLine, "new_length", newLength, "new_path", newPath,
                "conflicting_region_id", conflictingRegionId, "merge_parent_id", mergeParentId);
    }

    public String getCommitHash(){
        return getString("commit_hash");
    }

    public int getConflictingRegionId() {
        return getInteger("conflicting_region_id");
    }
}
