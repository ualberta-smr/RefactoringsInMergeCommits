select project.name, commit_url(merge_commit.commit_hash, project.url) as commit_hash_url
from merge_commit
inner join project on (project.id=merge_commit.project)
where
	has_conflict=1 and
	(select count(*) from merge_parent_refactoring
		where merge_parent_refactoring.merge_commit=merge_commit.commit_hash
        and merge_parent_refactoring.refactorings_count > 0)
	> 1
group by merge_commit.commit_hash
order by rand()
limit 10;