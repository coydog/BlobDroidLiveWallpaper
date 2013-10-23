package  org.neon.coydog.blobdroid;

import android.service.wallpaper.WallpaperService;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.graphics.Rect;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap.*;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import java.lang.Math;
import java.util.Random;
import java.lang.ArrayIndexOutOfBoundsException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
//import java.lang.Formatter;


public class BlobDroidService extends WallpaperService {

	// TODO: move into prefs?
	//static int FRAMERATE = 120;
	static final int SCALEFACTOR = 4;
	//static final int FRAMERATE = 60;
	//static final int FRAMERATE = 120;
	static final int FRAMERATE = 60;
	@Override
	public Engine onCreateEngine() {
		return new BlobDroidEngine();
	}

	private class BlobDroidEngine extends Engine {
		// TODO: should be an enum;
		static final int FADE_TYPE_NONE = 0;
		static final int FADE_TYPE_RED = 1;
		static final int FADE_TYPE_GREEN = 2;
		static final int FADE_TYPE_BLUE = 3;

		private final Handler handler = new Handler();
		private SurfaceHolder sh = getSurfaceHolder();
		private final Runnable drawRunner = new Runnable() {
			@Override
			public void run() {
				draw();
			}
		};

		class Blob {
			int x, y, vx, vy; // position and vector
		}

		class BlobSettings implements SharedPreferences.OnSharedPreferenceChangeListener {
			// TODO: Should use R.array.*? If so, "default" is reserved keyword right?
			static final String KEY_PRESET = "preset";
			static final String KEY_TOUCH_TYPE ="touchType";
			static final String KEY_COLOR_CENTER = "colorCenter";
			static final String KEY_COLOR_GLOW = "colorGlow";
			static final String KEY_COLOR_BACKGROUND = "colorBackground";
			static final String KEY_USE_CUSTOM_COLORS = "useCustomColors";
			static final String KEY_NUM_BLOBS = "numberOfBlobs";
			static final String TOUCH_TYPE_NONE = "none";
			static final String TOUCH_TYPE_CENTER = "center";
			static final String TOUCH_TYPE_GLOW = "glow";
			static final String TOUCH_TYPE_BACKGROUND = "background";
			static final String PRESET_DEFAULT = "default";
			static final String PRESET_DOTS = "dots";
			static final String PRESET_ELECTRIC_CHARGE = "electricCharge";
			static final String PRESET_FIRE = "fire";
			static final String PRESET_FLYING_DIRT = "flyingDirt";
			static final String PRESET_GLOW_WORMS = "glowWorms";
			static final String PRESET_GREEN_CONTOUR = "greenContour";
			static final String PRESET_LONELY_BLUE = "lonelyBlue";
			static final String PRESET_NOISE = "noise";
			static final String PRESET_ORANGE = "orange";
			static final String PRESET_WIGGLE = "wiggle";
			static final String PRESET_YELLOW = "yellow";

			// packed ARGB color-ints.
			int colorCenter;
			int colorGlow;
			int colorBackground;
			int numBlobs;
			boolean gravity;
			int blob_size;
			int blob_falloff;
			int blob_presence;
			int border_size;
			int border_falloff;
			int border_presence;
			short multiplication;

			String preset = PRESET_DEFAULT;

			boolean touchEnabledCenter;
			boolean touchEnabledGlow;
			boolean touchEnabledBackground;
			boolean useCustomColors;
			SharedPreferences prefs;

			// constructor just for testing, to play with defaults. Need to construct Colors from 
			// RGB values. TODO: replace with Preferences settings.
			BlobSettings() {
				colorCenter = Color.rgb(255, 255, 255);
				colorGlow = Color.rgb(0, 0, 255);
				colorBackground = Color.rgb(0, 0, 0);
				numBlobs = 5;
				gravity = false;
				blob_size = 2 * SCALEFACTOR;
				//blob_falloff = 16 * SCALEFACTOR;
				blob_falloff = 32;
				blob_presence = 255;
				border_size = 2 * SCALEFACTOR;
				border_falloff = 9;
				border_presence = 128;
				multiplication = 16;

				prefs = PreferenceManager.getDefaultSharedPreferences(BlobDroidService.this);
				prefs.registerOnSharedPreferenceChangeListener(this);

				useCustomColors = prefs.getBoolean(KEY_USE_CUSTOM_COLORS, false);
				preset = prefs.getString("preset", "default");
				applyPreset();
				applyColorOverrides();	// must be after preset
				applyTouchEnabled(prefs.getString(KEY_TOUCH_TYPE, TOUCH_TYPE_NONE));

				// load preset
				// should leave this up to preset or allow override??
				//numBlobs = Integer.valueOf(prefs.getString("numberOfBlobs", 
				//								//"4")); // TODO 4? default?
			}

