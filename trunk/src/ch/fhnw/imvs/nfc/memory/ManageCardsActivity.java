/*
 * Copyright (c) 2012 Fachhochschule Nordwestschweiz (FHNW) All Rights Reserved.
 */

package ch.fhnw.imvs.nfc.memory;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class ManageCardsActivity
    extends Activity
{
    private TextView statusBar;

    private TextView cardCounter;

    private NfcAdapter nfcAdapter;

    private PendingIntent nfcPendingIntent;

    private Vibrator vibrator;

    private final long[] VIBRATOR_ERROR_PATTERN = new long[]
    { 0, 100, 50, 200 };

    private List<String> cards;

    private CardAdapter cardAdapter;

    private Animation animationFadeIn;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.memorize_cards);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        TextView activityTitle = (TextView) findViewById(R.id.left_text);
        activityTitle.setText(R.string.manage_cards_activity_title);
        cardCounter = (TextView) findViewById(R.id.right_text);

        statusBar = (TextView) findViewById(R.id.status_bar);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        cards = new ArrayList<String>();
        animationFadeIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);

        GridView gv = (GridView) findViewById(R.id.cards_grid);
        registerForContextMenu(gv);
        cardAdapter = new CardAdapter(this);
        gv.setAdapter(cardAdapter);

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        try
        {
            cards = JSONSharedPreferences.loadStringList(this, SharedPreferencesKeys.PREF_NAME, SharedPreferencesKeys.CARDS_KEY);
            cardCounter.setText(cards.size() + getString(R.string.number_of_cards));
        }
        catch (JSONException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, null, null);

    }

    @Override
    protected void onPause()
    {
        super.onPause();

        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onBackPressed()
    {
        // check if it's an acceptable number of cards
        if (cards.size() % 2 == 0 || cards.size() % 3 == 0)
        {
            finish();
        }
        else
        {
            // analyze what is to do to solve this problem
            final CharSequence[] possibilities = getResources().getStringArray(R.array.impossible_number_of_cards_dialog);

            new AlertDialog.Builder(this).setItems(possibilities, new OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    switch (which)
                    {
                    // user selected to delete the last card
                    case 0:
                        cards.remove(cards.size() - 1);
                        saveCards();
                        finish();
                        break;
                    // user selects add one card
                    case 1:
                        break;
                    default:
                        finish();
                        break;
                    }
                }
            }).setTitle(String.format(getString(R.string.impossible_number_of_cards_title), cards.size())).show();

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.memorize_cards, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case R.id.delete_memorized_cards:
            new AlertDialog.Builder(this).setTitle(R.string.request_delete_all_cards).setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    // User clicked OK
                    cards.clear();
                    saveCards();
                    updateView();
                }
            }).setNegativeButton(android.R.string.cancel, null).show();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        if (v.getId() == R.id.cards_grid)
        {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.memorize_cards_context, menu);
            menu.setHeaderTitle(cards.get(info.position));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId())
        {
        case R.id.delete_card:
            new AlertDialog.Builder(this).setTitle(R.string.request_delete_card).setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    // User clicked OK
                    cards.remove(info.position);
                    saveCards();
                    updateView();
                }
            }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    // User clicked Cancel
                }
            }).show();
            break;
        }
        return true;
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        String item = getIdFromNfcTag(intent);
        if (item != null && item != "00")
        {
            // if not already scanned tag
            if (!cards.contains(item))
            {
                statusBar.setText(String.format(getString(R.string.tag_added), item));
                vibrator.vibrate(400);
                cards.add(item);
                saveCards();
                updateView();
            }
            else
            {
                statusBar.setText(getString(R.string.tag_already_existing));
                vibrator.vibrate(VIBRATOR_ERROR_PATTERN, -1);
            }
            // set info text back after some delay
            new Handler().postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    statusBar.setText(getString(R.string.scan_tag));
                }
            }, 3500);
        }
    }

    String getIdFromNfcTag(Intent intent)
    {
        String code = null;
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) || NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action))
        {
            byte[] p1 = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            code = getHex(p1);
        }
        return code;
    }

    /**
     * save Cards in the sharedPreferences
     */
    private void saveCards()
    {
        JSONSharedPreferences.saveJSONArray(this, SharedPreferencesKeys.PREF_NAME, SharedPreferencesKeys.CARDS_KEY, new JSONArray(cards));
    }

    private void updateView()
    {
        cardAdapter.setAnimationStatus(true);
        cardAdapter.notifyDataSetChanged();

        cardCounter.setText(cards.size() + getString(R.string.number_of_cards));
    }

    public static String getHex(byte[] raw)
    {
        final String HEXES = "0123456789ABCDEF";
        if (raw == null)
        {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw)
        {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }

    /**
     * to provide the cards for the gridView
     * 
     * @author jonas.lauener
     * 
     */
    public class CardAdapter
        extends BaseAdapter
    {
        private Context context;

        private boolean withAnimation;

        public CardAdapter(Context c)
        {
            context = c;
            withAnimation = false;
        }

        @Override
        public int getCount()
        {
            return cards.size();
        }

        @Override
        public Object getItem(int position)
        {
            return cards.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            ImageView imageView;
            if (convertView == null)
            {
                imageView = new ImageView(context);
                imageView.setLayoutParams(new GridView.LayoutParams(80, 80));
                imageView.setAdjustViewBounds(false);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
                if (withAnimation)
                {
                    imageView.startAnimation(animationFadeIn);
                }
            }
            else
            {
                imageView = (ImageView) convertView;
            }

            imageView.setImageResource(R.drawable.backimage);
            return imageView;
        }

        public void setAnimationStatus(boolean status)
        {
            withAnimation = status;
        }
    }
}
