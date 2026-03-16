import pandas as pd # vs import pandas as pd
#c_d_t = {"medallion":"str","a":"str","as":"category","asas":"category","asas":"category","pickup_datetime":"str","aslk":"str","passenger_count":"category","kl":"int64","klkl":"category","lklll":"float32","askk":"float32","kkkl":"float32","lkp":"float32","l;l":"category","fare_amount":"int64","jk":"category","jk":"category","cx":"category","sa":"category","wd":"category","we":"category","lkjj":"category"}
c_d_t = {"medallion":"str","a":"str","as":"category","asas":"category","asas":"category","pickup_datetime":"str","aslk":"str","passenger_count":"int64","kl":"int64","klkl":"category","lklll":"float32","askk":"float32","kkkl":"float32","lkp":"float32","l;l":"category","fare_amount":"float64","jk":"category","jk":"category","cx":"category","sa":"category","wd":"category","we":"category","lkjj":"category"}
columns=['fare_amount','pickup_datetime','passenger_count' ]
df = pd.read_csv('/home/bhushan/Downloads/data.csv',  parse_dates=['pickup_datetime'], dtype=c_d_t,usecols=columns) # fetch data
print(df.dtypes) # use dataframe
df = df[df.fare_amount > 0] # filter bad rows
df['day'] = df.pickup_datetime.dt.dayofweek # add features
print(df.info())
df = df.groupby(['day'])['passenger_count'].sum() # aggregation
print(df) # use dataframe