### involved_refactorings_scripts.py
This file contains scripts for finding projects, conflict regions, and merge commits with involved refactorings.

#### Possible commands:
* `python involved_refactorings_scripts.py --projects`: this will list all projects with involved refactorings and the number of involved merge commits for each project.
* `python involved_refactorings_scripts.py --merge_commits <project_id>`: this will show all merge commit ids with involved refactorings for the provided `project_id` (if any).
* `python involved_refactorings_scripts.py --conflict_regions <project_id> <merge_commit_id>`: this will show all conflict regions with involved refactorings for the provided `project_id` and `merge_commit_id` (if any).
* `python involved_refactorings_scripts.py --csv`: this will generate a `merge_commits_with_involved_refs.csv` file that lists a table with `merge_commit` columns found in the database, as well as two additional columns: `has_involved_refs` (boolean 0 or 1 if there are involved refactorings) and `involved_cr_count` (number of involved refactorings). This will only show data for merge commit ids and projects with involved refactorings.

#### `merge_commits_with_involved_refs.pickle`
Running the script with options `--projects`, `--merge_commits`, or `--csv` will take longer to execute if there is no existing `merge_commits_with_involved_refs.pickle` file; in this case, the script will read directly from the database and generate a `merge_commits_with_involved_refs.pickle` file for faster parsing.

Note that for the time being, running the script with `--conflict_regions` requires reading directly from the database and does not use this pickle file. 