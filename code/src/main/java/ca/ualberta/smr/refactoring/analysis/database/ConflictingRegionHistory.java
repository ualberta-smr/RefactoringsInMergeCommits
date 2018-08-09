package ca.ualberta.smr.refactoring.analysis.database;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("conflicting_region_history")
public class ConflictingRegionHistory extends Model {

    public ConflictingRegionHistory() {
    }

    public ConflictingRegionHistory(String commitHash, int oldStartLine, int oldLength, int newStartLine, int newLength,
                             int conflictingRegionId) {
        set("commit_hash", commitHash, "old_start_line", oldStartLine, "old_length", oldLength, "new_start_line",
                newStartLine, "new_length", newLength, "conflicting_region_id", conflictingRegionId);
    }

    public String getCommitHash(){
        return getString("commit_hash");
    }
}
