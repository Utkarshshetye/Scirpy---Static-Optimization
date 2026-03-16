
import numpy as np
import pandas as pd
SO_c_d_t = {"userId":"int64","movieId":"int64","rating":"float32","timestamp":"int64"}
ratings_data = pd.read_csv("/home/bhushan/intellijprojects/scirpy_benchmarks/data/ratings.csv",dtype=SO_c_d_t)
ratings_data.head()
movie_names = pd.read_csv("/home/bhushan/intellijprojects/scirpy_benchmarks/data/movies.csv")
movie_names.head()
movie_data = pd.merge(ratings_data,movie_names,on="movieId")
movie_data.info()
movie_data.groupby("title")["rating"].mean().head()
movie_data.groupby("title")["rating"].mean().sort_values(ascending=False).head()
movie_data.groupby("title")["rating"].count().sort_values(ascending=False).head()
ratings_mean_count = pd.DataFrame(movie_data.groupby("title")["rating"].mean())
ratings_mean_count["rating_counts"] = pd.DataFrame(movie_data.groupby("title")["rating"].count())
ratings_mean_count.head()
