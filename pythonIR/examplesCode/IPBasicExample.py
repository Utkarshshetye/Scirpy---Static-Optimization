def testPD(df):
    print("here")
    print(df.pickup_datetime[1:10])

import pandas as pd # vs import pandas as pd
#columns=['fare_amount','pickup_datetime','passenger_count' ]
df = pd.read_csv('/home/bhushan/Downloads/data.csv') # fetch data
#print(df.pickup_datetime) # use dataframe
while 3>2:
    testPD(df)
    break

