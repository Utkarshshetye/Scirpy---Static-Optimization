


# import pandas as pd
# df = pd.read_csv('employees.csv')
# df1 = df
# dfx = pd.read_csv('employees.csv')
# df2 = dfx[dfx['department'] == 'IT']
# df4 = pd.read_csv('employees.csv')
# df3 = df4
# df4 = df4[df4['salary'] > 60000]
# df3['salary'] = 0
# df5 = df1[df1['age'] < 30]
# df6 = df3[((df3['remote'] == True) & (df3['salary'] == 0))]
# print(df6)
# print(df5)

# df2 is not used, dfx read_csv should be dropped by either by liveness analysis or predicate

# df4 predicates for read_x should be salary > 60000 | remote == True

# df read_x should have age < 30

# df3 salary column update operation should result in removal of predicate, for salary in example above

import pandas as pd
df = pd.read_csv('./input/annex1.scaled.csv')
df.head()

df1 = pd.read_csv('./input/annex2.scaled.csv')
df1.head()

merge = pd.merge(df, df1, on ='Item Code')
merge.head()
Discount =merge['Discount'].value_counts()
Discount = pd.DataFrame(Discount)
print(Discount)

merge.head()

merge = merge.drop(['Date'],axis=1)

categorical_columns = merge.select_dtypes(include=['object']).columns

for col in categorical_columns:
    merge[col] = merge[col]
