import os
from joblib import Parallel, delayed
import multiprocessing


def get_repos(repo):
    os.system('cd ../projects; git clone ' + repo)


def clone_projects():
    os.system('mkdir ../projects')
    num_cores = multiprocessing.cpu_count()
    repositories = open('reposList.txt', 'rt').readlines()
    for repo in repositories:
        get_repos(repo)
    # Parallel(n_jobs=num_cores)(delayed(get_repos)(i) for i in repositories)


if __name__ == "__main__":
    clone_projects()
