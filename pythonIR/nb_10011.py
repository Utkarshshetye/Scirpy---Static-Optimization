#!/usr/bin/env python
# coding: utf-8

# Linear Regression - Data Cleanup
# ================================

# Here we get into the detailed steps involved in data cleanup of the Lending Club dataset. We glossed over this in the Data Exploration lesson and left it as an exercise.  This is where we actually do the cleanup step by step.

import pandas as pd
from numpy import nan as NA
loansData = pd.read_csv('https://spark-public.s3.amazonaws.com/dataanalysis/loansData.csv')
loansData
ir = loansData['Interest.Rate']
irbak = ir
loansDataBak = loansData
ldb = loansDataBak.reset_index() # explain
irates = ldb['Interest.Rate'][0:]



# In[335]:


srates = ldb['Interest.Rate']


# In[336]:


#nas = [ x for x in srates if x.isnull() ] # AttributeError: 'str' object has no attribute 'isnull'


# In[337]:


nas = [ x for x in srates if not(x[0].isdigit()) ] # AttributeError: 'str' object has no attribute 'isnull'


# In[338]:


len(nas)


# In[339]:


srates[0][:-1]


# In[340]:


float(srates[0][:-1])


# In[341]:


nopct = [ x[:-1] for x in srates ]


# In[342]:


flrates = [float(x) for x in nopct]


# In[343]:


flrates[0:5]


# In[344]:


flrate = map(float, nopct)


# In[345]:





# In[346]:




# In[347]:


ldb


# In[348]:


ldb['Interest.Rate'] = flrate


# In[349]:


ldb[0:5]


# In[350]:


ldb['Interest.Rate'][0:5]


# In[351]:


srates = loansData['Interest.Rate']


# In[352]:


nopct = [ x[:-1] for x in srates ]


# In[353]:


flrates = [float(x) for x in nopct]


# In[354]:


rates = [float(x[:-1]) for x in srates] # use this


# In[355]:


flrates == rates


# In[356]:


loansData['Interest.Rate'] = flrates


# In[357]:


loansData['Interest.Rate'][0:5]


# ### Conclusion of step 1
# * ok! whew! we're done with the % symbol stuff
# * we learnt quite a few things along the way that will be useful in the next part

# ## Step 2: Remove the months

# In[358]:


withmons = ldb['Loan.Length'] 


# In[358]:





# In[359]:


wmons = withmons[0:]


# In[360]:


wmons[0:5]


# In[361]:


wmons


# In[362]:


wmons[0].split()


# In[363]:


wmons[0].split()[0]


# In[364]:


int(wmons[0].split()[0])


# In[365]:


x = wmons[0].split()


# In[366]:


x[0]


# In[367]:


int(x[0])


# In[368]:


intmons = [ int(x.split()[0]) for x in wmons ] 


# In[369]:


intmons[0:10]


# In[370]:


loansData['Loan.Length']


# In[371]:


loansData['Loan.Length'].value_counts()


# ### Conclusion of Step 2
# * Here we used the techniques we learned in Step 1.
#  * Pull out a column from a data frame
#  * Operate on it, perform some transformations
#  * Replace the column in the original dataframe with this new column  
# * We applied them to removing the ' months' suffix in the Loan.Length column.

# ## Step 3: Remove bad data

# In[372]:


loansData['Monthly.Income'].describe()


# First remove implausible values.  We see the max value to be 102750.  
# This is a MONTHLY income of 100K dollars, which is certainly possible, but ....  
# highly implausible for a person seeking a loan of a few 10's of K dollars, i.e. implausible in this context.  

# In[373]:


loansData['Monthly.Income'].idxmax() # find the place where the max occurs


# But there's a better way - a row filter i.e. an expression used as a way to restrict the rows in a dataframe.  
# In our case we want to eliminate rows above 100K dollars. i.e. only keep those less than 100K dollars.

# In[374]:


loansData['Monthly.Income'][loansData['Monthly.Income'] < 100000]


# In[374]:





# In[375]:


loansData['Monthly.Income'].describe()


# In[376]:


ldlt100 = ldb[ldb['Monthly.Income'] < 100000]


# In[377]:


ldlt100


# In[378]:


len(ldlt100)


# Now drop any rows that have 'NA' values ie data not available.  
# In database terminiology these would be 'NULL' values.

# In[379]:


ldb2 = ldlt100.dropna()


# In[380]:


ldb2


# In[381]:


len(ldb2)


# So we dropped one row that had an NA value somewhere.

# ### Conclusion of step 3
# * We used techniques we learned in step 1 to pick out a columns and operate on it
# * We also learned how to filter the data based on expressions involving column values
# * Finally we learnt how to drop NA values

# Now that we have removed the "bad" data, let's take on the final data-cleaning task for this data set - converting the ranges 
# to single integers.

# ## Step 4: Change FICO range to a single value

# Note that the FICO values are given in a range which is in the form of a string that looks like lowerlimit-upperlimit, eg 720-724.  
# We want to convert these values to a single int value representing the lower limit. e.g. 720 in the above example.
# 

# In[382]:


ficostr = ldb2['FICO.Range'] 


# In[383]:


ficostr[0:10]


# In[384]:


ficostr[0]


# In[385]:


ficoint = [ int(x.split('-')[0]) for x in ficostr ]


# In[386]:


ficoint[0:10]


# In[387]:


ldb2['FICO.Range'] = ficoint


# In[388]:


len(ficoint)


# In[389]:


ldb2['FICO.Range']


# #### Conclusion of Step 4

# * We used techniques similar to the ones in Step 1
#  * we picked a column - the FICO.Range column
#  * we split the values on the separator '-'
#  * we picked the fist value, i.e. the lower limit
#  * we converted it to an int

# Now we have a dataset that we can use for our data exploration and analysis

# In[389]:




