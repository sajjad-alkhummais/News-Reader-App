package com.example.android.newsreader;


// this version of the app checks all the news to see if any of them is not
// updated, then it updates it, even if the order is changed in the website,
// it will change here too
// the updating happens once when the app starts



import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ListView newsListView;
    ArrayList<String> newsFetched;
    ArrayAdapter<String> arrayAdapter;
    JSONDownloader json;
    SQLiteDatabase database;
    ArrayList<String> newsUrls;
    Cursor c;
    int numberOfTitles = 5;
    Boolean listViewNeedsUpdate = false;
    Boolean areNewsRefreshed = false;



    public class JSONDownloader extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            String topStoriesData;

            URL url;
            URL propUrl;
            String propUrlString;

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

                    //the id of the article that will be in the url
                    int articleId = (int) arr.get(i);
                    //Setting the url that contain the information of each article
                    propUrlString = "https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty";
                    propUrl = new URL(propUrlString);
                    //now that we have the url, we fetch the json from it
                    topStoriesPropData = fetchingJSONData(propUrl);
                    //we get the information and store it inside variables
                    JSONObject jsonObject = new JSONObject(topStoriesPropData);
                    String title = jsonObject.getString("title");
                    String urlsOfTitles = jsonObject.getString("url");




                    //this is more efficient way to add items to a database
                    String sql = "INSERT INTO news (title, url) VALUES (?, ?)";

                    SQLiteStatement statement = database.compileStatement(sql);
                    statement.bindString(1, title);
                    statement.bindString(2, urlsOfTitles);


                    // If there aren't any news stored in the dataBase then we add news to the data base -->1
                    // And update the variable (listViewNeedsUpdate) to be true because this is indeed
                    // the first time we run the app because the database is empty-->2

                    if (c.getCount() < numberOfTitles) {   //1

                        listViewNeedsUpdate = true;  //2

                        statement.execute();
                    } else {



                        //The iDs in the database starts from 1 then 2 then 3....
                        //So we make "i" suitable for ids by adding 1 to it
                        int idValueFromI = i + 1;

                        //if the atricle's title in this turn is new compared to the old one in the same turn,
                        // then this method will return true

                        if (thereAreNewNews(title, idValueFromI)) {     //4

                            // since there is new title then we delete the row of the old one which will
                            // delete the old url too
                            //database.execSQL("DELETE FROM news WHERE id=" + idValueFromI);    //5

                            //this is how you update elements in SQL database
                            //we update the title and the url in the database
                                database.execSQL("UPDATE news SET title ='"+ title+"'," +
                                         "url ='"+ urlsOfTitles + "'" +
                                        "WHERE id =" + idValueFromI);

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


            //this variable is true in two places, first if this is the first time the app was ever run
            //and we check this above (inside the for loob:  if (c.getCount() < numberOfTitles))
            //the second place we update the variable to be true in is inside (thereAreNewNews) method
            //when it's true listViewNeedsUpdate is also true
            //these are the places where we need to update the list view in
            if (listViewNeedsUpdate) {            // listViewNeedsUpdate = true then there are new news in db that need to be shown

                //we clear the list view in case there was anything before
                newsFetched.clear();
                newsUrls.clear();

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
            //int idIndex = c.getColumnIndex("id");

            c.moveToFirst();
            while (!c.isAfterLast()) {


                newsFetched.add(c.getString(newsTitleIndex));
                newsUrls.add(c.getString(newsUrlIndex));
                //Log.i("id", c.getString(idIndex));
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


        newsListView = findViewById(R.id.newsListView);

        newsUrls = new ArrayList<>();
        newsFetched = new ArrayList<>();

        database = this.openOrCreateDatabase("News", MODE_PRIVATE, null);
        //database.execSQL("CREATE TABLE IF NOT EXISTS news (title VARCHAR, url VARCHAR, id INTEGER PRIMARY KEY)");

        //database.execSQL("DROP TABLE IF EXISTS news");
        database.execSQL("CREATE TABLE IF NOT EXISTS news (title VARCHAR, url VARCHAR, id INTEGER)");

        //database.execSQL("DELETE FROM news");

        //Initialize json
        json = new JSONDownloader();
        //Initialize the arrayAdapter

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, newsFetched);

        Cursor c = database.rawQuery("SELECT * FROM news", null);

        if (c.getCount() > 0) {
            showSavedNews();
            Log.i("databaseItems", "there is: " + c.getCount());
        }
        Log.i("databaseItems", "There are: " + c.getCount());

        c.close();
        json.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");


        areNewsRefreshed = true;


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

    //You give this method a title and
    public boolean thereAreNewNews(String newTitle, int newId) {


        //you have to initialize the cursor everyTime you use it, for an unknown reason
        Cursor c = database.rawQuery("SELECT * FROM news WHERE id =" + newId, null);
        int newsTitleIndex = c.getColumnIndex("title");

        c.moveToFirst();
        String oldTitle = c.getString(newsTitleIndex);

        //if this is true then there aren't new news because the new title equals to the old one
        c.close();
        if (newTitle.equals(oldTitle)) {
            return false;
        } else {
            Log.i("Titles", "old: " + oldTitle + ", new: " + newTitle);
            //we set it to true because there are new news
            listViewNeedsUpdate = true;
            return true;
        }
    }

}
