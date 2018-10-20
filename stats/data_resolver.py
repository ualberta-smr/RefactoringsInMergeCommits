import pandas as pd
from sqlalchemy import create_engine
import sys
import re


def get_db_connection():
    username = 'user'
    password = 'password'
    database_name = 'refactoring_analysis'
    server = '127.0.0.1'

    with open("../database.properties", 'r') as db_file:
        for line in db_file:
            line = line.strip()
            username_search = re.search('^development.username=(.*)$', line, re.IGNORECASE)
            password_search = re.search('^development.password=(.*)$', line, re.IGNORECASE)
            url_search = re.search('^development.url=jdbc:mysql://(.*)/(.*)$', line, re.IGNORECASE)

            if username_search:
                username = username_search.group(1)
            if password_search:
                password = password_search.group(1)
            if url_search:
                server = url_search.group(1)
                database_name = url_search.group(2)

    return create_engine('mysql+pymysql://{}:{}@{}/{}'.format(username, password, server, database_name))


def regions_intersect(region_1_start, region_1_length, region_2_start, region_2_length):
    if region_1_start + region_1_length < region_2_start:
        return False
    elif region_2_start + region_2_length < region_1_start:
        return False
    return True


def record_involved(x):
    is_source = (x['type'] == 's' and
                 x['old_path'] == x['path'] and
                 regions_intersect(x['old_start_line'], x['old_length'], x['start_line'], x['length']))
    is_dest = (x['type'] == 'd' and
               x['new_path'] == x['path'] and
               regions_intersect(x['new_start_line'], x['new_length'], x['start_line'], x['length']))
    return is_source or is_dest


accepted_types = ['Change Package', 'Extract And Move Method', 'Extract Interface', 'Extract Method',
                      'Extract Superclass', 'Inline Method', 'Move And Rename Class', 'Move Attribute', 'Move Class',
                      'Move Method', 'Pull Up Attribute', 'Pull Up Method', 'Pull Up Method', 'Push Down Method',
                      'Rename Class', 'Rename Method']


def get_refactoring_types_sql_condition():
    type_condition = str()
    for ref_type in accepted_types:
        type_condition += 'refactoring_type = \"{}\" or '.format(ref_type)
    return type_condition[:-4]


def read_sql_table(table):
    print('Reading table {} from the database'.format(table))
    query = 'SELECT * FROM ' + table
    df = pd.read_sql(query, get_db_connection())
    return df


def get_merge_commits():
    return read_sql_table('merge_commit')


def get_conflicting_regions():
    return read_sql_table('conflicting_region')


def get_conflicting_region_histories():
    return read_sql_table('conflicting_region_history')


def get_refactorings():
    return read_sql_table('refactoring')


def get_accepted_refactorings():
    query = 'select * from refactoring where ({})'.format(get_refactoring_types_sql_condition())
    return pd.read_sql(query, get_db_connection())


def get_refactoring_regions():
    return read_sql_table('refactoring_region')


def get_accepted_refactoring_regions():
    print('Reading table refactoring_region from the database')

    query = 'select * from refactoring_region where refactoring_id in (select id from refactoring where ({}))'\
        .format(get_refactoring_types_sql_condition())
    return pd.read_sql(query, get_db_connection())


def get_conflicting_regions_by_count_of_involved_refactoring():
    conflicting_regions = get_conflicting_regions()
    conflicting_region_histories = get_conflicting_region_histories()
    refactoring_regions = get_accepted_refactoring_regions()

    crs_with_involved_refs = pd.DataFrame()
    rr_grouped_by_project = refactoring_regions.groupby('project_id')
    counter = 0
    for project_id, project_crh in conflicting_region_histories.groupby('project_id'):
        counter += 1
        print('Processing project {}'.format(counter))
        if project_id not in rr_grouped_by_project.groups:
            continue
        project_rrs = rr_grouped_by_project.get_group(project_id)
        crh_rr_combined = pd.merge(project_crh.reset_index(), project_rrs.reset_index(), on='commit_hash', how='inner')
        crh_with_involved_refs = crh_rr_combined[crh_rr_combined.apply(record_involved, axis=1)]
        crs_with_involved_refs = crs_with_involved_refs.append(crh_with_involved_refs.groupby('conflicting_region_id').size().to_frame())

    crs_by_involved_refs = conflicting_regions[['id']]
    # The +2 is because length is actually the difference between the start and end line of the code range. So the
    # actual size of the code range should be incremented by one, for each parent.
    crs_by_involved_refs['size'] = conflicting_regions['parent_1_length'] + conflicting_regions['parent_2_length'] + 2
    crs_by_involved_refs = pd.merge(crs_by_involved_refs, crs_with_involved_refs,
                                    left_on='id', right_on='conflicting_region_id',
                                    how='left').fillna(0).astype(int).rename(columns={0: 'involved_refactorings'})

    return crs_by_involved_refs


