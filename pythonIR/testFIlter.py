import pandas as pd
df = pd.read_csv('large.csv')
print (df.Name[1:10] , df.Age[1:10])


# import pandas as pd
# df = pd.DataFrame({'num_legs': [2, 4, 8, 0],'num_wings': [2, 0, 0, 0],'num_specimen_seen': [10, 2, 1, 8], 'name':['falcon', 'dog', 'spider', 'fish']})
# print(df)
#
# df1 = pd.DataFrame({'head': [1,1,1,1],'food': ['v', 'nv', 'v', 'v'], 'name':['falcon', 'dog', 'spider', 'horse']})
# print(df1)
# df = df[df['name'].isin(df1['name'])]
# print(df)
# df1=df1.merge(df,on= 'name')
# print(df1)
