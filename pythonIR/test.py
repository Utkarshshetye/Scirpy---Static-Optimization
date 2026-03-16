#NOTE: Predicate Test-1

import pandas as pd

# Expressions:

# # NOTE: variable mutated inside loop, skip
# # Expected: filters=None
# threshold2 = 30
# df = pd.read_csv('data.csv')
# for i in range(n):
#     df = df[df['age'] > threshold2]
#     threshold2 = threshold2 + 1
# print(df)

# # NOTE: loop iteration variable i used directly, skip
# # Expected: filters=None
# df = pd.read_csv('data.csv')
# for i in range(n):
#     df = df[df['age'] > i]
# print(df)


# # NOTE: variable assigned by function call, skip
# # Expected: filters=None
# threshold3 = get_threshold()
# df = pd.read_csv('data.csv')
# df = df[df['age'] > threshold3]
# print(df)


#
# # NOTE: one operand of expression unknown, skip whole expression
# # Expected: filters=None
# x4 = 6
# df = pd.read_csv('data.csv')
# df = df[df['age'] > x4 * y]
# print(df)


# # NOTE: multiple chained constant filters
# # Expected: filters=[[(salary, >, 50000), (city, ==, 'NY')]]
# df = pd.read_csv('data.csv')
# df = df[df['salary'] > 50000]
# df = df[df['city'] == 'NY']
# print(df)


# # NOTE: constant inside if/else
# # Expected: filters=[[(age, >, 30)], [(city, ==, 'NY')]]
# df = pd.read_csv('data.csv')
# if x > 5:
#     df = df[df['age'] > 30]
# else:
#     df = df[df['city'] == 'NY']
# print(df)


# Loops

# NOTE: for inside while
# Expected: filters=[[(age, >, 30)]]
# df = pd.read_csv('data.csv')
# x=1
# while x<6:
#     for i in range(n):
#         x = x + 1
# df = df[df['age'] > 30]
# print(df)

# NOTE: for inside while — inner for touches df
# Expected: filters=[[(dept, ==, 'HR')]]
# df = pd.read_csv('data.csv')
# x=1
# while x<6:
#     for i in range(n):
#         df = df[df['age'] > 30]
# df = df[df['dept'] == 'HR']
# print(df)

# NOTE: while inside for # Expected: filters=[[(age, >, 30)]]
# df = pd.read_csv('data.csv')
# for i in range(n):
#     x=1
#     while x<6:
#         x = x + 1
# df = df[df['age'] > 30]
# print(df)

# NOTE: variable defined before loop, loop doesn't touch it, filter after loop
# Expected: filters=[[(age, >, 30)]]
# threshold4 = 30
# df = pd.read_csv('data.csv')
# for i in range(n):
#     x4 = x4 + 1          # threshold NOT touched inside loop
# df = df[df['age'] > threshold4]
# print(df)

# NOTE: variable defined before loop, filter INSIDE loop
# Expected: filters=None  (loop may not execute)
# threshold5 = 30
# df = pd.read_csv('data.csv')
# for i in range(n):
#     df = df[df['age'] > threshold5]
# print(df)



# Test_2
# df = pd.read_csv('a.csv')
# df = df[df['age'] > 30]
# print(df)
# df = pd.read_csv('b.csv')
# df = df[df['city'] == 'NY']
# print(df)

# NOTE: Loop [filter] -> print -> Filter

# df = pd.read_csv('data.csv')
# for i in range(n):
#     df = df[df['age'] > 30]
# print(df)
# df = df[df['city'] == 'NY']
# print(df)

#NOTE: for, if statements

# df = pd.read_csv('data.csv')
# df = df[(df['dept'] == 'HR')]
# x = 10
# for i in range(n):
#     df = df[(df['age'] > 30)]
# if (x > 5):
#     df = df[(df['city'] == 'NY')]
# else:
#     df = df[df['salary'] > 30000]
# print(df)

