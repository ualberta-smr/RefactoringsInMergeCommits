package ca.ualberta.smr.refactoring.analysis.database;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("conflicting_region")
public class ConflictingRegion extends Model {

    public ConflictingRegion(){}

    public ConflictingRegion(int startLine, int length, String mergeCommit, String mergeParent, int conflictingFileId){
        set("start_line", startLine, "length", length, "merge_commit", mergeCommit, "merge_parent", mergeParent,
                "conflicting_file_id", conflictingFileId);
    }

    public int getID() {
        return getInteger("id");
    }

    public String getMergeCommit() {
        return getString("merge_commit");
    }

    public String getMergeParent() {
        return getString("merge_parent");
    }
}
