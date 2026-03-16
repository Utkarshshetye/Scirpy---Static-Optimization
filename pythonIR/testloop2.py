import pandas as pd
df = pd.read_csv("./input/salary_data.scaled.csv")
df.head(5)
df.duplicated().sum()
df.isnull().sum()
df.describe(include = 'all')
df = df.drop(['wage_span'] , axis = 1)
df.sort_values('continent_name')

sorted_order = (
    df.groupby('continent_name' , as_index = False)
    .agg({'median_salary':'mean'})
    .sort_values('median_salary' , ascending = True)
)['continent_name'].values

tp5_highestslry_contry_conti = (
    df.sort_values('highest_salary' , ascending = False).groupby('continent_name')
    .head(5)
    .sort_values(['continent_name','highest_salary'] , ascending = [True, False])
)

cont_order = (
    df.groupby('continent_name')
    .agg({'highest_salary':"max"})
    .sort_values('highest_salary' , ascending = False)
    .index
).tolist()

for cont in cont_order:
    _ = tp5_highestslry_contry_conti.query('continent_name == @cont')

cont_order = (
    df.groupby('continent_name')
    .agg({'lowest_salary':"min"})
    .sort_values('lowest_salary' , ascending = False)
    .index
).tolist()

tp5_lwstslry_contry_conti = (
    df.sort_values('lowest_salary' , ascending = True).groupby('continent_name')
    .head(5)
    .sort_values(['continent_name','lowest_salary'] , ascending = [True, True])
)

for cont in cont_order:
    _ = tp5_lwstslry_contry_conti.query('continent_name == @cont')