
import pandas as pd
SO_columns = ["payment_type","tpep_pickup_datetime","tpep_dropoff_datetime", "tip_amount","fare_amount","passenger_count","trip_distance"]
SO_c_d_t = {"passenger_count":"int64","trip_distance":"float32","payment_type":"int64","fare_amount":"float32","tip_amount":"float32"}
SO_d_d_t = ["tpep_pickup_datetime"]
df = pd.read_csv("/home/bhushan/Downloads/yellow_tripdata_2015-02.csv",parse_dates=["tpep_pickup_datetime","tpep_dropoff_datetime"],usecols=SO_columns,dtype=SO_c_d_t)
print(df.info())
print(df.head())
print(df.groupby(df.passenger_count).trip_distance.mean())
print(df.info())
df2 = df[((df.tip_amount > 0) & (df.fare_amount > 0))]
df2["tip_fraction"] = (df2.tip_amount / df2.fare_amount)
print(df2.info())
dayofweek = df2.groupby(df2.tpep_pickup_datetime.dt.dayofweek).tip_fraction.mean()
hour = df2.groupby(df2.tpep_pickup_datetime.dt.hour).tip_fraction.mean()
payments = pd.Series({1:"Credit Card",2:"Cash",3:"No Charge",4:"Dispute",5:"Unknown",6:"Voided trip"})
df2 = df.merge(payments.to_frame(),left_on="payment_type",right_index=True)
df2.groupby(df2.payment_type).tip_amount.mean()
zero_tip = (df2.tip_amount == 0)
cash = (df2.payment_type == "Cash")
