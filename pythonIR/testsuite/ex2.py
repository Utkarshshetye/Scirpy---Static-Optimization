import numpy as np
import pandas as pd
#userId,movieId,rating,timestamp
ratings_data = pd.read_csv("/home/bhushan/intellijprojects/scirpy_benchmarks/data/ratings.csv")
ratings_data.head()
#movieId,title,genres
movie_names = pd.read_csv("/home/bhushan/intellijprojects/scirpy_benchmarks/data/movies.csv")
movie_names.head()
movie_data = pd.merge(ratings_data, movie_names, on='movieId')
movie_data.info()