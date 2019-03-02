package com.example.guyerez.todotiger;

import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

    }
    public static class PrefsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

            //Set preference screen
            setPreferencesFromResource(R.xml.preferences,rootKey);

            //Setup user's available preferences

            //Show createdDate in the Task UI
            Preference showCreated=findPreference("show_created_date");
            setPreference(showCreated);

            //Show dueDate in the Task UI
            Preference showDue=findPreference("show_due_date");
            setPreference(showDue);

            //Show completedDate in the Task UI
            Preference showCompleted=findPreference("show_completed_date");
            setPreference(showCompleted);

            //Show Due Today default TaskList
            Preference showDueToday=findPreference("show_due_today");
            setPreference(showDueToday);

            //Show Due This Week default TaskList
            Preference showDueWeek=findPreference("show_due_week");
            setPreference(showDueWeek);
        }

        /**
         * Updates the changed preference summary - hidden or visible
         */
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            if(stringValue.equals("true")){
                preference.setSummary("Visible");
            }
            else{
                preference.setSummary("Hidden");
            }

            return true;
        }

        /**
         * Starts PreferenceChangeListener, get's default values and set switch accordingly
         * registers the new value and updates the relevant summary
         */
        private void setPreference(Preference preference) {
            preference.setOnPreferenceChangeListener(this);
            SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(preference.getContext());

            SwitchPreference defaultPref = (SwitchPreference) getPreferenceManager().findPreference(preference.getKey());
            defaultPref.setChecked(preferences.getBoolean(preference.getKey(),true));
            preference.setDefaultValue(preferences.getBoolean(preference.getKey(),true));

            boolean preferenceBoolean = preferences.getBoolean(preference.getKey(), true);
            onPreferenceChange(preference, preferenceBoolean);
        }

    }
}