def get_conflicting_region_size_by_involved_refactoring_size():
    conflicting_regions = get_conflicting_regions()
    conflicting_region_histories = get_conflicting_region_histories()
    refactoring_regions = get_accepted_refactoring_regions()

    crs_with_involved_refs_size = pd.DataFrame()
    rr_grouped_by_project = refactoring_regions.groupby('project_id')
    counter = 0
    for project_id, project_crh in conflicting_region_histories.groupby('project_id'):
        counter += 1
        print('Processing project {}'.format(counter))
        if project_id not in rr_grouped_by_project.groups:
            continue
        project_rrs = rr_grouped_by_project.get_group(project_id)
        crh_rr_combined = pd.merge(project_crh.reset_index(), project_rrs.reset_index(), on='commit_hash', how='inner')
        crh_with_involved_refs = crh_rr_combined[crh_rr_combined.apply(record_involved, axis=1)]
        crs_with_involved_refs_size = crs_with_involved_refs_size.append(crh_with_involved_refs.groupby('conflicting_region_id').length.sum().to_frame())

    crs_with_involved_refs_size.rename(columns={'length': 'refactoring_size'}, inplace=True)
    crs_by_size = conflicting_regions[['id']]
    # The +2 is because length is actually the difference between the start and end line of the code range. So the
    # actual size of the code range should be incremented by one, for each parent.
    crs_by_size['conflicting_region_size'] = conflicting_regions['parent_1_length'] + conflicting_regions['parent_2_length'] + 2
    crs_by_size.set_index('id', inplace=True)
    crs_size_by_involved_refs_size = crs_by_size.join(crs_with_involved_refs_size,
                                                      how='left').fillna(0).astype(int)

    return crs_size_by_involved_refs_size


def get_conflicting_merge_commit_by_merge_author_involvement_in_conflict():
    merge_commits = get_merge_commits()[['id', 'author_email']].rename(columns={'author_email': 'merge_author_email'})
    conflicting_region_histories = get_conflicting_region_histories()
    refactoring_regions = get_accepted_refactoring_regions()

    mc_by_author_involvement = pd.DataFrame()
    rr_grouped_by_project = refactoring_regions.groupby('project_id')
    counter = 0
    for project_id, project_crh in conflicting_region_histories.groupby('project_id'):
        counter += 1
        print('Processing project {}'.format(counter))

        commits_with_involved_refs = pd.DataFrame(columns={'commit_hash'})
        if project_id in rr_grouped_by_project.groups:
            project_rrs = rr_grouped_by_project.get_group(project_id)
            crh_rr_combined = pd.merge(project_crh.reset_index(), project_rrs.reset_index(), on='commit_hash', how='inner')
            crh_with_involved_refs = crh_rr_combined[crh_rr_combined.apply(record_involved, axis=1)]
            commits_with_involved_refs = pd.DataFrame(crh_with_involved_refs.commit_hash.unique()).rename(columns={0: 'commit_hash'})

        crh_mc = project_crh.merge(merge_commits, how='inner', left_on='merge_commit_id', right_on='id')
        crh_mc_same_author = crh_mc[crh_mc['author_email'] == crh_mc['merge_author_email']]
        crh_mc_same_author_with_involved_ref = crh_mc_same_author.merge(commits_with_involved_refs, how='inner', on='commit_hash')
        crh_mc_with_involved_ref = crh_mc.merge(commits_with_involved_refs, how='inner', on='commit_hash')

        crh_mc_count = crh_mc.groupby('merge_commit_id').commit_hash.nunique().to_frame().rename(columns={'commit_hash': 'total_crh'})
        crh_mc_same_author_count = crh_mc_same_author.groupby('merge_commit_id').commit_hash.nunique().to_frame().rename(columns={'commit_hash': 'crh_merge_author'})
        crh_mc_same_author_with_involved_ref_count = crh_mc_same_author_with_involved_ref.groupby('merge_commit_id').commit_hash.nunique().to_frame().rename(columns={'commit_hash': 'crh_merge_author_involved_ref'})
        crh_mc_with_involved_ref_count = crh_mc_with_involved_ref.groupby('merge_commit_id').commit_hash.nunique().to_frame().rename(columns={'commit_hash': 'crh_involved_ref'})
        crh_mc_involvement = crh_mc_count.join(crh_mc_same_author_count, how='outer').join(crh_mc_with_involved_ref_count, how='outer').join(crh_mc_same_author_with_involved_ref_count, how='outer').fillna(0).astype(int).reset_index()
        crh_mc_involvement['project_id'] = project_id

        mc_by_author_involvement = mc_by_author_involvement.append(crh_mc_involvement)

    return mc_by_author_involvement


