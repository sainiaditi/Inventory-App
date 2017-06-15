package com.example.android.inventoryapp;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;
import java.io.ByteArrayOutputStream;

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EDIT_ITEM_LOADER = 0;
    private Uri currentItemUri;

    Intent intent;
    private String mSudoProduct;
    private int mSudoQuantity = 50;
    private String mSudoEmail;
    private EditText mNameEditText;
    private EditText mSupplierEditText;
    private EditText mPriceEditText;
    private EditText mDescriptionEditText;
    private EditText mQuantityEditText;
    private Button mOrderNowButton;
    private ImageView mImageView;
    static final int REQUEST_CAMERA=1;
    byte[]imageBlob=null;
    private String mCurrentPhotoUri = "no images";
    private boolean mItemHasChanged = false;
    
    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mItemHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        //Recieve the intent
        intent = getIntent();
        currentItemUri = intent.getData();
        if(currentItemUri==null) {
            setTitle(getString(R.string.editor_activity_title_new_item));
            invalidateOptionsMenu();
        }
        else {
            setTitle(getString(R.string.editor_activity_title_edit_item));
            getLoaderManager().initLoader(EDIT_ITEM_LOADER, null, this);
        }


        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_item_name);
        mSupplierEditText = (EditText) findViewById(R.id.edit_item_supplier);
        mPriceEditText = (EditText) findViewById(R.id.edit_item_price);
        mDescriptionEditText = (EditText) findViewById(R.id.edit_item_description);
        mQuantityEditText = (EditText) findViewById(R.id.edit_item_quantity);
        mOrderNowButton = (Button) findViewById(R.id.orderNowButton);
        mImageView = (ImageView) findViewById(R.id.product_photo);

        mNameEditText.setOnTouchListener(touchListener);
        mSupplierEditText.setOnTouchListener(touchListener);
        mDescriptionEditText.setOnTouchListener(touchListener);
        mPriceEditText.setOnTouchListener(touchListener);
        mQuantityEditText.setOnTouchListener(touchListener);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (intent.resolveActivity(getPackageManager())!=null)
                    startActivityForResult(intent,REQUEST_CAMERA);
            }
        });

        mOrderNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String[] TO = {mSudoEmail};
                Intent emailIntent = new Intent(Intent.ACTION_SEND);

                emailIntent.setData(Uri.parse("mailto:"));
                emailIntent.setType("text/plain");
                emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Order " + mSudoProduct);
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Please ship " + mSudoProduct +
                        " in quantities " + mSudoQuantity);

                try {
                    startActivity(Intent.createChooser(emailIntent, "Send mail..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveItem();
                finish();
                return true;

            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            case android.R.id.home:
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onBackPressed() {
        // If the pet hasn't changed, continue with handling back button press
        if (!mItemHasChanged) {
            super.onBackPressed();
            return;
        }
        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CAMERA) {
            if (data != null) {
                Bitmap image=(Bitmap)data.getExtras().get("data");
                mImageView.setImageBitmap(image);

                ByteArrayOutputStream stream= new ByteArrayOutputStream();
                image.compress(Bitmap.CompressFormat.PNG,100,stream);
                imageBlob=stream.toByteArray();
                Uri mProductPhotoUri = data.getData();
                mCurrentPhotoUri = mProductPhotoUri.toString();

                //We use Glide to import photo images
                Glide.with(this).load(imageBlob)
                        .placeholder(R.drawable.placeholder)
                        .into(mImageView);
            }
        }
    }


    /**
     * Helper method to get user input and save it to the database
     */
    private void saveItem() {

        //Id for the Row Inserted or Updated
        long rowId;

        //Get the user input from input fields
        String nameString = mNameEditText.getText().toString().trim();
        String supplierString = mSupplierEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String descriptionString = mDescriptionEditText.getText().toString().trim();

        if (TextUtils.isEmpty(nameString) || TextUtils.isEmpty(supplierString) ||
                TextUtils.isEmpty(priceString) || TextUtils.isEmpty(quantityString) || TextUtils.isEmpty(descriptionString)) {
            Toast.makeText(this,"Fields cannot be left empty",Toast.LENGTH_SHORT).show();
            return;
        }

        // If the price is not provided by the user, don't try to parse the string into an
        // integer value. Use 0 by default.
        int price = 0;
        if (!TextUtils.isEmpty(priceString)) {
            price = Integer.parseInt(priceString);
        }

        //Create the content values to be inserted into the database

        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_PRODUCT_NAME, nameString);
        values.put(InventoryEntry.COLUMN_SUPPLIER, supplierString);
        values.put(InventoryEntry.COLUMN_DESCRIPTION, descriptionString);
        values.put(InventoryEntry.COLUMN_PRODUCT_PRICE, price);
        values.put(InventoryEntry.COLUMN_QUANTITY,quantityString);
        values.put(InventoryEntry.COLUMN_IMAGE,mCurrentPhotoUri);

        if (currentItemUri == null) {
            rowId = ContentUris.parseId(getContentResolver().insert(InventoryEntry.CONTENT_URI, values));

            //Toast to show whether the insertion in database is sucessful or not
            if (rowId == -1) {
                Toast.makeText(this, getString(R.string.editor_insert_item_failed), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_insert_item_successful), Toast.LENGTH_SHORT).show();
                NavUtils.navigateUpFromSameTask(this);
            }

        } else {
            rowId = getContentResolver().update(currentItemUri, values, null, null);

            //Toast to show whether the Updation in database is sucessful or not
            if (rowId == -1) {
                Toast.makeText(this, getString(R.string.editor_update_item_failed), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_update_item_successful), Toast.LENGTH_SHORT).show();
            }
        }

    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new pet, hide the "Delete" menu item.
        if (currentItemUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }


    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                this,
                currentItemUri,
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data.moveToFirst()) {

            //Get Values of the CurrentPet From the Cursor
            String name = data.getString(data.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_NAME));
            String supplier = data.getString(data.getColumnIndex(InventoryEntry.COLUMN_SUPPLIER));
            String description = data.getString(data.getColumnIndex(InventoryEntry.COLUMN_DESCRIPTION));
            int price = data.getInt(data.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_PRICE));
            int quantity = data.getInt(data.getColumnIndex(InventoryEntry.COLUMN_QUANTITY));
            mSudoEmail = "orders@" + supplier + ".com";
            mSudoProduct = name;

            mCurrentPhotoUri = data.getString(data.getColumnIndex(InventoryEntry.COLUMN_IMAGE));

            //Replace the value of respective Edit Fields
            mNameEditText.setText(name);
            mSupplierEditText.setText(supplier);
            mDescriptionEditText.setText(description);
            mPriceEditText.setText(Integer.toString(price));
            mQuantityEditText.setText(Integer.toString(quantity));
            Glide.with(this).load(mCurrentPhotoUri)
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.drawable.placeholder)
                    .into(mImageView);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        mNameEditText.setText("");
        mSupplierEditText.setText("");
        mDescriptionEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("");
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the pet in the database.
     */
    private void deleteProduct() {
        if (currentItemUri != null) {
            getContentResolver().delete(currentItemUri, null, null);
            Toast.makeText(this, getString(R.string.editor_delete_item_successful), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.editor_delete_item_failed), Toast.LENGTH_SHORT).show();
        }

        finish();
    }

}
