package info.liuqy.adc.happynewyear;

import info.liuqy.adc.happynewyear.ContactHelper.Language;
import info.liuqy.adc.happynewyear.ContactHelper.Market;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;

public class HappyNewYearActivity extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);  //TODO: design hints: check the layout. "separator" is also view.
	}

    public static final String SENDLIST = "info.liuqy.adc.happynewyear.SENDLIST";
    public static final String CUSTOMER_CARER = "info.liuqy.adc.happynewyear.CUSTOMER_CARER";
    public static final String SMS_TEMPLATE = "info.liuqy.adc.happynewyear.SMS_TEMPLATE";   

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
        
        
        Bundle sendlist = ContactHelper.readContacts(targetMarket, targetLanguage, this);
        
        		//readContacts(targetMarket, targetLanguage);
        
        Intent i = new Intent(this, SendListActivity.class);
        i.putExtra(SENDLIST, sendlist);
        i.putExtra(CUSTOMER_CARER, cc);
        i.putExtra(SMS_TEMPLATE, tmpl);
        startActivity(i);
    }

}