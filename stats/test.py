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
    # TODO
    # 1. make a new table with merge_commit_id, cr_count, involved_cr_count, project_id
    # 2. get merge_commits table
    # 3. rename merge_commit_id in invovled table to id

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

    # updated_merge_commit_involved_table = pd.DataFrame()
    print('Final table!')
    print(involved_cr_count_per_merge)

    merge_commits = get_merge_commits()

    updated_merge_commit_involved_table = merge_commits.copy()
    print(updated_merge_commit_involved_table)

    # TODO - add a column is_involved or has_involved_refs to copied dataframe
    # TODO - fillna has_involved_refs with a placeholder value like 0
    # TODO - has_involved_refs values will be determined by involved_cr_count_per_merge value involved_cr_count > 0
    # TODO - if true, update has_involved_refs for corresponding merge commit id to 1

    # # x = tuple, y = actual row
    # for merge_commit_id, merge_commit in merge_commits.groupby('id'):
    #     # print(merge_commit)
    # # matches = involved_cr_count_per_merge[involved_cr_count_per_merge[]]
    #
    #     updated_merge_commit_involved_table.append(merge_commit)
    #
    #     # will take long becase there's too many merge_commits lol.
    #     print(updated_merge_commit_involved_table)


    # involved_merge_commits_table = cr_count_per_merge.join(involved_cr_count_per_merge, how='outer').fillna(0).astype(int)
    # involved_merge_commits_table.groupby('merge_commit_id').merge_commit_id.nunique().to_frame().rename(columns={'merge_commit_id': 'id'})
    # print(involved_merge_commits_table)

    # Filter by merge_commits and then paste into another table
    # merge_commits = get_merge_commits()
    # print(merge_commits)
    # mc_grouped_by_project = merge_commits.groupby('project_id')
    counter = 0
    # for project_id, project_crh in mc_grouped_by_project.groupby('project_id'):
    # for column in merge_commits:
    #     counter += 1
    #     print('Processing project {} from custom function'.format(counter))


        # involved_refs_count = pd.DataFrame(columns={'involved_refs'})
        # if project_id in rr_grouped_by_project.groups:
        #     project_rrs = rr_grouped_by_project.get_group(project_id)
        #     crh_rr_combined = pd.merge(project_crh.reset_index(), project_rrs.reset_index(), on='commit_hash',
        #                                how='inner')
        #     crh_with_involved_refs = crh_rr_combined[crh_rr_combined.apply(record_involved, axis=1)]
        #     involved_refs_count = crh_with_involved_refs.groupby('merge_commit_id').size().to_frame().rename(
        #         columns={0: 'involved_refs'})
        #
        # crh_count = project_crh.groupby('merge_commit_id').commit_hash.nunique().to_frame().rename(
        #     columns={'commit_hash': 'crh'})
        # devs_count = project_crh.groupby('merge_commit_id').author_email.nunique().to_frame().rename(
        #     columns={'author_email': 'devs'})
        #
        # this_project = crh_count.join(devs_count, how='outer').join(involved_refs_count, how='outer').fillna(0).astype(
        #     int)
        # mc_by_crh_and_devs_and_involved_refactorings = mc_by_crh_and_devs_and_involved_refactorings.append(this_project)

    # print(crs)
    # print(crs[crs['involved_cr_count'] > 0])

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