# NOTE: if-else inside for
# df = pd.read_csv('data.csv')
# for i in range(n):
#     x=10
#     if x > 5:
#         df = df[df['age'] > 30]
#     else:
#         df = df[df['city'] == 'NY']
# df = df[df['dept'] == 'HR']
# print(df)

#NOTE: condition inside loop, predicate after loop

# df = pd.read_csv('data.csv')
# df = df[df['salary'] > 50000]
# for i in range(n):
#     df = df[df['age'] > 30]
#     print(df)
# df = df[df['city'] == 'NY']
# print(df)


# NOTE: Complex Branching
# import pandas as pd
# df = pd.read_csv('customers.csv')
#
# if segment_type == 'age':
#     if age_group == 'young':
#         df1 = df[df['age'] < 30]
#     else:
#         df1 = df[df['age'] >= 30]
# else:
#     if value_group == 'high':
#         df1 = df[df['lifetime_value'] > 10000]
#     else:
#         df1 = df[df['lifetime_value'] <= 10000]
#
# print(df1)

# 2
# import pandas as pd
# df = pd.read_csv('data.csv')
# df = df[df['active'] == True]
#
# if x > 0:
#     df = df[df['age'] > 30]
#     df = df[df['salary'] > 50000]
#     print(df)
# else:
#     df = df[df['age'] < 25]
#     print(df)
#
# df = df[df['dept'] == 'HR']
# print(df)

# 3
# import pandas as pd
# df = pd.read_csv('data.csv')
# x = 2
#
# if x > 0:
#     df1 = df[df['age'] > 30]
#     print(df1)
# else:
#     df2 = df[df['salary'] > 50000]
#     print(df2)
#
# df = df[df['active'] == True]
# df = df[df['dept'] == 'HR']
# print(df)

####################### END PPA ###############################


results = []

cols_to_loop = ['TransactionAmt', 'D15', 'id_02']

for col in cols_to_loop:

    # If the sum is large, we do a complex calculation involving mean and std
    if train[col].sum() > 100000:

        # Subexpression 1: (train[col] - train[col].mean()) / (train[col].std() + 1e-5)
        # occurs here in the IF block
        s1 = (train[col] - train[col].mean()) / (train[col].std() + 1e-5)

        # Do something specific to the IF block
        res = s1 + np.log1p(np.abs(train[col].max()))
        results.append(res)

    else:

        # Subexpression 1: EXACTLY the same expression
        # occurs here in the ELSE block
        s1 = (train[col] - train[col].mean()) / (train[col].std() + 1e-5)

        # Do something smpecific to the ELSE block
        res = s1 - np.log1p(np.abs(train[col].min()))
        results.append(res)

if len(results) > 0:
    train['test14_out'] = sum(results)

