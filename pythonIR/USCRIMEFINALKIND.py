#!/usr/bin/env python
# coding: utf-8

#     # John-Alexander Hall Capstone

# The data set that I will be exploring was compiled and made available by the Murder Accountability Project. The csv file comes from kaggle.com and markets itself as the most complete database of homicides in the United States currently available. The data set contains information regarding the age, race, sex, ethnicity of both victims and perpetrators, relationship between victims and perpetrators, and the weapon used for the crime.
# ## Analytical Questions
# 1. What are the overall trend of crimes in the United States? How often are these cases being solved?
#
# 2. Is there a significant trend in how crimes are committed?
#
# 3. Are crime trends significantly different based on victim race? Perpetrator race?
# In[1]:
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
#get_ipython().magic(u'matplotlib inline')
# In[2]:
# helper function that calculates percent difference
def get_percent_difference(current, previous):
    if current == previous:
        return 100.0
    try:
        return (abs(current - previous)/previous) * 100.0
    except ZeroDivisionError:
        return 0

    # In[3]:
# list range of years in the data frame, useful for x axis markers
years = [x for x in range(1980, 2015)]
# In[4]:
# upload date set into data frame
fbi = pd.read_csv('fbi_reports.csv')
# data frame with US population data from 1980-2014
us_pop = pd.read_csv('US_Population.csv')
# In[5]:
# removes all rows where the record has a victim age outside the bounds of reality
# the oldest verified person lived to the age of 122
#fbi = fbi.ix[lambda df: df['Victim Age'] < 123, :]
# In[6]:
# removes unnecessary columns from the data frame
fbi.drop(['Record Source'],inplace=True,axis=1)
fbi.drop(['Agency Code'],inplace=True,axis=1)
fbi.drop(['Agency Name'],inplace=True,axis=1)
fbi.drop(['Agency Type'],inplace=True,axis=1)
# In[7]:
# info function provides a concise summary of a data frame
fbi.info()

# In[8]:
us_pop.info()
# In[9]:
# head function returns the first n rows, default is 5
fbi.head()
# In[10]:
# removes the record id and year columns, asthey are unnecessary
# describe function generates various summary statistics
fbi.loc[:, ['Incident', 'Victim Age', 'Perpetrator Age', 'Victim Count', 'Perpetrator Count']].describe()
# In[11]:

# corr function computes pairwise correlation of columns

fbi.loc[:, ['Incident', 'Victim Age', 'Perpetrator Age', 'Victim Count', 'Perpetrator Count']].corr()


# None of the numeric data columns has a strong negative or positive correlation with another column.

# Let's try and visualize some of this data and possible trends in a more graphical format. Firstly, I want to explore the overall trends of crime data and how often crimes are solved.

# # Crime Types

# In[12]:


fbi['Crime Type'].value_counts().plot(kind='bar')

plt.title('Number of Incidents by Crime Type')
plt.xlabel('Crime Type')
plt.ylabel('Number of Incidents')


# In[13]:


(628372 / (628372 + 9108)) * 100    # percentage of crimes that are Murder or Manslaughter


# In[14]:


(fbi[fbi['Crime Type'] == 'Murder or Manslaughter'].count()['Incident'] / fbi['Crime Type'].count()) * 100


# 98.57% of all crimes in this data set fall into the 'Murder or Manslaughter' category; the crime type is heavily skewed. When dealing with incidents in this data set, it is safe to assume that most of them pertain to murder or manslaugter.

# # Incidents by Year

# In[15]:


fbi['Year'].value_counts().sort_index(ascending=True).plot()
plt.tight_layout()
plt.title('Number of Incidents per Year')
plt.xlabel('Year')
plt.ylabel('Number of Incidents')
plt.tight_layout()


# The overall number of crimes has followed a downward trend since 1980, despite a large uptick in incidents during the early 1990s. However, these crime statistics are not normalized because the total US population changes over time. Normalizing the data and producing crime data per capita is a much better method of contrasting crime data over time.

# In[16]:


incidents_by_year = fbi.groupby('Year').count()['Incident']
total_population = us_pop['US_Population']

