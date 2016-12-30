package ru.surf.course.movierecommendations.tmdbTasks;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ru.surf.course.movierecommendations.BuildConfig;
import ru.surf.course.movierecommendations.MovieInfo;
import ru.surf.course.movierecommendations.TmdbImage;

/**
 * Created by andrew on 12/30/16.
 */

public class GetImagesTask extends AsyncTask<String, Void, List<TmdbImage>> {

    public static final String BACKDROPS = "backdrops";
    public static final String POSTERS = "posters";

    private final String IMAGES = "images";
    private final String API_KEY_PARAM = "api_key";
    private final String LOG_TAG = getClass().getSimpleName();

    private List<TaskCompletedListener> listeners = new ArrayList<>();

    String imageType;
    int movieId;

    public void execute(int movieId ,String imageType) {
        this.imageType = imageType;
        this.movieId = movieId;
        execute();
    }

    public void addListener(TaskCompletedListener listener) {
        listeners.add(listener);
    }

    private void invokeEvent(List<TmdbImage> result) {
        for (TaskCompletedListener listener:
             listeners) {
            listener.getImagesTaskCompleted(result);
        }
    }

    @Override
    protected List<TmdbImage> doInBackground(String... strings) {
        HttpURLConnection httpURLConnection = null;
        BufferedReader bufferedReader = null;

        String imagesJsonStr = null;

        try {

            Uri builtUri = buildUri(movieId);

            URL url = new URL(builtUri.toString());

            Log.v(LOG_TAG, "Built URI " + builtUri.toString());

            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.connect();

            InputStream inputStream = httpURLConnection.getInputStream();
            StringBuilder buffer = new StringBuilder();
            if (inputStream == null) {
                return null;
            }
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                buffer.append(line).append("\n");
            }
            if (buffer.length() == 0) {
                return null;
            }
            imagesJsonStr = buffer.toString();

            Log.v(LOG_TAG, "Images list: " + imagesJsonStr);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error", e);
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        try {
            List<TmdbImage> result = parseJson(imagesJsonStr);
            return result;
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

        return null;


    }

    @Override
    protected void onPostExecute(List<TmdbImage> strings) {
        invokeEvent(strings);
    }

    public List<TmdbImage> parseJson(String jsonString) throws JSONException {
        final String TMDB_FILE_PATH = "file_path";

        List<TmdbImage> result = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(jsonString);

        JSONArray jsonArray = jsonObject.getJSONArray(imageType);
        for (int i = 0; i < jsonArray.length(); i++) {
            result.add(new TmdbImage(jsonArray.getJSONObject(i).getString(TMDB_FILE_PATH)));
        }
        return result;
    }

    public Uri buildUri(int movieId) {
        final String TMDB_BASE_URL = "https://api.themoviedb.org/3/movie";
        return Uri.parse(TMDB_BASE_URL).buildUpon()
                .appendPath(String.valueOf(movieId))
                .appendPath(IMAGES)
                .appendQueryParameter(API_KEY_PARAM, BuildConfig.TMDB_API_KEY)
                .build();
    }

    public interface TaskCompletedListener {
        public void getImagesTaskCompleted(List<TmdbImage> result);
    }
}