# f1 = train['TransactionAmt'].diff(1)
# f2 = train['TransactionAmt'].shift(1)
# f3 = train['TransactionAmt'].diff(1) / (train['TransactionAmt'].shift(1) + 1e-5)
#
# f4 = train['D15'].diff(1)
# f5 = train['D15'].shift(1)
# f6 = train['D15'].diff(1) / (train['D15'].shift(1) + 1e-5)
#
# f7 = (train['TransactionAmt'].diff(1) / (train['TransactionAmt'].shift(1) + 1e-5)) * (train['D15'].diff(1) / (train['D15'].shift(1) + 1e-5))
#
# train['test10_out'] = f1.fillna(0) + f2.fillna(0) + f3.fillna(0) + f4.fillna(0) + f5.fillna(0) + f6.fillna(0) + f7.fillna(0)
# f1 = train.groupby('card1')['TransactionAmt'].transform('std') / (train.groupby('card1')['TransactionAmt'].transform('mean') + 1e-5)
#
# # 2. Distance from sum of group
# f2 = train['TransactionAmt'] / (train.groupby('card1')['TransactionAmt'].transform('sum') + 1e-5)
#
# # 3. Min vs Mean
# f3 = train.groupby('card1')['TransactionAmt'].transform('mean') - train.groupby('card1')['TransactionAmt'].transform('min')
#
# # 4. Max vs Mean
# f4 = train.groupby('card1')['TransactionAmt'].transform('max') - train.groupby('card1')['TransactionAmt'].transform('mean')
#
# # 5. Composite index
# f5 = (train['TransactionAmt'] - train.groupby('card1')['TransactionAmt'].transform('min')) / (train.groupby('card1')['TransactionAmt'].transform('max') - train.groupby('card1')['TransactionAmt'].transform('min') + 1e-5) \
#      * (train.groupby('card1')['TransactionAmt'].transform('sum') / (train.groupby('card1')['TransactionAmt'].transform('count') + 1e-5))
#
# # 6. Smooth standard scale
# f6 = (train['TransactionAmt'] - train.groupby('card1')['TransactionAmt'].transform('mean')) / (train.groupby('card1')['TransactionAmt'].transform('std') + train.groupby('card1')['TransactionAmt'].transform('mean') * 0.1 + 1e-5)
#
# # 7. Another composite log
# f7 = np.log1p(np.abs(
#     train.groupby('card1')['TransactionAmt'].transform('max') * train.groupby('card1')['TransactionAmt'].transform('min')
#     + train.groupby('card1')['TransactionAmt'].transform('std') * train.groupby('card1')['TransactionAmt'].transform('mean')
# ))
#
# # 8. Fractional contribution to sum average
# f8 = train.groupby('card1')['TransactionAmt'].transform('mean') * train.groupby('card1')['TransactionAmt'].transform('count') / (train.groupby('card1')['TransactionAmt'].transform('sum') + 1e-5)
#
# train['test5_out'] = f1 + f2 + f3 + f4 + f5 + f6 + f7 + f8

# Feature 1: range
# f1 = train.groupby('card1')['TransactionAmt'].transform('max') - train.groupby('card1')['TransactionAmt'].transform('min')
# # Feature 2: relative to min
# f2 = (train['TransactionAmt'] - train.groupby('card1')['TransactionAmt'].transform('min')) / (train.groupby('card1')['TransactionAmt'].transform('max') - train.groupby('card1')['TransactionAmt'].transform('min') + 1e-5)
# # Feature 3: relative to max
# f3 = (train['TransactionAmt'] - train.groupby('card1')['TransactionAmt'].transform('max')) / (train.groupby('card1')['TransactionAmt'].transform('max') - train.groupby('card1')['TransactionAmt'].transform('min') + 1e-5)
#
# # Feature 4: D15 range
# f4 = train.groupby('card1')['D15'].transform('max') - train.groupby('card1')['D15'].transform('min')
# # Feature 5: D15 relative to min
# f5 = (train['D15'] - train.groupby('card1')['D15'].transform('min')) / (train.groupby('card1')['D15'].transform('max') - train.groupby('card1')['D15'].transform('min') + 1e-5)
#
# # Feature 6: Cross product of ranges
# f6 = (train.groupby('card1')['TransactionAmt'].transform('max') - train.groupby('card1')['TransactionAmt'].transform('min')) * (train.groupby('card1')['D15'].transform('max') - train.groupby('card1')['D15'].transform('min'))
#
# # Feature 7: Ratio of Max
# f7 = train.groupby('card1')['TransactionAmt'].transform('max') / (train.groupby('card1')['D15'].transform('max') + 1e-5)
#
# train['f1_test2'] = f1 + f2 + f3 + f4 + f5 + f6 + f7