# finds the ratio of incidents per 100,000
incidents_per_population = [i/j * 100000 for i,j in zip(incidents_by_year, total_population)]

plt.plot(years, incidents_per_population, 'x-')
plt.title('Number of Incidents per 100,000 people')
plt.ylabel('Incidents per 100,000')
plt.tight_layout()


# In[17]:


get_percent_difference(incidents_per_population[-1], incidents_per_population[0])


# The normalized crime data certainly supports the claim that crime data has decreased fairly drastically in the 34-year span between 1980 and 2014. There has been a 57% decrease in incidents per 100,000 people in the time between 1980 and 2014.

# # Crime Solved?

# The rates at which crimes are 'solved' interests me. There is no basis for what solving a crime means, but I assume that it involves finding the perpetrator and the means to which a crime was carried out.

# In[18]:


fbi['Crime Solved'].value_counts().plot(kind='bar')
plt.title('Number of Incidents Solved')
plt.xlabel('Crime Solved?')
plt.ylabel('Number of Incidents')
plt.tight_layout()


# In[19]:


total_crimes = fbi['Crime Solved'].count()

#fbi_crimes_solved_y = fbi.ix[lambda df: df['Crime Solved'] == 'Yes', :]
#fbi_crimes_solved_n = fbi.ix[lambda df: df['Crime Solved'] == 'No', :]
fbi_crimes_solved_y = fbi[fbi['Crime Solved'] == 'Yes']
fbi_crimes_solved_n = fbi[fbi['Crime Solved'] == 'No']
total_crimes_solved = fbi_crimes_solved_y['Crime Solved'].count()

total_crimes_solved / total_crimes # equals 0.70196443283306231


# About 70% of all crimes in the data set are marked as solved crimes, which is not as high as I would like it to be, but it could certainly be lower, so I'm not complaining.

# Below is a graph that plots the percentage of crimes solved over time. These percentages help normalize the data over time.

# In[20]:


crimes_solved_per_year = fbi_crimes_solved_y.groupby('Year').count()['Crime Solved']
total_crimes_solved = fbi.groupby('Year').count()['Crime Solved']

crimes_solved_over_time = [(i/j) * 100 for i,j in zip(crimes_solved_per_year, total_crimes_solved)]
plt.plot(years, crimes_solved_over_time, 'o-')
plt.title('Percentages of Crimes Solved over Time')
plt.xlabel('Percentage of Crime Solved')
plt.ylabel('Year')
plt.ylim(60, 80)
plt.tight_layout()


# In[21]:


np.median(crimes_solved_over_time)


# The percentage of crime solved hovers between 67.5% and 75% over the 34-year time period. The median of the percentages is 69.94%, which falls in line with the overall data.

# The graph below merely shows the number of crimes solved per every 100,000 incidents. Consider it a more zoomed in plotting of the percentage information.

# In[22]:


# crimes solved per 100,000
crimes_not_solved = fbi_crimes_solved_n.groupby('Year').count()['Crime Solved']
crimes_solved = fbi_crimes_solved_y.groupby('Year').count()['Crime Solved']

y = fbi.groupby('Year').count()['Crime Solved']

crimes_solved_per_100000 = [i/j * 100000 for i,j in zip(crimes_solved, y)]
crimes_not_solved_per_100000 = [i/j * 100000 for i,j in zip(crimes_not_solved, y)]

# plots number of crimes that were solved per 100,000 incidents

plt.plot(years, crimes_solved_per_100000)
plt.title('Crimes Solved')
plt.ylabel('# of Crimes solved per 100,000 Incidents')
plt.ylim(65000, 75000)


# Below is a graph that plots both the total number of crimes solved and the total number of crimes that are unsolved.
#
# The blue line represents crimes solved.
# The orange line represents crimes unsolved.

# In[23]:


# plots the number of crimes solved over time
#fbi_crimes_solved_y = fbi.ix[lambda df: df['Crime Solved'] == 'Yes', :]
fbi_crimes_solved_y = fbi[fbi['Crime Solved'] == 'Yes']


fbi_crimes_solved_y.groupby('Year').count()['Crime Solved'].plot()

