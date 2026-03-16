#!/usr/bin/env python
# coding: utf-8

# # Iteration

# ## iterating strings, lists, dictionaries and file connections
# 
# Within Python there are several special obects which can be iterated over these are called iterables. 
# 
# Iterables include: strings, lists, dictionaries and  file connections.
# 
# Iterables have an associated Iter() method.
# 
# Iterators have an associated next() method, which returns the next value in th iteration.
# 
# The * wildcard will iterate all (or the remaining) values in an iteration.
# 
# To iterate the key values pairs in a dictionary first you need to unpack it with the items() method.
# 
# Iterating a file line by line is much like iterating a list or string with iter().

# In[27]:


# iterate a string
word="Example"
it2=iter(word)
print(next(it2))
print(next(it2))
print("\n")

# iterate a list
numbers=["one", "2","Tres","2squared"]
it=iter(numbers)
print(next(it), ' follows ', next(it), ' then comes ')
print(*it)
print("\n")

# iterate a dictionary
authors={'shakespear':'Romeo and Juliet','Orson Scott Card':'Ender\'s Game'}
for author, book in authors.items():
    print (author," wrote ", book)
print("\n")    
    
# iterate a file
file=open('files/bands.txt')
it3=iter(file)
print(next(it3))
print(next(it3))
print(next(it3))


# ## enumerate()
# 
# Allows you to add a counter to any iterable object.
# 
# This function returns a pair consisting of an object and its index within the iterable.
# 
# The enumerate object itself is also an iterable list, which can be iterated over while being unpacked with a for loop

# In[54]:


# enumerate a list

# create list
movies=['black hawk down', 'gladiator', 'blade runner','american gangster']
e=enumerate(movies)
# check type of result
print(type(e),'\n')
# covert the enumerated list to a list and print it
e_list=list(e)
print(e_list,'\n')

# enumerate and unpack list with a for loop

# create list
albums=['music','like a virgin','madona','erotica','True Blue']
# create for loop , enumerate, unpack and print 
for index,value in enumerate(albums):
    print (index,value)
    
print('\n')    
 
# create for loop , enumerate, unpack and print with a start value of 1 not 0
for index,value in enumerate(albums, start=1):
    print (index,value)


# ## zip()
# 
# Allows you to stich together a number of iterables.
# 
# The result is a list of tuples containing the corresponding elements of each list.
# 
# You can zip and unpack a list using a for loop.

# In[60]:


# stitching 2 lists together

#create list
movies=['Top Gun','Star Wars','The Thomas Crown Affair','Last of the Mohicans']
directors=['Tony Scott','George Lucas','John McTiernan','Michael Mann']
years=('1986','1977','1999','1992')

# zip the lists and check the object type
zipped=zip(movies,directors)
print(type(zipped),'\n')

#convert the zip object to a list and print it
print(list(zipped),'\n')

#using the spat object * zip to print all of the zip elements
zipped=zip(movies,directors) #we need to recreate zipped here because we've already printed it once.
print(*zipped,'\n')

#zip and unpack lists with a for loop

for movie,director in zip(movies,directors):
    print(movie,' directed by ',director)
print ('\n')

#zip and unpack 3 lists with a for loop

for movie,director,year in zip(movies,directors,years):
    print(movie,' directed by ',director,' in ',year)
    


# ## Reversing zip()
# 
# There is no unzip function for doing the reverse of what zip().
# 
# In order to reverse the 

# In[62]:


# create a couple of list
show=['Mash', 'Cheers', 'Friends', 'Last of the Mohicans']
year=('1972','1982','1994','2007')

# Create a zip object from show and year
sitcom = zip(show,year)

# 'Unzip' the tuples in sitcom by unpacking with * and zip(): result1, result2
result1, result2 = zip(*sitcom)

# Check if unpacked tuples are equivalent to original tuples
print(result1,result2)
print(result1 == show)
print(result2 == year)


# # Iterators for big data
# 
# If you have too much data to load it all into memory at the same time you can divide it into chucks, load each chunck into memory and iterate over it and store the results in a file. We can use panda's to do this.
# 
# The pandas read_csv() with the chunksize= argument for this.
# 
# A for loop is used to read in each chunk of data and a second (nested) for loop used to iterate over that data.

# In[ ]:


import pandas as pd

# Initialize an empty dictionary: counts_dict
counts_dict={}

# Iterate over the file chunk by chunk
for chunk in pd.read_csv("files/tweets.csv", chunksize=10):

    # Iterate over the column in DataFrame
    for entry in (chunk)['lang']:
        if entry in counts_dict.keys():
            counts_dict[entry] += 1
        else:
            counts_dict[entry] = 1

# Print the populated dictionary
print(counts_dict)


# In[ ]:


# Define count_entries()
def count_entries(csv_file,c_size,colname):
    """Return a dictionary with counts of
    occurrences as value for each key."""
    
    # Initialize an empty dictionary: counts_dict
    counts_dict = {}

    # Iterate over the file chunk by chunk
    for chunk in pd.read_csv(csv_file, chunksize=c_size):

        # Iterate over the column in DataFrame
        for entry in (chunk)[colname]:
            if entry in counts_dict.keys():
                counts_dict[entry] += 1
            else:
                counts_dict[entry] = 1

    # Return counts_dict
    return counts_dict

# Call count_entries(): result_counts
result_counts = count_entries('tweets.csv',10,'lang')

# Print result_counts
print(result_counts)

