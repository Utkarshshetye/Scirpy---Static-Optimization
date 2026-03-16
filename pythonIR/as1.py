def norm(df, df2, df3):
    result = df.copy()
    for feature in df.columns:
        max_v = df[feature].max()
        min_v = df[feature].min()
        if max_v!=min_v:
            result[feature] = (df[feature] - min_v) / (max_v - min_v)
    return result

def finalFeature(df):
    result = df.filter(['F6', 'base_time', 'pd_bd'], axis=1)
    return result