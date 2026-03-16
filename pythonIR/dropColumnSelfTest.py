import pandas as pd
a = 1
b = 1
#permalink,company,numEmps,category,city,state,fundedDate,raisedAmt,raisedCurrency,round
df = pd.read_csv('TechCrunchcontinentalUSA.csv')
print(df.permalink,df.company)
if a>b:
    print(df.numEmps,df.category)
else :
    print(df.city)
print(df.state)
print(df.fundedDate)
df.drop(['company','fundedDate','city'],inplace=True,axis=1)
df.drop(['city'],inplace=True,axis=1)
df.drop(['raisedCurrency'],inplace=True,axis=1)
df.drop(['AgencyCode'],inplace=True,axis=1)
df.drop(['round'],inplace=True,axis=1)
