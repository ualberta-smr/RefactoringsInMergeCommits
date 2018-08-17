package ca.ualberta.cs.smr.refactoring.analysis.database;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("conflicting_java_file")
public class ConflictingJavaFile extends Model {

    public ConflictingJavaFile() {}


    public ConflictingJavaFile(String path, String type, MergeCommit mergeCommit) {
        set("path", path, "type", type, "merge_commit_id", mergeCommit.getId(),
                "project_id", mergeCommit.getProjectId());
    }

    public int getMergeCommitId(){
        return getInteger("merge_commit_id");
    }


    public int getProjectId(){
        return getInteger("project_id");
    }


}
