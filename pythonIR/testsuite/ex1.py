import pandas as pd # vs import pandas as pd
df = pd.read_csv('/home/bhushan/intellijprojects/scirpy_benchmarks/examples/data/data.csv',  parse_dates=['pickup_datetime']) # fetch data
df.memory_usage(deep=True)
print(df.dtypes) # use dataframe
df = df[df.fare_amount > 0] # filter bad rows
df['day'] = df.pickup_datetime.dt.dayofweek # add features
print(df.info())
df = df.groupby(['day'])['passenger_count'].sum() # aggregationgit pgitgit add .
print(df) # use dataframe