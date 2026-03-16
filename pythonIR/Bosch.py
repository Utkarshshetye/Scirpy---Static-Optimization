import pandas as pd


def get_positive(chunksize):
    reader_numeric = pd.read_csv('/home/bhushan/Desktop/DataForOptimization/train_numeric.csv', chunksize=chunksize)
    reader_categorical = pd.read_csv('/home/bhushan/Desktop/DataForOptimization/train_categorical.csv', chunksize=chunksize)
    reader_date = pd.read_csv('/home/bhushan/Desktop/DataForOptimization/train_date.csv', chunksize=chunksize)
    reader = zip(reader_numeric, reader_categorical, reader_date)
    first = True
    for numeric, categorical, date in reader:
        categorical.drop('Id', axis=1, inplace=True)
        date.drop('Id', axis=1, inplace=True)
        data = pd.concat([numeric, categorical, date], axis=1)
        positive_data = data[data.Response == 1]
        if first:
            positive = positive_data.copy()
            first = False
        else:
            positive = pd.concat([positive, positive_data])
        print(positive_data.shape, positive.shape)
    return positive

def get_positive2(chunksize):
    reader_numeric = pd.read_csv('/home/bhushan/Desktop/DataForOptimization/train_numeric.csv', chunksize=chunksize)
    reader_categorical = pd.read_csv('/home/bhushan/Desktop/DataForOptimization/train_categorical.csv', chunksize=chunksize)
    reader_date = pd.read_csv('/home/bhushan/Desktop/DataForOptimization/train_date.csv', chunksize=chunksize)
    reader = zip(reader_numeric, reader_categorical, reader_date)
    #reader_categorical.drop(['Id'],inplace=True,axis=1)
    #reader_date.drop('Id', axis=1, inplace=True)
    for numeric, categorical, date in reader:
        data = pd.concat([numeric,categorical , date], axis=1)
        positive_data = data[data.Response == 1]
        print(positive_data.shape)
        #return positive


get_positive2(10000)
