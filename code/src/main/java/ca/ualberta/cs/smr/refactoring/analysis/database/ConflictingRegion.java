package ca.ualberta.cs.smr.refactoring.analysis.database;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("conflicting_region")
public class ConflictingRegion extends Model {

    public ConflictingRegion(){}

    public ConflictingRegion(int startLine, int length, String path, int mergeParentId, int conflictingFileId){
        set("start_line", startLine, "length", length, "path", path, "merge_parent_id", mergeParentId,
                "conflicting_file_id", conflictingFileId);
    }

    public int getID() {
        return getInteger("id");
    }

    public int getMergeParentId() {
        return getInteger("merge_parent_id");
    }

}
