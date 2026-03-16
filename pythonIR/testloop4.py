import pandas as pd
folder_path = './input/'
train_identity = pd.read_csv(f'{folder_path}train_identity.scaled.csv')
train_transaction = pd.read_csv(f'{folder_path}train_transaction.scaled.csv')
test_identity = pd.read_csv(f'{folder_path}test_identity.scaled.csv')
test_transaction = pd.read_csv(f'{folder_path}test_transaction.scaled.csv')
sub = pd.read_csv(f'{folder_path}sample_submission.scaled.csv')
# let's combine the data and work with the whole dataset
train = pd.merge(train_transaction, train_identity, on='TransactionID', how='left')
test = pd.merge(test_transaction, test_identity, on='TransactionID', how='left')

print(f'Train dataset has {train.shape[0]} rows and {train.shape[1]} columns.')
print(f'Test dataset has {test.shape[0]} rows and {test.shape[1]} columns.')
train_transaction.head()

# train_identity

# one_value_cols = [col for col in train.columns if train[col].nunique() <= 1]
# one_value_cols_test = [col for col in test.columns if test[col].nunique() <= 1]
# one_value_cols == one_value_cols_test

_ = train['id_01']

train['id_03'].value_counts(dropna=False, normalize=True).head()
train['id_11'].value_counts(dropna=False, normalize=True).head()
_ = train['id_07']

# for i in ['id_12', 'id_15', 'id_16', 'id_28', 'id_29', 'id_30', 'id_31', 'id_32', 'id_33', 'id_34', 'id_35', 'id_36', 'id_37', 'id_38']:
#     feature_count = train[i].value_counts(dropna=False).reset_index().rename(columns={i: 'count', 'index': i})

many_null_cols = [col for col in train.columns if train[col].isnull().sum() / train.shape[0] > 0.9]

many_null_cols_test = [col for col in test.columns if test[col].isnull().sum() / test.shape[0] > 0.9]