# f1 = (train['TransactionAmt'] - train.groupby('card1')['TransactionAmt'].transform('mean')) / (train.groupby('card1')['TransactionAmt'].transform('std') + 1e-5)
#     # Feature 2
# f2 = ((train['TransactionAmt'] - train.groupby('card1')['TransactionAmt'].transform('mean')) / (train.groupby('card1')['TransactionAmt'].transform('std') + 1e-5)) ** 2
# # Feature 3
# f3 = np.log1p(np.abs((train['TransactionAmt'] - train.groupby('card1')['TransactionAmt'].transform('mean')) / (train.groupby('card1')['TransactionAmt'].transform('std') + 1e-5)))
#
# # Feature 4
# f4 = (train['id_02'] - train.groupby('card1')['id_02'].transform('mean')) / (train.groupby('card1')['id_02'].transform('std') + 1e-5)
# # Feature 5
# f5 = ((train['id_02'] - train.groupby('card1')['id_02'].transform('mean')) / (train.groupby('card1')['id_02'].transform('std') + 1e-5)) ** 2
# # Feature 6
# f6 = np.log1p(np.abs((train['id_02'] - train.groupby('card1')['id_02'].transform('mean')) / (train.groupby('card1')['id_02'].transform('std') + 1e-5)))
#
# # Feature 7
# f7 = train.groupby('card1')['TransactionAmt'].transform('mean') / (train.groupby('card1')['id_02'].transform('mean') + 1e-5)
# # Feature 8
# f8 = train.groupby('card1')['TransactionAmt'].transform('std') / (train.groupby('card1')['id_02'].transform('std') + 1e-5)
#
# # Feature 9
# f9 = train.groupby('card1')['TransactionAmt'].transform('mean') * train.groupby('card1')['id_02'].transform('mean')
#
# # Feature 10
# f10 = train.groupby('card1')['TransactionAmt'].transform('std') * train.groupby('card1')['id_02'].transform('std')
#
# train['final_combined_feature_orig'] = f1 + f2 + f3 + f4 + f5 + f6 + f7 + f8 + f9 + f10

# train = pd.read_csv("train.csv")
# test = pd.read_csv("test.csv")
#
# # --- repeated groupby(card1) ---
# train['A1'] = train['TransactionAmt'] / train.groupby(['card1'])['TransactionAmt'].transform('mean')
# train['A2'] = train['TransactionAmt'] / train.groupby(['card1'])['TransactionAmt'].transform('mean')
# train['A3'] = train['TransactionAmt'] / train.groupby(['card1'])['TransactionAmt'].transform('std')
#
# train['B1'] = train['id_02'] / train.groupby(['card1'])['id_02'].transform('mean')
# train['B2'] = train['id_02'] / train.groupby(['card1'])['id_02'].transform('mean')
# train['B3'] = train['id_02'] / train.groupby(['card1'])['id_02'].transform('std')
#
# # --- repeated groupby(card4) ---
# train['C1'] = train['D15'] / train.groupby(['card4'])['D15'].transform('mean')
# train['C2'] = train['D15'] / train.groupby(['card4'])['D15'].transform('mean')
# train['C3'] = train['D15'] / train.groupby(['card4'])['D15'].transform('std')
#
# # --- repeated addr groupby ---
# train['D1'] = train['D15'] / train.groupby(['addr1'])['D15'].transform('mean')
# train['D2'] = train['D15'] / train.groupby(['addr1'])['D15'].transform('std')
# train['D3'] = train['D15'] / train.groupby(['addr1'])['D15'].transform('mean')
#
# # --- repeated string splits ---
# train[['P1','P2','P3']] = train['P_emaildomain'].str.split('.', expand=True)
# train[['P4','P5','P6']] = train['P_emaildomain'].str.split('.', expand=True)
#
# train[['R1','R2','R3']] = train['R_emaildomain'].str.split('.', expand=True)
# train[['R4','R5','R6']] = train['R_emaildomain'].str.split('.', expand=True)

# PPA

