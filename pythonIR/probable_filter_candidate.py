import pandas as pd
from IPython.display import display

def low_fare_multiple_passenger(fare_amt,passenger_count):
    if((fare_amt<10) & (passenger_count>=5)):
        tag='Low Fare Multiple Passenger'
    else:
        tag='Not Applicable'
    return tag

dask_data=pd.read_csv('/home/bhushan/Downloads/datahead.csv', parse_dates=['pickup_datetime'])
dask_data['year']=dask_data.pickup_datetime.dt.year
dask_data_2014=dask_data[dask_data['year']==2014]
print('Unique year after filter: ',dask_data_2014['year'].unique().tolist())
dask_data_high_fare=dask_data[dask_data['fare_amount']>100]
print('>$100 fare distribution:')
display(dask_data_high_fare['fare_amount'].describe([0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9]))


passenger_bins=[-0.001,0.99,1.99,4.99,100]
#dask_data['passenger_bucket']=dask_data['passenger_count'].map_partitions(pd.cut, bins=passenger_bins,labels=['0 Passenger','1 Passenger','2-5 Passengers','>5 Passengers'])
dask_data['passenger_bucket']=pd.cut (dask_data['passenger_count'], bins=passenger_bins,labels=['0 Passenger','1 Passenger','2-5 Passengers','>5 Passengers'])

fare_bins=[0,4.99,9.99,19.99,49.99,99.99,100000]
dask_data['fare_bucket']=pd.cut (dask_data['fare_amount'], bins=fare_bins,labels=['<$5','$5-$10','$10-$20','$20-$50','$50-$100','>$100'])
# Apply Function
dask_data['tag_fare_passenger']=dask_data.apply(lambda row:low_fare_multiple_passenger(row['fare_amount'],row['passenger_count']),axis=1)
print(dask_data.agg('passenger_bucket'))
print(dask_data.agg('fare_bucket'))

# columns=['passenger_count','year','fare_amount']
# train_model=dask_data[columns]
#
# train_model=train_model.sample(frac=.05, replace=False)
# train_model.head()



