## Tables

### merge_commit
The `is_conflicting` column indicates whether this commit had **ANY** conflicts reported by git.

### conflicting_file
This table only includes records from conflicting **JAVA** files. For example, a merge_conflict could have its `is_conflicting` set to 1, with no conflicting_file records refering to it (Meaning it had a conflict, just not a Java one).

### conflicting_region
There could be several conflicting regions in a conflicting file. For each conflicting region, this table stores the information about the region for of the merge parents. So there'll be two records in this table per conflicting region. Records blonging to the same region, share the same `id`. Since merges have two parents, we would have two records for a given `id`.