			protected void applyPreset() {
				// just sets the variables. Call reInit() to rebuild everything based on preset.
				if (preset.equals(PRESET_DEFAULT)) {
					colorCenter = Color.rgb(255, 255, 255);
					colorGlow = Color.rgb(0, 0, 255);
					colorBackground = Color.rgb(0, 0, 0);
					numBlobs = 5;
					gravity = false;
					blob_size = 2 * SCALEFACTOR;
					//blob_size = 4;
					//blob_falloff = 16;
					blob_falloff = 16 * 2; // needs to be based on MAXBLOB_SPAN? unsure exact relationship
					//blob_falloff = BlobUtils.MAXBLOB_SPAN / 2; // needs to be based on MAXBLOB_SPAN
					blob_presence = 255;
					border_size = 2 * SCALEFACTOR;
					border_falloff = 9 * 2;
					border_presence = 128;
					multiplication = 16;
				} else if (preset.equals(PRESET_DOTS)) {
					colorCenter = Color.rgb(0,0,0);
					colorGlow = Color.rgb(0, 191, 255);
					colorBackground = Color.rgb(255, 255, 255);
					//numBlobs = 40; // might have slowdown, maybe adjust
					numBlobs = 40; // might have slowdown, maybe adjust
					gravity = false;
					//blob_size = 2;
					blob_size = 2 * SCALEFACTOR; // tweaked for higher res
					//blob_falloff = 3;
					blob_falloff = (int)(3 * 1.5);
					blob_presence = 32;
					border_size = 0;//2*4;
					border_falloff = 16; 
					border_presence = 127;
					multiplication = 20;
				} else if (preset.equals(PRESET_ELECTRIC_CHARGE)) {
					colorCenter = Color.rgb(0, 0, 0);
					colorGlow = Color.rgb(0, 0, 255);
					colorBackground = Color.rgb(255, 255, 255);
					numBlobs = 50;
					gravity = false;
					//blob_size = 5;
					blob_size = 5 * SCALEFACTOR ;
					//blob_falloff = 32;
					blob_falloff = (int)(32 * 1.5);
					blob_presence = 255;
					border_size = 0;
					border_falloff = 0;
					border_presence = 0;
					multiplication = 64;
				} else if (preset.equals(PRESET_FLYING_DIRT)) {
					colorCenter = Color.rgb(64, 64, 0);
					colorGlow = Color.rgb(128, 128, 0);
					colorBackground = Color.rgb(240, 240, 220);
					numBlobs = 50;
					//gravity = true; // TODO:  gravity not yet implemented.
					gravity = false;
					//blob_size = 0;
					blob_size = 4;
					//blob_falloff = 0;
					blob_falloff = 2;
					blob_presence = 10;
					border_size = 0;
					border_falloff = 64;
					border_presence = 64;
					multiplication = 100;
				} else if (preset.equals(PRESET_FIRE)) {
					colorCenter = Color.rgb(238, 219, 0);
					colorGlow = Color.rgb(255, 82, 0);
					colorBackground = Color.rgb(165, 42, 42);
					numBlobs = 20;
					gravity = false; // TODO: enable gravity when implemented
					blob_size = 3 * SCALEFACTOR;
					blob_falloff = (int)(16 * 1.5);
					blob_presence = 224;
					border_size = 3 * SCALEFACTOR;
					border_falloff = 32;
					border_presence = 128;
					multiplication = 16;
				} else if (preset.equals(PRESET_GLOW_WORMS)) {
					colorCenter = Color.rgb(255, 255, 0);
					colorGlow = Color.rgb(30, 144, 255);
					colorBackground = Color.rgb(0, 0, 0);
					//numBlobs = 20;
					numBlobs = 60;
					gravity = false;
					//blob_size = 0;
					blob_size = 0;
					//blob_falloff = 0;
					blob_falloff = 4;
					blob_presence = 127;
					border_size = 0;
					border_falloff = 0;
					//border_falloff = 2;
					border_presence = 0;
					multiplication = 127;
				} else if (preset.equals(PRESET_GREEN_CONTOUR)) {
					colorCenter = Color.rgb(0, 0, 0);
					colorGlow = Color.rgb(0, 255, 0);
					colorBackground = Color.rgb(0, 0, 0);
					numBlobs = 3;
					gravity = false;
					blob_size = 20 * SCALEFACTOR * 8;
					//blob_size = BlobUtils.MAXBLOB_SPAN;
					//blob_falloff = (int)(16 * 1.5);
					blob_falloff =(int)(16 * 1.5);
					blob_presence = 255;
					border_size = 0;
					border_falloff = 16;
					border_presence = 128;
					multiplication = 24;
				} else if (preset.equals(PRESET_LONELY_BLUE)) {
					colorCenter = Color.rgb(255, 255, 255);
					colorGlow = Color.rgb(0, 0, 255);
					colorBackground = Color.rgb(0, 0, 0);
					numBlobs = 1;
					gravity = false;
					blob_size = 1 * SCALEFACTOR;
					blob_falloff = (int)(16 * 1.5);
					blob_presence = 255;
					border_size = 0;
					border_falloff = 8;
					border_presence = 255;
					multiplication = 64;
				} else if (preset.equals(PRESET_NOISE)) {
					colorCenter = Color.rgb(0, 0, 0);
					colorGlow = Color.rgb(255, 255, 0);
					colorBackground = Color.rgb(173, 216, 230);
					numBlobs = 45;
					gravity = false;
					blob_size = 190 * SCALEFACTOR * 4;
					blob_falloff = 255;
					blob_presence = 255;
					border_size = 0;
					border_falloff = 0;
					border_presence = 0;
					multiplication = 16;
				} else if (preset.equals(PRESET_ORANGE)) {
					colorCenter = Color.rgb(255, 255, 255);
					colorGlow = Color.rgb(255, 165, 0);
					colorBackground = Color.rgb(255, 255, 255);
					numBlobs = 4;
					gravity = false;
					blob_size = 50 * SCALEFACTOR;
					blob_falloff = (int)(16 * 1.5);
					blob_presence = 128;
					border_size = 0;
					//border_size = 4 * SCALEFACTOR;
					//border_falloff = (int)(64 * 1.5);
					border_falloff = 64;
					border_presence = 128;
					multiplication = 0;
				} else if (preset.equals(PRESET_WIGGLE)) {
					colorCenter = Color.rgb(0, 0, 0);
					colorGlow = Color.rgb(0, 191, 255);
					colorBackground = Color.rgb(255, 255, 255);
					numBlobs = 30;
					gravity = false;
					//blob_size = 10;
					//blob_size = 20;
					blob_size = 40;
					//blob_falloff = 20;
					//blob_falloff = 40;
					blob_falloff = 80;
					blob_presence = 64;
					//border_size = 3;
					border_size = 3 * SCALEFACTOR;
					border_falloff = 33;
					//border_falloff = 44;
					border_presence = 128;
					multiplication = 32;
				} else if (preset.equals(PRESET_YELLOW)) {
					colorCenter = Color.rgb(255, 255, 255);
					colorGlow = Color.rgb(255, 255, 0);
					colorBackground = Color.rgb(0, 0, 0);
					numBlobs = 5;
					gravity = false;
					blob_size = 2 * SCALEFACTOR;
					blob_falloff = (int)(20 * 1.5);
					blob_presence = 255;
					border_size = 0;
					border_falloff = 24;
					border_presence = 96;
					multiplication = 8;
				}

				/*SharedPreferences.Editor ed = PreferenceManager.getDefaultSharedPreferences(
						BlobDroidService.this).edit();
				ed.putString(KEY_NUM_BLOBS, Integer.toString(numBlobs));
				ed.apply();*/
				if (BlobDroidEngine.this.utils != null)
					BlobDroidEngine.this.utils.buildColorMap();
			}
			protected void applyPreset (String s) {
				if (preset != s) {
					preset = s;
					applyPreset();
				}
			}
			protected void applyTouchEnabled(String s) {

				touchEnabledCenter = false;
				touchEnabledGlow = false;
				touchEnabledBackground = false;

				if (s.equals(TOUCH_TYPE_CENTER))
					touchEnabledCenter = true;
				else if (s.equals(TOUCH_TYPE_GLOW))
					touchEnabledGlow = true;
				else if (s.equals(TOUCH_TYPE_BACKGROUND))
					touchEnabledBackground = true;
			}

