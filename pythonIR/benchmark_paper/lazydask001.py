import lazyfatpandas.pandas as pd
from time import time
start = time()
from glob import glob
# files = glob("/media/priyesh/BAECA12DECA0E53B/python_bench/yellow_tripdata_2015-0[0-1]*.csv")
files = glob("/media/bhushan/nvme/data/yellowtrip/yellow_tripd ata_2015-07.csv")
print(files)
df = pd.read_csv(files, parse_dates=['tpep_dropoff_datetime', 'tpep_pickup_datetime'])
df['day'] = df.tpep_pickup_datetime.dt.dayofweek # add features
df['discount'] = df.extra < 0
df['taken_highway'] = df.tolls_amount > 0
df = df[(df.tip_amount > 0)] # Get valid tipped trips
df = df[(df.fare_amount > 0) & (df.total_amount > 0)] # Get valid rides
df = df[(df.trip_distance <= 300)] # Trip distance cannot be greater than double the city diameter
df = df[(df.fare_amount > 100)] # Get expensive rides

df = df.groupby(['day'])['passenger_count'].sum() # aggregation
print(df.compute(prod=True, column_selection=True, row_selection=True, merge_filter=True, remove_deadcode=True))
print(time()-start)
