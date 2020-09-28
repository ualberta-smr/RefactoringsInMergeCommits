from data_resolver import test,\
    get_merge_commit_by_crh_and_devs_and_involved_refactorings,\
    get_conflicting_regions_by_involved_refactorings_per_merge_commit,\
    get_conflicting_regions_by_count_of_involved_refactoring ,\
    effect_size_conflicting_region_by_involved_refactoring,\
    print_stats,\
    get_merge_commits_with_involved_refs_in_crs,\
    get_conflicting_region_histories,\
    get_accepted_refactoring_regions,\
    get_merge_commits,\
    record_involved

import pandas as pd
from sqlalchemy import create_engine
import sys
import re
from scipy.stats import ranksums
import math

def get_merge_commits_with_involved_refs_in_crs():
    # from get_conflicting_regions_by_involved_refactorings_per_merge_commit
    print('attempting to get involved crs')
    conflicting_region_histories = get_conflicting_region_histories()
    refactoring_regions = get_accepted_refactoring_regions()

    # rename a groupby possible
    cr_count_per_merge = conflicting_region_histories.groupby(
        'merge_commit_id').conflicting_region_id.nunique().to_frame().rename(
        columns={'conflicting_region_id': 'cr_count'})

    # We will append later. Shows how many involved refactorings there are per merge scenario/commit.
    involved_cr_count_per_merge = pd.DataFrame()

    rr_grouped_by_project = refactoring_regions.groupby('project_id')
    counter = 0

    for project_id, project_crh in conflicting_region_histories.groupby('project_id'):
        # project_crh is a table specific to a project_id

        counter += 1
        # print('Processing project {}'.format(counter))
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
        # print(involved_cr_count_per_merge)

        print('For project {}'.format(project_id))
        print(involved_cr_count_per_merge)  # if empty dataframe, this means this project has no merge commit with involved refactorings
        # test
        if counter > 1: break

    update_merge_commit_table_with_involved_refactorings(involved_cr_count_per_merge)


def update_merge_commit_table_with_involved_refactorings(involved_cr_count_per_merge):
    print('Final table!')
    print(involved_cr_count_per_merge)

    merge_commits = get_merge_commits()

    updated_merge_commit_involved_table = merge_commits.copy()
    print(updated_merge_commit_involved_table)

    # TODO - add a column is_involved or has_involved_refs to copied dataframe
    # TODO - fillna has_involved_refs with a placeholder value like 0
    # TODO - has_involved_refs values will be determined by involved_cr_count_per_merge value involved_cr_count > 0
    # TODO - if true, update has_involved_refs for corresponding merge commit id to 1

    updated_merge_commit_involved_table['has_involved_refs'] = 0
    print(updated_merge_commit_involved_table)

    merge_commits_grouped_by_merge_commit_id = updated_merge_commit_involved_table.groupby('id')

    for merge_commit_id, involved_cr_count_data in involved_cr_count_per_merge.groupby('merge_commit_id'):
        if merge_commit_id not in merge_commits_grouped_by_merge_commit_id.groups:
            continue

        x = merge_commits_grouped_by_merge_commit_id.get_group(merge_commit_id)
        updated_merge_commit_involved_table.loc[updated_merge_commit_involved_table['id'] == merge_commit_id, 'has_involved_refs'] = 1

    print(updated_merge_commit_involved_table)
    print(updated_merge_commit_involved_table[updated_merge_commit_involved_table['has_involved_refs'] > 0])


if __name__ == '__main__':
    # test()
    get_merge_commits_with_involved_refs_in_crs()
    # values = get_merge_commit_by_crh_and_devs_and_involved_refactorings()
    # print(values)
    # print(values.at[28,'involved_refs'])

    # te = get_conflicting_regions_by_involved_refactorings_per_merge_commit()
    # print(te)

    # print(effect_size_conflicting_region_by_involved_refactoring())

    # print(get_conflicting_regions_by_count_of_involved_refactoring())

    # print_stats()

    # get_merge_commits_with_involved_refs_in_crs()