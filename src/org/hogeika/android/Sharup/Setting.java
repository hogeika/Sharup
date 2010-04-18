package org.hogeika.android.Sharup;

import org.hogeika.android.Sharup.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Setting extends PreferenceActivity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref);
	}
	

}
