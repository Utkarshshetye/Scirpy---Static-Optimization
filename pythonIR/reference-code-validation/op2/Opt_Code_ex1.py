
import pandas as pd
SO_columns = ["pickup_datetime","passenger_count","fare_amount"]
SO_c_d_t = {"pickup_datetime":"str","passenger_count":"int64","fare_amount":"float32"}
SO_d_d_t = ["pickup_datetime"]
df = pd.read_csv("/home/bhushan/intellijprojects/scirpy_benchmarks/examples/data/data.csv",parse_dates=["pickup_datetime"],usecols=SO_columns,dtype=SO_c_d_t,parse_dates=SO_d_d_t)
df.memory_usage(deep=True)
print(df.dtypes)
df = df[(df.fare_amount > 0)]
df["day"] = df.pickup_datetime.dt.dayofweek
print(df.info())
df = df.groupby(["day"])["passenger_count"].sum()
print(df)
