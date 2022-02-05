package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

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
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> contents = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articleDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView titleView = findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        titleView.setAdapter(arrayAdapter);
        titleView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "hey", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), ArticleActivity.class);
                intent.putExtra("content", contents.get(position));
                startActivity(intent);
            }
        });

        articleDB = this.openOrCreateDatabase("NewReaders",MODE_PRIVATE,null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articleinfo (id INTEGER PRIMARY KEY, articleid INTEGER, title VARCHAR, content VARCHAR)");

        updateListView();

        DownloadTask task = new DownloadTask();
        task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");



    }

    public class DownloadTask extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... urls) {
            StringBuilder Json_result = new StringBuilder();
            try {

                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(inputStream);

                int data = reader.read();
                while (data != -1){
                    char current = (char) data;
                    Json_result.append(current);
                    data = reader.read();
                }

                JSONArray jsonArray = new JSONArray(Json_result.toString());
                int num = 20;

                if(jsonArray.length() < 20){
                    num = jsonArray.length();
                }

                articleDB.execSQL("DELETE FROM articleinfo");
                for(int i = 0; i < num; i++){

                    String articleID = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+articleID+".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    inputStream = urlConnection.getInputStream();
                    reader = new InputStreamReader(inputStream);

                    data = reader.read();
                    StringBuilder articleInfo = new StringBuilder();
                    while (data!=-1){
                        char current = (char) data;
                        articleInfo.append(current);
                        data = reader.read();
                    }
                    JSONObject jsonObject = new JSONObject(articleInfo.toString());
                    if (!jsonObject.isNull("title")&&!jsonObject.isNull("url")) {
                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");

                        Log.i("testter", articleUrl+articleTitle+articleID);

                        url = new URL(articleUrl);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        inputStream = urlConnection.getInputStream();
                        reader = new InputStreamReader(inputStream);

                        data = reader.read();
                        StringBuilder articleContent = new StringBuilder();
                        while (data!=-1){
                            char current = (char) data;
                            articleContent.append(current);
                            data = reader.read();
                        }
                        Log.i("tester", articleContent.toString());
                        String sql = "INSERT INTO articleinfo (articleid, title, content) VALUES (? , ? , ?)";
                        SQLiteStatement statement = articleDB.compileStatement(sql);
                        statement.bindString(1, articleID);
                        statement.bindString(2, articleTitle);
                        statement.bindString(3, articleContent.toString());
                        statement.execute();
                    }




                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }
    }

    public void updateListView(){
        Cursor c = articleDB.rawQuery("SELECT * FROM articleinfo",null);
        int titleIndex = c.getColumnIndex("title");
        int contentIndex = c.getColumnIndex("content");

        if (c.moveToFirst()){
            titles.clear();
            contents.clear();

            do {
                titles.add(c.getString(titleIndex));
                titles.add(c.getString(titleIndex));
            }while (c.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        }

    }
}
