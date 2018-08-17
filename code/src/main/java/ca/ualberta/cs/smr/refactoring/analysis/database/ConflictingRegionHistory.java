package ca.ualberta.cs.smr.refactoring.analysis.database;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("conflicting_region_history")
public class ConflictingRegionHistory extends Model {

    public ConflictingRegionHistory() {
    }

    public ConflictingRegionHistory(String commitHash, int mergeParent, int oldStartLine, int oldLength, String oldPath,
                                    int newStartLine, int newLength, String newPath,
                                    ConflictingRegion conflictingRegion) {
        set("commit_hash", commitHash, "merge_parent", mergeParent,
                "old_start_line", oldStartLine, "old_length", oldLength, "old_path", oldPath,
                "new_start_line", newStartLine, "new_length", newLength, "new_path", newPath,
                "conflicting_region_id", conflictingRegion.getId(),
                "conflicting_java_file_id", conflictingRegion.getConflictingJavaFileId(),
                "merge_commit_id", conflictingRegion.getMergeCommitId(),
                "project_id", conflictingRegion.getProjectId());
    }

    public String getCommitHash(){
        return getString("commit_hash");
    }

    public int getMergeCommitId(){
        return getInteger("merge_commit_id");
    }

    public int getMergeParent() {
        return getInteger("merge_parent");
    }

    public int getProjectId() {
        return getInteger("project_id");
    }


}
