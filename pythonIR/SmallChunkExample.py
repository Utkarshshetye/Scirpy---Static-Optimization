#source
#https://github.com/wblakecannon/DataCamp/blob/master/06-python-data-science-toolbox-(part-2)/1-using-iterators-in-pythonland/processing-large-amounts-of-twitter-data.py
import pandas as pd

# Initialize an empty dictionary: counts_dict
counts_dict = {}

# Iterate over the file chunk by chunk
for chunk in pd.read_csv('/home/bhushan/projects/scirpy/pythonIR/data/tweets.csv', chunksize=10):

    # Iterate over the column in DataFrame
    for entry in chunk['lang']:
        if entry in counts_dict.keys():
            counts_dict[entry] += 1
        else:
            counts_dict[entry] = 1

# Print the populated dictionary
print(counts_dict)

# Iterate over the whole file
counts_dict2={}
df=pd.read_csv('/home/bhushan/projects/scirpy/pythonIR/data/tweets.csv')
for entry in df['lang']:
    if entry in counts_dict2.keys():
        counts_dict2[entry] += 1
    else:
        counts_dict2[entry] = 1
print(counts_dict2)

#print(df.groupby('lang').agg('count'))