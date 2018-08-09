package ca.ualberta.smr.refactoring.analysis.database;

import org.eclipse.jgit.api.MergeCommand;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("merge_commit")
public class MergeCommit extends Model {

    public MergeCommit(){}

    public MergeCommit(String commitHash, int projectId, boolean isConflicting){
        set("commit_hash", commitHash, "project_id", projectId, "is_conflicting", isConflicting);
    }
}
