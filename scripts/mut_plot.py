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


def bar_plot(data, labels, x_axis, title, xlabel, ylabel):
    assert len(data[x_axis]) > 0, "There must at least be a starting time"
    initial_time = data[x_axis][0]
    times = [t - initial_time for t in data[x_axis]]
    fig, ax = plt.subplots()
    ax.set_title(title)
    ax.set_xlabel(xlabel)
    ax.set_ylabel(ylabel)
    for label in labels:
        ax.plot(times, data[label], label=label)
    ax.legend()
    return fig


def path(k, e, f):
    return '/'.join([e, str(k),
                     "target", "fuzz-results",
                     "chocopy.fuzz.SemanticAnalysisTarget",
                     "differentialTest", f])


def killed(k, e):
    with open(path(k, e)) as f:
        return {s for s in f.readlines() if s.endswith("Killed\n")}


def proportion(k):
    z = killed(k, "zest")
    m = killed(k, "mutation")
    return z - m, z & m, m - z


def draw_venn():
    from matplotlib_venn import venn2
    za = ba = ma = 0
    for i in range(1, 11):
        z, b, m = proportion(i)
        za += len(z)
        ba += len(b)
        ma += len(m)
    za /= 10
    ba /= 10
    ma /= 10
    venn2(subsets=(za, ma, ba), set_labels=('zest', 'μ²'))


def trial_data(k):
    with open(path(k, 'mutation', 'plot_data')) as f:
        return read_csv(f)
