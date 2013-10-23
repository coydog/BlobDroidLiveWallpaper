package org.neon.coydog.blobdroid;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;
import android.graphics.Color;

public class BlobDroidPreferencesActivity extends PreferenceActivity {
	static final String KEY_NUM_BLOBS = "numberOfBlobs";
	static final String KEY_COLOR_CENTER = "colorCenter";
	static final String KEY_COLOR_GLOW = "colorGlow";
	static final String KEY_COLOR_BACKGROUND = "colorBackground";
	static final int BLOB_MAX = 200;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);

		// validate numeric input. Taken from vogella.
		//Preference blobsPreference = getPreferenceScreen().findPreference(KEY_NUM_BLOBS);
		//blobsPreference.setOnPreferenceChangeListener(numberCheckListener);
		// TODO Attach this listener to other numeric inputs? then clean it up?
		Preference centerPreference = getPreferenceScreen().findPreference(KEY_COLOR_CENTER);
		centerPreference.setOnPreferenceChangeListener(colorCheckListener);
		Preference glowPreference = getPreferenceScreen().findPreference(KEY_COLOR_GLOW);
		glowPreference.setOnPreferenceChangeListener(colorCheckListener);
		Preference backgroundPreference = getPreferenceScreen().findPreference(KEY_COLOR_BACKGROUND);
		backgroundPreference.setOnPreferenceChangeListener(colorCheckListener);
	}

	// validates numberic value
	/*Preference.OnPreferenceChangeListener numberCheckListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			String sValue;
			if (newValue != null) {
				sValue = newValue.toString().trim();
				if ( (newValue != null) && (sValue.length() > 0)
					&& (sValue.matches("\\d*"))) {
						Integer iValue = new Integer(sValue); // untested
						if (iValue < 0 && iValue <= BLOB_MAX)
							return true;
				}
				// Messy...
				Toast.makeText(BlobDroidPreferencesActivity.this, "Invalid Input",
										Toast.LENGTH_SHORT).show();
			}
			return false;
		}

	};*/
	Preference.OnPreferenceChangeListener colorCheckListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if (newValue != null) {
				String s = newValue.toString().trim();
				if (s.length() > 0) {
					try {
						int c = Color.parseColor(s);
						c &= 0x00FFFFFF;
						if (c >= 0 && c <= 0x00FFFFFF) {
							return true;
						}
					} catch (IllegalArgumentException e) {
					}
				}
			}
			Toast.makeText(BlobDroidPreferencesActivity.this, 
						"Please input a hexadecimal color value, eg #FFFFFF for white",
						Toast.LENGTH_SHORT).show();
			return false;
		}
	};
}
