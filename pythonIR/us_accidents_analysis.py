# This Python 3 environment comes with many helpful analytics libraries installed
# It is defined by the kaggle/python docker image: https://github.com/kaggle/docker-python
# For example, here's several helpful packages to load in 

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sb
import os
import datetime as dt
import sys

  

main_dataset = pd.read_csv('/home/mudra/Media/Downloads/US_accident_data/US_Accidents_June20.csv')
    

main_dataset['timestamp'] = pd.to_datetime(main_dataset['Weather_Timestamp'], errors='coerce')
main_dataset['Hour'] = main_dataset['timestamp'] .dt.hour
main_dataset['Minute'] = main_dataset['timestamp'] .dt.minute
hours = [hour for hour, df in main_dataset.groupby('Hour')]
plt.plot(hours, main_dataset.groupby(['Hour'])['ID'].count())
plt.xticks(hours)
plt.xlabel('Hour')
plt.ylabel('Numer of accidents')
plt.grid(True)
plt.show()
main_dataset['date'] = main_dataset['Start_Time'].str.split(n=1).str[0]
main_dataset['Date'] = pd.to_datetime(main_dataset['date'], errors='coerce')
main_dataset['Week'] = main_dataset['Date'] .dt.week
main_dataset['Year'] = main_dataset['Date'] .dt.year
plt.style.use('ggplot')
fig, ax = plt.subplots(figsize=(15,7))
main_dataset.groupby(['Year','Week','Severity']).count()['ID'].unstack().plot(ax=ax)
ax.set_xlabel('Week')
ax.set_ylabel('Number of Accidents')
main_dataset['timestamp'] = pd.to_datetime(main_dataset['Weather_Timestamp'], errors='coerce')
main_dataset['Hour'] = main_dataset['timestamp'] .dt.hour
main_dataset['Minute'] = main_dataset['timestamp'] .dt.minute
hours = [hour for hour, df in main_dataset.groupby('Hour')]
plt.plot(hours, main_dataset.groupby(['Hour'])['ID'].count())
plt.xticks(hours)
plt.xlabel('Hour')
plt.ylabel('Numer of accidents')
plt.grid(True)
plt.show()
