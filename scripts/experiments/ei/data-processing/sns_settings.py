#!/usr/bin/env python
# -*- coding: utf-8 -*-
import seaborn as sns
import matplotlib.pyplot as plt

sns.set_style("whitegrid", {'axes.grid' : True})
sns.set_context("paper", font_scale=1.5)

plt.rcParams.update({'axes.edgecolor': 'black', 'axes.linewidth': 2, 
                     'axes.grid': True, 'grid.linestyle': '--'})
colors = ['#2A587A', '#FABC75', '#83B828', '#F83A25', '#FDD8EB']
sns.palplot(colors)
sns.set_palette(sns.color_palette(colors), 8, .75)
sub_figure_title = {"fontweight": 700, 'fontname':'Times New Roman', 'fontsize': 18}
plt.tight_layout()
