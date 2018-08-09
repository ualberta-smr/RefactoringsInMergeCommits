package ca.ualberta.smr.refactoring.analysis.database;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("merge_parent")
public class MergeParent extends Model {

    public MergeParent(){}

    public MergeParent(String mergeCommit, String commitHash){
        set("merge_commit", mergeCommit, "commit_hash", commitHash);
    }
}
