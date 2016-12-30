package ru.surf.course.movierecommendations.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import ru.surf.course.movierecommendations.Adapters.GridMoviesAdapter;
import ru.surf.course.movierecommendations.Adapters.ListMoviesAdapter;
import ru.surf.course.movierecommendations.EndlessRecyclerViewScrollListener;
import ru.surf.course.movierecommendations.MovieInfo;
import ru.surf.course.movierecommendations.R;
import ru.surf.course.movierecommendations.tmdbTasks.GetMoviesTask;
import ru.surf.course.movierecommendations.tmdbTasks.Tasks;


public class MoviesListFragment extends Fragment implements GetMoviesTask.TaskCompletedListener {

    private static final String LOG_TAG = MoviesListFragment.class.getSimpleName();

    private final static String KEY_GRID = "grid";
    private final static String KEY_QUERY = "query";
    private final static String KEY_LINEAR_POS = "lin_pos";
    private final static String KEY_GRID_POS = "grid_pos";
    private final static String KEY_LANGUAGE = "language";
    private final static String KEY_TASK = "task";
    private final static String KEY_MOVIE_ID = "id";


    private static int PAGE;

    private String query;
    private String language;
    private int id;
    private String previousFilter;

    private RecyclerView recyclerView;
    private boolean grid;
    private List<MovieInfo> movieInfoList;
    private Tasks task;

    private GridMoviesAdapter gridMoviesAdapter;
    private StaggeredGridLayoutManager staggeredGridLayoutManager;

    private ListMoviesAdapter listMoviesAdapter;
    private LinearLayoutManager linearLayoutManager;

    private EndlessRecyclerViewScrollListener scrollListener;

    public static MoviesListFragment newInstance(String query, String language, Tasks task) {
        MoviesListFragment moviesListFragment = new MoviesListFragment();
        Bundle bundle = new Bundle();
        bundle.putString(KEY_LANGUAGE, language);
        bundle.putString(KEY_QUERY, query);
        bundle.putSerializable(KEY_TASK, task);
        moviesListFragment.setArguments(bundle);
        return moviesListFragment;
    }

    public static MoviesListFragment newInstance(int id, String language, Tasks task) {
        MoviesListFragment moviesListFragment = new MoviesListFragment();
        Bundle bundle = new Bundle();
        bundle.putString(KEY_LANGUAGE, language);
        bundle.putInt(KEY_MOVIE_ID, id);
        bundle.putSerializable(KEY_TASK, task);
        moviesListFragment.setArguments(bundle);
        return moviesListFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        query = getArguments().getString(KEY_QUERY);
        language = getArguments().getString(KEY_LANGUAGE);
        task = (Tasks) getArguments().getSerializable(KEY_TASK);
        id = getArguments().getInt(KEY_MOVIE_ID);
        PAGE = 1;
        previousFilter = query;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_movies_list, container, false);

        recyclerView = (RecyclerView) root.findViewById(R.id.movie_list_rv);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        staggeredGridLayoutManager = new StaggeredGridLayoutManager(2, 1);
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        grid = sharedPref.getBoolean(KEY_GRID, true);
        gridMoviesAdapter = new GridMoviesAdapter(getActivity(), new ArrayList<MovieInfo>(1));
        listMoviesAdapter = new ListMoviesAdapter(getActivity(), new ArrayList<MovieInfo>(1));
        if (grid) {
            switchToGrid();
        } else {
            switchToLinear();
        }

