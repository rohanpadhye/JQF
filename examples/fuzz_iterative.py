import os
import subprocess
import argparse
from multiprocessing import Process
from collections import defaultdict
from shutil import copy2

MVN_COMMAND = "mvn jqf:fuzz -Dclass={} -Dmethod={} -Dtarget={} -Dout={} -Dtime={} -DmaxIdentifiers={} -DmaxItems={} -DmaxDepth={} -Djqf.ei.DISABLE_SAVE_NEW_COUNTS=true"

def add_parent(parent, graph, bound, visited, depth, depth_map):
    if tuple(parent) in visited:
        return
    for i in range(len(parent)):
        child = parent[:]
        if child[i] == bound:
            continue
        child[i] += 1
        graph[tuple(child)].append(parent)
        add_parent(child, graph, bound, visited, depth + 1, depth_map)
    depth_map[depth].append(parent)
    visited.add(tuple(parent))

def create_graph(bound, num_params):
    graph = defaultdict(list)
    depth_map = defaultdict(list)
    visited = set()
    root = [1] * num_params
    add_parent(root, graph, bound, visited, 0, depth_map)
    return graph, depth_map

def join_seed_dirs(target_dir, seed_dirs):
    seed_dir_names = [''.join([str(p) for p in d]) for d in seed_dirs]
    new_seed_dir = '_'.join(seed_dir_names) + "_seed"
    full_new_seed_dir = os.path.join(target_dir, new_seed_dir)
    if not os.path.exists(full_new_seed_dir):
        os.mkdir(full_new_seed_dir)
    files = []
    for seed_dir in seed_dir_names:
        full_seed_dir = os.path.join(target_dir, seed_dir + "/corpus/")
        filenames = os.listdir(full_seed_dir)
        files += [os.path.join(full_seed_dir, f) for f in filenames]
    files.sort(key=lambda f: os.stat(f).st_size)
    counter = 0
    for f in files:
        new_file_name = 'id_' + str(counter).zfill(6)
        copy2(f, os.path.join(full_new_seed_dir, new_file_name))
        counter += 1
    return full_new_seed_dir


def run_experiment(class_name, method_name, param_list, runtime, target_dir, seed_dirs, save_only_valid):
    if len(param_list) == 2:
        experiment_dir = "{}{}".format(*param_list)
        # Dummy for identifier param
        param_list = [0] + param_list
    else:
        experiment_dir = "{}{}{}".format(*param_list)
    command = MVN_COMMAND.format(class_name, method_name, target_dir, experiment_dir, runtime, *param_list)
    if save_only_valid:
        command += " -Djqf.ei.SAVE_ONLY_VALID=true"
    if len(seed_dirs) > 0:
        full_seed_dir = join_seed_dirs(target_dir, seed_dirs)
        command += " -Din={}".format(full_seed_dir)
    print(command)
    process = subprocess.Popen(command, shell=True)
    process.wait()

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--runtime", required=True, type=str, help="Time of JQF fuzzing in minutes for each experiment")
    parser.add_argument("--target", required=True, type=str, help="one of 'analysis_valid', 'analysis_err', 'codegen'")
    parser.add_argument("--max_bound", required=True, type=int, help="Maximum bound for all params")
    parser.add_argument("--target_dir", required=True, type=str, help="Output directory of experiments")
    parser.add_argument("--num_experiments", required=True, type=str, help="Output directory of experiments")
    args = parser.parse_args()

    save_only_valid = False
    num_params = 3
    class_name, method_name = "", "testWithIterativeGenerator"
    if args.target == "closure":
        class_name = "edu.berkeley.cs.jqf.examples.closure.CompilerTest"
    elif args.target == "rhino":
        class_name = "edu.berkeley.cs.jqf.examples.rhino.CompilerTest"
    elif args.target == "ant":
        class_name = "edu.berkeley.cs.jqf.examples.ant.ProjectBuilderTest"
        num_params = 2
    elif args.target == "maven":
        class_name = "edu.berkeley.cs.jqf.examples.maven.ModelReaderTest"
        num_params = 2

    assert class_name != "", "Target must be one of 'analysis_valid', 'analysis_err', 'interpreter'"

    graph, depth_map = create_graph(args.max_bound, num_params)
    for i in range(len(depth_map)):
        params = depth_map[i]
        print(params)
        procs = []
        for param_list in params:
            p = Process(target=run_experiment, args=(class_name,
                                                     method_name,
                                                     param_list,
                                                     args.runtime,
                                                     args.target_dir,
                                                     graph[tuple(param_list)],
                                                     save_only_valid))
            p.start()
            procs.append(p)
        for p in procs:
            p.join()


