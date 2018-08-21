package ca.ualberta.cs.smr.refactoring.analysis.database;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("project")
public class Project extends Model {

    static {
        validatePresenceOf("url", "name");
    }

    public Project() {
    }

    public Project(String url, String name) {
        set("url", url, "name", name, "is_done", false);
    }

    public String getName() {
        return getString("name");
    }

    public String getURL(){
        return getString("url");
    }

    public boolean isDone() {
        return getBoolean("is_done");
    }

    public void setDone() {
        setBoolean("is_done", true);
    }

}