def get_involved_refactorings_by_refactoring_type():
    conflicting_region_histories = get_conflicting_region_histories()
    refactorings = get_accepted_refactorings()
    refactoring_regions = get_accepted_refactoring_regions()

    involved_refs_count_per_project = pd.DataFrame()
    rr_grouped_by_project = refactoring_regions.groupby('project_id')
    counter = 0
    for project_id, project_crh in conflicting_region_histories.groupby('project_id'):
        counter += 1
        print('Processing project {}'.format(counter))
        if project_id not in rr_grouped_by_project.groups:
            continue
        project_rrs = rr_grouped_by_project.get_group(project_id)
        crh_rr_combined = pd.merge(project_crh.reset_index(), project_rrs.reset_index(), on='commit_hash', how='inner')
        involved_crh_rr = crh_rr_combined[crh_rr_combined.apply(record_involved, axis=1)]
        involved_refactorings = refactorings[refactorings['id'].isin(involved_crh_rr['refactoring_id'])]
        involved_ref_per_type = involved_refactorings.groupby('refactoring_type').id.nunique().to_frame().rename(
            columns={'id': 'involved_refs_count'})

        # for refactoring_index in involved_ref_per_type.index:
        #     if refactoring_index not in accepted_types:
        #         involved_ref_per_type = involved_ref_per_type.drop(refactoring_index)

        involved_ref_per_type['involved_refs_count'] = involved_ref_per_type['involved_refs_count'] / sum(
            involved_ref_per_type['involved_refs_count'])
        involved_ref_per_type.rename(columns={'involved_refs_count': str(project_id)}, inplace=True)
        involved_refs_count_per_project = involved_refs_count_per_project.append(involved_ref_per_type.T)

    return involved_refs_count_per_project.T


def get_refactorings_by_refactoring_type():
    refactorings = get_accepted_refactorings()

    refactorings_count_per_project = pd.DataFrame()
    counter = 0
    for project_id, project_refactorings in refactorings.groupby('project_id'):
        counter += 1
        print('Processing project {}'.format(counter))

        refactorings_per_type = project_refactorings.groupby('refactoring_type').id.nunique().to_frame().rename(
            columns={'id': 'refs_count'})

        # for refactoring_index in refactorings_per_type.index:
        #     if refactoring_index not in accepted_types:
        #         refactorings_per_type = refactorings_per_type.drop(refactoring_index)

        refactorings_per_type['refs_count'] = refactorings_per_type['refs_count'] / sum(
            refactorings_per_type['refs_count'])
        refactorings_per_type.rename(columns={'refs_count': str(project_id)}, inplace=True)
        refactorings_count_per_project = refactorings_count_per_project.append(refactorings_per_type.T)

    return refactorings_count_per_project.T


