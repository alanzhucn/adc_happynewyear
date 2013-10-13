package info.liuqy.adc.happynewyear;

import info.liuqy.adc.happynewyear.ContactHelper.Language;
import info.liuqy.adc.happynewyear.ContactHelper.Market;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;

/**
 * 
 * read-me:
 *     v0.5 2013-10-13 the textview in SendListActivity.java is hidden after update the List.
 *                 when we set it to sendlist.xml
 *                             android:id = "@android:id/empty"
 *                 so it is caused by id.  
 */
public class HappyNewYearActivity extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main); 
		
		//TODO: design hints: check the layout. "separator" is also view.
		//  <View style="@style/Separator" />
		// res/values/theme.xml
	}

    public static final String TARGET_MARKET = "info.liuqy.adc.happynewyear.TARGET_MARKET";
    public static final String TARGET_LANGUAGE = "info.liuqy.adc.happynewyear.TARGET_LANGUAGE";
    public static final String CUSTOMER_CARER = "info.liuqy.adc.happynewyear.CUSTOMER_CARER";
    public static final String SMS_TEMPLATE = "info.liuqy.adc.happynewyear.SMS_TEMPLATE";   
    
    public static final String SEND_LIST =  "info.liuqy.adc.happynewyear.SENDER_LIST";
    public static final String SEND_LIST_PARMS = "info.liuqy.adc.happynewyear.SENDER_LIST_PARMS";
    

    public void genSendlist(View v) {
        RadioGroup rg = (RadioGroup)this.findViewById(R.id.customer_group);
        int id = rg.getCheckedRadioButtonId();
        Market targetMarket = (id == R.id.btn_north) ? Market.NORTH : Market.SOUTH;

        rg = (RadioGroup)this.findViewById(R.id.customer_lang);
        id = rg.getCheckedRadioButtonId();
        Language targetLanguage = (id == R.id.btn_cn) ? Language.CHINESE : Language.ENGLISH;

        Spinner sp = (Spinner)this.findViewById(R.id.customer_carer);
        String cc = sp.getSelectedItem().toString();
        
        EditText et = (EditText)this.findViewById(R.id.sms_template);
        String tmpl = et.getText().toString();
                
        Intent i = new Intent(this, SendListActivity.class);
        // TODO: what we get if we use i.putExtra(xxx, object);
        // i.putExtra(TARGET_LANGUAGE, targetLanguage.toString());
        
        i.putExtra(TARGET_LANGUAGE, targetLanguage.toString());
        i.putExtra(TARGET_MARKET, targetMarket.toString());
        i.putExtra(CUSTOMER_CARER, cc);
        i.putExtra(SMS_TEMPLATE, tmpl);
        
        startActivity(i);
    }

}