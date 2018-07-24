import os
import json
import csv
import re


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


def get_java_file_package(project_name, path):
    with open('./../projects/' + project_name + '/ ' + path, 'r') as file:
        package_pattern = re.compile('^package[\s.]*(\S+)\s*;$')
        for line in file:
            line = line.rstrip('\n').strip()
            if package_pattern.match(line):
                return re.search(package_pattern, line).group(1)
    return None


def get_merge_data(project_name, merge_commit_hash):
    merge_parents = get_parents(project_name, merge_commit_hash)
    merge_output = os.popen('cd ./../projects/' + project_name + ';' +
                            'git checkout {}; '.format(merge_parents[0]) +
                            'git merge ' + ' '.join(merge_parents[1:])).read().rstrip().lower()
    conflicts = list()
    content_conflict_pattern = re.compile('^CONFLICT \((.+)\):.* \s(\S +)$')
    file_conflict_pattern = re.compile('^CONFLICT \((.+)\):\s(\S+).+$')

    for output_line in merge_output.split('\n'):
        if output_line.startswith('CONFLICT'):

            if content_conflict_pattern.match(output_line):
                search_result = re.search(content_conflict_pattern, output_line)
                conflict_type, path = search_result.group(1), search_result.group(2)
                conflicts.append((conflict_type, path, get_java_file_package(project_name, path)))

            elif file_conflict_pattern.match(output_line):
                search_result = re.search(file_conflict_pattern, output_line)
                conflict_type, path = search_result.group(1), search_result.group(2)
                conflicts.append((conflict_type, path, get_java_file_package(project_name, path)))
            else:
                print("Unknown conflict: " + output_line)

    is_conflicting = 'automatic merge failed' in merge_output
    os.popen('cd ./../projects/' + project_name + '; git reset --hard')
    return is_conflicting, conflicts


def parse_refactoring_details(refactoring_type, refactoring_details):
    binary_patterns = {'Rename Class': 'Rename Class\s(.+) renamed to (.+)',
                       'Change Package': 'Change Package\s(.+) to (.+)',
                       'Move Class': 'Move Class\s(.+) moved to (.+)',
                       'Move Method': 'Move Method\s.+ from class (.+) to .+ from class (.+)',
                       'Pull Up Method': 'Pull Up Method\s.+ from class (.+) to (.+) from class (.+)',
                       'Push Down Method': 'Push Down Method\s.+ from class (.+) to (.+) from class (.+)',
                       'Move Attribute': 'Move Attribute\s.+ from class (.+) to class (.+)',
                       'Pull Up Attribute': 'Pull Up Attribute\s.+ from class (.+) to class (.+)',
                       'Push Down Attribute': 'Push Down Attribute\s.+ from class (.+) to class (.+)',
                       'Move And Rename Class': 'Move And Rename Class\s(.+) moved and renamed to (.+)',
                       'Extract And Move Method': 'Extract And Move Method\s.+ extracted from .+ in class (.+) & moved to class (.+)',
                       'Convert Anonymous Class to Type': 'Convert Anonymous Class to Type\s(.+) was converted to (.+)',
                       }
    unary_patterns = {
        'Inline Method': 'Inline Method\s.+ inlined to .+ in class (.+)',
        'Rename Method': 'Rename Method\s.+ renamed to .+ in class (.+)',
        'Extract Method': 'Extract Method\s.+ extracted from .+ in class (.+)',
        'Extract Interface': 'Extract Interface\s.+ from classes (.+)',
        'Extract Superclass': 'Extract Superclass\s.+ from classes (.+)',
    }

    if refactoring_type in binary_patterns:
        source_class, destination_class = re.search(binary_patterns[refactoring_type], refactoring_details).groups()
    elif refactoring_type in unary_patterns:
        source_class = re.search(unary_patterns[refactoring_type], refactoring_details).group(1)
        destination_class = source_class

    return source_class, destination_class


def get_refactoring_changes(project_name, start_commit, end_commit):
    os.popen('./../RefactoringMiner/bin/RefactoringMiner -bc ./../projects/{} {} {}'.format(project_name, start_commit,
                                                                                            end_commit)).read()
    file_name = './../projects/{}/refactorings_{}_{}.csv'.format(project_name, start_commit, end_commit)

    if not os.path.exists(file_name):
        return list()

    with open(file_name) as refactorings_file:
        content = csv.reader(refactorings_file, delimiter=';')
        return [row + parse_refactoring_details(row[1], row[2]) for row in content][1:]


def get_repos_info(url):
    jsonData = json.loads(os.popen('curl https://api.github.com/repos/' + url).read())
    return jsonData['id'], jsonData['full_name'], jsonData['description'], jsonData['language'], jsonData[
        'subscribers_count'], \
           jsonData['stargazers_count'], jsonData['forks'], jsonData['open_issues'], jsonData['size']