			protected void applyColorOverrides() {
				if (useCustomColors) {
					String prefColorCenter = prefs.getString(KEY_COLOR_CENTER, "").trim();
					String prefColorGlow = prefs.getString(KEY_COLOR_GLOW, "").trim();
					String prefColorBackground = prefs.getString(KEY_COLOR_BACKGROUND, "").trim();

					if (prefColorCenter.length() > 0) {
						try {
							colorCenter = Color.parseColor(prefColorCenter);
						} catch (IllegalArgumentException e) {
						}
					}
					if (prefColorGlow.length() > 0) {
						try {
							colorGlow = Color.parseColor(prefColorGlow);
						} catch (IllegalArgumentException e) {
						}
					}
					if (prefColorBackground.length() > 0) {
						try {
							colorBackground = Color.parseColor(prefColorBackground);
						} catch (IllegalArgumentException e) {
						}
					}

					if (BlobDroidEngine.this.utils != null)
						BlobDroidEngine.this.utils.buildColorMap();
				}
			}

			@Override
			public void  onSharedPreferenceChanged(SharedPreferences p, String key) {
				// TODO: other settings. More touch functionality.
				if (key.equals(KEY_PRESET)) {
					applyPreset(p.getString(KEY_PRESET, PRESET_DEFAULT));
					applyColorOverrides(); // checks flag in applyColorOverrides()

					if (BlobDroidEngine.this.utils != null)
						BlobDroidEngine.this.utils.reInit();

				} else if (key.equals(KEY_TOUCH_TYPE)) {
					String s = p.getString(KEY_TOUCH_TYPE, TOUCH_TYPE_NONE);
					applyTouchEnabled(s);
				} else if (key.equals(KEY_USE_CUSTOM_COLORS)) {
					boolean b = p.getBoolean(KEY_USE_CUSTOM_COLORS, false);
					if (b != useCustomColors) {
						useCustomColors = b;
						if (!useCustomColors){
							applyPreset();
						} else {
							applyColorOverrides();
						}
					}
				} else if (key.equals(KEY_COLOR_CENTER)
						|| key.equals(KEY_COLOR_GLOW)
						|| key.equals(KEY_COLOR_BACKGROUND))
				{
					useCustomColors = true; // touch triggers custom colors to be used.
					SharedPreferences.Editor ed = p.edit();
					ed.putBoolean(KEY_USE_CUSTOM_COLORS, useCustomColors);
					ed.apply();
					
					applyColorOverrides();
				} /*else if (key.equals(KEY_NUM_BLOBS)) {
					int num = p.getInt(KEY_NUM_BLOBS, numBlobs);
					if (num <= BlobUtils.MAX_N_BLOBS) {
						String sNum = p.getString(KEY_NUM_BLOBS, Integer.toString(numBlobs));
						numBlobs = Integer.valueOf(sNum).intValue();
					}
				}*/
			}
		}
		protected BlobSettings settings = new BlobSettings();

