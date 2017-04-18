package ru.surf.course.movierecommendations.models;

import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.table.DatabaseTable;
import java.util.List;

/**
 * Created by sergey on 14.04.17.
 */

@DatabaseTable(tableName = TVShowGenre.TABLE_NAME_TVSHOW_GENRES)
public class TVShowGenre extends Genre {

  public static final String TABLE_NAME_TVSHOW_GENRES = "tvshow_genres";

  public static class RetrofitResult {

    @SerializedName("genres")
    public List<TVShowGenre> tvShowGenres;
  }

}
