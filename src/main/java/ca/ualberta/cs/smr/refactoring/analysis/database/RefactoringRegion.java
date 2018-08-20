package ca.ualberta.cs.smr.refactoring.analysis.database;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("refactoring_region")
public class RefactoringRegion extends Model {

    public RefactoringRegion(){ }
    public RefactoringRegion(char type, String path, int startLine, int length, Refactoring refactoring) {
        set("type", String.valueOf(type).toLowerCase(), "path", path, "start_line", startLine, "length", length,
                "refactoring_id", refactoring.getId(),
                "merge_commit_id", refactoring.getMergeCommitId(),
                "project_id", refactoring.getProjectId());
    }
}
