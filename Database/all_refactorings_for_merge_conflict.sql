select 	project_name,
		merge_commit_hash,
        commit_url(merge_parent_hash, project_url) as merge_parent_url,
        commit_url(refactoring_commit_hash, project_url) as refactoring_commit_url,
        refactoring_type,
        count(*)
from all_refactorings
where merge_commit_hash="3cb189454b8ecf11da9acaf6c3f4f3425f9385b9"
group by merge_parent_hash, refactoring_commit_hash, refactoring_type
order by merge_parent_url, refactoring_commit_url, count(*) desc;