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
from scipy import stats
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
fbi = fbi.ix[lambda df: df['Victim Age'] < 123, :]
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

fbi_crimes_solved_y = fbi.ix[lambda df: df['Crime Solved'] == 'Yes', :]
fbi_crimes_solved_n = fbi.ix[lambda df: df['Crime Solved'] == 'No', :]
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
fbi_crimes_solved_y = fbi.ix[lambda df: df['Crime Solved'] == 'Yes', :]
fbi_crimes_solved_y.groupby('Year').count()['Crime Solved'].plot()

# plots the number of crimes that were NOT solved over time
fbi_crimes_solved_n = fbi.ix[lambda df: df['Crime Solved'] == 'No', :]
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


fbi_handgun = fbi.ix[lambda df: df['Weapon'] == 'Handgun', :]
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
fbi_rel = fbi.ix[lambda df: df['Relationship'] != 'Unknown', :]


fbi_rel['Relationship'].value_counts()[:15].plot(kind='bar')

plt.title('Number of Incidents by Relationship')
plt.xlabel('Victim Relationship to Perpetrator')
plt.ylabel('Number of Incidents')
plt.tight_layout()


# In[31]:


fbi_acq = fbi.ix[lambda df: df['Relationship'] == 'Acquaintance', :].count()['Incident']
fbi_stranger = fbi.ix[lambda df: df['Relationship'] == 'Stranger', :].count()['Incident']

# adds the percentages of strangers and acquaintances together
(fbi_stranger / fbi['Incident'].count()) + (fbi_acq / fbi['Incident'].count())


# Acquaintances and strangers are the most prolific groups to commit crimes. These two groups generate the largest amount of incidents by a wide margin. The two combine for 35% of the reported relationships (acquaintances account for 19% and strangers count for 15% within the data). This is a very large number when 42.7% of the relationships in the data set are marked as unknown.

# One should be much more wary of acquaintances (a person one knows slightly, but who is not considered a close friend) and strangers when it comes to potential crimes, as expected.

# # Age Distributions

# In[32]:


# filters out Perpetrator ages that are 0
fbi_perp = fbi.ix[lambda df: df['Perpetrator Age'] > 0, :]

# filters out ic ages that are 0
fbi_vic = fbi.ix[lambda df: df['Victim Age'] > 0, :]


# The data set uses 0 to denote an unknown perpetrator age value. Such instances have been filtered out.

# In[33]:


plt.figure(figsize=(10, 6))

plt.subplot(1, 2, 1)
plt.boxplot(fbi_vic['Victim Age'])
plt.title('Range of Victim Ages')
plt.ylabel('Age')


# remove the 1 and the tickmark from the bottom axis

plt.tick_params(
    axis='x',
    bottom='off',
    labelbottom='off'
)
plt.tight_layout()


# =================================================


plt.subplot(1, 2, 2)
plt.boxplot(fbi_perp['Perpetrator Age'])
plt.title('Range of Perpetrator Ages')
plt.ylabel('Age')

# remove the 1 and the tickmark from the bottom axis
plt.tick_params(
    axis='x',
    bottom='off',
    labelbottom='off'
)
plt.tight_layout()


# I have decided to keep the 'outliers' shown in the boxplot inside of the dataset. I am looking for a holistic view of the age distribution and I do not want to remove valuable pieces of data simply because perpetrators or victims are moving closer towards the natural twilight years.

# In[34]:


print('The median victim age is: {}'.format(np.median(fbi_vic['Victim Age'])))
print('The standard deviation is: {}'.format(np.std(fbi_vic['Victim Age'])))


# In[35]:


print('The median perpetrator age is: {}'.format(np.median(fbi_perp['Perpetrator Age'])))
print('The standard deviation is: {:.2f}'.format(np.std(fbi_perp['Perpetrator Age'])))


# In[36]:


fbi.groupby('Year')['Victim Age'].median().plot(kind='line')
fbi_perp.groupby('Year')['Perpetrator Age'].median().plot()


plt.ylim(24, 35)
plt.title('Median Ages over Time')
plt.ylabel('Median Age')
plt.xlabel('Year')

plt.legend()
plt.tight_layout()


# From 1980-2014, the median perpetrator age has consistently been lower than the median victim age. The median victim age has seen less fluctuation over time but the median perpetrator age has seen upward movement in recent years after a steep decline.

# In[37]:


plt.figure(figsize=(12, 4))

plt.subplot(1, 2, 1)
plt.hist(fbi_vic['Victim Age'], bins=20)
plt.title('Distribution of Victim Ages')
plt.xlabel('Victim Age')
plt.ylabel('Number of Occurences')
plt.tight_layout()

plt.subplot(1, 2, 2)
plt.hist(fbi_perp['Perpetrator Age'], bins=20)
plt.title('Distribution of Perpetrator Ages')
plt.xlabel('Perpetrator Age')
plt.ylabel('Number of Occurences')
plt.tight_layout()