# train = pd.read_csv('/home/utkarsh/Latest/trai.csv')
# test = pd.read_csv('/home/utkarsh/Latest/tes.csv')
#
# train['TransactionAmt_to_mean_card1'] = train['TransactionAmt'] / train.groupby(['card1'])['TransactionAmt'].transform('std')
# train['TransactionAmt_to_std_card1'] = train['TransactionAmt'] / train.groupby(['card1'])['TransactionAmt'].transform('std')
# train['TransactionAmt_to_std_card4'] = train['TransactionAmt'] / train.groupby(['card4'])['TransactionAmt'].transform('std')
# test['TransactionAmt_to_mean_card1'] = test['TransactionAmt'] / test.groupby(['card1'])['TransactionAmt'].transform('mean')
# test['TransactionAmt_to_mean_card4'] = test['TransactionAmt'] / test.groupby(['card4'])['TransactionAmt'].transform('mean')
# test['TransactionAmt_to_std_card1'] = test['TransactionAmt'] / test.groupby(['card1'])['TransactionAmt'].transform('std')
# test['TransactionAmt_to_std_card4'] = test['TransactionAmt'] / test.groupby(['card4'])['TransactionAmt'].transform('mean')
# train['id_02_to_mean_card1'] = train['id_02'] / train.groupby(['card1'])['id_02'].transform('mean')
# train['id_02_to_mean_card4'] = train['id_02'] / train.groupby(['card4'])['id_02'].transform('mean')
# train['id_02_to_std_card1'] = train['id_02'] / train.groupby(['card1'])['id_02'].transform('std')
# train['id_02_to_std_card4'] = train['id_02'] / train.groupby(['card4'])['id_02'].transform('std')
# train['D15_to_mean_card1'] = train['D15'] / train.groupby(['card1'])['D15'].transform('mean')
# train['D15_to_mean_card4'] = train['D15'] / train.groupby(['card4'])['D15'].transform('mean')
# train['D15_to_std_card1'] = train['D15'] / train.groupby(['card1'])['D15'].transform('std')
# train['D15_to_std_card4'] = train['D15'] / train.groupby(['card4'])['D15'].transform('std')
# test['D15_to_mean_card1'] = test['D15'] / test.groupby(['card1'])['D15'].transform('mean')
# test['D15_to_mean_card4'] = test['D15'] / test.groupby(['card1'])['D15'].transform('mean')
# test['D15_to_std_card1'] = test['D15'] / test.groupby(['card1'])['D15'].transform('std')
# test['D15_to_std_card4'] = test['D15'] / test.groupby(['card4'])['D15'].transform('std')
# train['D15_to_mean_addr1'] = train['D15'] / train.groupby(['addr1'])['D15'].transform('mean')
# train['D15_to_mean_addr2'] = train['D15'] / train.groupby(['addr2'])['D15'].transform('mean')
# train['D15_to_std_addr1'] = train['D15'] / train.groupby(['addr1'])['D15'].transform('std')
# train['D15_to_std_addr2'] = train['D15'] / train.groupby(['addr2'])['D15'].transform('std')
# test['D15_to_mean_addr1'] = test['D15'] / test.groupby(['addr1'])['D15'].transform('mean')
# test['D15_to_mean_addr2'] = test['D15'] / test.groupby(['addr2'])['D15'].transform('mean')
# test['D15_to_std_addr1'] = test['D15'] / test.groupby(['addr1'])['D15'].transform('std')
# test['D15_to_std_addr2'] = test['D15'] / test.groupby(['addr2'])['D15'].transform('std')
# train[['P_emaildomain_1', 'P_emaildomain_2', 'P_emaildomain_3']] = train['P_emaildomain'].str.split('.', expand=True)
# train[['R_emaildomain_1', 'R_emaildomain_2', 'R_emaildomain_3']] = train['R_emaildomain'].str.split('.', expand=True)
# test[['P_emaildomain_1', 'P_emaildomain_2', 'P_emaildomain_3']] = test['P_emaildomain'].str.split('.', expand=True)
# test[['R_emaildomain_1', 'R_emaildomain_2', 'R_emaildomain_3']] = test['R_emaildomain'].str.split('.', expand=True)





