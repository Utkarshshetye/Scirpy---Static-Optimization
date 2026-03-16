
import pandas as pd
SO_columns = ["pickup_datetime","passenger_count","fare_amount"]
df = pd.read_csv("/home/bhushan/intellijprojects/scirpy_benchmarks/examples/data/data.csv",parse_dates=["pickup_datetime"],usecols=SO_columns)
df.memory_usage(deep=True)
print(df.dtypes)
df = df[(df.fare_amount > 0)]
df["day"] = df.pickup_datetime.dt.dayofweek
print(df.info())
df = df.groupby(["day"])["passenger_count"].sum()
print(df)
