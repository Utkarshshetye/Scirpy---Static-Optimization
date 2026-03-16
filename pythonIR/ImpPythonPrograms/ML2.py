import numpy as np
def activation(z, kind):

    implemented = ('linear', 'unit step', 'logistic', 'piecewise linear', 'tanh')
    if kind not in implemented:
        raise AttributeError('%s not in %s' % (kind, implemented))

    if kind == 'unit step':
        return np.where(z >= 0.0, 1, 0)

    elif kind == 'logistic':
        return 1.0 / (1.0 + np.exp(-z))

    elif kind == 'tanh':
        e_p = np.exp(z)
        e_m = np.exp(-z)
        return (e_p - e_m) / (e_p + e_m)

    elif kind == 'piecewise linear':
        if z >= 0.5:
            return 1
        elif z <= -0.5:
            return 0
        else:
            return z + 0.5

    else:
        return z