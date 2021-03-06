package edu.wwu.avilatstudents.journey;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_OK = 0;
    static final int REQUEST_CODE = 1;

    private String username;

    private ViewGroup transitionContainer;
    private FrameLayout buddiesLayout;
    private FrameLayout journeysLayout;
    private FrameLayout settingsLayout;
    private FrameLayout visibleLayout;

    private ActionBar actionBar;
    private SearchView mainSearch;
    private Menu mainOptionsMenu;

    private FloatingActionButton buddiesFab;
    private FloatingActionButton journeysFab;
    private FloatingActionButton settingsFab;
    private Button signOutButton;

    private ImageView settingsAccountIcon;
    private TextView settingsAccountName;

    ListView buddiesList;
    ExpandableHeightGridView activeJourneyCards;
    ExpandableHeightGridView invitedJourneyCards;

    ArrayList<BuddiesListItem> testBuddies = new ArrayList<>();
    ArrayList<JourneyListItem> testActiveJourneys = new ArrayList<>();
    ArrayList<JourneyListItem> testInvitedJourneys = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SessionManager sessionManager = new SessionManager(this);
        final DatabaseManager databaseManager = new DatabaseManager(this);
        Log.d("login", sessionManager.isLoggedIn() ? "true" : "false");
        if(!sessionManager.isLoggedIn()) {
            sessionManager.login();
            Log.d("login", "Logging in");
        }

        // set the layout for this activity
        setContentView(R.layout.activity_main);

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        SensorEventListener sensorEventListener = new SensorEventListener() {
            float stepsLastUpdate = 0;
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if(sessionManager.isLoggedIn() && (sensorEvent.values[0] - stepsLastUpdate) >= 10){
                    Log.d("database", "" + (sensorEvent.values[0] - stepsLastUpdate) + "more steps");
                    Log.d("database", "Walked more than 10 steps");
                    databaseManager.updateSteps(
                            sessionManager.getEmail(),
                            sessionManager.getAuthentication(),
                            Float.toString(sensorEvent.values[0]));
                    stepsLastUpdate = sensorEvent.values[0];
                }else {
                    Log.d("database", "" + (sensorEvent.values[0] - stepsLastUpdate) + "more steps");
                    Log.d("database", "Walked less than 10 steps");
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        boolean batchSupported = sensorManager.registerListener(
                sensorEventListener, sensor, 1000000, 10000000);

        // temporary user login info logging
        Log.d("database", "Hello " + sessionManager.getUsername() + "!");
        Log.d("database", "Your email is " + sessionManager.getEmail() + "!");
        Log.d("database", "Authentication is " + sessionManager.getAuthentication() + "!");

        settingsAccountIcon = (ImageView) findViewById(R.id.settings_account_picture);
        settingsAccountIcon.getDrawable().setColorFilter(ContextCompat.getColor(
                MainActivity.this, R.color.activityBkg), PorterDuff.Mode.MULTIPLY);

        settingsAccountName = (TextView) findViewById(R.id.settings_account_name);
        settingsAccountName.setText(sessionManager.getUsername());

        // set toolbar as the activity's ActionBar and hide the title
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar_main));
        actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayShowTitleEnabled(false);
        mainSearch = (SearchView) findViewById(R.id.search_main);

        // link main navigation buttons
        buddiesFab = (FloatingActionButton) findViewById(R.id.buddies_fab);
        journeysFab = (FloatingActionButton) findViewById(R.id.journeys_fab);
        settingsFab = (FloatingActionButton) findViewById(R.id.settings_fab);

        signOutButton = (Button) findViewById(R.id.sign_out);

        // link the layouts (switched by the nav buttons)
        transitionContainer = (ViewGroup) findViewById(R.id.transition_container);
        buddiesLayout = (FrameLayout) findViewById(R.id.buddies_layout);
        journeysLayout = (FrameLayout) findViewById(R.id.journeys_layout);
        settingsLayout = (FrameLayout) findViewById(R.id.settings_layout);

        // set current layout to journey list view (and color nav button)
        visibleLayout = journeysLayout;
        setNavButtonColorSelected(journeysFab);

        // establish listener for navigation button clicks (switch visibility of layouts)
        View.OnClickListener navFabsOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TransitionManager.beginDelayedTransition(transitionContainer);
                visibleLayout.setVisibility(View.GONE);

                switch (view.getId()) {
                    case R.id.buddies_fab:
                        actionBar.show();
                        visibleLayout = buddiesLayout;
                        setNavButtonColorSelected(buddiesFab);
                        updateActionBar(R.id.buddies_fab);
                        break;
                    case R.id.journeys_fab:
                        actionBar.show();
                        visibleLayout = journeysLayout;
                        setNavButtonColorSelected(journeysFab);
                        updateActionBar(R.id.journeys_fab);
                        break;
                    case R.id.settings_fab:
                        actionBar.hide();
                        visibleLayout = settingsLayout;
                        setNavButtonColorSelected(settingsFab);
                }
                visibleLayout.setVisibility(View.VISIBLE);
                visibleLayout.requestFocus();
            }
        };
        buddiesFab.setOnClickListener(navFabsOnClickListener);
        journeysFab.setOnClickListener(navFabsOnClickListener);
        settingsFab.setOnClickListener(navFabsOnClickListener);

        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sessionManager.logout();
                databaseManager.signOut(sessionManager.getEmail(), sessionManager.getAuthentication());
                actionBar.show();
                // reset nav buttons to base color (selected color applied appropriately in switch)
                setNavButtonColorSelected(journeysFab);
                updateActionBar(R.id.journeys_fab);
                visibleLayout.setVisibility(View.GONE);
                journeysLayout.setVisibility(View.VISIBLE);
                visibleLayout = journeysLayout;
            }
        });

        // bind data and listeners to ListView in view_buddies
        testBuddies.add(new BuddiesListItem("Mark"));
        testBuddies.add(new BuddiesListItem("Brendan"));
        testBuddies.add(new BuddiesListItem("Michael"));
        testBuddies.add(new BuddiesListItem("Tyler"));
        prepareBuddiesList();

        // bind data and listeners to active GridViews in view_journeys
        testActiveJourneys.add(new JourneyListItem("Active Journey 1", 20));
        testActiveJourneys.add(new JourneyListItem("Active Journey 2", 65));
        testActiveJourneys.add(new JourneyListItem("Active Journey 3", 70));
        testInvitedJourneys.add(new JourneyListItem("Invited Journey 1", 10));
        testInvitedJourneys.add(new JourneyListItem("Invited Journey 2", 35));
        testInvitedJourneys.add(new JourneyListItem("Invited Journey 3", 90));
        prepareJourneysLists();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mainOptionsMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        updateActionBar(R.id.journeys_fab);

        // link all menu items
        MenuItem addBuddyItem = mainOptionsMenu.findItem(R.id.add_buddy_item);
        MenuItem journeyHistoryItem = mainOptionsMenu.findItem(R.id.journey_hist_item);
        MenuItem newJourneyItem = mainOptionsMenu.findItem(R.id.new_journey_item);

        MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Class nextActivity;
                switch (item.getItemId()) {
                    case R.id.add_buddy_item:
                        nextActivity = AddBuddyActivity.class;
                        break;
                    case R.id.journey_hist_item:
                        nextActivity = JourneyHistoryActivity.class;
                        break;
                    default: // R.id.new_journey_item
                        nextActivity = NewJourneyNameActivity.class;
                }
                Intent menuItemRes = new Intent(MainActivity.this, nextActivity);
                startActivity(menuItemRes);
                return true;
            }
        };
        addBuddyItem.setOnMenuItemClickListener(listener);
        journeyHistoryItem.setOnMenuItemClickListener(listener);
        newJourneyItem.setOnMenuItemClickListener(listener);

        return true;
    }

    private void prepareBuddiesList() {
        buddiesList = (ListView) findViewById(R.id.buddies_list);
        BuddiesListItemAdapter bla = new BuddiesListItemAdapter(this, testBuddies);
        buddiesList.setAdapter(bla);
        buddiesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                BuddiesListItem buddy = (BuddiesListItem) parent.getItemAtPosition(pos);
                //View buddiesView = findViewById(R.id.buddies_layout);

                Intent showBuddyInfo = new Intent(MainActivity.this, BuddyInfoActivity.class);
                showBuddyInfo.putExtra("buddyName", buddy.getName());
                startActivity(showBuddyInfo);
            }
        });
    }

    private void prepareJourneysLists() {
        // prepare listener/handler for transition to JourneyActivity
        AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                JourneyListItem journey = (JourneyListItem) parent.getItemAtPosition(pos);
                //View journeyView = findViewById(R.id.activity_journey);

                Intent showJourney = new Intent(MainActivity.this, JourneyActivity.class);
                showJourney.putExtra("journeyName", journey.getName());
                startActivity(showJourney);
            }
        };

        // bind data to active journeys view
        activeJourneyCards = (ExpandableHeightGridView) findViewById(R.id.active_journey_cards);
        JourneyListItemAdapter jla1 = new JourneyListItemAdapter(this, testActiveJourneys);
        activeJourneyCards.setAdapter(jla1);
        activeJourneyCards.setExpanded(true);
        activeJourneyCards.setOnItemClickListener(listener);

        // bind data to active journeys view
        invitedJourneyCards = (ExpandableHeightGridView) findViewById(R.id.invited_journey_cards);
        JourneyListItemAdapter jla2 = new JourneyListItemAdapter(this, testInvitedJourneys);
        invitedJourneyCards.setAdapter(jla2);
        invitedJourneyCards.setExpanded(true);
        invitedJourneyCards.setOnItemClickListener(listener);
    }

    private void updateActionBar(int fabId) {
        switch (fabId) {
            case R.id.buddies_fab:
                // (CHANGE WHAT mainSearch ACTUALLY DOES)
                mainSearch.setQueryHint(getResources().getString(R.string.buddies_search_hint));
                showOption(R.id.add_buddy_item);
                hideOption(R.id.new_journey_item);
                hideOption(R.id.journey_hist_item);
                break;
            case R.id.journeys_fab:
                // (CHANGE WHAT mainSearch ACTUALLY DOES)
                mainSearch.setQueryHint(getResources().getString(R.string.journeys_search_hint));
                hideOption(R.id.add_buddy_item);
                showOption(R.id.new_journey_item);
                showOption(R.id.journey_hist_item);
                break;
            case R.id.settings_fab:
        }
    }

    private void hideOption(int optId) {
        MenuItem item = mainOptionsMenu.findItem(optId);
        item.setVisible(false);
    }

    private void showOption(int optId) {
        MenuItem item = mainOptionsMenu.findItem(optId);
        item.setVisible(true);
    }

    private void setNavButtonColorSelected(FloatingActionButton navButton) {
        // reset nav buttons to original color
        buddiesFab.getDrawable().clearColorFilter();
        buddiesFab.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(MainActivity.this, R.color.mainNavBase)));
        journeysFab.getDrawable().clearColorFilter();
        journeysFab.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(MainActivity.this, R.color.mainNavBase)));
        settingsFab.getDrawable().clearColorFilter();
        settingsFab.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(MainActivity.this, R.color.mainNavBase)));
        // set specified nav button to selected color
        navButton.getDrawable().setColorFilter(ContextCompat.getColor(
                MainActivity.this, android.R.color.white), PorterDuff.Mode.SRC_ATOP);
        navButton.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(MainActivity.this, R.color.journeyAccent)));
    }

}
