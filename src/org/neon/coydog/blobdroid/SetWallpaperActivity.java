package org.neon.coydog.blobdroid;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
//import android.content.ActivityNotFoundException;
import android.os.Build;

public class SetWallpaperActivity extends Activity {
	@Override
    protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);
	}

	public void onClick(View view) {
		ComponentName intentComponent = new ComponentName(this, BlobDroidService.class);
			Intent intent = new Intent();
			if(Build.VERSION.SDK_INT >= 16) {
				intent.setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
				intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, intentComponent);
			}
			else {
				intent.setAction(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
			}
			startActivity(intent);
	}
} 
