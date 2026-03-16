import pandas as pd

train = pd.DataFrame({'card1': [1, 2, 1], 'TransactionAmt': [100, 200, 300]})

# Large redundant chains
v1 = train.groupby(['card1'])['TransactionAmt'].transform('mean')
v2 = train.groupby(['card1'])['TransactionAmt'].transform('mean')

# Nested but different terminals
v3 = train.groupby(['card1'])['TransactionAmt'].transform('std')
v4 = train.groupby(['card1'])['TransactionAmt'].transform('std')

print(v1, v2, v3, v4)
