from util import *
from database import *


def extract_merging_data(project_url, project_name):
    print('Analyzing {}...'.format(project_name))
    project_query = Project.select().where(Project.url == project_url)
    if len(project_query) == 0:
        project = Project(name=project_name, url=project_url)
        project.save()
    else:
        project = project_query[0]

    merge_commits = get_merge_commits(project_name)

    for merge_commit_index, merge_commit_hash in enumerate(merge_commits):
        print('Analyzing merge commit {} ({}/{})...'.format(merge_commit_hash[:7], merge_commit_index + 1, len(merge_commits)))

        # Merge commit has not been analyzed already.
        if len(MergeCommit.select().where(MergeCommit.commit_hash == merge_commit_hash)) == 0:
            common_ancestor_hash = get_common_ancestor_commit(project_name, get_parents(project_name, merge_commit_hash))
            if common_ancestor_hash is None or common_ancestor_hash.strip() == '':
                continue

            has_conflict, conflicts = get_merge_data(project_name, merge_commit_hash)
            merge_commit = MergeCommit(commit_hash=merge_commit_hash, common_ancestor_hash=common_ancestor_hash,
                                       has_conflict=has_conflict, project=project)
            merge_commit.save(force_insert=True)
            for conflict in conflicts:
                conflicting_file = ConflictingFile(merge_commit=merge_commit.commit_hash, type=conflict[0],
                path=conflict[1], package=conflict[2])
                conflicting_file.save()

            merge_parents = get_parents(project_name, merge_commit_hash)
            for merge_parent_hash in merge_parents:
                merge_parent = MergeParent(commit_hash=merge_parent_hash, merge_commit=merge_commit_hash)
                merge_parent.save()

                refactoring_changes = get_refactoring_changes(project_name, merge_parent_hash, common_ancestor_hash)
                for refactoring_commit_hash, refactoring_type, refactoring_detail, source_class, dest_class in refactoring_changes:
                    refactoring_commit = RefactoringCommit(commit_hash=refactoring_commit_hash,
                                                           merge_parent=merge_parent,
                                                           refactoring_type=refactoring_type,
                                                           refactoring_detail=refactoring_detail,
                                                           source_class=source_class,
                                                           destination_class=dest_class)
                    refactoring_commit.save()
