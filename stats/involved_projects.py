import pandas as pd
from data_resolver import get_conflicting_region_histories, get_accepted_refactoring_regions, get_merge_commits, record_involved
from sys import argv, modules, stderr

# TODO
# to pickle for project id
# to pickle for general stats
# to csv to project id
# to csv for general stats
# include refactoring size filtering + attached to project id stats
# include number of merge commits per project for general stats


def get_data_frame(df_name):
    # TODO
    # runs the appropriate get method if no pickle available
    try:
        return pd.read_pickle(df_name + '.pickle')
    except FileNotFoundError:
        df = getattr(modules[__name__], 'get_' + df_name)()
        df.to_pickle(df_name + '.pickle')
        return df


def get_merge_commits_with_involved_refs():
    """Returns DataFrame of all merge commits with involved refactorings and additional column has_involved_refs = 1.

    :return: Empty panda DataFrame if no merge commits with involved refactorings; otherwise, a panda DataFrame with
    default merge_commit DataFrame columns and has_involved_refs.
    """
    conflicting_region_histories = get_conflicting_region_histories()
    refactoring_regions = get_accepted_refactoring_regions()

    involved_cr_count_per_merge = pd.DataFrame()
    rr_grouped_by_project = refactoring_regions.groupby('project_id')
    counter = 0

    for project_id, project_crh in conflicting_region_histories.groupby('project_id'):
        # project_crh is a DataFrame specific to a project_id.
        counter += 1
        if project_id not in rr_grouped_by_project.groups:
            continue

        project_rrs = rr_grouped_by_project.get_group(project_id)
        crh_rr_combined = pd.merge(project_crh.reset_index(), project_rrs.reset_index(), on='commit_hash', how='inner')

        # Filter fields in crh_rr_combined that have involved refactorings.
        involved = crh_rr_combined[crh_rr_combined.apply(record_involved, axis=1)]

        # Add to the end of the existing table involved_cr_count_per_merge (which is initially empty).
        involved_cr_count_per_merge = involved_cr_count_per_merge.append(
            involved.groupby('merge_commit_id').conflicting_region_id.nunique().to_frame().rename(
                columns={'conflicting_region_id': 'involved_cr_count'}))

        # FIXME temporary
        if counter > 10:
            break

    merge_commits_with_involved_refs = create_merge_commit_dataframe_with_involved_refs(involved_cr_count_per_merge)

    return merge_commits_with_involved_refs[merge_commits_with_involved_refs['has_involved_refs'] > 0]


def create_merge_commit_dataframe_with_involved_refs(involved_cr_count_per_merge):
    """Creates and returns a DataFrame similar to merge_commits DataFrame, except it has an additional has_involved_refs
    column, which has boolean value 1 if a merge commit has more than one involved refactoring; otherwise 0.

    :param involved_cr_count_per_merge: DataFrame with number of involved conflict regions per merge commit id.
    :return: panda DataFrame with merge_commit columns and has_involved_refs.
    """
    # Copy merge_commit DataFrame to create a new one with merge_commit fields; add has_involved refs column here.
    merge_commits = get_merge_commits()
    updated_merge_commit_involved_table = merge_commits.copy()
    updated_merge_commit_involved_table['has_involved_refs'] = 0

    merge_commits_grouped_by_merge_commit_id = updated_merge_commit_involved_table.groupby('id')

    for merge_commit_id, involved_cr_count_data in involved_cr_count_per_merge.groupby('merge_commit_id'):
        if merge_commit_id not in merge_commits_grouped_by_merge_commit_id.groups:
            continue

        # If merge_commit_id has involved_cr_count > 0, this means there is at least one conflict region affected by an
        # involved refactoring. Set boolean for has_involved_refs to true (1).
        updated_merge_commit_involved_table.loc[updated_merge_commit_involved_table['id'] == merge_commit_id, 'has_involved_refs'] = 1

    return updated_merge_commit_involved_table


def get_dict_projects_with_merge_commits_and_involved_refs():
    """ Returns a dictionary of projects and their merge commit ids that have involved refactorings.
    Keys are the project ids, while values are the commit ids for a project id (if any).

    :return: dict with keys as project ids and values as a list of merge commit ids a project may have.
    """
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

    projects = get_dict_projects_with_merge_commits_and_involved_refs()

    print('-' * 80)
    print('Number of projects with involved refactorings: {}'.format(len(projects)))
    print('Projects involved:')

    for project_id in projects:
        print('\tProject {}'.format(project_id))

    print('-' * 80)


def get_list_of_merge_commits_for_project_id(project_id):
    """ Returns a list of merge commit ids for a given project id that have involved refactorings.

    :param project_id: integer value representing a project id
    :return: list of integers representing merge commit ids
    """
    projects = get_dict_projects_with_merge_commits_and_involved_refs()
    # print("OUR INPUT: ", project_id)
    # print("OUR LIST: ", projects)
    for project in projects:
        # print("CURRENT READ PROJECT IN LIST: ", project)
        if project_id == project:
            return projects[project]

    return None


def print_merge_commits_for_project_id(project_id):
    print("Getting involved merge commits for project {}".format(str(project_id)))
    merge_commits = get_list_of_merge_commits_for_project_id(project_id)

    if merge_commits is None:
        print('No merge commits with involved refactorings were found for project {}!'.format(str(project_id)))
    else:
        print('Involved merge commits for project {}:'.format(str(project_id)))
        for merge_commit in merge_commits:
            print('\tMerge commit {}'.format(merge_commit))


if __name__ == '__main__':
    """ Running python file with project id as a parameter will output all involved merge commits for the currently
    specified project id.
    """
    if len(argv) == 1:
        print_projects_with_involved_refs_stats()
    else:
        try:
            if isinstance(int(argv[1]), int):
                print_merge_commits_for_project_id(int(argv[1]))
        except Exception:
            stderr.write('Project id should be an integer!\n')
