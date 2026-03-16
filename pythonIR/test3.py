# 2 Level is worked

#Input:
import pandas as pd

df = pd.read_csv("/home/utkarsh/Desktop/Data/data.csv")

filtered_df = df[(
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
print(result)

# Output: For each inner list for &, one separate list is created, followed by or of it. (..&..&..) | (..&..&..) ... is supported structure in parquet

# Actual Output:

import pandas as pd
SO_columns = ["age","remote_worker","experience_years","department","region","salary","joining_date","performance_rating","promotion_eligible"]
SO_c_d_t = {"age":"int64","salary":"int64","experience_years":"int64","department":"category","remote_worker":"category"}
df = pd.read_csv('/home/utkarsh/Desktop/Data/data.csv',usecols=SO_columns,dtype=SO_c_d_t,filters=[[('age', '>', 28), ('remote_worker', '==', true), ('experience_years', '>', 10)], [('department', '==', HR), ('region', '==', 'West'), ('region', '==', 'South'), ('salary', '>', 60000)], [('joining_date', '<', '2020-01-01'), ('performance_rating', '>=', 4.5), ('remote_worker', '==', True), ('promotion_eligible', '==', true)]])

filtered_df = df[(((((df['age'] > 28) & (df['remote_worker'] == True)) & (df['experience_years'] > 10)) | (((df['department'] == 'HR') & df['region'].isin(['West','South'])) & (df['salary'] > 60000))) | ((((df['joining_date'] < '2020-01-01') & (df['performance_rating'] >= 4.5)) & (df['remote_worker'] == True)) & (df['promotion_eligible'] == True)))]
result = filtered_df.groupby('department')['salary'].mean().reset_index()
print(result)