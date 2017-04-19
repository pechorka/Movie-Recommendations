package ru.surf.course.movierecommendations.ui.screen.person;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import ru.surf.course.movierecommendations.R;
import ru.surf.course.movierecommendations.ui.screen.gallery.GalleryActivityView;
import ru.surf.course.movierecommendations.ui.screen.person.adapters.PersonInfosPagerAdapter;
import ru.surf.course.movierecommendations.util.Utilities;
import ru.surf.course.movierecommendations.domain.people.Person;
import ru.surf.course.movierecommendations.domain.TmdbImage;
import ru.surf.course.movierecommendations.interactor.tmdbTasks.GetImagesTask;
import ru.surf.course.movierecommendations.interactor.tmdbTasks.GetPersonsTask;
import ru.surf.course.movierecommendations.interactor.tmdbTasks.ImageLoader;
import ru.surf.course.movierecommendations.interactor.tmdbTasks.Tasks;

/**
 * Created by andrew on 2/15/17.
 */

public class PersonActivity extends AppCompatActivity {

  final static String KEY_PERSON = "person";
  final static String KEY_PERSON_ID = "person_id";
  final static int DATA_TO_LOAD = 2;
  final String LOG_TAG = getClass().getSimpleName();

  private ProgressBar progressBar;
  private TextView name;
  private TextView birthDate;
  private Person currentPerson;
  private ImageView pictureProfile;
  private CollapsingToolbarLayout collapsingToolbarLayout;
  private AppBarLayout appBarLayout;
  private ViewPager infosPager;
  private View fakeStatusBar;
  private Toolbar toolbar;

  private int dataLoaded = 0;

  public static void start(Context context, Person person) {
    Intent intent = new Intent(context, PersonActivity.class);
    intent.putExtra(KEY_PERSON, person);
    context.startActivity(intent);
  }

