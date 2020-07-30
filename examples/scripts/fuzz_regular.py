import subprocess
import argparse
import os
from multiprocessing import Process

MVN_COMMAND = "mvn jqf:fuzz -Dclass={} -Dmethod={} -Dtarget={} -Dout={} -Dtime={} -DmaxIdentifiers={} -DmaxItems={} -DmaxDepth={} -Dexcludes=kotlin,venus -Djqf.ei.DISABLE_SAVE_NEW_COUNTS"

def run_experiment(experiment_num, class_name, method_name, param_list, runtime, target_dir, save_only_valid, blind):
    param_dir = "".join([str(d) for d in param_list])
    experiment_dir = "exp{}".format(experiment_num)
    save_dir = os.path.join(param_dir, experiment_dir)

    if len(param_list) == 2:
        param_list = [0] + param_list
    command = MVN_COMMAND.format(class_name, method_name, target_dir, save_dir, runtime, *param_list)
    if save_only_valid:
        command += " -Djqf.ei.SAVE_ONLY_VALID"
    if blind:
        command += " -Dblind=true"
    print(command)
    process = subprocess.Popen(command, shell=True)
    process.wait()

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--param_list", required=True, type=str, help="Params for generator")
    parser.add_argument("--target", required=True, type=str, help="one of 'analysis_valid', 'analysis_err', 'codegen'")
    parser.add_argument("--runtime", required=True, type=str, help="Time of JQF fuzzing in minutes for each experiment")
    parser.add_argument("--target_dir", required=True, type=str, help="Output directory of experiments and output list")
    parser.add_argument("--num_experiments", required=True, type=int, help="Number of experiments / processes")
    parser.add_argument("--blind", action="store_true", help="Use blind flag in Zest")
    args = parser.parse_args()

    save_only_valid = False
    class_name, method_name = "", "testWithIterativeGenerator"
    if args.target == "closure":
        class_name = "edu.berkeley.cs.jqf.examples.closure.CompilerTest"
    elif args.target == "rhino":
        class_name = "edu.berkeley.cs.jqf.examples.rhino.CompilerTest"
    elif args.target == "ant":
        class_name = "edu.berkeley.cs.jqf.examples.ant.ProjectBuilderTest"
    elif args.target == "maven":
        class_name = "edu.berkeley.cs.jqf.examples.maven.ModelReaderTest"

    assert class_name != ""

    params = [int(d) for d in args.param_list]
    num_params = len(params)

    procs = []
    for i in range(args.num_experiments):
        p = Process(target=run_experiment,
                    args=(i,
                          class_name,
                          method_name,
                          params,
                          args.runtime,
                          args.target_dir,
                          save_only_valid,
                          args.blind))
        p.start()
        procs.append(p)
    for p in procs:
        p.join()
