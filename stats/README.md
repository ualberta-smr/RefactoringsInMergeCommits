### involved_projects.py
This file contains scripts for finding projects and merge commits with involved refactorings.

Running `python involved_projects.py` will list all projects with involved refactorings and the number of involved merge commits for each project. The command will take longer to execute if there is no existing `merge_commits_with_involved_refs.pickle` file; in this case, the script will read directly from the database and generate a `merge_commits_with_involved_refs.pickle` file for faster parsing. There are also two possible options for the command:
* `python involved_projects.py <project_id>`: this will show all merge commit ids with involved refactorings for the provided `project_id` (if any).
* `python involved_projects.py --csv`: this will generate a `merge_commits_with_involved_refs.csv` file that lists a table with `merge_commit` columns found in the database, as well as two additional columns: `has_involved_refs` (boolean 0 or 1 if there are involved refactorings) and `involved_cr_count` (number of involved refactorings). This will only show data for merge commit ids and projects with involved refactorings.

#### Conflict Region Size
This script does not currently filter merge commit ids by conflict region size. You may use merge commit ids and project ids from the script output in a mysql query like below:
```
SELECT * from conflicting_region
WHERE project_id = <project_id> AND
merge_commit_id = <merge_commit_id> AND
parent_1_length + parent_2_length < <conflict_region_size>

ORDER BY project_id, merge_commit_id
```
