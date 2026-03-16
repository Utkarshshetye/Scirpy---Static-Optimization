import pandas as pd
SO_columns = ["pickup_datetime","passenger_count","fare_amount"]
SO_d_d_t = ["pickup_datetime"]
SO_c_d_t = {"pickup_datetime":"str","passenger_count":"int64","fare_amount":"float32"}
df = pd.read_csv("/home/bhushan/intellijprojects/scirpy_benchmarks/examples/data/data.csv",usecols=SO_columns,parse_dates=SO_d_d_t,dtype=SO_c_d_t)
df.memory_usage(deep=True)
print(df.dtypes)
#df = df[(df.fare_amount > 0)]
df = pd.eval[df.fare_amount > 0]
df["day"] = df.pickup_datetime.dt.dayofweek
print(df.info())
df = df.groupby(["day"])["passenger_count"].sum()
print(df)