def get_refactorings_by_refactoring_type_split_by_involved():
    all_refs = get_data_frame('refactorings_by_refactoring_type').fillna(0).T
    involved_refs = get_data_frame('involved_refactorings_by_refactoring_type').fillna(0).T

    plot_df = pd.DataFrame(columns=['project_id', 'refactoring_type', 'percent', 'overall_or_involved'])
    for project_id in all_refs.index:
        for refactoring_type in all_refs.loc[project_id].index:
            overall_percent = all_refs.loc[project_id].loc[refactoring_type]
            plot_df = plot_df.append({'project_id': project_id, 'refactoring_type': refactoring_type,
                                      'percent': overall_percent, 'overall_or_involved': 'overall'},
                                     ignore_index=True)
            try:
                involved_percent = involved_refs.loc[project_id].loc[refactoring_type]
            except KeyError:
                involved_percent = 0.

            plot_df = plot_df.append({'project_id': project_id, 'refactoring_type': refactoring_type,
                                      'percent': involved_percent, 'overall_or_involved': 'involved'},
                                     ignore_index=True)
    return plot_df


def get_conflicting_regions_by_involved_refactorings_per_merge_commit():
    conflicting_region_histories = get_conflicting_region_histories()
    refactoring_regions = get_accepted_refactoring_regions()

    cr_count_per_merge = conflicting_region_histories.groupby('merge_commit_id').conflicting_region_id.nunique().to_frame().rename(columns={'conflicting_region_id': 'cr_count'})

    involved_cr_count_per_merge = pd.DataFrame()
    rr_grouped_by_project = refactoring_regions.groupby('project_id')
    counter = 0
    for project_id, project_crh in conflicting_region_histories.groupby('project_id'):
        counter += 1
        print('Processing project {}'.format(counter))
        if project_id not in rr_grouped_by_project.groups:
            continue
        project_rrs = rr_grouped_by_project.get_group(project_id)
        crh_rr_combined = pd.merge(project_crh.reset_index(), project_rrs.reset_index(), on='commit_hash', how='inner')
        involved = crh_rr_combined[crh_rr_combined.apply(record_involved, axis=1)]
        involved_cr_count_per_merge = involved_cr_count_per_merge.append(involved.groupby('merge_commit_id').conflicting_region_id.nunique().to_frame().rename(columns={'conflicting_region_id': 'involved_cr_count'}))

    rq1_table = cr_count_per_merge.join(involved_cr_count_per_merge, how='outer').fillna(0).astype(int)
    rq1_table['percent'] = rq1_table['involved_cr_count'] / rq1_table['cr_count']
    return rq1_table


def get_merge_commit_by_crh_and_devs_and_involved_refactorings():
    conflicting_region_histories = get_conflicting_region_histories()
    refactoring_regions = get_accepted_refactoring_regions()

    mc_by_crh_and_devs_and_involved_refactorings = pd.DataFrame()
    rr_grouped_by_project = refactoring_regions.groupby('project_id')
    counter = 0
    for project_id, project_crh in conflicting_region_histories.groupby('project_id'):
        counter += 1
        print('Processing project {}'.format(counter))

        involved_refs_count = pd.DataFrame(columns={'involved_refs'})
        if project_id in rr_grouped_by_project.groups:
            project_rrs = rr_grouped_by_project.get_group(project_id)
            crh_rr_combined = pd.merge(project_crh.reset_index(), project_rrs.reset_index(), on='commit_hash',
                                       how='inner')
            crh_with_involved_refs = crh_rr_combined[crh_rr_combined.apply(record_involved, axis=1)]
            involved_refs_count = crh_with_involved_refs.groupby('merge_commit_id').size().to_frame().rename(columns={0: 'involved_refs'})

        crh_count = project_crh.groupby('merge_commit_id').commit_hash.nunique().to_frame().rename(columns={'commit_hash': 'crh'})
        devs_count = project_crh.groupby('merge_commit_id').author_email.nunique().to_frame().rename(columns={'author_email': 'devs'})

        this_project = crh_count.join(devs_count, how='outer').join(involved_refs_count, how='outer').fillna(0).astype(int)
        mc_by_crh_and_devs_and_involved_refactorings = mc_by_crh_and_devs_and_involved_refactorings.append(this_project)

    return mc_by_crh_and_devs_and_involved_refactorings