  public static void start(Context context, int personId) {
    Intent intent = new Intent(context, PersonActivity.class);
    intent.putExtra(KEY_PERSON_ID, personId);
    context.startActivity(intent);
  }


  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!getIntent().hasExtra(KEY_PERSON) && !getIntent().hasExtra(KEY_PERSON_ID)) {
      onDestroy();
    }

    setContentView(R.layout.activity_person);

    initViews();
    setupViews();

  }

  private void initViews() {
    progressBar = (ProgressBar) findViewById(R.id.person_progress_bar);
    if (progressBar != null) {
      progressBar.setIndeterminate(true);
      progressBar.getIndeterminateDrawable()
          .setColorFilter(ContextCompat.getColor(this, R.color.colorAccent),
              PorterDuff.Mode.MULTIPLY);
    }
    name = (TextView) findViewById(R.id.person_name);
    birthDate = (TextView) findViewById(R.id.person_birth_date);
    pictureProfile = (ImageView) findViewById(R.id.person_backdrop);
    collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(
        R.id.person_collapsing_toolbar_layout);
    appBarLayout = (AppBarLayout) findViewById(R.id.person_appbar_layout);
    infosPager = (ViewPager) findViewById(R.id.person_infos_pager);
    fakeStatusBar = findViewById(R.id.person_fake_status_bar);
    toolbar = (Toolbar) findViewById(R.id.person_toolbar);
  }

  private void setupViews() {
    toolbar.setNavigationIcon(R.drawable.ic_action_back);
    toolbar.setNavigationOnClickListener(v -> onBackPressed());

    appBarLayout.addOnOffsetChangedListener((appBarLayout1, verticalOffset) -> {
      verticalOffset = Math.abs(verticalOffset);
      int offsetToToolbar =
          appBarLayout1.getTotalScrollRange() - Utilities.getActionBarHeight(PersonActivity.this)
              - fakeStatusBar.getMeasuredHeight();
      if (verticalOffset >= offsetToToolbar) {
        fakeStatusBar.setAlpha((float) (verticalOffset - offsetToToolbar) / Utilities
            .getActionBarHeight(PersonActivity.this));
      }
    });
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    if (Utilities.isConnectionAvailable(this)) {
      startLoading();
    } else {
      final View errorPlaceholder = findViewById(R.id.person_no_internet_screen);
      errorPlaceholder.setVisibility(View.VISIBLE);
      findViewById(R.id.message_no_internet_button)
          .setOnClickListener(view -> {
            if (Utilities.isConnectionAvailable(PersonActivity.this)) {
              errorPlaceholder.setVisibility(View.GONE);
              startLoading();
            }
          });
    }
  }

  private void startLoading() {
    dataLoaded = 0;
    if (getIntent().hasExtra(KEY_PERSON)) {
      currentPerson = (Person) getIntent().getSerializableExtra(KEY_PERSON);
    } else if (getIntent().hasExtra(KEY_PERSON_ID)) {
      currentPerson = new Person(getIntent().getIntExtra(KEY_PERSON_ID, -1));
    }
    if (currentPerson != null) {
      loadInformationInto(currentPerson, Utilities.getSystemLanguage());
      loadProfilePicturesInto(currentPerson);
    }

    if (Build.VERSION.SDK_INT >= 19) {
      fakeStatusBar.setVisibility(View.VISIBLE);
    }
  }

  private void loadInformationInto(final Person person, String language) {
    GetPersonsTask getPersonsTask = new GetPersonsTask();
    getPersonsTask.addListener(result -> {
      if (person != null) {
        person.fillFields(result.get(0));
      }
      dataLoadComplete();
    });
    getPersonsTask.getPersonById(person.getId(), language);
  }

  private void loadProfilePicturesInto(final Person person) {
    GetImagesTask getImagesTask = new GetImagesTask();
    getImagesTask.addListener(new GetImagesTask.TaskCompletedListener() {
      @Override
      public void getImagesTaskCompleted(List<TmdbImage> result) {
        person.setProfilePictures(result);
        dataLoadComplete();
      }
    });
    getImagesTask.getPersonImages(person.getId(), Tasks.GET_PROFILE_PICTURES);
  }

  private boolean checkInformation(Person person) {
    return Utilities.checkString(person.getBiography());
    //for now checking only biography
  }

  private String firstLetterToUpper(String str) {
    return str.substring(0, 1).toUpperCase() + str.substring(1);
  }


  private void fillInformation() {
    name.setText(currentPerson.getName());

    DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(this);

    if (currentPerson.getBirthday() != null) {
      birthDate.setText("(" + dateFormat.format(currentPerson.getBirthday()) + ")");
    }

    if (currentPerson.getProfilePictures() != null
        && currentPerson.getProfilePictures().size() != 0) {
      ImageLoader
          .putPosterNoResize(this, currentPerson.getProfilePictures().get(0).path, pictureProfile,
              ImageLoader.sizes.w500);
      pictureProfile.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          ArrayList<String> paths = new ArrayList<String>();
          for (TmdbImage image :
              currentPerson.getProfilePictures()) {
            paths.add(image.path);
          }
          GalleryActivityView.start(PersonActivity.this, paths);
        }
      });
    }
    PersonInfosPagerAdapter personInfosPagerAdapter = new PersonInfosPagerAdapter(
        getSupportFragmentManager(), this, currentPerson);
    infosPager.setAdapter(personInfosPagerAdapter);
  }

  private void dataLoadComplete() {
    dataLoaded++;
    Log.v(LOG_TAG, "data loaded:" + dataLoaded);
    if (dataLoaded == DATA_TO_LOAD) {
      fillInformation();
      View progressBarPlaceholder = null;
      progressBarPlaceholder = findViewById(R.id.person_progress_bar_placeholder);
      if (progressBarPlaceholder != null) {
        progressBarPlaceholder.setVisibility(View.GONE);
      }
    }
  }



}
