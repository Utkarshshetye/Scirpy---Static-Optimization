import csv
import numpy as np

n_rows = 1_000_000
n_cols = 1000
# field names

n_rows = 1_000_000
n_cols = 1000
# name of csv file
filename = "/home/bhushan/intellijprojects/scirpy/pythonIR/examplesCode/largeFile/csv_files/18GB.csv"

# writing to csv file
with open(filename, 'w') as csvfile:
    csvwriter = csv.writer(csvfile)
    fields = ['col%d' % i for i in range(n_cols)]
    csvwriter.writerow(fields)
    for i in range(1, n_rows):
        row= (np.random.uniform(0, 100, size=(n_cols)) )
        csvwriter.writerow(row)
