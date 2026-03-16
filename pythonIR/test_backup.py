import pandas as pd
import time
import os

# PPA

# NOTE: --1

# import pandas as pd
# df = pd.read_csv('data.csv')
# x = 2
#
# if x > 0:
#     df1 = df[df['age'] > 30]
#     print(df1)
# else:
#     df2 = df[df['age'] > 30]
#     print(df2)

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
start_time = time.time()

train = pd.read_csv('/home/utkarsh/Latest/train.csv')
test = pd.read_csv('/home/utkarsh/Latest/test.csv')

train['TransactionAmt_to_mean_card1'] = train['TransactionAmt'] / train.groupby(['card1'])['TransactionAmt'].transform('mean')
train['TransactionAmt_to_std_card1'] = train['TransactionAmt'] / train.groupby(['card1'])['TransactionAmt'].transform('std')
train['TransactionAmt_to_std_card4'] = train['TransactionAmt'] / train.groupby(['card4'])['TransactionAmt'].transform('std')

train['id_02_to_mean_card1'] = train['id_02'] / train.groupby(['card1'])['id_02'].transform('mean')
train['id_02_to_mean_card4'] = train['id_02'] / train.groupby(['card4'])['id_02'].transform('mean')
train['id_02_to_std_card1'] = train['id_02'] / train.groupby(['card1'])['id_02'].transform('std')
train['id_02_to_std_card4'] = train['id_02'] / train.groupby(['card4'])['id_02'].transform('std')

# train['D15_to_mean_card1'] = train['D15'] / train.groupby(['card1'])['D15'].transform('mean')
# train['D15_to_mean_card4'] = train['D15'] / train.groupby(['card4'])['D15'].transform('mean')
# train['D15_to_std_card1'] = train['D15'] / train.groupby(['card1'])['D15'].transform('std')
# train['D15_to_std_card4'] = train['D15'] / train.groupby(['card4'])['D15'].transform('std')
#
# train['D15_to_mean_addr1'] = train['D15'] / train.groupby(['addr1'])['D15'].transform('mean')
# train['D15_to_mean_addr2'] = train['D15'] / train.groupby(['addr2'])['D15'].transform('mean')
# train['D15_to_std_addr1'] = train['D15'] / train.groupby(['addr1'])['D15'].transform('std')
# train['D15_to_std_addr2'] = train['D15'] / train.groupby(['addr2'])['D15'].transform('std')
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

print(train)
print(test)

elapsed_time = time.time() - start_time

print('elapsed time:', elapsed_time)
