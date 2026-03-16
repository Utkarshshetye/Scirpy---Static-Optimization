#!/usr/bin/env python
# coding: utf-8

# In[ ]:


get_ipython().magic(u'matplotlib inline')
get_ipython().magic(u'reset')
import numpy as np
import pandas as pd
import matplotlib
import matplotlib.pyplot as plt 

# pd.options.display.mpl_style='default'
matplotlib.style.use('ggplot')
filename='campus21.log.20151201-20160201.log.gz.csv'
# filename='campus21.log.20150427-20150602.log.gz.csv.csv'
chunksize= 10**6
for chunk in pd.read_csv('./Data/{}'.format(filename), header=0,sep=";", chunksize= chunksize):
    process(chunk)


# In[ ]:


df.head()


# In[ ]:


# setting the index to timestamp
df['Timestamp']=pd.to_datetime(df['Timestamp'],)
df=df.set_index(['Timestamp'])


# In[ ]:


#extracting energy values 
acc_df= df[['Device 1---MOD-EL01-10-65-160-155-ME1','Device 1---MOD-EL02-10-65-160-155-ME1','Device 1---MOD-EL03-10-65-160-155-ME1','Device 1---MOD-EL19-10-65-160-160-ME1','Device 1---MOD-EL20-10-65-160-160-ME2','Device 1---MOD-EL21-10-65-160-160-ME3'
,'Device 1---MOD-EL22-10-65-160-161-ME1','Device 1---MOD-EL23-10-65-160-161-ME2','Device 1---MOD-EL24-10-65-160-162-ME10','Device 1---MOD-EL25-10-65-160-162-ME11', 'Device 1---MOD-EL26-10-65-160-162-ME12','Device 1---MOD-EL27-10-65-160-162-ME13','Device 1---MOD-EL07-10-65-160-151-ME1','Device 1---MOD-EL08-10-65-160-151-ME4', 'Device 1---MOD-EL09-10-65-160-152-ME1','Device 1---MOD-EL10-10-65-160-152-ME4','Device 1---MOD-EL11-10-65-160-153-ME1','Device 1---MOD-EL12-10-65-160-153-ME4','Device 1---MOD-EL13-10-65-160-154-ME1','Device 1---MOD-EL14-10-65-160-154-ME4' ,'Device 1---MOD-EL15-10-65-160-162-ME1','Device 1---MOD-EL16-10-65-160-162-ME4','Device 1---MOD-EL18-10-65-160-160-ME4', 'Device 1---MOD-EL17-10-65-160-162-ME7','Device 1---MOD-EL28-10-65-160-156-ME3','Device 1---MOD-EL29-10-65-160-156-ME6','Device 1---MOD-EL15-10-65-160-162-ME3','Device 1---MOD-EL16-10-65-160-162-ME6','Device 1---MOD-EL18-10-65-160-160-ME6']]
acc_df.to_excel("./Data/quality_check/{}_extracted.xls".format(filename))
acc_df.head()


# In[ ]:


# since a lot of data was in exponentials, i had to convert it into numerica
acc_df=acc_df.convert_objects(convert_numeric=True)
acc_df.head()


# In[ ]:


acc_df.loc["2015-10-14":"2015-10-20", "Device 1---MOD-EL18-10-65-160-160-ME4"]


# In[ ]:


# the above readings are accumulative electricity data, so we need to obtain the electriccty consumption in each time step
df=acc_df.diff(periods=1)
df


# In[ ]:


# converting all values to kWh 
df.iloc[:,12:]/=1000


# In[ ]:


df.head()


# In[ ]:


df.plot(figsize=(20,6), legend=False)


# In[ ]:


# we need to write a code to identify good sections in the data ( one's without large gaps in the meter readings)


# In[ ]:


# a lot of times the meter resets and gets back again; we need