# import pandas as pd
# df = pd.read_csv('data.csv')
# df = pd.read_csv('data.csv')
# X=2
# if X>5:
#     print(df)
#     df1 = df[df['age'] > 30]
# else:
#     df2 = df[df['age'] < 25]


# import pandas as pd
# df = pd.read_csv('data.csv')
# df.to_csv('output.csv')
# df1 = df[df['age'] > 30]

# df = pd.read_csv('customers.csv')
#
# if segment_type == 'age':
#     if age_group == 'young':
#         df1 = df[df['age'] < 30]
#     else:
#         df1 = df[df['age'] >= 30]
# else:
#     if value_group == 'high':
#         df1 = df[df['lifetime_value'] > 10000]
#     else:
#         df1 = df[df['lifetime_value'] <= 10000]
#
# print(df1)

# NOTE: -- 1
# import pandas as pd
# df = pd.read_csv('data.csv')
# df = df[df['age'] > 30]
# df = df[df['salary'] > 50000]
# df = df[df['active'] == True]
# df = df[df['dept'] == 'HR']
# df = df[df['experience'] > 5]
# print(df)

# NOTE: --2

# import pandas as pd
# df = pd.read_csv('data.csv')
# x = 2
#
# if x > 0:
#     df1 = df[(df['age'] > 30) & (df['status'] == 'active')]
#     print(df1)
# else:
#     df2 = df[(df['age'] > 30) & (df['dept'] == 'HR')]
#     print(df2)
#
# if x > 5:
#     df3 = df[df['salary'] > 40000]
#     print(df3)
# else:
#     df4 = df[df['age'] > 30]
#     print(df4)

# NOTE: --3

# import pandas as pd
# df = pd.read_csv('data.csv')
# x = 2
# y = 3
#
# if x > 0:
#     if y > 0:
#         df1 = df[df['age'] > 30]
#         print(df1)
#     else:
#         df2 = df[df['salary'] > 50000]
#         print(df2)
# else:
#     df3 = df[df['dept'] == 'HR']
#     print(df3)

# NOTE: --3

# import pandas as pd
# df = pd.read_csv('data.csv')
# df_filtered = df[(df['age'] > 30) & (df['salary'] > 50000) & df['remote'] & ~df['married']]
# x = 2
#
# if x > 0:
#     df1 = df[df['status'] == 'active']
#     print(df1)
# else:
#     df3 = df[(df['dept'] == 'ACCNT')]
#     df2 = df_filtered[(df_filtered['dept'] == 'HR')]
#     print(df2)

# NOTE --4

# import pandas as pd
# df = pd.read_csv('data.csv')
# x = 2
#
# if x > 0:
#     df1 = df[df['age'] > 30]
#     print(df1)
# else:
#     df2 = df[df['salary'] > 50000]
#     print(df2)



# NOTE--3

# import pandas as pd
# df = pd.read_csv('data.csv')
# x = 2
# y = 3
# df=df[df['age']>30]
# print(df)
# df = df[df['salary'] > 50000]
# df2 = df[df['salary'] > 50000]
# if x > 0:
#     if y > 0:
#         df1 = df[df['age'] > 30]
#         print(df1)
#     else:
#         df2 = df[df['salary'] > 50000]
#         print(df2)
# else:
#     df3 = df[df['dept'] == 'HR']
#     print(df3)


# NOTE -- 4

# import pandas as pd
# df = pd.read_csv('data.csv')
# x = 2
#
# if x > 0:
#     df1 = df[df['age'] > 30]
# else:
#     df2 = df[df['salary'] > 50000]
# if x > 0:
#     df_final = df1[df1['status'] == 'active']
# else:
#     df_final = df2[df2['status'] == 'active']
# print(df_final)

# NOTE -- 5

