import os
import json
import csv


def get_commit_date(projectName, commit):
    return os.popen('cd ./../projects/' + projectName + ';' + 'git show -s --format=%ct ' + commit).read().rstrip()


def get_parents(project_name, commit):
    goToProjectDir = 'cd ./../projects/' + project_name + ';'
    return os.popen(goToProjectDir + 'git log --pretty=%P -n 1 ' + commit).read().split()


def get_merge_commits(project_name):
    cList = os.popen('cd ./../projects/' + project_name + ';' + 'git log --all --format="%H"').readlines()
    commitsList = []
    for ind, item in enumerate(cList):
        commitsList.append(item.split()[0])
    return [commit for commit in commitsList if len(get_parents(project_name, commit)) > 1]


def get_common_ancestor_commit(project_name, commits):
    goToProjectDir = 'cd ./../projects/' + project_name + ';'
    return os.popen(goToProjectDir + 'git merge-base ' + ' '.join(commits)).read().rstrip()


def is_merge_commit_conflicting(project_name, merge_commit_hash):
    merge_parents = get_parents(project_name, merge_commit_hash)
    is_conflicting = 'automatic merge failed' in os.popen('cd ./../projects/' + project_name + ';' +
                                                              'git checkout {}; '.format(merge_parents[0]) +
                                                              'git merge ' + ' '.join(merge_parents[1:])).read().rstrip().lower()
    os.popen('cd ./../projects/' + project_name + '; git reset --hard')
    return is_conflicting


def get_refactoring_changes(project_name, start_commit, end_commit):
    os.popen('./../RefactoringMiner/bin/RefactoringMiner -bc ./../projects/{} {} {}'.format(project_name, start_commit, end_commit)).read()
    file_name = './../projects/{}/refactorings_{}_{}.csv'.format(project_name, start_commit, end_commit)

    if not os.path.exists(file_name):
        return list()

    with open(file_name) as refactorings_file:
        content = csv.reader(refactorings_file, delimiter=';')
        return [row for row in content][1:]


def get_repos_info(url):
    jsonData = json.loads(os.popen('curl https://api.github.com/repos/' + url).read())
    return jsonData['id'], jsonData['full_name'], jsonData['description'], jsonData['language'], jsonData['subscribers_count'], \
            jsonData['stargazers_count'], jsonData['forks'], jsonData['open_issues'], jsonData['size']
