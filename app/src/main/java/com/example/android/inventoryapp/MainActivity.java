package com.example.android.inventoryapp;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;



public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    private static final int INVENTORY_LOADER = 0;

    public final String[] projections = {InventoryEntry._ID,
            InventoryEntry.COLUMN_PRODUCT_NAME,
            InventoryEntry.COLUMN_PRODUCT_PRICE,
            InventoryEntry.COLUMN_QUANTITY,
            InventoryEntry.COLUMN_IMAGE};

    private InventoryCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup FAB to open EditorActivity
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });
        ListView productListView = (ListView) findViewById(R.id.list_view_items);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        productListView.setEmptyView(emptyView);

        //Setup the Adapter and Initialize to null
        //since there will be no data until loader finishes
        mCursorAdapter = new InventoryCursorAdapter(this, null);
        productListView.setAdapter(mCursorAdapter);
        //Set up onItem Click Listener
        productListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Uri currentProductUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, id);
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                intent.setData(currentProductUri);
                startActivity(intent);
            }
        });

        //Initialize the Loader
        getLoaderManager().initLoader(INVENTORY_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        return new CursorLoader(
                this,
                InventoryEntry.CONTENT_URI,
                projections,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        //Update the PetCursorAdapter with the new Cursor Containing Updated Data
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mCursorAdapter.swapCursor(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertDummyData();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                // Deletes all the rows
                deleteAllProducts();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Helper method to delete all pets in the database.
     */
    private void deleteAllProducts() {
        getContentResolver().delete(InventoryEntry.CONTENT_URI, null, null);
    }

    private void insertDummyData() {

        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_PRODUCT_NAME, "Dettol");
        values.put(InventoryEntry.COLUMN_QUANTITY, 10);
        values.put(InventoryEntry.COLUMN_PRODUCT_PRICE, 49);
        values.put(InventoryEntry.COLUMN_DESCRIPTION, "Liquid Hand Wash");
        values.put(InventoryEntry.COLUMN_SUPPLIER,"Dettol");
        values.put(InventoryEntry.COLUMN_IMAGE,R.drawable.placeholder);

        getContentResolver().insert(InventoryEntry.CONTENT_URI, values);

        Toast.makeText(this, getString(R.string.editor_insert_item_successful), Toast.LENGTH_SHORT).show();
    }

}
