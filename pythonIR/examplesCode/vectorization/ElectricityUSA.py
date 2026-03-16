import pandas as pd
df = pd.read_csv('demand_profile.csv')
df['date_time'] = pd.to_datetime(df['date_time'])
print(df.head())