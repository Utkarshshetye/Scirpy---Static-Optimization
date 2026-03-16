#https://www.kaggle.com/mastmustu/pandas-on-steroids-100x-faster-iterations
import pandas as  pd
import numpy as np
import timeit
import_module = "import pandas as pd"
testcode = ''' 
df= pd.DataFrame()
x= list(range(500000))
y= list(range(500000))

df['X'] = x
df['Y'] = y
sum_x = 0
sum_y = 0

for index, rows in df.iterrows():
    sum_x = sum_x  + (rows.X +1)    
    sum_y = sum_y  + (rows.Y +2)
        
print(sum_x)
print(sum_y)
'''

#print(timeit.timeit(stmt=testcode, setup=import_module))
#125000250000
#124999750000

#125000750000
df= pd.DataFrame()
x= list(range(500000))
y= list(range(500000))

df['X'] = x
df['Y'] = y
sum_x = 0
sum_y = 0
sum=df.sum(axis=0)
print(sum)