df[df<0]=np.nan
# this upper limit on main meter to identify absurd data is based on the the estimation that a 5MW plant will consume 80kWh
# in a time step of 5 min => 400kWh for the mains meter
df[abs(df)>400]=np.nan
# For all the other readers the data is reported  in Wh so here we set the upper limit to 400000 Wh 
# df[abs(df.iloc[:,3:])>400000]=np.nan


# In[ ]:


df.shape[0]


# In[ ]:


drop_out=df.isnull().sum()
drop_out_rate=drop_out/df.shape[0]
d= {'Dropout':drop_out,'Dropout Rate':drop_out_rate}
drop_out_perf=pd.DataFrame(d, index=drop_out.index)
print drop_out_perf


# In[ ]:


df.plot(figsize=(20,6),colormap='Accent')


# Exploratory analysis of data

# In[ ]:


# filling minor drops in the sensor data 
df=df.fillna(method="pad",limit=2)


# In[ ]:


# extracting the mains supply meter readings
elec_main=df[['Device 1---MOD-EL01-10-65-160-155-ME1','Device 1---MOD-EL02-10-65-160-155-ME1','Device 1---MOD-EL03-10-65-160-155-ME1']]
elec_main['energy']=elec_main.sum(axis=1)
daily_elec_main=elec_main.groupby(elec_main.index.date).sum()
daily_elec_main.plot(kind='bar',figsize=(20,6))
plt.xlabel('Days', fontsize= 15)
plt.ylabel('kWh', fontsize= 15)
plt.title("Daily electricity consumption from Mains")


# In[ ]:


electricty_consumption_day = daily_elec_main["energy"].mean(axis=0, skipna=True)
print electricty_consumption_day,'kWh'


# In[ ]:


elec_sup=df[['Device 1---MOD-EL19-10-65-160-160-ME1','Device 1---MOD-EL20-10-65-160-160-ME2','Device 1---MOD-EL21-10-65-160-160-ME3'
,'Device 1---MOD-EL22-10-65-160-161-ME1','Device 1---MOD-EL23-10-65-160-161-ME2','Device 1---MOD-EL24-10-65-160-162-ME10','Device 1---MOD-EL25-10-65-160-162-ME11', 'Device 1---MOD-EL26-10-65-160-162-ME12','Device 1---MOD-EL27-10-65-160-162-ME13','Device 1---MOD-EL07-10-65-160-151-ME1','Device 1---MOD-EL08-10-65-160-151-ME4', 'Device 1---MOD-EL09-10-65-160-152-ME1','Device 1---MOD-EL10-10-65-160-152-ME4','Device 1---MOD-EL11-10-65-160-153-ME1','Device 1---MOD-EL12-10-65-160-153-ME4','Device 1---MOD-EL13-10-65-160-154-ME1','Device 1---MOD-EL14-10-65-160-154-ME4','Device 1---MOD-EL15-10-65-160-162-ME1','Device 1---MOD-EL16-10-65-160-162-ME4','Device 1---MOD-EL18-10-65-160-160-ME4', 'Device 1---MOD-EL17-10-65-160-162-ME7']]
elec_sup['energy_sup']=elec_sup.sum(axis=1, numeric_only=float)


# In[ ]:


# plotting daily electricity consumption recorded at the supply level 
daily_elec_sup = elec_sup.groupby(elec_sup.index.date).sum()
daily_elec_sup.plot(kind='bar',figsize=(20,6))
plt.xlabel('Days', fontsize= 15)
plt.ylabel('kWh', fontsize= 15)
plt.title("Daily electricity consumption from supply")
plt.figure()


# In[ ]:


electricty_consumption_day_sup = daily_elec_sup["energy_sup"].mean(axis=0, skipna=True)
print electricty_consumption_day_sup,'kWh'


# Analyzing the consumption of floodlights

# In[ ]:


