# Alias Case
import pandas as pd
df = pd.read_csv('employees.csv')
df1 = df
dfx = pd.read_csv('employees.csv')
df2 = dfx[dfx['department'] == 'IT']
df4 = pd.read_csv('employees.csv')
df3 = df4
df4 = df1[df1['salary'] > 60000]
# df3['remote'] = False
df5 = df[df['age'] < 30]
df6 = df3[df3['remote'] == True]
print(dfx)
# print(df), skips filters on df, if it is printed

# Output: df1 is alias for df. Any filters applied on df1, should applied to df also. df3 alias for df4. df1['salary'] > 60000, should be applied on df similarly, df3['remote'] == true, applied to df4 also

# df3 alias for df4, df3['remote'] is killed so, filter on remote should also be killed. And for df4 also should be killed.

import pandas as pd
SO_columns = ["age"]
df = pd.read_csv('employees.csv',usecols=SO_columns,filters=[('age', '<', 30), ('salary', '>', 60000)])
df1 = df
SO_columns = ["department"]
dfx = pd.read_csv('employees.csv',usecols=SO_columns)
df2 = dfx[(dfx['department'] == 'IT')]
df4 = pd.read_csv('employees.csv',filters=None)
df3 = df4
# df3['remote'] = False
print(dfx)