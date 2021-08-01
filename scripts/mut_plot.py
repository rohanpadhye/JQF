import matplotlib.pyplot as plt


def read_value(v):
    return float(v.rstrip("%")) / 100 if v.endswith("%") else float(v)


def skip_comments(csv_file):
    first_two = csv_file.read(2)
    if first_two != "# ":
        csv_file.seek(0)


def read_csv(in_file):
    import csv
    skip_comments(in_file)
    dr = csv.DictReader(in_file)
    fields = {field.strip(): [] for field in dr.fieldnames}
    for row in dr:
        for k, v in row.items():
            fields[k.strip()].append(read_value(v))
    return fields


def plot_data(data, labels, x_axis):
    assert len(data[x_axis]) > 0, "There must at least be a starting time"
    initial_time = data[x_axis][0]
    times = [t - initial_time for t in data[x_axis]]
    fig, ax = plt.subplots()
    ax.set_title(', '.join(labels) + " vs " + x_axis)
    ax.set_xlabel(x_axis)
    for label in labels:
        ax.plot(times, data[label], label=label)
    ax.legend()
    return fig


def plot_csv(in_file, labels, x_axis, out_file):
    data = read_csv(in_file)
    plot = plot_data(data, labels, x_axis)
    plot.savefig(out_file)


if __name__ == "__main__":
    from argparse import ArgumentParser
    p = ArgumentParser()
    p.add_argument("input_file",
                   help="the CSV input file",
                   type=open)
    p.add_argument("-o", "--output",
                   help="the output file path",
                   type=str,
                   required=False, default="./plot.png")
    p.add_argument("-l", "--labels",
                   help="the labels that need to be plotted",
                   nargs="+",
                   type=str)
    p.add_argument("-x", "--x_axis",
                   help="The label to be used as the x axis",
                   type=str,
                   required=False, default="unix_time")
    a = p.parse_args()
    plot_csv(a.input_file, set(a.labels), a.x_axis, a.output)
    a.input_file.close()
