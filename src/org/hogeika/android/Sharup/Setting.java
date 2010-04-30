package org.hogeika.android.Sharup;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;

public class Setting extends PreferenceActivity implements OnPreferenceChangeListener{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref);
		
		PreferenceScreen ps = getPreferenceScreen();
		int count = ps.getPreferenceCount();
		for(int i = 0; i < count; i++){
			Preference pref = ps.getPreference(i);
			if(pref instanceof EditTextPreference){
				EditTextPreference edit = (EditTextPreference)pref;
				edit.setSummary(edit.getText());
			}
			if(pref instanceof ListPreference){
				ListPreference list = (ListPreference)pref;
				list.setSummary(list.getEntry());
			}
			pref.setOnPreferenceChangeListener(this);
		}
	}

	public boolean onPreferenceChange(Preference pref, Object value) {
		if(pref instanceof EditTextPreference){
			EditTextPreference edit = (EditTextPreference)pref;
			edit.setSummary((String)value);
		}
		if(pref instanceof ListPreference){
			ListPreference list = (ListPreference)pref;
			list.setSummary(list.getEntries()[list.findIndexOfValue((String)value)]);
		}
		return true;
	}
	

}
