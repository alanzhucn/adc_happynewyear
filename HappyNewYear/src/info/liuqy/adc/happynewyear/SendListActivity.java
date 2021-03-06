package info.liuqy.adc.happynewyear;



import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

//internal import
import info.liuqy.adc.happynewyear.ContactHelper.Language;
import info.liuqy.adc.happynewyear.ContactHelper.Market;;


public class SendListActivity extends ListActivity {
		
    static final String KEY_TO = "TO";
    static final String KEY_SMS = "SMS";

    static final String SENT_ACTION = "SMS_SENT_ACTION";
    static final String DELIVERED_ACTION = "SMS_DELIVERED_ACTION";
    static final String EXTRA_IDX = "contact_adapter_idx";
    static final String EXTRA_TONUMBER = "sms_to_number";
    static final String EXTRA_SMS = "sms_content";
    
    private static final int HAPPYNEWYEAR_ID = 1;

    private static final String DB_NAME = "data";
    private static final int DB_VERSION = 2;
    
    private static final String TBL_NAME = "sms";
    static final String FIELD_TO = "to_number";
    static final String FIELD_SMS = "sms";
    static final String KEY_ROWID = "_id";
    
    //[<TO, number>,<SMS, sms>]
    List<Map<String, String>> smslist = new LinkedList<Map<String, String>>();
    SimpleAdapter adapter;

    static BroadcastReceiver smsSentReceiver = null;
	static BroadcastReceiver smsDeliveredReceiver = null;
    