elec_fl=df[['Device 1---MOD-EL07-10-65-160-151-ME1','Device 1---MOD-EL08-10-65-160-151-ME4', 'Device 1---MOD-EL09-10-65-160-152-ME1','Device 1---MOD-EL10-10-65-160-152-ME4','Device 1---MOD-EL11-10-65-160-153-ME1','Device 1---MOD-EL12-10-65-160-153-ME4','Device 1---MOD-EL13-10-65-160-154-ME1','Device 1---MOD-EL14-10-65-160-154-ME4']]
elec_fl['energy_fl']=elec_fl.sum(axis=1, numeric_only=float)


# In[ ]:


# plotting daily electricity consumption recorded at the supply level 
daily_elec_fl = elec_fl.groupby(elec_fl.index.date).sum()
plt.figure()
plt.xlabel('Days', fontsize= 15)
plt.ylabel('kWh', fontsize= 15)
plt.title("Daily electricity consumption of floodlights")
daily_elec_fl["energy_fl"].plot(kind='bar',figsize=(20,6))


# In[ ]:


elec_co=df[['Device 1---MOD-EL15-10-65-160-162-ME1', 'Device 1---MOD-EL16-10-65-160-162-ME4','Device 1---MOD-EL18-10-65-160-160-ME4','Device 1---MOD-EL17-10-65-160-162-ME7']]


# In[ ]:


elec_co.rename(columns={'Device 1---MOD-EL15-10-65-160-162-ME1': 'energy_ch1a', 'Device 1---MOD-EL16-10-65-160-162-ME4': 'energy_ch1b','Device 1---MOD-EL18-10-65-160-160-ME4':'energy_ch2','Device 1---MOD-EL17-10-65-160-162-ME7':'energy_ct'}, inplace=True)
elec_co_power=df[['Device 1---MOD-EL15-10-65-160-162-ME3','Device 1---MOD-EL16-10-65-160-162-ME6','Device 1---MOD-EL18-10-65-160-160-ME6']]


# In[ ]:


daily_elec_co=elec_co.groupby(elec_co.index.date).sum()
plt.figure()
daily_elec_co.plot(kind='bar',figsize=(20,6))
plt.xlabel('Days', fontsize= 15)
plt.ylabel('kWh', fontsize= 15)
plt.title("Daily electricity consumption of cooling ")


# In[ ]:


print elec_co.loc['2015-06-27 00:00:00':'2015-06-30 00:00:00', 'energy_ch2']



# Analyzing AHU

# In[ ]:


elec_au=df[['Device 1---MOD-EL28-10-65-160-156-ME3','Device 1---MOD-EL29-10-65-160-156-ME6']]
elec_au.rename(columns={'Device 1---MOD-EL28-10-65-160-156-ME3':'energy_ahu1','Device 1---MOD-EL29-10-65-160-156-ME6':'energy_ahu2'}, inplace=True)
elec_au['energy_au']=elec_au.sum(axis=1)


# In[ ]:


daily_elec_au=elec_au.groupby(elec_au.index.date).sum()
plt.figure()
daily_elec_au.plot(kind='bar',figsize=(20,6))
plt.xlabel('Days', fontsize= 15)
plt.ylabel('kWh', fontsize= 15)
plt.title("Daily electricity consumption of AHU ")


# In[ ]:


elec_au["energy_ahu1"].plot(figsize=(20,6))


# In[ ]:


daily_elec_tot = pd.concat([daily_elec_main["energy"],daily_elec_sup['energy_sup'], daily_elec_co['energy_ch1a'],daily_elec_co['energy_ch1b'], daily_elec_co['energy_ch2'],daily_elec_co['energy_ct'], daily_elec_fl['energy_fl'],daily_elec_au['energy_au']], axis=1)


# In[ ]:


daily_elec_tot.describe()


# In[ ]:


stat_list= [daily_elec_tot.describe(),drop_out_perf]
xls_path="./Data/quality_check/{}_stats.xls".format(filename)
from pandas import ExcelWriter
writer = ExcelWriter(xls_path)
for n, df in enumerate(stat_list):
    df.to_excel(writer,'sheet%s' % n)
writer.save()

