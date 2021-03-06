/**
 * 
 */
package info.liuqy.adc.happynewyear;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Phone;

/**
 * @author 
 *
 */
public class ContactHelper {

	enum Market {
		NORTH, SOUTH, ANY;
		@Override
		public String toString() {
			switch (this) {
			case NORTH:
				return "NC";
			case SOUTH:
				return "SC";
			case ANY:
				return "";
			default:
				return super.toString();
			}
		}
		
		public static Market fromString (String data) {
			
			if ("NC".equalsIgnoreCase(data)) {
				return NORTH;
			}
			
			if ("SC".equalsIgnoreCase(data)) {
				return SOUTH;
			}
			
			return ANY;
		}
	};

	enum Language {
		CHINESE, ENGLISH, ANY;
		@Override
		public String toString() {
			switch (this) {
			case CHINESE:
				return "CN";
			case ENGLISH:
				return "EN";
			case ANY:
				return "";
			default:
				return super.toString();
			}
		}
		
		public static Language fromString (String data) {
			
			if ("CN".equalsIgnoreCase(data)) {
				return CHINESE;
			}
			
			if ("EN".equalsIgnoreCase(data)) {
				return ENGLISH;
			}
			
			return ANY;
		}
	};


	/**
	 * Return all number ~ nickname pairs according to the rule. Be careful: the
	 * same numbers will be in only one pair.
	 * 
	 * @return <number, nickname>s
	 */
	static public Bundle readContacts(Market market, Language lang, Activity Ac) {
		Bundle sendlist = new Bundle();

		/*
		 * ContactsContract defines an extensible database of contact-related
		 * information. Contact information is stored in a three-tier data
		 * model:
		 * 
		 * A row in the ContactsContract.Data table can store any kind of
		 * personal data, such as a phone number or email addresses. The set of
		 * data kinds that can be stored in this table is open-ended. There is a
		 * predefined set of common kinds, but any application can add its own
		 * data kinds.
		 * 
		 * A row in the ContactsContract.RawContacts table represents a set of
		 * data describing a person and associated with a single account (for
		 * example, one of the user's Gmail accounts).
		 * 
		 * A row in the ContactsContract.Contacts table represents an aggregate
		 * of one or more RawContacts presumably describing the same person.
		 * When data in or associated with the RawContacts table is changed, the
		 * affected aggregate contacts are updated as necessary.
		 * 
		 * In this program, what we want to get are the <phone number, nickname,
		 * note> data triplets. So let's go through the contacts.
		 */
		Cursor cur = Ac.getContentResolver().query(
				ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

		// attributes for the contact
		Set<String> attrs = new HashSet<String>();
		
		while (cur.moveToNext()) {
			String contactId = cur.getString(cur.getColumnIndex(Contacts._ID));

			// retrieve phone numbers
			int phoneCount = cur.getInt(cur
					.getColumnIndex(Contacts.HAS_PHONE_NUMBER));

			// only process contacts with phone numbers
			if (phoneCount > 0) {

				Cursor nicknames = Ac.getContentResolver().query(
						Data.CONTENT_URI,
						new String[] { Data._ID, Nickname.NAME },              //TODO: this could be a common variable.
						Data.CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
								+ Nickname.CONTENT_ITEM_TYPE + "'",              //TODO: also a common variable.
						new String[] { contactId }, null);

				// only process contacts with nickname (the first one)
				if (nicknames.moveToFirst()) {
					String nickname = nicknames.getString(nicknames  
                            .getColumnIndex(Nickname.NAME));
					
					Cursor notes = Ac.getContentResolver().query(  
	                        Data.CONTENT_URI,  
	                        new String[] { Data._ID, Note.NOTE },  
	                        Data.CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"  
	                                + Note.CONTENT_ITEM_TYPE + "'",  
	                        new String[] { contactId }, null);
					
					// retrieve all attributes from all notes
					attrs.clear();
					while (notes.moveToNext()) {
						String noteinfo = notes.getString(notes  
                                .getColumnIndex(Note.NOTE));
						
						//FIXME better regex?
						String[] fragments = noteinfo.toUpperCase(Locale.US).split(",");
						
						for (String attr : fragments) {
							//remove blank spaces.
							String temp = attr.trim();
							if (null!=temp && 0 < temp.length() ) {
							      attrs.add(temp);
							}
						}
					}
					
					notes.close();
					
					//set defaults
					if (!attrs.contains(Market.NORTH.toString())
							&& !attrs.contains(Market.SOUTH.toString()))
						attrs.add(Market.NORTH.toString());
					
					if (!attrs.contains(Language.CHINESE.toString())
							&& !attrs.contains(Language.ENGLISH.toString()))
						attrs.add(Language.CHINESE.toString());
					
					// only process contacts with the matching market & language
					if (attrs.contains("ADC") //FIXME for class demo only
							&& (market.equals(Market.ANY) || attrs.contains(market.toString())) 
							&& (lang.equals(Language.ANY) || attrs.contains(lang.toString()))) {
						
						Cursor phones = Ac.getContentResolver().query(
								ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
								null, Phone.CONTACT_ID + "=" + contactId, null, null);

						// process all phone numbers
						while (phones.moveToNext()) {
							String phoneNumber = phones.getString(phones
									.getColumnIndex(Phone.NUMBER));
							int phoneType = phones.getInt(phones
									.getColumnIndex(Phone.TYPE));
							
							if (ContactHelper.isMobile(phoneNumber, phoneType)) {
								sendlist.putString(phoneNumber, nickname);
							}
						}
						
						phones.close();
					}
				}
				
				nicknames.close();
			}
		}
		
		cur.close();

		return sendlist;
	}
	
	
	// the tricky pattern for identifying Chinese mobile numbers
	static final Pattern MOBILE_PATTERN = Pattern.compile("(13|15|18)\\d{9}");

	/**
	 * utility isMobile to determine if it is a mobile directory number.
	 * @param number - 
	 * @param type - 
	 * @return
	 */
	static public boolean isMobile(String number, int type) {
		if (type == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) {
			Matcher m = MOBILE_PATTERN.matcher(number);
			
			if (m.find()) {
				return true;
			}
		}
		
		// FIXME: the codes use tricky pattern to check if it is for Chinese mobile.
		//return false;
		return true;
	}

}
