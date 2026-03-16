# 3 Level is skipped

#Input:
import pandas as pd

df = pd.read_csv("/home/utkarsh/Desktop/Data/data.csv")

filtered_df = df[(
                         (df['department'].isin(['Engineering', 'IT', 'Data'])) &
                         (df['age'] > 28) &
                         (df['experience'] >= 3) &
                         (df['remote'] == True) &
                         ((df['remote_worker'] =='True') | (df['experience_years' > 10]))
                 )
                 |
                 (
                         (df['department'] == 'HR') &
                         (df['region'].isin(['West', 'South'])) &
                         (df['salary'] > 60000) &
                         (df['manager'] == 'Alice')
                 )
                 |
                 (
                         (df['joining_date'] < '2020-01-01') &
                         (df['performance_rating'] >= 4.5) &
                         ((df['remote_worker'] =='True') | (df['experience_years' > 10])) &
                         (df['promotion_eligible'] == True) &
                         ((df['remote_worker'] =='True') | (df['experience_years' > 10]))
                 )
                 ]

result = filtered_df.groupby('department')['salary'].mean().reset_index()
print(result)

# Output: For each inner list for &, one separate list is created, followed by or of it. If & (..|..) then it simply discard the filter, as it is not supported parquet.

# Actual Output:
import pandas as pd
SO_columns = ["department","age","experience","remote","remote_worker","region","salary","manager","joining_date","performance_rating","promotion_eligible"]
SO_c_d_t = {"age":"int64","salary":"int64","department":"category","remote_worker":"category"}

df = pd.read_csv('/home/utkarsh/Desktop/Data/data.csv',usecols=SO_columns,dtype=SO_c_d_t,filters=[[('department', '==', 'Engineering'), ('department', '==', 'IT'), ('department', '==', 'Data'), ('age', '>', 28), ('experience', '>=', 3), ('remote', '==', true)], [('department', '==', HR), ('region', '==', 'West'), ('region', '==', 'South'), ('salary', '>', 60000), ('manager', '==', Alice)], [('joining_date', '<', 2020-01-01), ('performance_rating', '>=', 4.5), ('promotion_eligible', '==', true)]])


filtered_df = df[((((((df['department'].isin(['Engineering','IT','Data']) & (df['age'] > 28)) & (df['experience'] >= 3)) & (df['remote'] == True)) & ((df['remote_worker'] == 'True') | df[('experience_years' > 10)])) | ((((df['department'] == 'HR') & df['region'].isin(['West','South'])) & (df['salary'] > 60000)) & (df['manager'] == 'Alice'))) | (((((df['joining_date'] < '2020-01-01') & (df['performance_rating'] >= 4.5)) & ((df['remote_worker'] == 'True') | df[('experience_years' > 10)])) & (df['promotion_eligible'] == True)) & ((df['remote_worker'] == 'True') | df[('experience_years' > 10)])))]
result = filtered_df.groupby('department')['salary'].mean().reset_index()
print(result)