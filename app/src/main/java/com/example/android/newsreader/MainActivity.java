package com.example.android.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.service.autofill.FieldClassification;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    ListView newsListView;
    ArrayList<String> newsFetched;
    ArrayAdapter<String> arrayAdapter;
    JSONDownloader json;
    SQLiteDatabase database;
    ArrayList<String> newsUrls;
    Cursor c;
    int numberOfTitles = 5;

    Button refreshNewsBtn;

    Boolean listViewNeedsUpdate = false;
    Boolean areNewsRefreshed = false;

    public class JSONDownloader extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            String topStoriesData;

            URL url;
            URL propUrl;
            String propUrlString;
            HttpURLConnection urlConnection, urlConnection1 = null;

            try {
                //storing the top stories url into "url" URL variable
                url = new URL(urls[0]);
                //fetching data from it and store it in this string
                topStoriesData = fetchingJSONData(url);


                //We store the json array of top stories inside arr variable
                JSONArray arr = new JSONArray(topStoriesData);

                String topStoriesPropData = "";

                //We initialize the cursor every time we want to use it to get how many titles are
                //in the data base
                Cursor c = database.rawQuery("SELECT * FROM news", null);

                for (int i = 0; i < numberOfTitles; i++) {
                    Log.i("Count", Integer.toString(i));


                    //Setting the url that contain the information of each report
                    propUrlString = "https://hacker-news.firebaseio.com/v0/item/" + arr.get(i) + ".json?print=pretty";
                    propUrl = new URL(propUrlString);
                    //now that we have the url, we fetch the json from it
                    topStoriesPropData = fetchingJSONData(propUrl);
                    //we get the information and store it inside variables
                    JSONObject jsonObject = new JSONObject(topStoriesPropData);

                    String title = jsonObject.getString("title").replace("\'", "");


                    Log.i("titles", title);
                    String urlsOfTitles = jsonObject.getString("url");
                    //Log.i("titles", title);
                    //Log.i("urls", urlsOfTitles);


                    String s = "INSERT INTO news (title, url) VALUES ('" + title + "','" + urlsOfTitles + "')";


                    //If there aren't any news stored in the dataBase then we add news to the data base -->1
                    //And update the variable (listViewNeedsUpdate) to be true because this is indeed
                    // the first time we run the app because the database is empty-->2

                    if (c.getCount() < numberOfTitles) {   //1
                        
                        listViewNeedsUpdate = true;  //2

                        database.execSQL(s);

                    } else {


                        //1-If there are news in the DB and i = 0 which means it is the first turn of the loop (we can't
                        //keep checking we need to check only one time, then we decide whether we need to
                        //keep adding the news and the urls 5 times or not, according to the result).
                        //2-Check if stored news are updated.
                        //3-if the news are up to date, then do nothing
                        //4-if they aren't and (i = 0) it is the first turn,
                        //5-then delete the old news from the data base
                        //6-and clear the arrays
                        //7-Then add the new news to the dataBase

                        if (i == 0) {        //1


                            listViewNeedsUpdate = thereAreNewNews(title);     //2


                        }

                        if (listViewNeedsUpdate) {     //4

                            if (i == 0) {  //4
                                database.execSQL("DELETE FROM news");    //5
                                newsFetched.clear();                     //6
                                newsUrls.clear();                        //6
                            }

                            database.execSQL(s);  //7
                        }
                    }

                }
                //We close the cursor since We don't need it any more
                c.close();

                return topStoriesPropData;
            } catch (Exception e) {

                return null;
            }

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            //If there are new news in the data base or if this is the first time the user
            // run the app, then the database will be empty, but we  put the news after fetching in doInBackground method
            // which onPostExecute method run after, so there will be news in the data base then we
            //need to show them
            //
            if (listViewNeedsUpdate ) {            // listViewNeedsUpdate = true then there are new news in db that need to be shown


                //update/Show them by updating the list view arrays
                showSavedNews();

            }
        }
    }


    //this method gets the data out of the data base then add the information to the arraysLists
    //and then it notifies the array adapter
    public void showSavedNews() {
        try {

            c = database.rawQuery("SELECT * FROM news", null);
            int newsTitleIndex = c.getColumnIndex("title");
            int newsUrlIndex = c.getColumnIndex("url");

            c.moveToFirst();
            while (!c.isAfterLast()) {

                newsFetched.add(c.getString(newsTitleIndex));
                newsUrls.add(c.getString(newsUrlIndex));

                c.moveToNext();
                arrayAdapter.notifyDataSetChanged();


            }
            //We close the cursor since We don't need it any more

            c.close();

            Log.i("counterOfShowing", "1 ");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        refreshNewsBtn = (Button) findViewById(R.id.refreshButton);

        newsListView = (ListView) findViewById(R.id.newsListView);

        newsUrls = new ArrayList<>();
        newsFetched = new ArrayList<>();

        database = this.openOrCreateDatabase("News", MODE_PRIVATE, null);
        database.execSQL("CREATE TABLE IF NOT EXISTS news (title VARCHAR, url VARCHAR)");

        database.execSQL("DELETE FROM news");

        //Initialize json
        json = new JSONDownloader();
        //Initialize the arrayAdapter

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, newsFetched);

        Cursor c = database.rawQuery("SELECT * FROM news", null);

        if (c.getCount() > 0) {
            showSavedNews();
            Log.i("databa", "there is: " + c.getCount());
        }
        Log.i("datab", "There are: " + c.getCount() );

        c.close();
        json.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");


        Log.i("ThereIs ", String.valueOf(c.getCount()));
        areNewsRefreshed = true;


        Log.i("IsThere?", String.valueOf(c.getCount()));

        newsListView.setAdapter(arrayAdapter);

        newsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), WebActivity.class);

                intent.putExtra("clickedReportUrl", newsUrls.get(position));

                startActivity(intent);


            }
        });

    }

    //this is an unused function
    public void addMovie(String title, int year) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("year", year);
        //database.insert( , null, values);
        database.close();
    }

    public String fetchingJSONData(URL url) {
        String result = "";

        try {

            HttpURLConnection urlConnection;

            urlConnection = (HttpURLConnection) url.openConnection();

            InputStream in = urlConnection.getInputStream();

            InputStreamReader reader = new InputStreamReader(in);
            int data = reader.read();

            while (data != -1) {

                char current = (char) data;
                result += current;

                data = reader.read();
            }

            return result;
        } catch (Exception e) {


            e.printStackTrace();
            return null;
        }
    }

    public boolean thereAreNewNews(String newTitle) {


        //you have to initialize the cursor everyTime you use it, for an unknown reason
        Cursor c = database.rawQuery("SELECT * FROM news", null);
        int newsTitleIndex = c.getColumnIndex("title");

        c.moveToFirst();

        String oldTitle = c.getString(newsTitleIndex);

        Log.i("Titles", "old: " + oldTitle + ", new: " + newTitle);
        //if this is true then there aren't new news because the new title equals to the old one
        c.close();
        if (newTitle.equals(oldTitle)) {
            return false;
        } else {

            return true;
        }
    }

    public void refreshNews(View view) {


        database.execSQL("DELETE FROM news");
        newsFetched.clear();
        newsUrls.clear();
        if (!areNewsRefreshed) {

            json.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
            areNewsRefreshed = true;
        } else {

            Toast.makeText(this, "All News Are UpToDate", Toast.LENGTH_SHORT).show();
        }


    }
}