# plots the number of crimes that were NOT solved over time
#fbi_crimes_solved_n = fbi.ix[lambda df: df['Crime Solved'] == 'No', :]
fbi_crimes_solved_n = fbi[fbi['Crime Solved'] == 'No']
fbi_crimes_solved_n.groupby('Year').count()['Crime Solved'].plot()

plt.title('Crimes Solved by Year')
plt.xlabel('Year')
plt.ylabel('Number of Crimes Solved')
plt.tight_layout()


# The graph below plots the number of crimes solved and number of crimes that are unsolved per 100,000 people in the US over time. This data further supports the evidence that crime has seen relevant drops in the last 40 years.
#
# As the number of crimes go down, the overall number of crimes available to solve decreases as well.

# In[24]:


x = fbi_crimes_solved_y.groupby('Year').count()['Crime Solved']
y = us_pop['US_Population']

z = [(i/j) * 100000 for i,j in zip(x, y)]
plt.plot(years, z)

a = fbi_crimes_solved_n.groupby('Year').count()['Crime Solved']
b = us_pop['US_Population']

c = [(i/j) * 100000 for i,j in zip(a, b)]
plt.plot(years, c)

plt.tight_layout()
plt.title('Number of Crimes per 100,000')
plt.xlabel('Year')
plt.ylabel('Number of Crimes solved')

plt.tight_layout()


# # Weapon Usage

# I would also like to explore how crimes are committed. The data set contains information about the types of weapon being used during incidents.

# In[25]:


fbi['Weapon'].value_counts().plot(kind='bar')
plt.title('Weapons Used in Reported Incidents')
plt.xlabel('Weapon Type')
plt.ylabel('Number of Incidents')


# Handguns are, by far, the weapon of choice for most perpetrators. I want to tug on this thread a little further.

# Below is the breakdown for handgun incidents by year.

# In[26]:


fbi_handgun = fbi[fbi['Weapon'] == 'Handgun']
fbi_crimes_solved_n = fbi[fbi['Crime Solved'] == 'No']
fbi_handgun.groupby('Year').count()['Weapon'].plot()

plt.title('Handgun Incidents by Year')
plt.xlabel('Year')
plt.ylabel('Handgun Incidents')
plt.tight_layout()


# Below is the breakdown for how many incidents out of 1000 involve a handgun.

# In[27]:


x = fbi_handgun.groupby('Year').count()['Weapon']
y = fbi.groupby('Year').count()['Crime Solved']

handgun_per_1000 = [i/j * 1000 for i,j in zip(x, y)]

plt.plot(years, handgun_per_1000)
plt.title('Handgun Incidents per 1000 Total Incidents')
plt.ylabel('Number of Handgun Incidents')
plt.xlabel('Year')


# In[28]:


np.mean(handgun_per_1000)


# Fortunately, the trend for handgun usage, alongside crime is trending downwards. This behavior is expected. If overall crime is going down and handguns are the most prevalent weapon used in crimes, the rate of crimes involving handguns should fall.

# In[29]:


x = fbi_handgun.groupby('Year').count()['Weapon']
y = us_pop['US_Population']

handgun_per_100000 = [i/j * 100000 for i,j in zip(x, y)]

plt.plot(years, handgun_per_100000)
plt.title('Handgun Incidents per 100000 People')
plt.ylabel('Number of Handgun Incidents')
plt.xlabel('Year')


# # Incidents by Relationship

# I also want to analyze the most common relationships, age distributions, and break down racial data for both victims and perpetrators.

# In[30]:


# removes unknown values from relationship column
fbi_rel = fbi[fbi['Relationship'] != 'Unknown']


fbi_rel['Relationship'].value_counts()[:15].plot(kind='bar')

plt.title('Number of Incidents by Relationship')
plt.xlabel('Victim Relationship to Perpetrator')
plt.ylabel('Number of Incidents')
plt.tight_layout()


# In[31]:


fbi_acq = fbi[fbi['Relationship'] == 'Acquaintance'].count()['Incident']
fbi_stranger = fbi[fbi['Relationship'] == 'Stranger'].count()['Incident']

# adds the percentages of strangers and acquaintances together
(fbi_stranger / fbi['Incident'].count()) + (fbi_acq / fbi['Incident'].count())




