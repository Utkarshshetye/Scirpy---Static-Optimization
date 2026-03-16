#input
import pandas as pd
df = pd.read_parquet('employees.csv')
df1 = df
dfx = pd.read_parquet('employees.csv')
df4 = pd.read_parquet('employees.csv')
df3 = df4
df_al = pd.read_parquet("employees.csv")
dfy = pd.read_parquet("employees.csv")
df_al = dfy
df_al['age'] = 25
df_al['department'] = df_al['joining_date'] + 20
df_al['remote_worker'] = True
df_al['experience_years'] = 20
df_al['salary'] = df_al['salary'] * 3.80
df_al['performance_rating'] = df_al['performance_rating'] * 2
filtered_df = df_al[(
                            (df_al['department'].isin(['Engineering', 'IT', 'Data'])) &
                            (df_al['age'] > 28) &
                            (df_al['remote_worker'] == True) &
                            (df_al['experience_years'] > 10)
                    )
                    |
                    (
                            (df_al['department'] == 'HR') &
                            (df_al['region'].isin(['West', 'South'])) &
                            (df_al['salary'] > 60000)
                    )
                    |
                    (
                            (df_al['joining_date'] < '2020-01-01') &
                            (df_al['performance_rating'] >= 4.5) &
                            (df_al['remote_worker'] == True) &
                            (df_al['promotion_eligible'] == True)
                    )
                    ]

result = filtered_df.groupby('department')['salary'].mean().reset_index()
df4 = df1[df1['salary'] > 60000]
dfx = dfy
df5 = df[df['age'] < 30]
df6 = df3[df3['remote'] == True]

# df1 predicate salary > 60000, should be copied to df
# df predicate age < 30, should be added in read_csv with or of previous
# df_al some filters should be killed, joining_date < '2020-01-01', promotion_eligible == True should remain for df_al filters

# Actual output
import pandas as pd
df = pd.read_parquet('employees.csv',filters=[('age', '<', 30), ('salary', '>', 60000)])
df1 = df
df4 = pd.read_parquet('employees.csv',filters=[('remote', '==', true)])
df3 = df4
df_al = pd.read_parquet('employees.csv',filters=[[('region', 'isin', [['West','South']])], [('joining_date', '<', 2020-01-01), ('promotion_eligible', '==', true)]])
dfy = pd.read_parquet('employees.csv',filters=[[('region', 'isin', [['West','South']])], [('joining_date', '<', 2020-01-01), ('promotion_eligible', '==', true)]])
df_al = dfy
df_al['age'] = 25
df_al['department'] = (df_al['joining_date'] + 20)
df_al['remote_worker'] = True
df_al['experience_years'] = 20
df_al['salary'] = (df_al['salary'] * 3.8)
df_al['performance_rating'] = (df_al['performance_rating'] * 2)
result = filtered_df.groupby('department')['salary'].mean().reset_index()
dfx = dfy
