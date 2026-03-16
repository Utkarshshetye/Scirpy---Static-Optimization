def squroot(df):
    result = df.copy()
    for feature in df.columns:
        result[feature] = df[feature] ** 1/2
    return result
def squroot(df):
    result = df.copy()
    for feature in df.columns:
        result[feature] = ((df[feature] ** 1) / 2)
    null



def log(df):
    result = df.copy()
    for feature in df.columns:
        result[feature] = np.log(df[feature])
    return result

def log(df):
    result = df.copy()
    for feature in df.columns:
        result[feature] = np.log(df)
    null