		class BlobUtils {
			static final int COLORID_CENTER = 0;
			static final int COLORID_GLOW = 1;
			static final int COLORID_BACKGROUND = 2;
			static final int MAX_N_BLOBS = 200;
			static final short MAX_PRIMARY = 255; // max for r, g, or b
			static final short MID_PRIMARY = 128; // used for falloff calculations ?
			static final short  CEILING = 4095; // max pixel value in buffers? no based on sine table
			static final int TABLE_SIZE = 4096;
			//static final int RENDER_X = 128;
			//static final int RENDER_Y = 128;
			static final int RENDER_X = 64 * SCALEFACTOR;
			static final int RENDER_Y = 64 * SCALEFACTOR;
			//static final int COLORMAP_SIZE = 64;
			//static final int COLORMAP_SIZE = RENDER_X / 2;
			static final int COLORMAP_SIZE = 256;
			/*static final int RENDER_X = 128;
			static final int RENDER_Y = 128; // testing*/
			//static final int MAXBLOB_SPAN = 32; // span
			static final int MAXBLOB_SPAN = RENDER_X / 2; // span
			/*static final int MAXBLOB_SPAN = 64;*/
			static final int BLOB_RADIUS = MAXBLOB_SPAN / 2;
			static final int MAXRADIUS = (MAXBLOB_SPAN / 2) - 1; // index?. 15
			static final int RENDER_SIZE = RENDER_X * RENDER_Y;
			static final int RENDER_ORIGIN = 0;
			static final int MOVEMENT_ADJUST_Y = 256; // seems to give more vertical movement.
			//static final int MIDFACTOR_X = 2048;
			//static final int MIDFACTOR_Y = 2048;
			static final int MIDFACTOR_X = 8192;
			static final int MIDFACTOR_Y = 8192;

			protected Blob[] blobs = new Blob[MAX_N_BLOBS]; // TODO: better container

			short[] sine_table = new short[TABLE_SIZE];
			int[] myColorMap = new int[COLORMAP_SIZE];
			short[] blob_buf = new short[MAXBLOB_SPAN * MAXBLOB_SPAN];
			short[] buf = new short[RENDER_SIZE];  // indices to colormap
			int[] pixels = new int[RENDER_SIZE]; // colors mapped, ready to draw
			short[] back = new short[RENDER_SIZE];
			Matrix mtx = new Matrix();
			private BlobSettings mSettings;
			private SurfaceHolder sh;
			int frameClock = 0;
			//long lastTime = System.currentTimeMillis();

			// Android drawing stuff we don't want to keep reinstantiating w/ every frame.
			Bitmap bmp;

			Random mRandom = new Random();

			BlobUtils(BlobSettings s, SurfaceHolder h) {
				mSettings = s;
				sh = h;
				buildSineTable();
				buildBlob();
				initSpeed();
				// TODO: buildBackground(). Need to solve size problem.
				buildBackground();
				buildColorMap();

				bmp = Bitmap.createBitmap(RENDER_X, RENDER_Y, Bitmap.Config.ARGB_8888);
			}

			public void reInit() {
				buildSineTable();
				buildBlob();
				initSpeed();
				buildBackground();
				buildColorMap();
			}

			private int maxRadiusSq = MAXRADIUS * MAXRADIUS; // TODO: rename?. It's outer radius I think.
			private int maximum; // ??? 
			private int radius2; // diameter?
			private int center;

			// really not proud of this. Missed a place where a scaling factor should have been applied in
			// setParams(). Found it as I was getting ready to release and tweaking the themes. Borders
			// look much better with this scaling factor, but blob falloff is affected negatively. So
			// we'll use a separate getValue() function for the border rendering.
			private int centerBorder;

