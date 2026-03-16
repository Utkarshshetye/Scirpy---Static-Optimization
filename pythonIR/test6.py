# Alias Case
import pandas as pd
df = pd.read_parquet('employees.csv')
df1 = df
dfx = pd.read_parquet('employees.csv')
df2 = dfx[dfx['department'] == 'IT']
df4 = pd.read_parquet('employees.csv')
df3 = df4
df_al = pd.read_parquet("employees.csv")
dfy = pd.read_parquet("employees.csv")
df_al = dfy
filtered_df = df_al[(
                            (df['department'].isin(['Engineering', 'IT', 'Data'])) &
                            (df['age'] > 28) &
                            (df['remote_worker'] == True) &
                            (df['experience_years'] > 10)
                    )
                    |
                    (
                            (df['department'] == 'HR') &
                            (df['region'].isin(['West', 'South'])) &
                            (df['salary'] > 60000)
                    )
                    |
                    (
                            (df['joining_date'] < '2020-01-01') &
                            (df['performance_rating'] >= 4.5) &
                            (df['remote_worker'] == True) &
                            (df['promotion_eligible'] == True)
                    )
                    ]

result = filtered_df.groupby('department')['salary'].mean().reset_index()
df4 = df1[df1['salary'] > 60000]
dfx = dfy
df4 = dfx
df5 = df[df['age'] < 30]
df6 = df3[df3['remote'] == True]

# Output: df1 alias for df, df3 for df4, df_al for dfy, dfx foy dfy, and df4 for dfx. df_al filters copied to dfy, df3 filters copied to df4, df1 filters copied to df


import pandas as pd
df = pd.read_parquet('employees.csv',filters=[('age', '<', 30), ('salary', '>', 60000)])
df1 = df
dfx = pd.read_parquet('employees.csv',filters=[('department', '==', IT)])
df4 = pd.read_parquet('employees.csv',filters=[('remote', '==', true)])
df3 = df4
df_al = pd.read_parquet('employees.csv',filters=[[('department', '==', 'Engineering'), ('department', '==', 'IT'), ('department', '==', 'Data'), ('age', '>', 28), ('remote_worker', '==', true), ('experience_years', '>', 10)], [('department', '==', HR), ('region', '==', 'West'), ('region', '==', 'South'), ('salary', '>', 60000)], [('joining_date', '<', 2020-01-01), ('performance_rating', '>=', 4.5), ('remote_worker', '==', true), ('promotion_eligible', '==', true)]])

dfy = pd.read_parquet('employees.csv',filters=[[('department', '==', 'Engineering'), ('department', '==', 'IT'), ('department', '==', 'Data'), ('age', '>', 28), ('remote_worker', '==', true), ('experience_years', '>', 10)], [('department', '==', HR), ('region', '==', 'West'), ('region', '==', 'South'), ('salary', '>', 60000)], [('joining_date', '<', 2020-01-01), ('performance_rating', '>=', 4.5), ('remote_worker', '==', true), ('promotion_eligible', '==', true)]])
df_al = dfy
filtered_df = df_al[(((((df['department'].isin(['Engineering','IT','Data']) & (df['age'] > 28)) & (df['remote_worker'] == True)) & (df['experience_years'] > 10)) | (((df['department'] == 'HR') & df['region'].isin(['West','South'])) & (df['salary'] > 60000))) | ((((df['joining_date'] < '2020-01-01') & (df['performance_rating'] >= 4.5)) & (df['remote_worker'] == True)) & (df['promotion_eligible'] == True)))]
result = filtered_df.groupby('department')['salary'].mean().reset_index()
dfx = dfy
df4 = dfx
