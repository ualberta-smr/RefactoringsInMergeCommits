import pandas as pd
from data_resolver import get_conflicting_region_histories, \
    get_accepted_refactoring_regions, \
    get_merge_commits, \
    record_involved
from sys import argv, stderr

# TODO
# test output with actual db and see if merge commits are properly represented
# to pickle for project id
# to pickle for general stats
# to csv to project id
# to csv for general stats
# include refactoring size filtering + attached to project id stats
# include number of merge commits per project for general stats


def get_merge_commits_with_involved_refs():
    # from get_conflicting_regions_by_involved_refactorings_per_merge_commit
    conflicting_region_histories = get_conflicting_region_histories()
    refactoring_regions = get_accepted_refactoring_regions()

    # We will append later. Shows how many involved refactorings there are per merge scenario/commit.
    involved_cr_count_per_merge = pd.DataFrame()

    rr_grouped_by_project = refactoring_regions.groupby('project_id')
    counter = 0

    for project_id, project_crh in conflicting_region_histories.groupby('project_id'):
        # project_crh is a table specific to a project_id

        counter += 1
        if project_id not in rr_grouped_by_project.groups:
            continue

        # table of projects with refactoring regions that coincide with conflict history regions
        project_rrs = rr_grouped_by_project.get_group(project_id)

        # table that is a combination of conflict region histories and refactoring regions according to commit hash
        crh_rr_combined = pd.merge(project_crh.reset_index(), project_rrs.reset_index(), on='commit_hash', how='inner')

        # filter fields in crh_rr_combined that match condition - that is, if it has involved refactorings or not
        involved = crh_rr_combined[crh_rr_combined.apply(record_involved, axis=1)]

        # Add to the end of the existing table involved_cr_count_per_merge (which is initially empty). What we append
        # is data that was grouped by merge_commit_id. Then, we count each UNIQUE instance or value of the
        # conflicting region id. Afterwards, rename to the proper label name: involved_cr_count. Sample output:
        '''            
        merge_commit_id                 involved_cr_count         
        294                             12
        356                             1
        430                             2
        '''
        involved_cr_count_per_merge = involved_cr_count_per_merge.append(
            involved.groupby('merge_commit_id').conflicting_region_id.nunique().to_frame().rename(
                columns={'conflicting_region_id': 'involved_cr_count'}))

        if counter > 5: break

    merge_commits_with_involved_refs = create_merge_commit_dataframe_with_involved_refs(involved_cr_count_per_merge)
    return merge_commits_with_involved_refs[
        merge_commits_with_involved_refs['has_involved_refs'] > 0]


def create_merge_commit_dataframe_with_involved_refs(involved_cr_count_per_merge):
    # Copy merge_commit dataframe to create a new dataframe with merge_commit fields; add has_involved refs field here
    merge_commits = get_merge_commits()
    updated_merge_commit_involved_table = merge_commits.copy()
    updated_merge_commit_involved_table['has_involved_refs'] = 0

    merge_commits_grouped_by_merge_commit_id = updated_merge_commit_involved_table.groupby('id')

    for merge_commit_id, involved_cr_count_data in involved_cr_count_per_merge.groupby('merge_commit_id'):
        if merge_commit_id not in merge_commits_grouped_by_merge_commit_id.groups:
            continue

        # If merge commit has involved_cr_count > 0, this means there is at least one conflict region affected by an
        # involved refactoring. Set boolean to true (1).
        updated_merge_commit_involved_table.loc[
            updated_merge_commit_involved_table['id'] == merge_commit_id, 'has_involved_refs'] = 1

    return updated_merge_commit_involved_table


def get_projects_with_merge_commits_and_involved_refs():
    merge_commits_with_involved_refs = get_merge_commits_with_involved_refs()
    merge_commits_with_involved_refs_grouped = merge_commits_with_involved_refs.groupby('id')
    projects = {}

    for merge_commit_id, merge_commit_data in merge_commits_with_involved_refs_grouped:
        index = int(merge_commit_id) - 1
        project_id = merge_commit_data.at[index, 'project_id']
        if project_id not in projects.keys():
            projects[project_id] = []
        projects[project_id].append(merge_commit_id)

    return projects


def print_projects_with_involved_refs_stats():
    print("Getting stats for projects with involved refactorings...")

    projects = get_projects_with_merge_commits_and_involved_refs()

    print('-' * 80)
    print('Number of projects with involved refactorings: {}'.format(len(projects)))
    print('Projects involved:')

    for project_id in projects:
        print('\tProject {}'.format(project_id))

    print('-' * 80)


def get_merge_commits_for_project_id(project_id):
    projects = get_projects_with_merge_commits_and_involved_refs()
    # print("OUR INPUT: ", project_id)
    # print("OUR LIST: ", projects)
    for project in projects:
        # print("CURRENT READ PROJECT IN LIST: ", project)
        if project_id == project:
            return projects[project]

    return None


def print_merge_commits_for_project_id(project_id):
    print("Getting involved merge commits for project {}".format(str(project_id)))
    merge_commits = get_merge_commits_for_project_id(project_id)

    if merge_commits is None:
        print('No merge commits with involved refactorings were found for project {}!'.format(str(project_id)))
    else:
        print('Involved merge commits for project {}:'.format(str(project_id)))
        for merge_commit in merge_commits:
            print('\tMerge commit {}'.format(merge_commit))


if __name__ == '__main__':
    if len(argv) == 1:
        print_projects_with_involved_refs_stats()
    else:
        try:
            if isinstance(int(argv[2]), int):
                print_merge_commits_for_project_id(int(argv[2]))
            else:
                stderr.write('\'-mc\' requires an integer argument!\n')
        except Exception: # account for possible index error
            stderr.write('\'-mc\' requires an integer argument!\n')