        recyclerView.addOnScrollListener(scrollListener);
        loadInformation();
        setHasOptionsMenu(true);
        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (GetMoviesTask.isFilter(query)) {
            inflater.inflate(R.menu.movie_list_menu, menu);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(KEY_GRID, grid);
        int scrollPositionLinear = -1;
        int scrollPositionGrid = -1;
        if (staggeredGridLayoutManager != null && linearLayoutManager != null) {
            scrollPositionLinear = linearLayoutManager.findFirstVisibleItemPosition();
            scrollPositionGrid = staggeredGridLayoutManager.findFirstVisibleItemPositions(null)[0];
        }
        editor.putInt(KEY_GRID_POS, scrollPositionGrid);
        editor.putInt(KEY_LINEAR_POS, scrollPositionLinear);
        editor.apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (staggeredGridLayoutManager != null && linearLayoutManager != null) {
            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            int lin_pos = sharedPref.getInt(KEY_LINEAR_POS, -1);
            int grid_pos = sharedPref.getInt(KEY_GRID_POS, -1);
            staggeredGridLayoutManager.scrollToPosition(grid_pos);
            linearLayoutManager.scrollToPosition(lin_pos);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_switch_filter:
                showPopup();
                return true;
            case R.id.action_search:
                SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
                item.expandActionView();
                searchView.requestFocus();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void taskCompleted(List<MovieInfo> result) {
        if (result != null) {
            if (movieInfoList != null && previousFilter.equalsIgnoreCase(query)) {
                movieInfoList.addAll(result);
            } else {
                previousFilter = query;
                movieInfoList = result;
            }
            dataLoadComplete();
        }
    }

    private void showPopup() {
        View menuItemView = getActivity().findViewById(R.id.action_switch_filter);
        PopupMenu popup = new PopupMenu(getActivity(), menuItemView);
        previousFilter = query;
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.popup_popular:
                        query = GetMoviesTask.FILTER_POPULAR;
                        getActivity().setTitle("Popular");
                        loadInformation();
                        break;
                    case R.id.popup_top_rated:
                        query = GetMoviesTask.FILTER_TOP_RATED;
                        getActivity().setTitle("Top Rated");
                        loadInformation();
                        break;
                    case R.id.popup_now_playing:
                        query = GetMoviesTask.FILTER_NOW_PLAYING;
                        getActivity().setTitle("Now Playing");
                        loadInformation();
                        break;
                    case R.id.popup_upcoming:
                        query = GetMoviesTask.FILTER_UPCOMING;
                        getActivity().setTitle("Upcoming");
                        loadInformation();
                        break;
                }
                return true;
            }
        });
        MenuInflater inflate = popup.getMenuInflater();
        inflate.inflate(R.menu.popup_menu, popup.getMenu());
        popup.show();

    }

    private void loadInformation() {
        GetMoviesTask getMoviesTask = new GetMoviesTask();
        getMoviesTask.addListener(this);
        switch (task) {
            case SEARCH_BY_FILTER:
                getMoviesTask.getMoviesByFilter(query, language,String.valueOf(PAGE));
                break;
            case SEARCH_BY_NAME:
                getMoviesTask.getMoviesByName(query);
                break;
            case SEARCH_BY_ID:
                getMoviesTask.getMovieById(id, language);
                break;
            case SEARCH_BY_GENRE:
                getMoviesTask.getMoviesByGenre(id, language);
                break;
            case SEARCH_SIMILAR:
                getMoviesTask.getSimilarMovies(id, language);
                break;
            case SEARCH_BY_KEYWORD:
                getMoviesTask.getMoviesByKeyword(id, language);
                break;
        }
    }

    private void switchToLinear() {
        recyclerView.setAdapter(listMoviesAdapter);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(listMoviesAdapter);
        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                PAGE++;
                loadInformation();
            }
        };
        grid = false;
    }

    private void switchToGrid() {
        recyclerView.setAdapter(gridMoviesAdapter);
        recyclerView.setLayoutManager(staggeredGridLayoutManager);
        recyclerView.setAdapter(gridMoviesAdapter);
        scrollListener = new EndlessRecyclerViewScrollListener(staggeredGridLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                PAGE++;
                loadInformation();
            }
        };
        grid = true;
    }

    public void dataLoadComplete() {
        fillInformation();
        View progressBarPlaceholder = null;
        if (getView() != null)
            progressBarPlaceholder = getView().findViewById(R.id.movie_list_progress_bar_placeholder);
        if (progressBarPlaceholder != null)
            progressBarPlaceholder.setVisibility(View.GONE);
    }

    public void fillInformation() {
        gridMoviesAdapter.setMovieInfoList(movieInfoList);
        listMoviesAdapter.setMovieInfoList(movieInfoList);

        gridMoviesAdapter.notifyDataSetChanged();
        listMoviesAdapter.notifyDataSetChanged();

    }

}
