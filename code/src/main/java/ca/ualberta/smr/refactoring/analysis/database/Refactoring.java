package ca.ualberta.smr.refactoring.analysis.database;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("refactoring")
public class Refactoring extends Model {

    public Refactoring() {
    }

    public Refactoring(String commitHash, String type, String detail) {
        setCommitHash(commitHash);
        setRefactoringTypeAndDetail(type, detail);
    }

    public void setCommitHash(String commitHash) {
        set("commit_hash", commitHash);
    }

    public void setRefactoringTypeAndDetail(String type, String detail) {
        set("refactoring_type", type, "refactoring_detail", detail);
    }

    public int getID() {
        return getInteger("id");
    }

}
