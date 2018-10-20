import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns
import matplotlib.ticker as ticker


from data_resolver import get_data_frame


def plot_conflicting_region_by_involved_refactoring():
    crs_by_involved_refactoring = get_data_frame('conflicting_regions_by_count_of_involved_refactoring')
    cr_size_with_involved = crs_by_involved_refactoring[crs_by_involved_refactoring['involved_refactorings'] > 0]['size'].tolist()
    cr_size_without_involved = crs_by_involved_refactoring[crs_by_involved_refactoring['involved_refactorings'] == 0]['size'].tolist()

    fig, ax = plt.subplots()
    ax.boxplot([cr_size_with_involved, cr_size_without_involved], showmeans=True)

    ax.set_xticklabels(['With Involved Refactorings', 'Without Involved Refactorings'])

    ax.set_ylabel("Conflicting Region Size")
    ax.set_yscale("log")
    ax.yaxis.grid(True)

    fig.tight_layout()

    # plt.subplots_adjust(left=.15, right=.97)
    plt.savefig('conflicting_region_by_involved_refactoring.pdf')
    plt.show()


def plot_number_of_conflicting_region_histories_by_involved_per_merge_commit():
    df = get_data_frame('merge_commit_by_crh_and_devs_and_involved_refactorings')

    crh_involved_count = df[df['involved_refs'] > 0]['crh'].tolist()
    crh_not_involved_count = df[df['involved_refs'] == 0]['crh'].tolist()

    fig, ax = plt.subplots()
    ax.boxplot([crh_involved_count, crh_not_involved_count], showmeans=True)

    ax.set_xticklabels(['Conflicting Merge Scenarios\nwith Involved Refactorings',
                        'Conflicting Merge Scenarios\nwithout Involved Refactorings'])
    ax.set_ylabel("Number of Evolutionary Commits")
    ax.set_yscale("log")
    ax.yaxis.grid(True)

    fig.tight_layout()

    # plt.subplots_adjust(left=.15, right=.97)
    plt.savefig('number_of_conflicting_region_histories_by_involved_per_merge_commit.pdf')
    plt.show()


def plot_number_of_devs_by_involved_per_merge_commit():
    df = get_data_frame('merge_commit_by_crh_and_devs_and_involved_refactorings')

    devs_involved_count = df[df['involved_refs'] > 0]['devs'].tolist()
    devs_not_involved_count = df[df['involved_refs'] == 0]['devs'].tolist()

    fig, ax = plt.subplots()
    ax.boxplot([devs_involved_count, devs_not_involved_count], showmeans=True)

    ax.set_xticklabels(['Conflicting Merge Scenarios\nwith Involved Refactorings',
                        'Conflicting Merge Scenarios\nwithout Involved Refactorings'])

    ax.set_ylabel("Number of Developers")
    # ax.set_yscale("log")
    ax.yaxis.grid(True)

    fig.tight_layout()

    # plt.subplots_adjust(left=.15, right=.97)
    plt.savefig('number_of_devs_by_involved_per_merge_commit.pdf')
    plt.show()


def plot_conflicting_region_size_by_involved_refactoring_size():
    crs_size_by_involved_refs_size = get_data_frame('conflicting_region_size_by_involved_refactoring_size')
    crs_size_by_involved_refs_size = crs_size_by_involved_refs_size[crs_size_by_involved_refs_size['refactoring_size'] > 0]

    plot_df = crs_size_by_involved_refs_size.groupby(['conflicting_region_size', 'refactoring_size']).size().reset_index().rename(columns={0: 'frequency'})
    b, m = np.polynomial.polynomial.polyfit(crs_size_by_involved_refs_size['conflicting_region_size'], crs_size_by_involved_refs_size['refactoring_size'], 1)

    fig, ax = plt.subplots()
    ax.scatter(x=plot_df['conflicting_region_size'], y=plot_df['refactoring_size'], s=plot_df['frequency'], alpha=.75)

    ax.set_xlabel("Conflicting Region Size")
    ax.set_ylabel("Involved Refactoring Size")
    # ax.set_xscale("log")
    # ax.set_yscale("log")

    X_plot = np.array([ax.get_xlim()[0], ax.get_xlim()[1]])
    ax.plot(X_plot, b + X_plot * m, '-')

    fig.tight_layout()

    # plt.subplots_adjust(left=.15, right=.97)
    plt.savefig('conflicting_region_size_by_involved_refactoring_size.pdf')
    plt.show()