# The histogram distributions of both age categories suggest a pretty typical gamma distribution. The likelihood of a perpetrator or victim being very young is low, but that likelihood increases as time goes on and decreases once again as older people die.

# # FBI Reports Victim Groups

# In[38]:


plt.figure(figsize=(10, 8))

plt.subplot(1, 2, 1)
fbi['Victim Race'].value_counts().plot(kind='bar')
plt.xlabel('Victim Race')
plt.ylabel('Number of Incidents')
plt.ylim(0, 350000)

plt.subplot(1, 2, 2)
fbi['Perpetrator Race'].value_counts().plot(kind='bar')
plt.xlabel('Perpetrator Race')
plt.ylim(0, 350000)

plt.tight_layout()


# Breaking down the overall population data is not very helpful. The majority groups are going to have the most incidents. I would like to normalize the data and calculate the victim and perpetrator rates for every 100,000 people in a particular race group.

# In[39]:


# shows the distinct values for a Victim's Race
fbi['Victim Race'].value_counts()


# # Incidents by Victim Race

# This section is marred by missing information. The US census (data gathering about the American people) is conducted once every 10 years. Thus, I only have specific data concerning race for the census years that overlap with FBI reports data set. The overlapping years are 1980, 1990, 2000, and 2010.

# The lack of population data separated by race is certainly a limitation. In lieu of actual data I make the assumption that the race population stays the same during the 10 year spans between US census data points.
#
# For example, I consider the population of black people in the United States from 1980-1989 to be the same number. This is not a perfect solution, but the rate of change between years is not drastic enough to botch analysis.

# ### Incidents Concerning Black Victims

# In[40]:


black_incidents = fbi[fbi['Victim Race'] == 'Black']

x = black_incidents.groupby('Year').count()['Crime Solved']

y = us_pop['black_population']

black_vic = [i/j * 100000 for i,j in zip(x, y)]
plt.plot(years, black_vic, 'x-', color='black')
plt.title('Number of Victims per 100,000 Black people')
plt.ylabel('Victims per 100,000')
plt.tight_layout()


# In[41]:


# calculates the percent difference between the number of victims per 100,000 in the 2010
# and 1980
get_percent_difference(black_vic[-1], black_vic[0])


# The number of black victims per 100,000 black people has greatly decreased but it is still the highest amongst racial groups.

# ### Incidents Concerning White Victims

# In[42]:


white_incidents = fbi[fbi['Victim Race'] == 'White']

x = white_incidents.groupby('Year').count()['Crime Solved']

y = us_pop['white_population']

white_vic = [i/j * 100000 for i,j in zip(x, y)]
plt.plot(years, white_vic, 'x-', color='green')
plt.title('Number of Victims per 100,000 White people')
plt.ylabel('Victims per 100,000')
plt.tight_layout()


# In[43]:


# calculates the percent difference between the number of victims per 100,000 in the 2010
# and 1980
get_percent_difference(white_vic[-1], white_vic[0])


# ### Incidents Concerning Asian Victims

# In[44]:


asian_incidents = fbi[fbi['Victim Race'] == 'Asian/Pacific Islander']

x = asian_incidents.groupby('Year').count()['Crime Solved']

y = us_pop['asian_population']

asian_vic = [i/j * 100000 for i,j in zip(x, y)]
plt.plot(years, asian_vic, 'x-', color='red')
plt.title('Number of Victims per 100,000 Asian people')
plt.ylabel('Victims per 100,000')
plt.tight_layout()


# In[45]:


# calculates the percent difference between the number of victims per 100,000 in the 2010
# and 1980
get_percent_difference(asian_vic[-1], asian_vic[0])


# ### Incidents Concerning Native American Victims

# In[46]:


native_incidents = fbi[fbi['Victim Race'] == 'Native American/Alaska Native']

x = native_incidents.groupby('Year').count()['Crime Solved']

y = us_pop['native_population']

native_vic = [i/j * 100000 for i,j in zip(x, y)]
native_vic
plt.plot(years, native_vic, 'x-', color='blue')
plt.title('Number of Incidents per Native Americans')
plt.ylabel('Victims per 100,000')
plt.tight_layout()


# In[47]:


# calculates the percent difference between the number of victims per 100,000 in the 2010
# and 1980
get_percent_difference(native_vic[-1], native_vic[0])


# According to the data set, the black population has been significantly more victimized than other races during the four years plotted on the above graphs. Fortunately, the rate of victimization has decreased fairly drastically across the board.

# ### Statistical significance?

# To test for statistical significance, I use a non-parametric test because the values that I am testing for do not conform to a normal distribution. The statistical test checks for the probability that sample data collected comes from the same distribution.
#
# The distributions are plotted as a times series with a span of 34 years. Each point of the time series represents a state. The time series for each race is compared to the others.
#
# I am using a Kruskal-Wallis test because I am comparing more than 3 independent groups.

