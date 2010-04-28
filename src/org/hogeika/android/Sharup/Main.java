package org.hogeika.android.Sharup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class Main extends Activity {

	private static final String SHARUP_TMP = "Sharup_tmp";

	private static final String HELP_SITE = "http://wiki.github.com/hogeika/Sharup/help";

	private int localId = 1;
	private int genId(){
		return localId++;
	}
	
	private static final Uri externalContnetURI = Images.Media.EXTERNAL_CONTENT_URI;

	private Button takePictureButton;
	private Button choicePictureButton;
	private Button sendMailButton;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        takePictureButton = (Button) findViewById(R.id.Button_TakePicture);
        takePictureButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				takePicture();
			}
		});
        
        choicePictureButton = (Button) findViewById(R.id.Button_ChoicePicture);
        choicePictureButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				choicePicture();			
			}
		});
        
        sendMailButton = (Button) findViewById(R.id.Button_SendMail);
        sendMailButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				sendMail();
			}
		}); 
        sendMailButton.setEnabled(false);
        
        Intent intent = getIntent();
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action) && intent.hasExtra(Intent.EXTRA_STREAM)) {
        	Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        	addItem(uri);
        	sendMail();
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)
        		&& intent.hasExtra(Intent.EXTRA_STREAM)) {
        	ArrayList<Parcelable> list = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        	if (list != null) {
        		for (Parcelable parcelable : list) {
        			Uri uri = (Uri) parcelable;
        			addItem(uri);
        		}
        		sendMail();
        	}
        } else if(Intent.ACTION_MAIN.equals(action) && intent.getCategories().contains(Intent.CATEGORY_LAUNCHER)){
        	if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("auto_start_camera", false)){
               	takePicture();
        	}else{
        		cleanTempDir();
        	}
         }
    }
    
    private class ItemInfo {
    	private final int itemId;
    	private final Uri uri;
		public ItemInfo(int itemId, Uri uri) {
			super();
			this.itemId = itemId;
			this.uri = uri;
		}
		public int getItemId() {
			return itemId;
		}
		public Uri getUri() {
			return uri;
		}
    }
    
    private Map<Integer, ItemInfo> itemMap = new LinkedHashMap<Integer, ItemInfo>();
    
    private final int REQUEST_TAKE_PICTURE = 0;
    private final int REQUEST_CHOICE_PICTURE = 1;
    private final int REQUEST_SEND_MAIL = 2;
    
    private Uri tmpUri;
    
    private void takePicture(){
    	SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyyMMddHHmmssSS");
    	String filename = timeStampFormat.format(new Date());

    	ContentValues values = new ContentValues();
    	values.put(Images.Media.TITLE, filename);

    	tmpUri = getContentResolver().insert(externalContnetURI, values);

    	Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    	intent.putExtra(MediaStore.EXTRA_OUTPUT, tmpUri);
    	startActivityForResult(intent, REQUEST_TAKE_PICTURE);
    }
    
    private void choicePicture(){
        Uri uri = Images.Media.INTERNAL_CONTENT_URI;
        Intent intent = new Intent(Intent.ACTION_PICK,uri);
        startActivityForResult(intent,REQUEST_CHOICE_PICTURE);
    }
    
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	if (resultCode == Activity.RESULT_OK){
    		switch(requestCode){
    		case REQUEST_TAKE_PICTURE:
    			scanMedia();
     			addItem(tmpUri);
    			break;
    		case REQUEST_CHOICE_PICTURE:
    			addItem(data.getData());
    			break;
    		}
    	}
    	if(requestCode == REQUEST_SEND_MAIL){
    		String setting = PreferenceManager.getDefaultSharedPreferences(this).getString("auto_quit", "NO");
    		if(setting.equals("NO")){
    			
    		}
    		else if(setting.equals("ALWAYS")){
    			finish();
    		}
    		else if(setting.equals("CONFIRM")){
    			new AlertDialog.Builder(this)
    				.setTitle("Quit?")
    				.setMessage("Quit OK?")
    				.setPositiveButton("OK", new DialogInterface.OnClickListener() {				
    					public void onClick(DialogInterface arg0, int arg1) {
    						finish();
    					}
    				})
    				.show();
    		}
    	}
    }
     	
	private void addItem(Uri uri) {
    	Bitmap thumbnailBitmap = getThumbnailBitmap(uri, 100, 100);
    	if(thumbnailBitmap == null){
    		return;
    	}
    	
    	LinearLayout lineItem = new LinearLayout(this);
    	lineItem.setId(genId());
    	
    	CheckBox checkBox = new CheckBox(this);
    	checkBox.setChecked(true);
    	checkBox.setId(genId());
    	checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {		
			public void onCheckedChanged(CompoundButton button, boolean isChecked) {
				if(!isChecked){
					int id = button.getId();
					removeItem(id);
				}
			}
		});
    	lineItem.addView(checkBox);
    	
    	ImageView thumbnail = new ImageView(this);   	
		thumbnail.setImageBitmap(thumbnailBitmap);
    	lineItem.addView(thumbnail);
    	
    	LinearLayout listView = (LinearLayout) findViewById(R.id.ItemList);
    	listView.addView(lineItem);	
    	
    	itemMap.put(checkBox.getId(), new ItemInfo(lineItem.getId(), uri));
        sendMailButton.setEnabled(true);
	}
	
	private void removeItem(int id){
		ItemInfo info = itemMap.get(id);
		if(info != null){
	    	LinearLayout listView = (LinearLayout) findViewById(R.id.ItemList);
	    	listView.removeView(findViewById(info.getItemId()));	
	    	itemMap.remove(id);
		}
		if(itemMap.isEmpty()){
			sendMailButton.setEnabled(false);
		}
	}
	
	private void removeAllItem(){
    	LinearLayout listView = (LinearLayout) findViewById(R.id.ItemList);
		for(ItemInfo info : itemMap.values()){
			listView.removeView(findViewById(info.getItemId()));
		}
		itemMap.clear();
		sendMailButton.setEnabled(false);
	}
	
	private void sendMail(){
		if(itemMap.isEmpty()){
			return;
		}
		
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

		String email = pref.getString("mail_address", "");
		String subject = pref.getString("subject_format", "");
		String body = pref.getString("mail_body", "");
		
		Intent intent;
		if(itemMap.size() == 1){
	    	intent = new Intent(Intent.ACTION_SEND);
	    	intent.putExtra(Intent.EXTRA_EMAIL, email.split(",")); // TODO conformance RFC2822
	    	intent.putExtra(Intent.EXTRA_SUBJECT, formatSubject(subject));
	    	intent.putExtra(Intent.EXTRA_TEXT, body);
	    	intent.setType("image/*");    	
	    	Uri uri = (itemMap.values().toArray(new ItemInfo[0]))[0].getUri();
	    	intent.putExtra(Intent.EXTRA_STREAM, getActualUri(uri));
		}else{
	    	intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
	    	intent.putExtra(Intent.EXTRA_EMAIL, email.split(",")); // TODO conformance RFC2822
	    	intent.putExtra(Intent.EXTRA_SUBJECT, formatSubject(subject));
	    	intent.putExtra(Intent.EXTRA_TEXT, body);
	    	intent.setType("image/*");    	
	    	intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, makeActualParcelableArrayList());
		}

    	try{
    		startActivityForResult(intent, REQUEST_SEND_MAIL);
    	}catch(ActivityNotFoundException e){
    		new AlertDialog.Builder(this)
				.setTitle("Error!")
				.setMessage("Target Activity not found.")
				.setPositiveButton("OK", null)
				.show();
    	}
	}

	private ArrayList<Parcelable> makeParcelableArrayList() {
		ArrayList<Parcelable> params = new ArrayList<Parcelable>();
    	for(ItemInfo info : itemMap.values()){
    		params.add(info.getUri());
    	}
		return params;
	}
	
	private ArrayList<Parcelable> makeActualParcelableArrayList(){
		ArrayList<Parcelable> params = new ArrayList<Parcelable>();
    	for(ItemInfo info : itemMap.values()){
    		Uri tmpUri = getActualUri(info.getUri());
    		if(tmpUri != null){ // Ugh! take care error case.
    			params.add(tmpUri);
    		}
    	}
		return params;		
	}
	
	private void openSetting(){
		Intent setting = new Intent(this, Setting.class);
		startActivity(setting);
	}

	// Saving activity state
	private static final String BUNDLE_URI_LIST = "bundle_uri_list";
	private static final String BUNDLE_TMP_URI = "tmp_url";

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(BUNDLE_TMP_URI, tmpUri);
		outState.putParcelableArrayList(BUNDLE_URI_LIST, makeParcelableArrayList());
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		tmpUri = savedInstanceState.getParcelable(BUNDLE_TMP_URI);
		for(Parcelable parcelable : savedInstanceState.getParcelableArrayList(BUNDLE_URI_LIST)){
			Uri uri = (Uri)parcelable;
			addItem(uri);
		}
	}
	
	
