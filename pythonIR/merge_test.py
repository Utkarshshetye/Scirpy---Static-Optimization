#https://www.kaggle.com/singhbhushan07/merge-join-and-concat-with-pandas/edit
import pandas as pd
import numpy as np

user_usage=pd.read_csv('/home/bhushan/intellijprojects/notebooks/data/devices/user_usage.csv')
print(user_usage.head())
user_device=pd.read_csv('/home/bhushan/intellijprojects/notebooks/data/devices/user_device.csv')
print(user_device.head())
result = pd.merge(user_usage,user_device[['use_id', 'platform', 'device']],on='use_id', how='outer',indicator=True)
print(result.head())

