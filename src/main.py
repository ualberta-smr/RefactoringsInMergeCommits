from joblib import Parallel, delayed
import multiprocessing
from extract_merge import *
from clone_projects import *


if __name__ == '__main__':
    clone_projects()
    num_cores = multiprocessing.cpu_count()
    # num_cores = 1
    reposURLs = open('reposList.txt', 'rt').readlines()
    reposName = [i[i.rfind('/') + 1:] for i in reposURLs]
    Parallel(n_jobs=num_cores)(delayed(extract_merging_data)(reposURLs[i].rstrip(), reposName[i].rstrip()) for i in range(len(reposURLs)))