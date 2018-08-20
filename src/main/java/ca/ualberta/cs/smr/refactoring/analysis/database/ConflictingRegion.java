package ca.ualberta.cs.smr.refactoring.analysis.database;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("conflicting_region")
public class ConflictingRegion extends Model {

    public ConflictingRegion(){}

    public ConflictingRegion(int startLineParent1, int lengthParent1, String pathParent1,
                             int startLineParent2, int lengthParent2, String pathParent2,
                             ConflictingJavaFile conflictingJavaFile) {
        set("parent_1_start_line", startLineParent1, "parent_1_length", lengthParent1, "parent_1_path", pathParent1,
                "parent_2_start_line", startLineParent2, "parent_2_length", lengthParent2, "parent_2_path", pathParent2,
                "conflicting_java_file_id", conflictingJavaFile.getId(),
                "merge_commit_id", conflictingJavaFile.getMergeCommitId(),
                "project_id", conflictingJavaFile.getProjectId());
    }

    public int getConflictingJavaFileId() {
        return getInteger("conflicting_java_file_id");
    }


    public int getMergeCommitId() {
        return getInteger("merge_commit_id");
    }


    public int getProjectId() {
        return getInteger("project_id");
    }

}