def plot_conflicting_merge_commit_by_merge_author_involvement_in_conflict():
    mg_by_author = get_data_frame('conflicting_merge_commit_by_merge_author_involvement_in_conflict')
    mg_by_author['percent_same_author'] = mg_by_author['crh_merge_author'] / mg_by_author['total_crh']
    mg_by_author['percent_commits_with_ref'] = mg_by_author['crh_involved_ref'] / mg_by_author['total_crh']

    percent_commits_with_ref_involved_author = mg_by_author[(mg_by_author.percent_same_author > 0)]['percent_commits_with_ref'].tolist()
    percent_commits_with_ref_noninvolved_author = mg_by_author[(mg_by_author.percent_same_author == 0)]['percent_commits_with_ref'].tolist()

    mg_involved_author_by_author_ref = mg_by_author[(mg_by_author.percent_same_author > 0) & (mg_by_author.crh_merge_author_involved_ref > 0)]['percent_same_author'].tolist()
    mg_involved_author_by_author_no_ref = mg_by_author[(mg_by_author.percent_same_author > 0) & (mg_by_author.crh_merge_author_involved_ref == 0)]['percent_same_author'].tolist()

    # cr_size_with_involved = np.random.choice(cr_size_with_involved, 10000)
    # cr_size_without_involved = np.random.choice(cr_size_without_involved, 10000)

    fig, ax = plt.subplots()
    # axs[0].boxplot([percent_commits_with_ref_involved_author, percent_commits_with_ref_noninvolved_author], showmeans=True)
    ax.boxplot([mg_involved_author_by_author_ref, mg_involved_author_by_author_no_ref], showmeans=True)

    # axs[0].set_xticklabels(['Involved Merger', 'Non-involved Merger'])
    ax.set_xticklabels(['With Involved Refactorings', 'Without Involved Refactorings'])

    # axs[0].set_xlabel('All Merge Commits')
    # ax.set_xlabel('Merge Commits with Involved Merger')

    # axs[0].set_ylabel("Percentage of Evolutionary Commits with Involved Refactoring")
    ax.set_ylabel("Percentage of Evolutionary Commits by the Merger ")
    # ax.set_yscale("log")
    # axs[0].yaxis.grid(True)
    ax.yaxis.grid(True)

    fig.tight_layout()

    # plt.subplots_adjust(left=.15, right=.97)
    plt.savefig('conflicting_merge_commit_by_merge_author_involvement_in_conflict.pdf')
    plt.show()


def plot_refactorings_by_refactoring_type():
    plot_df = get_data_frame('refactorings_by_refactoring_type_split_by_involved').replace('overall', 'Overall').replace('involved', "Involved")

    fig, ax = plt.subplots(figsize=(15, 6))
    sns.violinplot(x="refactoring_type", y="percent", hue="overall_or_involved", data=plot_df,
                   palette="muted", cut=0, ax=ax)

    # ax.yaxis.grid(True)
    ax.yaxis.set_major_formatter(ticker.PercentFormatter(xmax=1))
    ax.set_xlabel("")
    ax.set_ylabel("Percentage of Refactorings with Corresponding Type")
    ax.legend().set_title('')
    plt.setp(ax.xaxis.get_majorticklabels(), rotation=-45, ha="left", rotation_mode="anchor")

    fig.tight_layout()
    plt.savefig('refactorings_by_refactoring_type.pdf')
    plt.show()


def plot_conflicting_regions_by_involved_refactorings_per_merge_commit():
    plot_df = get_data_frame('conflicting_regions_by_involved_refactorings_per_merge_commit')
    plot_df = plot_df.groupby(['cr_count', 'involved_cr_count']).size().reset_index().rename(columns={0: 'frequency'})

    fig, ax = plt.subplots(figsize=(6, 4.7))

    size_scale = lambda x : x / 10 + 1
    plot_df['involved_cr_count'] = plot_df['involved_cr_count'].replace(0, .5)
    # sns.scatterplot(x="cr_count", y="involved_cr_count", size="frequency", data=plot_df, ax=ax, sizes=(10, 1000))
    ax.scatter(x=plot_df['cr_count'], y=plot_df['involved_cr_count'], s=size_scale(plot_df['frequency']), alpha=.8)

    # Identity line
    x_plot = np.array([10**x for x in range(4)])
    ax.plot(x_plot, x_plot, '-', c='k', alpha=.3)

    ax.set_xlabel("Conflicting Regions")
    ax.set_ylabel("Conflicting Regions with Involved Refactorings")

    # ax.yaxis.set_label_coords(-.1,5)
    ax.set_xscale("log")
    ax.set_yscale("log")

    ax.set_yticks([.5] + [10**x for x in range(0, 5)])
    ax.set_yticklabels([0] + [10**x for x in range(0, 5)])
    ax.grid(True)

    legend_markers = [plt.scatter(x=[], y=[], s=size_scale(1), c='grey', alpha=.8),
                      plt.scatter(x=[], y=[], s=size_scale(1000), c='grey', alpha=.8),
                      plt.scatter(x=[], y=[], s=size_scale(10000), c='grey', alpha=.8)]
    labels = ['1 Merge Scenario', '1,000 Merge Scenarios', '10,000 Merge Scenarios']
    ax.legend(handles=legend_markers, labels=labels, frameon=True, borderpad=1.2, labelspacing=1.5)

    fig.tight_layout()

    # plt.subplots_adjust(left=.15, right=.97)
    plt.savefig('conflicting_regions_by_involved_refactorings_per_merge_commit.pdf')
    plt.show()


plot_functions = [x for x in dir() if x[:5] == 'plot_']
print('Options available:')
for i in range(len(plot_functions)):
    print(str(i + 1) + '. ' + plot_functions[i])
print(str(len(plot_functions) + 1) + '. Exit')
while True:
    inp = int(input('Choose an option: '))
    if inp < 1 or inp > len(plot_functions):
        break
    locals()[plot_functions[inp - 1]]()
