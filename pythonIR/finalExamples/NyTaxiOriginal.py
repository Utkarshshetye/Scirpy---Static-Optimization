import pandas as pd # vs import pandas as pd
df = pd.read_csv('/home/bhushan/Downloads/data.csv',  parse_dates=['pickup_datetime']) # fetch data
print(df.dtypes) # use dataframe
df = df[df.fare_amount > 0] # filter bad rows
df['day'] = df.pickup_datetime.dt.dayofweek # add features
print(df.info())
df = df.groupby(['day'])['passenger_count'].sum() # aggregation
print(df) # use dataframe