# In[48]:


stats.kruskal(black_vic, white_vic, native_vic, asian_vic)


# Because the p-value is so low, this suggets the differences between the racial groups is statistically significant, and not a result of random chance.
#
# The data set and corresponding p-value suggests that there is a statistical difference between racial groups and their victim rates, a rejection of the null hypothesis that there is no difference between groups.

# # Incidents by Perpetrator Race

# ### Incidents concerning Black perpetrators

# In[49]:


black_incidents = fbi[fbi['Perpetrator Race'] == 'Black']

x = black_incidents.groupby('Year').count()['Crime Solved']

y = us_pop['black_population']

black_perp = [i/j * 100000 for i,j in zip(x, y)]
plt.plot(years, black_perp, 'x-', color='black')
plt.title('Number of Perpetrators per 100,000 Black people')
plt.ylabel('Perpetrators per 100,000')
plt.tight_layout()


# In[50]:


# calculates the percent difference between the number of victims per 100,000 in the 2010
# and 1980
get_percent_difference(black_perp[-1], black_perp[0])


# ### Incidents concerning White perpetrators

# In[51]:


white_incidents = fbi[fbi['Perpetrator Race'] == 'White']

x = white_incidents.groupby('Year').count()['Crime Solved']

y = us_pop['white_population']

white_perp = [i/j * 100000 for i,j in zip(x, y)]
plt.plot(years, white_perp, 'x-', color='green')
plt.title('Number of Perpetrators per 100,000 White people')
plt.ylabel('Perpetrators per 100,000')
plt.tight_layout()


# In[52]:


# calculates the percent difference between the number of victims per 100,000 in the 2010
# and 1980
get_percent_difference(white_perp[-1], white_perp[0])


# ### Incidents concerning Asian perpetrators

# In[53]:


asian_incidents = fbi[fbi['Perpetrator Race'] == 'Asian/Pacific Islander']

x = asian_incidents.groupby('Year').count()['Crime Solved']

y = us_pop['asian_population']

asian_perp = [i/j * 100000 for i,j in zip(x, y)]

plt.plot(years, asian_perp, 'x-', color='red')
plt.title('Number of Perpetrators per 100,000 Asian people')
plt.ylabel('Perpetrators per 100,000')
plt.tight_layout()


# In[54]:


# calculates the percent difference between the number of victims per 100,000 in the 2010
# and 1980
get_percent_difference(asian_perp[-1], asian_perp[0])


# ### Incidents concerning Native American perpetrators

# In[55]:


native_incidents = fbi[fbi['Perpetrator Race'] == 'Native American/Alaska Native']

x = native_incidents.groupby('Year').count()['Crime Solved']

y = us_pop['native_population']

native_perp = [i/j * 100000 for i,j in zip(x, y)]
plt.plot(years, native_perp, 'x-', color='blue')
plt.title('Number of Perpetrators per 100,000 Native Americans')
plt.ylabel('Perpetrators per 100,000')
plt.tight_layout()


# In[56]:


# calculates the percent difference between the number of victims per 100,000 in the 2010
# and 1980
get_percent_difference(native_perp[-1], native_perp[0])


# According to the data set, the rate of black perpetrators has, historically, been much higher than other racial groups. Fortunately, the perpetrator rates across all races have decreased in the same manner that victim rates have.

# ### Statistical Significance

# To test for statistical significance, I use a non-parametric test because the values that I am testing for do not conform to a normal distribution. The statistical test checks for the probability that sample data collected comes from the same distribution.
#
# The distributions are plotted as a times series with a span of 34 years. Each point of the time series represents a state. The time series for each race is compared to the others.
#
# I am using a Kruskal-Wallis test because I am comparing more than 3 independent groups.

# In[57]:


stats.kruskal(black_perp, white_perp, asian_perp, native_perp)


# Because the p-value is so low, this suggets the differences between the racial groups is statistically significant, and not a result of random chance.
#
# The data set and corresponding p-value suggests that there is a statistical difference between racial groups and their perpetrator rates, a rejection of the null hypothesis that there is no difference between groups.

# # Final Thoughts

# Handguns are the most popular means to commit a crime. Acquaintances and strangers are the most likely to commit a crime. The average perpetrator is slightly older than the average victim. Black people are the most victimized racial group, but they are also the the most likely perpetrator.
#
# Most importantly, the overall trend of crime has decreased significantly over time.

# # Questions for further Research

# I would like to explore the possible relationship between victim race and perpetrator race. My initial hypothesis is that most crimes are intra-racial (races of the same kind commit crimes against members of the same race). I would also like to probe the data for rates concerning inter-racial crime statistics to see if there are signficant trends that refute my initial thinking.

# I would also like to analyze specific location data looking for correlations with weapons used or most likely relationships that end in a crime.