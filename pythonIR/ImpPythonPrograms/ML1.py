import sys
import pandas as pd
import numpy as np

sys.path = ['/Users/sebastian/github/mlxtend/'] + sys.path
from mlxtend.evaluate import plot_decision_regions


df = pd.read_csv('https://archive.ics.uci.edu/ml/machine-learning-databases/iris/iris.data', header=None)

# setosa and versicolor
y = df.iloc[0:100, 4].values
y = np.where(y == 'Iris-setosa', 0, 1)

# sepal length and petal length
X = df.iloc[0:100, [0,2]].values

# standardize features
X_std = np.copy(X)
X_std[:,0] = (X[:,0] - X[:,0].mean()) / X[:,0].std()
X_std[:,1] = (X[:,1] - X[:,1].mean()) / X[:,1].std()

lr = LogisticRegression(eta=0.1, epochs=100)
lr.fit(X_std, y)

plot_decision_regions(X_std, y, clf=lr, res=0.02)