def get_data_frame(df_name):
    try:
        return pd.read_pickle(df_name + '.pickle')
    except FileNotFoundError:
        df = getattr(sys.modules[__name__], 'get_' + df_name)()
        df.to_pickle(df_name + '.pickle')
        return df


def to_csv():
    cr_size_by_ir_size = get_conflicting_region_size_by_involved_refactoring_size()
    cr_by_ir_count = get_conflicting_regions_by_count_of_involved_refactoring()

    cr_size_by_ir_size.rename(columns={'refactoring_size': 'RefactoringSize'}, inplace=True)
    cr_size_by_ir_size.rename(columns={'conflicting_region_size': 'ConflictSize'}, inplace=True)
    cr_size_by_ir_size = cr_size_by_ir_size[['RefactoringSize', 'ConflictSize']]
    cr_size_by_ir_size.to_csv(path_or_buf="RefactoringSizeConflictSize.csv", index=False)

    cr_by_ir_count['InvolvedRefactoring'] = cr_by_ir_count['involved_refactorings'] > 0
    cr_by_ir_count.rename(columns={'size': 'ConflictSize'}, inplace=True)
    cr_by_ir_count[['InvolvedRefactoring', 'ConflictSize']].to_csv(path_or_buf="ConflictSize.csv", index=False)


def print_stats():
    df = get_data_frame('conflicting_regions_by_involved_refactorings_per_merge_commit')
    print("Involved Merge Scenarios: " + str(df[df['involved_cr_count'] > 0].shape[0]))
    print("Involved Conflicting Regions: " + str(df['involved_cr_count'].sum()))
    print(('-' * 80) + '\nGeneral Stats Table')
    print_general_stats()
    print(('-' * 80) + '\nGit Conflicts Table')
    print_git_conflict_stats()


def print_general_stats():
    con = get_db_connection()
    stat_list = list()
    stat_list.append(('All Merge Scenarios', pd.read_sql_query('select count(*) from merge_commit group by project_id', con)))
    stat_list.append(('Conflicting Mer Ss', pd.read_sql_query('select count(*) from merge_commit where is_conflicting = 1 group by project_id', con)))
    stat_list.append(('CMS w/ Java Conf', pd.read_sql_query('select count(*) from merge_commit where id in (select merge_commit_id from conflicting_java_file) group by project_id', con)))
    stat_list.append(('Conflicting Region', pd.read_sql_query('select count(*) from conflicting_region group by project_id', con)))
    stat_list.append(('Evolutionary Cmt', pd.read_sql_query('select count(*) from conflicting_region_history group by project_id', con)))
    stat_list.append(('Refactoring in ECmt', pd.read_sql_query('select count(*) from refactoring where ({}) group by project_id'.
                                       format(get_refactoring_types_sql_condition()), con)))

    layout_str = '{}\t|{}\t|\t{}\t|\t{} | {}'
    print(layout_str.format('Stat', 'Total', 'Corresponding Repos', 'Mean', 'SD'))
    for (title, stat) in stat_list:
        print(layout_str.format(title, stat.sum().iloc[0], stat.size, stat.mean().iloc[0], stat.std().iloc[0]))


def print_git_conflict_stats():
    con = get_db_connection()
    conflict_types = pd.read_sql_query('select type as conflict_type, count(*) from conflicting_java_file group by type order by count(*) desc', con)

    stat_list = list()
    for conflict_type in conflict_types['conflict_type']:
        stat_list.append(pd.read_sql_query('select count(*) from conflicting_java_file where type = "{}" group by project_id'.format(conflict_type), con))

    layout_str = '{}\t|\t{}\t|\t{}\t|\t{} | {}'
    print(layout_str.format('Type', 'Total', 'Corresponding Repos', 'Mean', 'SD'))
    for i in range(len(conflict_types)):
        conflict_type = conflict_types['conflict_type'][i]
        stat = stat_list[i]
        print(layout_str.format(conflict_type, stat.sum().iloc[0], stat.size, stat.mean().iloc[0], stat.std().iloc[0]))


if __name__ == '__main__':
    print_stats()