# import pandas as pd
# df = pd.read_csv('data.csv')
# x = 2
#
# if x > 0:
#     df1 = df[df['age'] > 30]
# else:
#     df2 = df[df['salary'] > 50000]
#
# if x > 5:
#     df3 = df[df['dept'] == 'HR']
# else:
#     df4 = df[df['status'] == 'active']
#
# if x > 10:
#     df5 = df[df['location'] == 'NYC']
# else:
#     df6 = df[df['remote'] == True]

# CSE

################## 3 ###################
# import pandas as pd
# import time
# import os
#
# start_time = time.time()
#
# pd.set_option('display.max_columns', None)
#
# train = pd.read_csv('/home/utkarsh/Latest/train.csv')
# test = pd.read_csv('/home/utkarsh/Latest/test.csv')
#
# # Train
# train['ma10'] = train['TransactionAmt'].rolling(10).mean()
# train['std10'] = train['TransactionAmt'].rolling(10).std()
#
# train['upper_band'] = train['TransactionAmt'].rolling(10).mean() + 2 * train['TransactionAmt'].rolling(10).std()
# train['lower_band'] = train['TransactionAmt'].rolling(10).mean() - 2 * train['TransactionAmt'].rolling(10).std()
#
# train['return_to_ma'] = train['TransactionAmt'] / train['TransactionAmt'].rolling(10).mean() - 1
#
# test['ma10'] = test['TransactionAmt'].rolling(10).mean()
# test['std10'] = test['TransactionAmt'].rolling(10).std()
#
# test['upper_band'] = test['TransactionAmt'].rolling(10).mean() + 2 * test['TransactionAmt'].rolling(10).std()
# test['lower_band'] = test['TransactionAmt'].rolling(10).mean() - 2 * test['TransactionAmt'].rolling(10).std()
#
# test['return_to_ma'] = test['TransactionAmt'] / test['TransactionAmt'].rolling(10).mean() - 1
#
# end_time = time.time()
#
# print("Elapased Time: ", end_time - start_time)

################## 2 ###################

# df['feat1'] = df['A'] / df.groupby('key')['A'].transform('mean') + df.groupby('key')['B'].transform('std')
# df['feat2'] = df['A'] / df.groupby('key')['A'].transform('mean') + df.groupby('key')['B'].transform('std')
# df['feat3'] = df.groupby('key')['A'].transform('mean') * df.groupby('key')['B'].transform('std')

################## NOTE 1 ###################
# start_time = time.time()

