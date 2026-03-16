#https://stackoverflow.com/questions/54451746/how-to-properly-use-pandas-vectorization
import pandas as pd

df = pd.DataFrame({'IP': [ '1.0.64.2', '100.23.154.63', '54.62.1.3']})

def compare3rd(ip):
    """Check if the 3dr part of an IP is greater than 100 or not"""
    ip_3rd = ip.str.split('.')[2]
    if int(ip_3rd) > 100:
        return True
    else:
        return False


# This works but very slow
#df['check_results'] = df.IP.apply(lambda x: compare3rd(x))
#print(df)

# This is supposed to be much faster
# But it doesn't work ...
df['check_results_2'] = compare3rd(df['IP'])
print(df)