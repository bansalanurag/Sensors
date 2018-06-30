package com.iiit.a3_mt17005;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Anurag Bansal on 21-Apr-18.
 */

public class Database extends SQLiteOpenHelper {


    private static final String TAG = "DatabaseHelper";
    private static final String TABLE_NAME = "Sensors";

    private static final String COL1 = "Accelerometer";
    private static final String COL2 = "Gyroscope";
    private static final String COL3 = "GPS";
    private static final String COL4 = "Cell";
    private static final String COL5 = "Wifi";
    private static final String COL6 = "Mic";
    private static final String COL7 = "Timestamp";


    public Database(Context context) {
        super(context, TABLE_NAME, null,1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT," + COL1 + " TEXT," + COL2 + " TEXT," + COL3 + " TEXT," + COL4 + " TEXT," + COL5 + " TEXT," + COL6 + " TEXT," + COL7 + " TEXT);";
        sqLiteDatabase.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    public boolean addData(String acc, String gyro, String gps, String cell, String wifi, String mic,String timestamp){
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(COL1, acc);
        contentValues.put(COL2, gyro);
        contentValues.put(COL3, gps);
        contentValues.put(COL4, cell);
        contentValues.put(COL5, wifi);
        contentValues.put(COL6, mic);
        contentValues.put(COL7, timestamp);

        long result = sqLiteDatabase.insert(TABLE_NAME,null,contentValues);
        if(result == -1)
            return false;
        else
            return true;
    }

    public Cursor getAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from "+TABLE_NAME,null);
        return res;
    }
}