			protected void setParams(int size, int falloff, int presence) {
				maximum = CEILING * presence / MAX_PRIMARY;
				radius2 = size; 

				// map falloff to center 
				// wtf is 128 and 255 here? TODO: figure out these values
				if (falloff < MID_PRIMARY) {
					center = radius2 - 1 - (falloff * falloff * 800 / 16129);
					centerBorder = radius2 - 1 - (falloff * falloff * SCALEFACTOR * 800 / 16129);
				} else {
					falloff = MAX_PRIMARY - falloff;
					center = maxRadiusSq + 1 + (falloff * falloff * 800 / 16129);
					centerBorder = maxRadiusSq + 1 + (falloff * falloff * SCALEFACTOR * 800 / 16129);
				}
			}

			protected int getValue(int x2) {
				int i;
				if (x2 <= radius2)
					return maximum;
				else if (x2 >= maxRadiusSq)
					return 0;
				else {
					i = maximum * (center - radius2) * (x2 - maxRadiusSq) /
						((center - x2) * (radius2 - maxRadiusSq));
					return (i <=0 ? 0 : i);
				}
			}
			protected int getValueBorder(int x2) {
				int i;
				if (x2 <= radius2)
					return maximum;
				else if (x2 >= maxRadiusSq)
					return 0;
				else {
					i = maximum * (centerBorder - radius2) * (x2 - maxRadiusSq) /
						((centerBorder - x2) * (radius2 - maxRadiusSq));
					return (i <=0 ? 0 : i);
				}
			}

			void buildSineTable() {
				// probably won't need to increase sine table size w/ resolution. Small
				// tables work reasonably well on my DSP projects.
				int i;
				Double d = Double.valueOf(0.0);
				for (i = 0; i < TABLE_SIZE; i++) {
					d =  Math.sin( (float)i * Math.PI * 2.0 / 4096.0) * (MAXBLOB_SPAN*MAXBLOB_SPAN) + 0.5;
					sine_table[i] = d.shortValue();
				}
			}

			void buildBackground() { 
				// Should probably reference colormap; check C source implementation.

				int c, x, y;
				int index = 0;
				int halfX = RENDER_X / 2;
				int halfY = RENDER_Y / 2;

				setParams(settings.border_size,
						settings.border_falloff,
						settings.border_presence);
				for (int i = 0; i < RENDER_X; i++) {	// make sure it's Y and not X here
					for (int j = 0; j < RENDER_Y; j++) {
						if (i < halfX)
							x = i;
						else
							x = RENDER_X-1 - i;

						if (j < halfY)
							y = j;
						else
							y = RENDER_Y-1 - j;

						if (x < y)
							c = x;
						else
							c = y;

						// clip edges
						 if (Math.abs(x-y) < 4) // wbk corner case? 
							 c -= (4 - Math.abs(x - y)) / 2;

						 back[index++] = (short)getValueBorder(c * c);
					}
				}
			}

			void buildBlob() {
				int x, y;
				int blobIndex = 0;
				setParams(mSettings.blob_size, mSettings.blob_falloff, mSettings.blob_presence);

				int min = (MAXRADIUS+1) * -1;
				int max = (MAXRADIUS+1);
				for (y = min; y < max; y++)
					for (x = min; x < max; x++)
						blob_buf[blobIndex++] = (short)getValue(x * x + y * y);
			}

			void initSpeed() {
				int i;
				final int SPEED_THRESHOLD_X = RENDER_X * 20 + RENDER_X + BLOB_RADIUS;
				final int SPEED_THRESHOLD_Y = RENDER_Y * 20 + RENDER_Y + BLOB_RADIUS;
				// TODO: gravity setting here.
				if (settings.gravity) {
					for (i = 0; i < MAX_N_BLOBS; i++) {
						blobs[i] = new Blob();
						blobs[i].vx = 0;
						blobs[i].vy = 0;
						blobs[i].x = MIDFACTOR_X;
						blobs[i].y = (int)(MIDFACTOR_Y * 1.90);	//* 2; // ROUGH approximation; original 
																//was hardcoded 1000
					}
				} else {
					Random r = mRandom;
					for (i = 0; i < MAX_N_BLOBS; i++) {
						//d = andom(); // ARRRGH! TODO
						blobs[i] = new Blob();
						/*blobs[i].vx = r.nextInt() % 1280 + 80; // ARRRGH! TODO*/
						blobs[i].vx = r.nextInt() % SPEED_THRESHOLD_X; // ARRRGH! TODO

						//d = Math.random();
						/*blobs[i].vy = r.nextInt() % 1280 + 80;*/
						blobs[i].vy = r.nextInt() % SPEED_THRESHOLD_Y;
						/*int middle = 512;*/
						blobs[i].x = MIDFACTOR_X; // TODO: calc independently
						blobs[i].y = MIDFACTOR_Y;
					}	
				}
			}

