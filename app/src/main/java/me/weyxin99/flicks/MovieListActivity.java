package me.weyxin99.flicks;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;
import me.weyxin99.flicks.models.Config;
import me.weyxin99.flicks.models.Movie;

public class MovieListActivity extends AppCompatActivity {

    //constants

    //base URL for the API
    public final static String API_BASE_URL = "https://api.themoviedb.org/3";
    //the parameter name for the API key
    public final static String API_KEY_PARAM = "api_key";
    //tag for logging from this activity
    public final static String TAG = "MovieListActivity";

    //instance fields
    AsyncHttpClient client;
    //the list of currently playing movies
    ArrayList<Movie> movies;
    //the recycler view
    @BindView(R.id.rvMovies) RecyclerView rvMovies;
    //the adapter wired to the recycler view
    MovieAdapter adapter;
    //image config
    Config config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_list);
        //initialize the client
        client = new AsyncHttpClient();
        //initialize the list of movies
        movies = new ArrayList<>();

        //initialize the adapter - movies array cannot be reinitialized after this point
        adapter = new MovieAdapter(movies);
        ButterKnife.bind(this);
        //resolve the recycler view and connect a layout manager + adapter
        rvMovies.setLayoutManager(new LinearLayoutManager(this));
        rvMovies.setAdapter(adapter);

        //get the configuration
        getConfiguration();
    }

    //get the list of currently playing movies
    private void getNowPlaying() {
        //create the url
        String url = API_BASE_URL + "/movie/now_playing";
        //set the request params
        RequestParams params = new RequestParams();
        params.put(API_KEY_PARAM, getString(R.string.api_key));
        //execute a GET request expecting a JSON object response
        client.get(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                //load the results into the movie list
                try {
                    JSONArray result = response.getJSONArray("results");
                    //iterate through the result set and create Movie objects
                    for(int i=0; i < result.length(); i++) {
                        Movie movie = new Movie(result.getJSONObject(i));
                        movies.add(movie);
                        //notify adapter that a row was added
                        adapter.notifyItemInserted(movies.size()-1);
                    }
                    Log.i(TAG, String.format("Loaded %s movies", result.length()));
                }
                catch (JSONException e) {
                    logError("Failed to parse now_playing movies", e, true);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                logError("Failed to get data from now_playing endpoint", throwable, true);
            }
        });
    }

    //get the configuration from the API
    private void getConfiguration() {
        //create the url
        String url = API_BASE_URL + "/configuration";
        //set the request params
        RequestParams params = new RequestParams();
        params.put(API_KEY_PARAM, getString(R.string.api_key));
        //execute a GET request expecting a JSON object response
        client.get(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                //get the image base URL
                try {
                    config = new Config(response);
                    Log.i(TAG, String.format("Loaded configuration with imageBaseURL %s and posterSize %s",
                            config.getImageBaseURL(), config.getPosterSize()));
                    //pass config to adapter
                    adapter.setConfig(config);
                    //get the now playing movie list
                    getNowPlaying();
                } catch (JSONException e) {
                    logError("Failed parsing configuration", e, true);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                logError("Failed getting configuration", throwable, true);
            }
        });
    }

    //handle error, log and alert user
    private void logError(String message, Throwable error, boolean alertUser) {
        //always log the error
        Log.e(TAG, message, error);
        //alert the user to avoid silent errors
        if(alertUser) {
            //show a long toast with the error message
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
    }
}