# train = pd.read_csv('/home/utkarsh/Latest/trai.csv')
# test = pd.read_csv('/home/utkarsh/Latest/tes.csv')
#
# train['TransactionAmt_to_mean_card1'] = train['TransactionAmt'] / train.groupby(['card1'])['TransactionAmt'].transform('mean')
# train['TransactionAmt_to_std_card1'] = train['TransactionAmt'] / train.groupby(['card1'])['TransactionAmt'].transform('std')
# train['TransactionAmt_to_std_card4'] = train['TransactionAmt'] / train.groupby(['card4'])['TransactionAmt'].transform('std')
#
# train['id_02_to_mean_card1'] = train['id_02'] / train.groupby(['card1'])['id_02'].transform('mean')
# train['id_02_to_mean_card4'] = train['id_02'] / train.groupby(['card4'])['id_02'].transform('std')
# train['id_02_to_std_card1'] = train['id_02'] / train.groupby(['card1'])['id_03'].transform('mean')
# train['id_02_to_std_card4'] = train['id_02'] / train.groupby(['card4'])['id_02'].transform('std')
#
# train['D15_to_mean_card1'] = train['D15'] / train.groupby(['card1'])['D15'].transform('mean')
# train['D15_to_mean_card4'] = train['D15'] / train.groupby(['card4'])['D15'].transform('mean')
# train['D15_to_std_card1'] = train['D15'] / train.groupby(['card1'])['D15'].transform('mean')
# train['D15_to_std_card4'] = train['D15'] / train.groupby(['card4'])['D15'].transform('mean')
#
# train['D15_to_mean_addr1'] = train['D15'] / train.groupby(['addr1'])['D15'].transform('mean')
# train['D15_to_mean_addr2'] = train['D15'] / train.groupby(['addr2'])['D15'].transform('mean')
# train['D15_to_std_addr1'] = train['D15'] / train.groupby(['addr1'])['D15'].transform('mean')
# train['D15_to_std_addr2'] = train['D15'] / train.groupby(['addr2'])['D15'].transform('mean')
#
# test['TransactionAmt_to_mean_card1'] = test['TransactionAmt'] / test.groupby(['card1'])['TransactionAmt'].transform('mean')
# test['TransactionAmt_to_mean_card4'] = test['TransactionAmt'] / test.groupby(['card4'])['TransactionAmt'].transform('mean')
# test['TransactionAmt_to_std_card1'] = test['TransactionAmt'] / test.groupby(['card1'])['TransactionAmt'].transform('std')
# test['TransactionAmt_to_std_card4'] = test['TransactionAmt'] / test.groupby(['card4'])['TransactionAmt'].transform('std')
#
# test['D15_to_mean_card1'] = test['D15'] / test.groupby(['card1'])['D15'].transform('mean')
# test['D15_to_mean_card4'] = test['D15'] / test.groupby(['card4'])['D15'].transform('mean')
# test['D15_to_std_card1'] = test['D15'] / test.groupby(['card1'])['D15'].transform('std')
# test['D15_to_std_card4'] = test['D15'] / test.groupby(['card4'])['D15'].transform('std')
#
# test['D15_to_mean_addr1'] = test['D15'] / test.groupby(['addr1'])['D15'].transform('mean')
# test['D15_to_mean_addr2'] = test['D15'] / test.groupby(['addr2'])['D15'].transform('mean')
# test['D15_to_std_addr1'] = test['D15'] / test.groupby(['addr1'])['D15'].transform('std')
# test['D15_to_std_addr2'] = test['D15'] / test.groupby(['addr2'])['D15'].transform('std')

# print(train)
# print(test)
#
# elapsed_time = time.time() - start_time
#
# print('elapsed time:', elapsed_time)


#NOTE 2:

# import pandas as pd
# import numpy as np
# import time
#
# train = pd.DataFrame({
#     'card1': np.random.randint(0, 100, 1000),
#     'TransactionAmt': np.random.rand(1000) * 100,
#     'id_02': np.random.rand(1000) * 1000,
#     'D15': np.random.rand(1000) * 500
# })
#
# a = train['TransactionAmt']
# b = train['id_02']
# c = train['D15']
# d = train['card1']
#
# train['metric_1'] = (a * b) / (c + d) + (a * b).mean()
# train['metric_2'] = (a * b) / (c + d) - (a * b).std()
# train['metric_3'] = ((a * b) / (c + d)) * 1.5
#
# train['id_str'] = train['id_02'].astype(str).str.zfill(10)
# train['id_prefix'] = train['id_02'].astype(str).str.zfill(10).str[:3]
# train['id_suffix'] = train['id_02'].astype(str).str.zfill(10).str[-3:]
#
# train['roll_mean_7'] = train['TransactionAmt'].rolling(window=7).mean()
# train['roll_mean_7_norm'] = train['TransactionAmt'] / train['TransactionAmt'].rolling(window=7).mean()
# train['roll_mean_7_diff'] = train['TransactionAmt'] - train['TransactionAmt'].rolling(window=7).mean()
#
# heavy_filter = (train['TransactionAmt'] > 50) & (train['id_02'] < 500)
# train['flag_heavy'] = heavy_filter.map({True: 1, False: 0})
# train['heavy_amt'] = train.loc[heavy_filter, 'TransactionAmt']
# train['heavy_d15'] = train.loc[heavy_filter, 'D15']
#
# print(train.head())
#
# elapsed_time = time.time() - start_time
#
# print('elapsed time:', elapsed_time)
