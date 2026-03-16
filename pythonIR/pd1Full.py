import pandas as pd

ab=['aa','bb',2,3]
df = pd.read_csv('large.csv')
print (df.Name[1:10] , df.Age[1:10])
print("Test string")
df = pd.read_csv('large.csv')
print (df.Address[1:10] , df.Age[1:10])
print(df)
df1= pd.read_csv('large.csv')
print(df1.Address1[1:10], df.Age[1:10])
print(df1.Age1[1:10], df.Name[1:10])
#print(df1.Ag, df.xy, df.za)






