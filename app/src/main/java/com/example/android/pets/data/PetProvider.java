package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.pets.data.PetContract.PetEntry;

public class PetProvider extends ContentProvider {

    private static final String LOG_TAG = PetProvider.class.getSimpleName();
    //Defining the UriMatcher for the content Patterns
    private static final int PETS = 100;
    private static final int PET_ID = 101;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private PetDbHelper mDbHelper;

    static {
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS, PETS);
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS + "/#", PET_ID);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new PetDbHelper(getContext());
        return false;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor = null;
        int match = sUriMatcher.match(uri);

        switch (match) {
            case PETS:
                cursor = database.query(PetContract.PetEntry.TABLE_NAME,
                        projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;

            case PET_ID:
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(PetContract.PetEntry.TABLE_NAME,
                        projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;

            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(),uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return PetEntry.CONTENT_LIST_TYPE;
            case PET_ID:
                return PetEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return insertPet(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    public Uri insertPet(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        //Validation
        checkForName(contentValues);
        checkForGender(contentValues);
        checkForWeight(contentValues);

        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        long id_row = database.insert(PetContract.PetEntry.TABLE_NAME, null, contentValues);
        //if it go wrong
        if (id_row == -1) {
            Log.e(LOG_TAG, "Insertion Failed for the row ID " + uri);
            return null;
        }

        getContext().getContentResolver().notifyChange(uri,null);
        //new URI with the given ID appended to the end of the path
        return ContentUris.withAppendedId(uri, id_row);
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int rowDeletedID;
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                // Delete all rows that match the selection and selection args
                rowDeletedID= database.delete(PetEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PET_ID:
                // Delete a single row given by the ID in the URI
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowDeletedID= database.delete(PetEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
        if (rowDeletedID!=0)
            getContext().getContentResolver().notifyChange(uri,null);

        return rowDeletedID;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return updatePet(uri, contentValues, selection, selectionArgs);
            case PET_ID:
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updatePet(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update pets in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more pets).
     * Return the number of rows that were successfully updated.
     */
    private int updatePet(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        //Validation
        if (values.size() == 0)
            return 0;
        if (values.containsKey(PetEntry.COLUMN_PET_NAME))
            checkForName(values);
        if (values.containsKey(PetEntry.COLUMN_PET_GENDER))
            checkForGender(values);
        if (values.containsKey(PetEntry.COLUMN_PET_WEIGHT))
            checkForWeight(values);

        //Initialize and update the dB
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int rowId=database.update(PetEntry.TABLE_NAME, values, selection, selectionArgs);
        if (rowId!=0)
            getContext().getContentResolver().notifyChange(uri,null);
        return rowId;
    }

    private void checkForName(ContentValues values) {
        String name = values.getAsString(PetEntry.COLUMN_PET_NAME);
        if (name == null)
            throw new IllegalArgumentException("Pet Require a name!!");
    }

    private void checkForGender(ContentValues values) {
        Integer gender = values.getAsInteger(PetEntry.COLUMN_PET_GENDER);
        if (gender == null || !PetEntry.isValidGender(gender))
            throw new IllegalArgumentException("Pet Require a Gender!!");
    }

    private void checkForWeight(ContentValues values) {
        Integer weight = values.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
        if (weight != null && weight < 0)
            throw new IllegalArgumentException("Weight in Negative!! Come on!");

    }
}

