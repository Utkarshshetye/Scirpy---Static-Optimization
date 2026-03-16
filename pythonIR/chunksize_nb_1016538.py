
import pandas as pd
from sklearn.grid_search import RandomizedSearchCV
from sklearn.linear_model import LinearRegression
from sklearn.cross_validation import train_test_split
import numpy as np
from sklearn.cross_validation import cross_val_score
import matplotlib
import seaborn as sb
import statsmodels.api as sm
import statsmodels.formula.api as smf


def regress(data, yvar, xvars):
    Y = data[yvar]
    X = data[xvars]
    X['intercept'] = 1.
    result = sm.OLS(Y, X).fit()
    return result.params

table_split = {'sales_prd': ['Semana','Agencia_ID','Canal_ID','Ruta_SAK','Cliente_ID','Producto_ID'],
              'sales_rtn':['Semana','Venta_uni_hoy','Venta_hoy','Dev_uni_proxima','Dev_proxima', 'Demanda_uni_equil']}

store = pd.HDFStore('../data/mystore.h5') 
model = LinearRegression()

byweek = store.select('df',column=['Producto_ID']).groupby('Semana')

byweek.apply(regress, 'Producto_ID', ['Venta_uni_hoy'])
tempdf = pd.read_hdf('../data/mystore.h5', chunksize = 20000, where =0 "Producto_ID = 34053 or Producto_ID = 1146     or Producto_ID = 41938 or Producto_ID = 2095 or Producto_ID = 8766 or Producto_ID = 30227")
df = pd.concat(tempdf)
df = df.rename(columns = {'Semana':'WeekNum','Venta_uni_hoy':'SaleUnits','Venta_hoy':'SaleNum',                'Dev_uni_proxima':'ReturnUnits','Dev_proxima':'ReturnNum', 'Demanda_uni_equil':'Demand',                'Agencia_ID':'SalesDepotId','Canal_ID':'SalesChannelId','Ruta_SAK':'RouteId','Cliente_ID':'ClientId',                'Producto_ID':'ProductId'})

dfsubset = pd.DataFrame()
dfsubset['SaleUnits'] = df['SaleUnits']
dfsubset['WeekNum'] = df['WeekNum']
dfsubset['ReturnUnits'] = df['ReturnUnits']

saletrain, saletest, saleLabelTrain, saleLabelTest = train_test_split(dfsubset, dfsubset['ReturnUnits'], test_size = 0.4)    
get_ipython().magic(u'time print("%.16f" % np.sqrt(-cross_val_score(model, saletrain, saleLabelTrain, cv=10, scoring=\'mean_squared_error\')).mean())')

import pandas as pd
from sklearn.linear_model import LinearRegression
from sklearn.cross_validation import cross_val_score
import numpy as np

model = LinearRegression()
tempdf = pd.read_hdf('../data/mystore.h5', chunksize = 20000, columns = "['Semana','Venta_uni_hoy','Dev_uni_proxima','Canal_ID']")
df = pd.concat(tempdf)
df = df.rename(columns = {'Semana':'WeekNum','Venta_uni_hoy':'SaleUnits',                'Dev_uni_proxima':'ReturnUnits','Canal_ID':'SalesChannelId'})
print(df.columns)
dftest = pd.read_csv('../data/test.csv', usecols=['id','Semana','Canal_ID'], header = 0)
dftest.rename(columns = {'Semana':'WeekNum','Canal_ID':'SalesChannelId'}, inplace=True)

dftest['SaleUnits'] = pd.Series()
dftest['ReturnUnits'] = pd.Series()

features = ['WeekNum','SalesChannelId']
X_train = df[features]
Y_train = df['SaleUnits']
model.fit(X_train, Y_train)
X_test = dftest[features]
Y_test = dftest['SaleUnits']
pred = model.predict(X_test)
pred

