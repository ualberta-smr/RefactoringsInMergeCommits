package ca.ualberta.smr.refactoring.analysis.database;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("conflicting_file")
public class ConflictingFile extends Model {

    public ConflictingFile() {}


    public ConflictingFile(String mergeCommit, String path, String type) {
        set("merge_commit", mergeCommit, "path", path, "type", type);
    }

    public int getID() {
        return getInteger("id");
    }


}