    SQLiteOpenHelper dbHelper = null;
    SQLiteDatabase db = null;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sendlist);
        
        initdb();
        createReceivers();
        
        adapter = new SimpleAdapter(this, smslist,
                android.R.layout.simple_list_item_2,
                new String[]{KEY_TO, KEY_SMS},
                new int[]{android.R.id.text1, android.R.id.text2});
        this.setListAdapter(adapter);
        handleIntent();
        
        if (smslist.size() == 0)  //FIXME need a better judge if from notification
            loadFromDatabase();
    }
	
	 private class GenSendListTask extends AsyncTask<Bundle, Integer, Bundle> {
	     protected Bundle doInBackground(Bundle... datas) {
	    	 Log.i("GenSendListTask", "doInBackground");
	    	 
	    	 
	    	 int count = datas.length;
	         
	         if (1!=count) {
	        	 return null;
	         }
	         
	         Bundle data = datas[0];
	         
	         //Bundle sendlist = data.getParcelable(HappyNewYearActivity.SENDLIST);
	         if (null == data) {
	        	 return null;
	         }
	         
	         
	         String cc = data.getString(HappyNewYearActivity.CUSTOMER_CARER);
	         String tmpl = data.getString(HappyNewYearActivity.SMS_TEMPLATE);
	             
	                         
	         String targetLanguage = data.getString(HappyNewYearActivity.TARGET_LANGUAGE);
	         String targetMarket = data.getString(HappyNewYearActivity.TARGET_MARKET);

	         publishProgress (0);
	         
	         Bundle sendlist = ContactHelper.readContacts(Market.fromString(targetMarket), 
	             	 	                   Language.fromString(targetLanguage), SendListActivity.this);
	             
	         // publishProgress((int) ((i / (float) count) * 100));
	         // Escape early if cancel() is called
	         // if (isCancelled()) break;
	        
	         Bundle result = new Bundle();
	         result.putBundle(HappyNewYearActivity.SEND_LIST, sendlist);
	         result.putBundle(HappyNewYearActivity.SEND_LIST_PARMS, data);
	         
	         publishProgress (100);
	         
	         return result;
	         
	     }

	     // Called in UI thread
	     protected void onProgressUpdate(Integer... progress) {
	    	 
	    	 //TODO: display a "cycle progress"..
	    	 Log.i("GenSendListTask", "onProgressUpdate");
	    	 
	    	 String progressStr = SendListActivity.this.getString(R.string.progress) 
	    			                  + progress[0].toString() + "%";
	    	 TextView view = (TextView) SendListActivity.this.findViewById(R.id.default_send);
	    	 view.setText(progressStr);
	    	 
	         Toast.makeText(SendListActivity.this, "Progress " + progress[0].toString() + "%", 
	        		        Toast.LENGTH_SHORT).show();
	     }

         // Called in UI thread
	     protected void onPostExecute(Bundle result) {

	    	 Log.i("GenSendListTask", "onPostExecute");

	    	 //TODO: validate parms.
	    	 //      read bundle..
	    	 Bundle sendlist = result.getBundle(HappyNewYearActivity.SEND_LIST);

	    	 // hidden the default view if it is not empty.
	    	 // 
	    	 if (sendlist.isEmpty()) {

	    		 TextView view = (TextView) SendListActivity.this.findViewById(R.id.default_send);
	    		 view.setText(SendListActivity.this.getString(R.string.no_name));

	    	 } else {

	    		 TextView view = (TextView) SendListActivity.this.findViewById(R.id.default_send);
	    		 view.setVisibility(TRIM_MEMORY_UI_HIDDEN);

	    		 Bundle data = result.getBundle(HappyNewYearActivity.SEND_LIST_PARMS);

	    		 String cc = data.getString(HappyNewYearActivity.CUSTOMER_CARER);

	    		 String tmpl = data.getString(HappyNewYearActivity.SMS_TEMPLATE);


	    		 tmpl = tmpl.replaceAll("\\{FROM\\}", cc);

	    		 for (String n : sendlist.keySet()) {
	    			 String sms = tmpl.replaceAll("\\{TO\\}", sendlist.getString(n));
	    			 Map<String, String> rec = new Hashtable<String, String>();
	    			 rec.put(KEY_TO, n);
	    			 rec.put(KEY_SMS, sms);
	    			 smslist.add(rec);
	    			 adapter.notifyDataSetChanged();
	    		 }
	    		 //TODO: only enable button after list is updated.
	    	 }
	     }
	 }
	 
	public void handleIntent() {
        
		Bundle data = this.getIntent().getExtras();
        
        if (data != null) {
        	
        	new GenSendListTask().execute(data);
        }

	}

	public void sendSms(View v) {
        SmsManager sender = SmsManager.getDefault();
        if (sender == null) {
            // TODO toast error msg
        }

        for (int idx = 0; idx < smslist.size(); idx++) {
            Map<String, String> rec = smslist.get(idx);
            String toNumber = rec.get(KEY_TO);
            String sms = rec.get(KEY_SMS);

            // SMS sent pending intent
            Intent sentActionIntent = new Intent(SENT_ACTION);
            sentActionIntent.putExtra(EXTRA_IDX, idx);
            sentActionIntent.putExtra(EXTRA_TONUMBER, toNumber);
            sentActionIntent.putExtra(EXTRA_SMS, sms);
            PendingIntent sentPendingIntent = PendingIntent.getBroadcast(
                    this, 0, sentActionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            // SMS delivered pending intent
            Intent deliveredActionIntent = new Intent(DELIVERED_ACTION);
            deliveredActionIntent.putExtra(EXTRA_IDX, idx);
            deliveredActionIntent.putExtra(EXTRA_TONUMBER, toNumber);
            deliveredActionIntent.putExtra(EXTRA_SMS, sms);
            PendingIntent deliveredPendingIntent = PendingIntent.getBroadcast(
                    this, 0, deliveredActionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            //send
            sender.sendTextMessage(toNumber, null, sms, sentPendingIntent,
                    deliveredPendingIntent);
        }
    }

	@Override
	protected void onStart() {
		super.onStart();
		// Question for you: where is the right place to register receivers?
		registerReceivers();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		// Question for you: where is the right place to unregister receivers?
		unregisterReceivers();
	}
	
	protected void createReceivers() {
		if (smsSentReceiver == null)
			smsSentReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					int idx = intent.getIntExtra(EXTRA_IDX, -1);
					String toNum = intent.getStringExtra(EXTRA_TONUMBER);
					String sms = intent.getStringExtra(EXTRA_SMS);
					int succ = getResultCode();
					if (succ == Activity.RESULT_OK) {
						// TODO better notification
						Toast.makeText(SendListActivity.this,
								"Sent to " + toNum + " OK!", Toast.LENGTH_SHORT)
								.show();
					} else {
						// TODO
					}
				}
			};

		if (smsDeliveredReceiver == null)
			smsDeliveredReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					int idx = intent.getIntExtra(EXTRA_IDX, -1);
					String toNum = intent.getStringExtra(EXTRA_TONUMBER);
					String sms = intent.getStringExtra(EXTRA_SMS);
					int succ = getResultCode();
					if (succ == Activity.RESULT_OK) {
						// TODO better notification
						//Toast.makeText(SendListActivity.this, "Delivered to " + toNum + " OK!", Toast.LENGTH_SHORT).show();
						saveToDatabase(toNum, sms);
						notifySuccessfulDelivery("Delivered to " + toNum + " OK!", sms);
					} else {
						// TODO
					}
				}
			};
	}

	protected void registerReceivers() {
		this.registerReceiver(smsSentReceiver, new IntentFilter(SENT_ACTION));
		this.registerReceiver(smsDeliveredReceiver, new IntentFilter(DELIVERED_ACTION));
	}
	
	protected void unregisterReceivers() {
		this.unregisterReceiver(smsSentReceiver);
		this.unregisterReceiver(smsDeliveredReceiver);
	}
	
    public void notifySuccessfulDelivery(String title, String text) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
        
        int icon = R.drawable.ic_launcher;
        CharSequence tickerText = "HappyNewYear";
        long when = System.currentTimeMillis();
        Notification notification = new Notification(icon, tickerText, when);
        
        Context context = getApplicationContext();
        CharSequence contentTitle = title;
        CharSequence contentText = text;
        Intent notificationIntent = new Intent(this, SendListActivity.class); //if click, then open SendListActivity
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.defaults |= Notification.DEFAULT_LIGHTS;
        
        mNotificationManager.notify(HAPPYNEWYEAR_ID, notification);
    }

    protected void initdb() {
        dbHelper = new SQLiteOpenHelper(this, DB_NAME, null, DB_VERSION) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL("create table sms (_id integer primary key autoincrement, " +
                        "to_number text not null, sms text not null)");
            }
            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
                //TODO on DB upgrade
            }
            
        };
        
        db = dbHelper.getWritableDatabase();
    }
    
    protected void loadFromDatabase() {
        Cursor cur = db.query(TBL_NAME, new String[]{KEY_ROWID, FIELD_TO, FIELD_SMS},
                null, null, null, null, null);

        while (cur.moveToNext()) {
            String toNumber = cur.getString(cur.getColumnIndex(FIELD_TO));
            String sms = cur.getString(cur.getColumnIndex(FIELD_SMS));
            Map<String, String> rec = new Hashtable<String, String>();
            rec.put(KEY_TO, toNumber);
            rec.put(KEY_SMS, sms);
            smslist.add(rec);
        }
        
        cur.close();
        
        adapter.notifyDataSetChanged();
    }
    
    protected void saveToDatabase(String toNum, String sms) {
        ContentValues values = new ContentValues();
        values.put(FIELD_TO, "Successfully delivered to " + toNum); //FIXME string constant
        values.put(FIELD_SMS, sms);
        db.insert(TBL_NAME, null, values);
    }
    
}
