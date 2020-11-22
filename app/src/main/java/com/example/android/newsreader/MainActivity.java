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

    Boolean areThereNews = false;
    Boolean areNewsRefreshed = false;

    public class  JSONDownloader extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            String topStoriesData = "";

            URL url;
            URL propUrl = null;
            String propUrlString;
            HttpURLConnection urlConnection, urlConnection1 = null;

            try {
                //the top stories url
                url = new URL(urls[0]);
                //fetching data from it and store it in this string
                topStoriesData = fetchingJSONData(url);


                JSONArray arr = new JSONArray(topStoriesData);

                String topStoriesPropData = "";
                Log.i("Case", "Json ran");

                for (int i = 0; i < numberOfTitles; i++) {
                    Log.i("Case", "Json pulling data");


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


                    c = database.rawQuery("SELECT * FROM news", null);


                    String s = "INSERT INTO news (title, url) VALUES ('" + title + "','" + urlsOfTitles + "')";
                    database.execSQL(s);

                    //showSavedNews();

                    Log.i("CaseOfDb", "ThereIsNone");
                    /* else {
                        Log.i("CaseOfDb", "ThereIsSome");

                        if(i==0){

                            areThereNews = thereAreNewNews(title);
                            Log.i("CaseOfNewNews", "There is new news:" + areThereNews.toString());

                        }
                        if (areThereNews) {

                            if(i == 0) {
                                database.execSQL("DELETE FROM news");
                                newsFetched.clear();
                                newsUrls.clear();
                            }
                            String s = "INSERT INTO news (title, url) VALUES ('" + title + "','" + urlsOfTitles + "')";
                            database.execSQL(s);


                            if(i == 4) {
                                showSavedNews();
                            }

                            Log.i("CaseOfNewNews", "There is new news");


                        }
                    }*/


                }


                return topStoriesPropData;
            } catch (Exception e) {

                return null;
            }

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            showSavedNews();
        }
    }


    //this method gets the data out of the data base and add the information to the arraysLists
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
            Log.i("counterOfShowing3", "1 ");

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

        newsUrls = new ArrayList<String>();
        newsFetched = new ArrayList<String>();

        database = this.openOrCreateDatabase("News", MODE_PRIVATE, null);
        database.execSQL("CREATE TABLE IF NOT EXISTS news (title VARCHAR, url VARCHAR)");

        //database.execSQL("DELETE FROM news");
        json = new JSONDownloader();
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, newsFetched);

        Cursor c = database.rawQuery("SELECT * FROM news", null);

        //if there aren't anything in the database then we run the json to put the information
        if (c.getCount() < 1) {

            json.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

            Log.i("ThereIs ", String.valueOf(c.getCount()));
            areNewsRefreshed = true;

        } else {
            showSavedNews();

        }

        c.close();


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

            HttpURLConnection urlConnection = null;

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


        int newsTitleIndex = c.getColumnIndex("title");

        String oldTitle = c.getString(newsTitleIndex);
        c.moveToFirst();

        Log.i("Titles", "old: " + oldTitle + ", new: " + newTitle);
        //if this is true then there aren't new news because the new title equals to the old one
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