			int interpolate(int i, int max, int rangeMin, int rangeMax) {
				int r, g, b;
				r = (Color.red(rangeMin) * (max - i) + Color.red(rangeMax) * i + max / 2) / max;
				g = (Color.green(rangeMin) * (max - i) + Color.green(rangeMax) * i + max / 2) / max;
				b = (Color.blue(rangeMin) * (max - i) + Color.blue(rangeMax) * i + max / 2) / max;
				return Color.rgb(r, g, b);
			}
			// analog to wmblob's alloc_colors(). Don't need to worry about X11 color map allocation.
			void buildColorMap() {
				// This part's iffy. Might increase colormap size. Shouldn't need to be dependent on 
				// screen dimenensions tho, unlike some other data in this class.
				int i;
				int halfmap = COLORMAP_SIZE / 2;
				int colorToAdd;
				for (i = 0; i < COLORMAP_SIZE; i++) {
					if (i <= halfmap) {
						colorToAdd = interpolate(i, halfmap, 
												mSettings.colorBackground, mSettings.colorGlow);
					} else {
						colorToAdd = interpolate(i - halfmap, halfmap-1, 
												mSettings.colorGlow, mSettings.colorCenter);
					}
					myColorMap[i] = colorToAdd | 0xFF000000; // Add alpha channel
				}
			}

			int sineLookup(int a) {
				return sine_table[a & 0xFFF]; // wrap table?
				//return 0; // TODO: TROUBLESHOOTING FIRST FRAME!
			}
			
			void moveBlobs() {
				// TODO: from move() in original.
				int i = 0;

				if (mSettings.gravity) {
					// TODO:
				} else { // normal, no gravity
					// TODO: frameClock * scaleFactor so I can drop framerate
					//int scaledClock = frameClock * SCALEFACTOR / 3;
					int scaledClock = frameClock;// * SCALEFACTOR / 3;
					for (i = 0; i < mSettings.numBlobs; i++) {
						blobs[i].x = 
							sineLookup(scaledClock * blobs[i].vx / BLOB_RADIUS) / 2 + MIDFACTOR_X; // TODO vars
						blobs[i].y = 
							sineLookup(scaledClock * blobs[i].vy / BLOB_RADIUS + MOVEMENT_ADJUST_Y) 
										/ 2 + MIDFACTOR_Y;
						//blobs[i].y = sineLookup(frameClock * blobs[i].vy / 16) / 2 + 512; // experimenting
					}
				}
			}
			
			void drawBlobs() {
				// clear background, otherwise colors are added in render buf.
				// TODO: build background properly.
				int i;
				for (i = 0; i < RENDER_SIZE; i++)
					buf[i] = back[i];
				
				// number here is width or height divided by blob max?
				for (i = 0; i < mSettings.numBlobs; i++)
					// totally unsure - 16 might not be radius
					/*putBlob(blobs[i].x / 16, blobs[i].y / 16); // TODO: was 16. BLOB_RADIUS?*/
					putBlob(blobs[i].x / BLOB_RADIUS, blobs[i].y / BLOB_RADIUS); // TODO: was 16. BLOB_RADIUS?

				frameClock++;
			}

			void putBlob(int px, int py) {
				int blobOffset = 0, bufOffset;
				int sx, sy, x;
				int blob_jump, buf_jump;
				int c;

				if (px <= (RENDER_ORIGIN - BLOB_RADIUS) || py <= (RENDER_ORIGIN - BLOB_RADIUS)
						|| px >= RENDER_X + BLOB_RADIUS || py >= RENDER_Y + BLOB_RADIUS)
					return;

				bufOffset = (px - BLOB_RADIUS) + (py - BLOB_RADIUS) * RENDER_Y;

				// I think the next block determines how much of blob to copy, leaving out edge
				// if we're at the edge of our render grid. Not sure though.
				if (px < BLOB_RADIUS) { // px is parameter X and sx is side X? start X? Sure, let's go with that.
					sx = BLOB_RADIUS + px;
					blobOffset += BLOB_RADIUS - px;
	 				bufOffset += BLOB_RADIUS - px;
				} else if (px > (RENDER_X - BLOB_RADIUS)) {
					sx = (RENDER_X + BLOB_RADIUS) - px;
				} else {
					sx = MAXBLOB_SPAN;
				}

				blob_jump = MAXBLOB_SPAN - sx; // was 32. 
				buf_jump = RENDER_X - sx; // was 64. probably RENDER_X

				if (py < BLOB_RADIUS) {
					sy = BLOB_RADIUS + py;
					blobOffset += (BLOB_RADIUS - py) * MAXBLOB_SPAN;
					bufOffset += (BLOB_RADIUS - py) * RENDER_X;
				} else if (py > (RENDER_Y - BLOB_RADIUS)) { // was 48
					sy = (RENDER_Y + BLOB_RADIUS) - py; // was 80 - py
				} else {
					sy = MAXBLOB_SPAN; // was 32
				} 

				while (sy-- > 0) {
					x = sx;

					while (x-- > 0) {
						c = buf[bufOffset] + blob_buf[blobOffset] +
							buf[bufOffset] * blob_buf[blobOffset] *
							mSettings.multiplication / CEILING;  
						if (c < 0)
							c = 0;
						buf[bufOffset] = (short)((c < CEILING) ? c : CEILING);
						bufOffset++;
						blobOffset++;
					}
					bufOffset += buf_jump;
					blobOffset += blob_jump;
				}
			}

