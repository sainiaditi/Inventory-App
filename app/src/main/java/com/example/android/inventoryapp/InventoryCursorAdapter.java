package com.example.android.inventoryapp;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;
import static android.content.ContentValues.TAG;
import static com.example.android.inventoryapp.R.drawable.placeholder;
import static com.example.android.inventoryapp.R.id.quantity;

/**
 * Created by Dell on 6/7/2017.
 */

public class InventoryCursorAdapter extends CursorAdapter {


    public InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view,final Context context, Cursor cursor) {


        //Find Views to be Updated
        TextView name_txtvw = (TextView) view.findViewById(R.id.name);
        TextView summary_txtvw = (TextView) view.findViewById(R.id.summary);
        TextView quantity_txtvw = (TextView) view.findViewById(quantity);
        Button decButton_vw = (Button) view.findViewById(R.id.decButton);
        Button incButton_vw = (Button) view.findViewById(R.id.incButton);
        ImageView imageView = (ImageView) view.findViewById(R.id.imageView);

        //Extract Value From Cursor

        final int id = cursor.getInt(cursor.getColumnIndex(InventoryEntry._ID));
        byte[] imageColumnIndex = cursor.getBlob(cursor.getColumnIndex(InventoryEntry.COLUMN_IMAGE));
        final String name = cursor.getString(cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_NAME));
        final String summary = "Price: " + cursor.getString(cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_PRICE));
        final int quantity = cursor.getInt(cursor.getColumnIndex(InventoryEntry.COLUMN_QUANTITY));

        int thumbnailColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_IMAGE);

        String productQuantity = "Inventory: " + String.valueOf(quantity);
        Uri thumbUri = Uri.parse(cursor.getString(thumbnailColumnIndex));

        final Uri currentProductUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, id);

        // Update the TextViews with the attributes for the current item
        name_txtvw.setText(name);
        summary_txtvw.setText(summary);
        quantity_txtvw.setText(productQuantity);
        Glide.with(context).load(thumbUri)
                .placeholder(R.mipmap.ic_launcher)
                .error(placeholder)
                .crossFade()
                .centerCrop()
                .into(imageView);

        decButton_vw.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Log.d(TAG, name + " quantity= " + quantity);
                ContentResolver resolver = view.getContext().getContentResolver();
                ContentValues values = new ContentValues();
                if (quantity > 0) {
                    int qq = quantity;
                    values.put(InventoryEntry.COLUMN_QUANTITY, --qq);
                    Log.d(TAG, "new quabtity= " + qq);
                    resolver.update(
                            currentProductUri,
                            values,
                            null,
                            null
                    );
                    context.getContentResolver().notifyChange(currentProductUri, null);
                } else {
                    Toast.makeText(context, "Item out of stock", Toast.LENGTH_SHORT).show();
                }
            }
        });

        incButton_vw.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Log.d(TAG, name + " quantity= " + quantity);
                ContentResolver resolver = view.getContext().getContentResolver();
                ContentValues values = new ContentValues();
                if (quantity >= 0) {
                    int qq = quantity;
                    values.put(InventoryEntry.COLUMN_QUANTITY, ++qq);
                    Log.d(TAG, "new quabtity= " + qq);
                    resolver.update(
                            currentProductUri,
                            values,
                            null,
                            null
                    );
                    context.getContentResolver().notifyChange(currentProductUri, null);
                }
            }
        });
    }
}
