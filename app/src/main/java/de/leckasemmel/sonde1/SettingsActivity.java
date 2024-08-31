package de.leckasemmel.sonde1;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;


public class SettingsActivity extends AppCompatActivity
    implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback
{
    public final static String EXTRA_THEME_RESOURCE_ID = "extraThemeResId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Must set the theme before calling super()!
        Intent intent = getIntent();
        int resource_id = intent.getIntExtra(EXTRA_THEME_RESOURCE_ID, -1);
        if (resource_id >= 0) {
            setTheme(resource_id);
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preferences);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FragmentManager supportFragmentManager = getSupportFragmentManager();

        supportFragmentManager
                .addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        if (supportFragmentManager.getBackStackEntryCount() == 0) {
                            setTitle(R.string.title_activity_settings);
                        }
                    }
                });

        // Display settings fragment as main window
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.list_container, new SettingsFragment())
                .commit();
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment.
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                Objects.requireNonNull(pref.getFragment()));
        fragment.setArguments(args);
        //fragment.setTargetFragment(caller, 0);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.list_container, fragment)
                .addToBackStack(null)
                .commit();

        setTitle(pref.getTitle());

        return true;
    }
}