			void show(Canvas canvas) {
				int i = 0; 
				float x, y;
				//Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
				// TODO: TODO: TODO: !@!!@!@1 replace with utils.bmp
				bmp = Bitmap.createBitmap(RENDER_X, RENDER_Y, Bitmap.Config.ARGB_8888);

				//Bitmap bmp = Bitmap.createBitmap(RENDER_X, RENDER_Y, Bitmap.Config.ARGB_4444);
				// TODO: replace short array with int array
				//int[] tmp = new int[RENDER_SIZE];
				int mapIndex;
				for (i = 0; i < RENDER_SIZE; i++) {
					mapIndex = buf[i] * (COLORMAP_SIZE-1) / CEILING;
					pixels[i] = myColorMap[mapIndex]; 
				}
				//Bitmap bmp = Bitmap.createBitmap(tmp, RENDER_X, RENDER_Y, Bitmap.Config.ARGB_8888);

				//float scaleX = RENDER_X / canvas.getWidth(), scaleY = RENDER_Y / canvas.getHeight();
				//mtx.setScale(1, 1);

				//bmp.eraseColor(0xFF000000);
				bmp.setPixels(pixels, 0, RENDER_X, 0, 0, RENDER_X, RENDER_Y); 
				/*int testWidth = bmp.getWidth();
				int testHeight = bmp.getHeight();
				int [] testBuf = new int[testWidth * testHeight];
				int j;
				int count = 0;
				for (i = 0; i < testHeight; i++)
					for (j = 0; j < testWidth; j++)
						testBuf[count++] = bmp.getPixel(j, i);


				int TestPixel = bmp.getPixel(31, 32);*/
				// TODO: might be good to get these out of the render loop, but are they guarunteed not
				// to change?
				float scaleX = canvas.getWidth() / (bmp.getWidth());
				float scaleY = canvas.getHeight() / bmp.getHeight();
				scaleX = (float)canvas.getWidth() / (float)(bmp.getWidth());
				scaleY = (float)canvas.getHeight() / (float)bmp.getHeight();
				//Matrix mtx = new Matrix(); // moved to member
				//mtx.reset();
				//mtx.setScale(scaleX*2, scaleX*2);
				mtx.setScale(scaleX, scaleY); // can we get this out of loop? See TODO above
				//mtx.setScale((float)7.0, (float)5.0);

				canvas.drawBitmap(bmp, mtx, null);
			}

			// experimental touch toy stuff
			void changeColors(int glow, int center) {
				//mSettings.colorCenter += mSettings.colorCenter & 0x00010101;
				//mSettings.colorGlow += mSettings.colorGlow & 0x00010101;
				//mSettings.colorCenter += mRandom.nextInt();
				mSettings.colorGlow = glow;
				mSettings.colorCenter = center;
				//mSettings.colorBackground += mRandom.nextInt();
				
				buildColorMap();
			}
			void fadeColor(int fade, int id, int fadeType) {
				if (fadeType != FADE_TYPE_NONE) {
					int mask = 0xFFFFFF;
					if (fadeType == FADE_TYPE_RED) // prevents black?
						mask &= 0x00FFFF;
					if (fadeType == FADE_TYPE_GREEN)
						mask &= 0xFF00FF;
					if (fadeType == FADE_TYPE_BLUE)
						mask &= 0xFFFF00;

					switch (id) {
						case COLORID_CENTER:
							mSettings.colorCenter &= mask;
							mSettings.colorCenter += fade;
							break;
						case COLORID_GLOW:	
							mSettings.colorGlow &= mask;
							mSettings.colorGlow += fade;
							break;
						case COLORID_BACKGROUND:
							mSettings.colorBackground &= mask;
							mSettings.colorBackground += fade;
							break;
						default:
					}
					buildColorMap();
				}
			}
		}
		protected BlobUtils utils = new BlobUtils(settings, sh);


		// members for parameters, based on preferences.
		//private int blobCount; // TODO: merge this with BlobSettings
		private boolean visible = true;
		private int width, height;
		private int count = 0; // for testing.

		public BlobDroidEngine() {
			// TODO: read in preferences; set members
			// maybe generate sine table from here too?
			//SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(BlobDroidService.this);
			//settings.numBlobs = Integer.valueOf(prefs.getString("numberOfBlobs", "4")); // TODO 4? default?
			
			// TODO: set up colors, etc here.

			handler.post(drawRunner);

			// Do I start a thread here to render bitmap and post back to handler?
			// Might run into thread sync issues unless we copy the bitmap :/
			// Alternative is run in this thread, but how would we handle things like touch events?
			//  ***with postDelayed(), post() or similar apparently.
		}
    
		@Override
		public void onVisibilityChanged(boolean visible) {
			this.visible = visible;
			if (visible) {
				handler.post(drawRunner);
			  } else {
				handler.removeCallbacks(drawRunner);
			}
		}