//  for Android 2.1 (API Level 7)
//	@Override
//	public void onBackPressed() {
//		if(itemMap.isEmpty()){
//			super.onBackPressed();
//			return;
//		}
//		new AlertDialog.Builder(this)
//			.setTitle("Quit?")
//			.setMessage("Really Quit and dispose selection?")
//			.setPositiveButton("OK",new DialogInterface.OnClickListener() {				
//				public void onClick(DialogInterface arg0, int arg1) {
//					finish();
//				}
//			})
//			.setNegativeButton("Cancel", null)
//			.show();
//	}

	// for Android 1.6 (API Level 4)
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			if(itemMap.isEmpty()){
				return super.onKeyDown(keyCode, event);
			}
			new AlertDialog.Builder(this)
				.setTitle("Quit?")
				.setMessage("Really Quit and dispose selection?")
				.setPositiveButton("OK",new DialogInterface.OnClickListener() {				
					public void onClick(DialogInterface arg0, int arg1) {
						finish();
					}
				})
				.setNegativeButton("Cancel", null)
				.show();
			return true;
	    }
		return super.onKeyDown(keyCode, event);
	}

	// menu handling 

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the currently selected menu XML resource.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()){
		case R.id.menu_setting:
			openSetting();
			break;
		case R.id.menu_clear:
			removeAllItem();
			break;
		case R.id.menu_help:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(HELP_SITE))); 
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	}
	
	// Utility
	
	private Bitmap getThumbnailBitmap(Uri uri, int boundWidth, int boundHeight) {
    	InputStream is = null;
    	float width, height;
		try {
			is = getContentResolver().openInputStream(uri);
			
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(is,null,options);
			width = options.outWidth;
			height = options.outHeight;	
		} catch (IOException e) {
			return null; // TODO return broken image
		} finally {
			try {
				is.close();
			} catch (IOException e) {
			}
		}
		
		try {
			is = getContentResolver().openInputStream(uri);

			if(width < boundWidth && height < boundHeight){
				return BitmapFactory.decodeStream(is);
			}else{
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = Math.max(Math.round(width / boundWidth),Math.round(height / boundHeight));
				return BitmapFactory.decodeStream(is,null,options);
			}
		} catch (IOException e) {
			return null; // TODO return broken image
		} finally {
			try {
				is.close();
			} catch (IOException e) {
			}
		}
	}
	
	private Uri getActualUri(Uri src){
		int resize_factor = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("resize_factor", "1"));
		if(resize_factor == 1){
			return src;
		}
		
		ContentResolver resolver = getContentResolver();
		Cursor cursor = resolver.query(src, new String[]{Images.Media.TITLE}, null, null, null);
		if(cursor.getCount() != 1){
			// TODO Ugh!
			return null;
		}
		cursor.moveToFirst();
		String base_filename = cursor.getString(cursor.getColumnIndex(Images.Media.TITLE));    	
    	
    	String tmp_filename = base_filename + "_resized.jpg";

		File tmp_file;
		InputStream is = null;
		OutputStream os = null;
		try {
			is = getContentResolver().openInputStream(src);
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = resize_factor;
			Bitmap bitmap = BitmapFactory.decodeStream(is,null,options);
			
			File tmp_dir = new File(Environment.getExternalStorageDirectory(), SHARUP_TMP);
			if(!tmp_dir.exists()){
				if(!tmp_dir.mkdir()){
					// TODO Ugh! 
					return null;
				}
			}
			tmp_file = new File(tmp_dir, tmp_filename);
			if(tmp_file.exists()){
				if(!tmp_file.delete()){
					// TODO Ugh!
					return null;
				}
			}
			if (tmp_file.createNewFile()) {  
				os  = new FileOutputStream(tmp_file);  
				if (bitmap != null) {  
					bitmap.compress(CompressFormat.JPEG, 75, os);  
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}  finally {
			try {
				if(is != null){
					is.close();
				}
				if(os != null){
					os.close();
				}
			} catch (IOException e) {
			}
		}
		
    	ContentValues values = new ContentValues();
    	values.put(Images.Media.TITLE, tmp_filename);
       	values.put(Images.Media.MIME_TYPE, "image/jpeg");
       	values.put(Images.Media.DATA, tmp_file.toString());
    	tmpUri = getContentResolver().insert(externalContnetURI, values);
    	
		return tmpUri;
	}
	
	private void cleanTempDir(){
		File tmp_dir = new File(Environment.getExternalStorageDirectory(), SHARUP_TMP);
		if(!tmp_dir.exists()){
			return;
		}
		File files[] = tmp_dir.listFiles();
		if(files.length == 0){
			return;
		}
		// TODO Ugh! show progress dialog
		long now = new Date().getTime();
		for(File f : files){
			if(now - f.lastModified() >  24*60*60*1000){
				f.delete();
			}
		}
	}
	
	private String formatSubject(String format){
		Date now = new Date();
		Pattern p = Pattern.compile("%(%|G|yyyy|MM|ww|WW|D|dd|F|E|a|HH|kk|KK|hh|mm|ss|SS|z|Z)");
		Matcher m = p.matcher(format);
		StringBuffer buffer = new StringBuffer();
		while(m.find()){
			String type = m.group(1);
			if(type.equals("%")){
				m.appendReplacement(buffer, "%");
			}
			else {
				m.appendReplacement(buffer, new SimpleDateFormat(type).format(now));
			}
		}
		m.appendTail(buffer);
		
		return buffer.toString();
	}
	
	private void scanMedia(){
		//Ugh!
		final class Scanner implements MediaScannerConnectionClient {
			final private MediaScannerConnection connector;
			Scanner(Context context){
				connector = new MediaScannerConnection(context, this);
			}
			
			public void onMediaScannerConnected() {
		        connector.scanFile(externalContnetURI.toString(), "image/*");
			}

			public void onScanCompleted(String arg0, Uri arg1) {
				connector.disconnect();
			}			
		};
		new Scanner(this);
	}

}