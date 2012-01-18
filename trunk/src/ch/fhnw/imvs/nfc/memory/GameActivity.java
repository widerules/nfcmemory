/*
 * Copyright (c) 2012 Fachhochschule Nordwestschweiz (FHNW)
 * All Rights Reserved. 
 */

package ch.fhnw.imvs.nfc.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class GameActivity
    extends Activity
{
    private NfcAdapter nfcAdapter;

    private PendingIntent nfcPendingIntent;

    private Vibrator vibrator;

    private final long[] VIBRATOR_ERROR_PATTERN = new long[]
    { 0, 100, 50, 200 };

    private LinearLayout gameView;

    private ImageView[] cardViews;

    private LinearLayout playersView;

    private TextView[] playerTextViews;

    private Map<String, Card> cards;

    private int numberOfCards;

    private int pairSize;

    private List<Player> players;

    private int curPlayerIndex;

    private List<String> showedCards;

    private int cardBackImage;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcPendingIntent =
            PendingIntent.getActivity(this, 0, new Intent(this, getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        playersView = (LinearLayout) findViewById(R.id.players_view_layout);
        gameView = (LinearLayout) findViewById(R.id.game_view_layout);

        
        players = new ArrayList<Player>();
        cards = new HashMap<String, Card>();
        showedCards = new ArrayList<String>();

        // set the back image of the card
        cardBackImage = R.drawable.backimage;

        // for not showing the to back image transition
        showedCards.add("firstStart");

        setPlayers();

        setCards();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, null, null);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        nfcAdapter.disableForegroundDispatch(this);
    }

    private void setPlayers()
    {
        List<String> playerNames = new ArrayList<String>();
        try
        {
            playerNames =
                JSONSharedPreferences.loadStringList(this,
                    SharedPreferencesKeys.PREF_NAME,
                    SharedPreferencesKeys.PLAYERS_KEY);
        }
        catch (JSONException e)
        {
        }

        // if no players are set, show alert which redirects to
        // ManagePlayersActivity
        if (playerNames.size() > 0)
        {
            for (String item : playerNames)
            {
                players.add(new Player(item));
            }

            playerTextViews = new TextView[players.size()];
            for (int i = 0; i < playerTextViews.length; i++)
            {
                playerTextViews[i] = new TextView(this);
                playerTextViews[i].setTextSize(25);
                playerTextViews[i].setPadding(5, 0, 5, 0);
                playersView.addView(playerTextViews[i]);
            }
            curPlayerIndex = players.size() - 1;
            setNextPlayer(false);
            updatePlayersView();
        }
        else
        {
            new AlertDialog.Builder(this)
                .setTitle(R.string.not_enough_players)
                .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            startActivity(new Intent(GameActivity.this,
                                ManagePlayersActivity.class));
                            finish();
                        }
                    }).show();
        }
    }

    private void setCards()
    {
        List<String> tmpNfcTags = new ArrayList<String>();
        try
        {
            // get saved cards from the sharedPreferences
            tmpNfcTags =
                JSONSharedPreferences.loadStringList(this,
                    SharedPreferencesKeys.PREF_NAME,
                    SharedPreferencesKeys.CARDS_KEY);
        }
        catch (JSONException e)
        {
        }

        final List<String> nfcTags = tmpNfcTags;

        numberOfCards = nfcTags.size();

        if (numberOfCards % 2 == 0 && numberOfCards % 3 == 0
            && numberOfCards != 0)
        {
            new AlertDialog.Builder(this)
                .setTitle(R.string.select_pair_size)
                .setPositiveButton(R.string.double_pairs,
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            // user selected double pairs
                            pairSize = 2;
                            generateCardsAndImageViews(nfcTags);
                        }
                    })
                .setNegativeButton(R.string.triple_pairs,
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            // user selected triple pairs
                            pairSize = 3;
                            generateCardsAndImageViews(nfcTags);
                        }
                    }).show();
        }
        else if (numberOfCards % 2 == 0 && numberOfCards != 0)
        {
            pairSize = 2;
            generateCardsAndImageViews(nfcTags);
        }
        else if (numberOfCards % 3 == 0 && numberOfCards != 0)
        {
            pairSize = 3;
            generateCardsAndImageViews(nfcTags);
        }
        else
        {
            pairSize = 0;
            // if no or not the right number of cards were set, show alert which
            // redirects to ManageCardsActivity
            new AlertDialog.Builder(this)
                .setTitle(
                    String.format(
                        getString(R.string.impossible_number_of_cards_title),
                        numberOfCards))
                .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            startActivity(new Intent(GameActivity.this,
                                ManageCardsActivity.class));
                            finish();
                        }
                    }).show();
        }
    }

    private void generateCardsAndImageViews(List<String> nfcTags)
    {
        List<Integer> imageIdList = getImageIDList();

        if (numberOfCards % pairSize == 0)
        {
            int n = 1;
            int j = 0;
            for (int i = 0; i < numberOfCards; i++)
            {
                cards.put(nfcTags.get(i), new Card(imageIdList.get(j)));
                if (n == pairSize)
                {
                    n = 1;
                    j++;
                }
                else
                {
                    n++;
                }
            }

            // randomize cards like bubblesort principle
            Random rnd = new Random();

            for (int i = 0; i < 100; i++)
            {
                int rnd1 = rnd.nextInt(nfcTags.size());
                int rnd2 = rnd.nextInt(nfcTags.size());

                Card oldCard = cards.get(nfcTags.get(rnd1));
                cards.put(nfcTags.get(rnd1), cards.get(nfcTags.get(rnd2)));
                cards.put(nfcTags.get(rnd2), oldCard);
            }
        }

        // create ImageView for cards
        cardViews = new ImageView[pairSize];
        for (int i = 0; i < cardViews.length; i++)
        {
            cardViews[i] = new ImageView(this);
            cardViews[i].setLayoutParams(new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, (float) 1
                    / pairSize));

            cardViews[i].setPadding(0, 10, 0, 10);
            cardViews[i].setImageResource(cardBackImage);
            gameView.addView(cardViews[i]);
        }
    }

    private void invokeActivity(final String id)
    {
        // if a nfc tag is detected and it exists
        if (cards.containsKey(id))
        {
            // if the card not is already found and showed already in this round
            if (!cards.get(id).isTaken && !showedCards.contains(id))
            {
                vibrator.vibrate(400);

                long delay = 0;
                if (showedCards.size() == 0)
                {
                    // turn the cards to the back side
                    for (ImageView item : cardViews)
                    {
                        applyRotation(item, -1, 180, 90);
                    }
                    delay = 1000;
                }
                else if (showedCards.get(0).equals("firstStart"))
                {
                    showedCards.clear();
                }

                final int showedCardsSize = showedCards.size();
                Handler handler = new Handler();
                // prevent turning card if it's already turning to the back side
                handler.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        applyRotation(cardViews[showedCardsSize], cards.get(id)
                            .getImageId(), 0, 90);
                    }
                }, delay);

                showedCards.add(id);

                // true if it is the last card in the round
                if (showedCards.size() >= pairSize)
                {
                    // test if the showedCards are the same image i.e. pair
                    // found
                    boolean allSameImage = true;
                    int tmpId = 0;
                    for (String item : showedCards)
                    {
                        if (cards.get(item).getImageId() != tmpId && tmpId != 0)
                        {
                            allSameImage = false;
                        }
                        tmpId = cards.get(item).getImageId();
                    }

                    if (allSameImage)
                    {
                        String found = "";
                        if (pairSize == 2)
                        {
                            found = getString(R.string.double_found);
                        }
                        else if (pairSize == 3)
                        {
                            found = getString(R.string.triple_found);
                        }
                        // show message if a pair was found
                        Toast toast = Toast.makeText(this, found, 2000);
                        toast.show();
                        for (String item : showedCards)
                        {
                            cards.get(item).isTaken = true;
                        }
                        players.get(curPlayerIndex).addPoints(5);
                        updatePlayersView();
                        if (allCardsTaken())
                        {
                            gameFinish();
                        }
                    }
                    else
                    {
                        setNextPlayer(true);
                    }
                    showedCards.clear();
                }
            }
            else
            {
                vibrator.vibrate(VIBRATOR_ERROR_PATTERN, -1);
            }
        }
        else
        {
            vibrator.vibrate(VIBRATOR_ERROR_PATTERN, -1);
        }
    }

    private List<Integer> getImageIDList()
    {
        List<Integer> imageIdList = new ArrayList<Integer>();
        imageIdList.add(R.drawable.android1);
        imageIdList.add(R.drawable.android2);
        imageIdList.add(R.drawable.android3);
        imageIdList.add(R.drawable.android4);
        imageIdList.add(R.drawable.android5);
        imageIdList.add(R.drawable.android6);
        imageIdList.add(R.drawable.android7);
        imageIdList.add(R.drawable.android8);
        imageIdList.add(R.drawable.android9);
        imageIdList.add(R.drawable.android10);
        imageIdList.add(R.drawable.android11);
        imageIdList.add(R.drawable.android12);
        imageIdList.add(R.drawable.android13);
        imageIdList.add(R.drawable.android14);
        imageIdList.add(R.drawable.android15);
        imageIdList.add(R.drawable.android16);
        imageIdList.add(R.drawable.android17);
        imageIdList.add(R.drawable.android18);
        imageIdList.add(R.drawable.android19);
        imageIdList.add(R.drawable.android20);
        imageIdList.add(R.drawable.android21);
        imageIdList.add(R.drawable.android22);
        imageIdList.add(R.drawable.android23);
        imageIdList.add(R.drawable.android24);
        imageIdList.add(R.drawable.android25);
        imageIdList.add(R.drawable.android26);
        imageIdList.add(R.drawable.android27);
        imageIdList.add(R.drawable.android28);
        imageIdList.add(R.drawable.android29);
        imageIdList.add(R.drawable.android30);

        Random rnd = new Random();
        for (int i = 0; i < 100; i++)
        {
            int rnd1 = rnd.nextInt(imageIdList.size());
            int rnd2 = rnd.nextInt(imageIdList.size());

            int oldCardImage = imageIdList.get(rnd1);
            imageIdList.set(rnd1, imageIdList.get(rnd2));
            imageIdList.set(rnd2, oldCardImage);
        }
        return imageIdList;
    }

    private void setNextPlayer(boolean delay)
    {
        long delayMs = 0;
        if (delay)
        {
            delayMs = 2500;
        }
        final int oldPlayerIndex = curPlayerIndex;
        curPlayerIndex++;

        // set next player with a delay because of better usability
        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                playerTextViews[oldPlayerIndex]
                    .setBackgroundColor(Color.TRANSPARENT);
                playerTextViews[oldPlayerIndex].setTextColor(Color.LTGRAY);

                if (curPlayerIndex >= players.size())
                {
                    curPlayerIndex = 0;
                }
                playerTextViews[curPlayerIndex]
                    .setBackgroundDrawable(getResources().getDrawable(
                        R.drawable.roundedcorner));
                playerTextViews[curPlayerIndex].setTextColor(Color.BLACK);
            }
        }, delayMs);
    }

    private void updatePlayersView()
    {
        for (int i = 0; i < playerTextViews.length; i++)
        {
            playerTextViews[i].setText(players.get(i).getPlayerName() + ": "
                + players.get(i).getScore());
        }
    }

    private boolean allCardsTaken()
    {
        for (Card item : cards.values())
        {
            if (!item.isTaken)
            {
                return false;
            }
        }
        return true;
    }

    private void gameFinish()
    {
        // sort player per score
        Collections.sort(players);
        Collections.reverse(players);

        // show dialog with a table of the scores and buttons to finish or play
        // again
        final Dialog gameFinishedDialog = new Dialog(GameActivity.this);
        gameFinishedDialog.setContentView(R.layout.game_finished);

        TableLayout playerTable =
            (TableLayout) gameFinishedDialog.findViewById(R.id.player_table);

        String winners = "";
        for (int i = 0; i < players.size(); i++)
        {
            TableRow playerRow = new TableRow(gameFinishedDialog.getContext());

            TextView name = new TextView(gameFinishedDialog.getContext());
            name.setText(players.get(i).getPlayerName());
            name.setTextSize(18);
            name.setPadding(5, 3, 3, 3);

            TextView score = new TextView(gameFinishedDialog.getContext());
            score.setText(String.valueOf(players.get(i).getScore()));
            score.setTextSize(18);
            score.setPadding(3, 3, 5, 3);
            score.setGravity(Gravity.RIGHT);

            // mark the players with the most of the points
            if (i == 0)
            {
                name.setBackgroundColor(Color.YELLOW);
                name.setTextColor(Color.BLACK);
                score.setBackgroundColor(Color.YELLOW);
                score.setTextColor(Color.BLACK);
                winners = players.get(i).getPlayerName();
            }
            else
            {
                if (players.get(0).getScore() == players.get(i).getScore())
                {
                    name.setBackgroundColor(Color.YELLOW);
                    name.setTextColor(Color.BLACK);
                    score.setBackgroundColor(Color.YELLOW);
                    score.setTextColor(Color.BLACK);
                    winners += ", " + players.get(i).getPlayerName();
                }
            }

            playerRow.addView(name);
            playerRow.addView(score);
            playerTable.addView(playerRow);
        }
        // set dialog title with the names of the winners
        gameFinishedDialog.setTitle(String.format(
            getString(R.string.winner_is), winners));

        Button buttonBackToMenu =
            (Button) gameFinishedDialog.findViewById(R.id.buttonQuit);
        buttonBackToMenu.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        });

        Button buttonPlayAgain =
            (Button) gameFinishedDialog.findViewById(R.id.buttonPlayAgain);
        buttonPlayAgain.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // restart gameActivity
                gameFinishedDialog.dismiss();
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        });

        gameFinishedDialog.show();
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        String item = getIdFromNfcTag(intent);
        if (item != null && item != "00")
        {
            invokeActivity(item);
        }
    }

    private String getIdFromNfcTag(Intent intent)
    {
        String code = null;
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)
            || NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
            || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action))
        {
            byte[] p1 = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            code = getHex(p1);
        }
        return code;
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
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(
                HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }

    /**
     * Setup a new 3D rotation on the container view.
     * 
     * @param position the item that was clicked to show a picture, or -1 to
     *            show the list
     * @param start the start angle at which the rotation must begin
     * @param end the end angle of the rotation
     */
    private void applyRotation(ImageView imgView, int position, float start,
        float end)
    {
        // Find the center of the container
        final float centerX = imgView.getWidth() / 2.0f;
        final float centerY = imgView.getHeight() / 2.0f;

        // Create a new 3D rotation with the supplied parameter
        // The animation listener is used to trigger the next animation
        final Rotate3dAnimation rotation =
            new Rotate3dAnimation(start, end, centerX, centerY, 310.0f, true);
        rotation.setDuration(500);
        rotation.setFillAfter(true);
        rotation.setInterpolator(new AccelerateInterpolator());
        rotation.setAnimationListener(new DisplayNextView(imgView, position));

        imgView.startAnimation(rotation);
    }

    /**
     * This class listens for the end of the first half of the animation. It
     * then posts a new action that effectively swaps the views when the
     * container is rotated 90 degrees and thus invisible.
     */
    private final class DisplayNextView
        implements Animation.AnimationListener
    {
        private final int mPosition;

        private final ImageView mImgView;

        private DisplayNextView(ImageView imgView, int position)
        {
            mPosition = position;
            mImgView = imgView;
        }

        public void onAnimationStart(Animation animation)
        {
        }

        public void onAnimationEnd(Animation animation)
        {
            mImgView.post(new SwapViews(mImgView, mPosition));
        }

        public void onAnimationRepeat(Animation animation)
        {
        }
    }

    /**
     * This class is responsible for swapping the views and start the second
     * half of the animation.
     */
    private final class SwapViews
        implements Runnable
    {
        private final int mPosition;

        private final ImageView mImgView;

        public SwapViews(ImageView imgView, int position)
        {
            mImgView = imgView;
            mPosition = position;
        }

        public void run()
        {
            final float centerX = mImgView.getWidth() / 2.0f;
            final float centerY = mImgView.getHeight() / 2.0f;
            Rotate3dAnimation rotation;

            if (mPosition > -1)
            {
                mImgView.setImageResource(mPosition);

                rotation =
                    new Rotate3dAnimation(90, 180, centerX, centerY, 310.0f,
                        false);
            }
            else
            {
                mImgView.setImageResource(cardBackImage);

                rotation =
                    new Rotate3dAnimation(90, 0, centerX, centerY, 310.0f,
                        false);
            }

            rotation.setDuration(500);
            rotation.setFillAfter(true);
            rotation.setInterpolator(new DecelerateInterpolator());

            mImgView.startAnimation(rotation);
        }
    }

}