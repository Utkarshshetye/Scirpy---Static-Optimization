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
train[['P_emaildomain_1', 'P_emaildomain_2', 'P_emaildomain_3']] = train['P_emaildomain'].str.split('.', expand=True)
train[['R_emaildomain_1', 'R_emaildomain_2', 'R_emaildomain_3']] = train['R_emaildomain'].str.split('.', expand=True)
test[['P_emaildomain_1', 'P_emaildomain_2', 'P_emaildomain_3']] = test['P_emaildomain'].str.split('.', expand=True)
test[['R_emaildomain_1', 'R_emaildomain_2', 'R_emaildomain_3']] = test['R_emaildomain'].str.split('.', expand=True)