		@Override
	    public void onSurfaceDestroyed(SurfaceHolder holder) {
			super.onSurfaceDestroyed(holder);
	        this.visible = false;
	    	handler.removeCallbacks(drawRunner);
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			this.width = width;
	        this.height = height;
			super.onSurfaceChanged(holder, format, width, height);
		}

		@Override
		public void onTouchEvent(MotionEvent event) {
			// TODO: trippy shit.
			if (settings.touchEnabledCenter 
					|| settings.touchEnabledGlow 
					|| settings.touchEnabledBackground) 
			{ 
				Rect frame = sh.getSurfaceFrame();
				int maxX = frame.width();
				int maxY = frame.height();
				float x = event.getX();
				float y = event.getY();
				double percentX = x / maxX;
				double percentY = y / maxY;
				//int xFade, yFade;
				int fade = 0;
				int fadeType = FADE_TYPE_NONE;
				/*  _________
				 * |         | hi =  -->255
				 * |   hi    |
				 * |         | lo = 0-->
				 * |         |
				 * | R  G  B | RGB = columns for respective color selection
				 * |   lo    |
				 * | _  _  _ | dead = inactive area, won't interfere w/ buttons
				 * |  dead   |
				 * |_________|
				 *
				 */  
				
				// calculate fade magnitude based on Y position. In Android, Y starts
				// at upper left. Flip and only cover top two thirds.
				//percentY = 1.0 - percentY; // flip Y
				if (percentY <= 0.67) {
					percentY = percentY / 0.67;
					percentY = 1.0 - percentY;

					// ensure pure black and white are achievable in case of fluke of geometry
					if (percentY <= 0.08)
						percentY = .0;
					else if (percentY >= 0.96)
						percentY = 1.0;

					fade = (int)(255 * percentY);

					// determine R, G, or B based on X position.
					if (percentX <= 0.33) {
						fade = Color.rgb(fade, 0, 0);
						fadeType = FADE_TYPE_RED;
					} else if (percentX < 0.67) {
						fade = Color.rgb(0, fade, 0);
						fadeType = FADE_TYPE_GREEN;
					} else {
						fade = Color.rgb(0, 0, fade);
						fadeType = FADE_TYPE_BLUE;
					}
				}

				if (fadeType != FADE_TYPE_NONE) {
					// persist values.
					SharedPreferences.Editor ed = settings.prefs.edit();

					// 0-padding code is ugly. Java format strings don't support padding w/ leading
					// 0's apparently like C printf() would.
					// lol encapsulation
					if (settings.touchEnabledCenter) {
						utils.fadeColor(fade, BlobUtils.COLORID_CENTER, fadeType);
						ed.putString(settings.KEY_COLOR_CENTER, 
										//"#" + Integer.toHexString(settings.colorCenter & 0x00FFFFFF));
										"#" + String.format("%6X", 
													settings.colorCenter & 0x00FFFFFF).replace(' ', '0'));
					} // yech, apparently String.format doesn't support precision for ints, hence replace ' ', '0'
					if (settings.touchEnabledGlow) {
						utils.fadeColor(fade, BlobUtils.COLORID_GLOW, fadeType);
						ed.putString(settings.KEY_COLOR_GLOW, 
										//"#" + Integer.toHexString(settings.colorGlow & 0x00FFFFFF));
										"#" + String.format("%6X", 
												settings.colorGlow & 0x00FFFFFF).replace(' ', '0'));
					}
					if (settings.touchEnabledBackground) {
						utils.fadeColor(fade, BlobUtils.COLORID_BACKGROUND, fadeType);
						ed.putString(settings.KEY_COLOR_BACKGROUND, 
										//"#" + Integer.toHexString(settings.colorBackground & 0x00FFFFFF));
										"#" + String.format("%6X", 
												settings.colorBackground & 0x00FFFFFF).replace(' ', '0'));
					}
					ed.apply();
				}

				//handler.post(drawRunner); // test code
				super.onTouchEvent(event);
			}
		}

		private void draw() {
			// should be able to loop in UI thread by essentially posting this from itself.
			// Kind of like recursion, but no stack growth. I guess removeCallbacks() prevents
			// a backlog from forming?
			//SurfaceHolder sh = getSurfaceHolder();
			Canvas canvas = null;

			try {
				canvas = sh.lockCanvas();
				if (canvas != null) {
					//canvas.drawColor(Color.BLACK + count++);
					
					try {
					utils.moveBlobs();
					utils.drawBlobs();
					utils.show(canvas);
					} catch (ArrayIndexOutOfBoundsException e){}
				}
			} finally {
				if (canvas != null) {
					sh.unlockCanvasAndPost(canvas);
				}
			}
			handler.removeCallbacks(drawRunner);
			if (visible) {
				//handler.postDelayed(drawRunner, 1000 / 34); // TODO: timing
				handler.postDelayed(drawRunner, 1000 / FRAMERATE); // TODO: timing
			}
		}
	}
}
