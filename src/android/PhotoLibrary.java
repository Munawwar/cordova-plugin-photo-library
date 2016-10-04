package com.terikon.cordova.photolibrary;

import android.provider.MediaStore;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Date;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PhotoLibrary extends CordovaPlugin {

  public static final String ACTION_GET_LIBRARY = "getLibrary";
  public static final String ACTION_GET_THUMBNAIL= "getThumbnail";
  public static final String ACTION_GET_PHOTO = "getPhoto";
  public static final String ACTION_STOP_CACHING = "stopCaching";

  //TODO: remove
  public static final String ACTION_ECHO = "echo";

  @Override
  protected void pluginInitialize() {
    super.pluginInitialize();
    // initialization
    dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm a z");
  }

  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    try {

      if (ACTION_GET_LIBRARY.equals(action)) {

        cordova.getThreadPool().execute(new Runnable() {
          public void run() {
            try {
              ArrayList<JSONObject> library = getLibrary();
              callbackContext.success(new JSONArray(library));
            } catch (Exception e) {
              e.printStackTrace();
              callbackContext.error(e.getMessage());
            }
          }
        });
        return true;

      } else if (ACTION_GET_THUMBNAIL.equals(action)) {

        final String photoId = args.getString(0);
        final JSONObject options = args.optJSONObject(1);
        final int thumbnailWidth = options.getInt("thumbnailWidth");
        final int thumbnailHeight = options.getInt("thumbnailHeight");
        final double quality = options.getDouble("quality");

        cordova.getThreadPool().execute(new Runnable() {
          public void run() {
            try {
              PictureData thumbnail = getThumbnail(photoId, thumbnailWidth, thumbnailHeight, quality);
              callbackContext.sendPluginResult(createPluginResult(PluginResult.Status.OK, thumbnail));
            } catch (Exception e) {
              e.printStackTrace();
              callbackContext.error(e.getMessage());
            }
          }
        });
        return true;

      } else if (ACTION_GET_PHOTO.equals(action)) {

        final String photoId = args.getString(0);

        cordova.getThreadPool().execute(new Runnable() {
          public void run() {
            try {
              PictureData thumbnail = getPhoto(photoId);
              callbackContext.sendPluginResult(createPluginResult(PluginResult.Status.OK, thumbnail));
            } catch (Exception e) {
              e.printStackTrace();
              callbackContext.error(e.getMessage());
            }
          }
        });
        return true;

      } else if (ACTION_STOP_CACHING.equals(action)) {

        // Nothing to do - it's ios only functionality
        callbackContext.success();
        return true;

      } else if (ACTION_ECHO.equals(action)) { // TODO: remove this

        String message = args.getString(0);
        this.echo(message, callbackContext);
        return true;

      }
      return false;

    } catch(Exception e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
      return false;
    }
  }

  private ArrayList<JSONObject> getLibrary() throws JSONException {

    // All columns here: https://developer.android.com/reference/android/provider/MediaStore.Images.ImageColumns.html,
    // https://developer.android.com/reference/android/provider/MediaStore.MediaColumns.html
    JSONObject columns = new JSONObject() {{
      put("int.id", MediaStore.Images.Media._ID);
      put("filename", MediaStore.Images.ImageColumns.DISPLAY_NAME);
      put("nativeURL", MediaStore.MediaColumns.DATA);
      put("int.width", MediaStore.Images.ImageColumns.WIDTH);
      put("int.height", MediaStore.Images.ImageColumns.HEIGHT);
      put("date.creationDate", MediaStore.Images.ImageColumns.DATE_TAKEN);
      //put("mime_type", MediaStore.Images.ImageColumns.MIME_TYPE);
      //put("int.size", MediaStore.Images.ImageColumns.SIZE);
      //put("int.thumbnail_id", MediaStore.Images.ImageColumns.MINI_THUMB_MAGIC);
    }};

    final ArrayList<JSONObject> queryResults = queryContentProvider(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, "");

    ArrayList<JSONObject> results = new ArrayList<JSONObject>();

    for (JSONObject queryResult : queryResults) {
      if (queryResult.getInt("height") <=0 || queryResult.getInt("width") <= 0) {
        System.err.println(queryResult);
      } else {
        queryResult.put("id", queryResult.get("id") + ";" + queryResult.get("nativeURL")); // photoId is in format "imageid;imageurl"
        results.add(queryResult);
      }
    }

    Collections.reverse(results);

    return results;
  }

  private PictureData getThumbnail(String photoId, int thumbnailWidth, int thumbnailHeight, double quality) {

//    BitmapFactory.Options options = new BitmapFactory.Options();
//    options.inJustDecodeBounds = true;

    Bitmap bitmap;

    if (thumbnailHeight == 512 && thumbnailHeight == 384) { // In such case, thumbnail will be cached by MediaStore
      int imageId = getImageId(photoId);
      bitmap = MediaStore.Images.Thumbnails.getThumbnail(
        getContext().getContentResolver(),
        imageId ,
        MediaStore.Images.Thumbnails.MINI_KIND,
        (BitmapFactory.Options) null);
    } else { // No free caching here
      String imageUrl = getImageUrl(photoId);
      // TODO: inSampleSize
      bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(imageUrl),
        thumbnailWidth, thumbnailWidth);
    }

    byte[] bytes = getJpegBytesFromBitmap(bitmap, quality);
    String mimeType = "image/jpeg";

    bitmap.recycle();

    return new PictureData(bytes, mimeType);
  }

  private PictureData getPhoto(String photoId) {


    return null;
  }

  private void stopCaching() {

  }

  // TODO: remove this
  private void echo(String message, CallbackContext callbackContext) {
    if (message != null && message.length() > 0) {
      callbackContext.success(message);
    } else {
      callbackContext.error("Expected one non-empty string argument.");
    }
  }

  private ArrayList<JSONObject> queryContentProvider(Uri collection, JSONObject columns, String whereClause) throws JSONException {

    final ArrayList<String> columnNames = new ArrayList<String>();
    final ArrayList<String> columnValues = new ArrayList<String>();

    Iterator<String> iteratorFields = columns.keys();

    while (iteratorFields.hasNext()) {
      String column = iteratorFields.next();

      columnNames.add(column);
      columnValues.add("" + columns.getString(column));
    }

    final String sortOrder = MediaStore.Images.Media.DATE_TAKEN;

    final Cursor cursor = getContext().getContentResolver().query(
      collection,
      columnValues.toArray(new String[columns.length()]),
      whereClause, null, sortOrder);

    final ArrayList<JSONObject> buffer = new ArrayList<JSONObject>();

    if (cursor.moveToFirst()) {
      do {
        JSONObject item = new JSONObject();

        for (String column : columnNames) {
          int columnIndex = cursor.getColumnIndex(columns.get(column).toString());

          if (column.startsWith("int.")) {
            item.put(column.substring(4), cursor.getInt(columnIndex));
            if (column.substring(4).equals("width") && item.getInt("width") == 0) {
              System.err.println("cursor: " + cursor.getInt(columnIndex));
            }
          } else if (column.startsWith("float.")) {
            item.put(column.substring(6), cursor.getFloat(columnIndex));
          } else if (column.startsWith("date.")) {
            long intDate = cursor.getLong(columnIndex);
            Date date = new Date(intDate);
            item.put(column.substring(5), dateFormatter.format(date));
          } else {
            item.put(column, cursor.getString(columnIndex));
          }
        }
        buffer.add(item);
      }
      while (cursor.moveToNext());
    }

    cursor.close();

    return buffer;
  }

  private Context getContext() {
    return this.cordova.getActivity().getApplicationContext();
  }

  private byte[] getJpegBytesFromBitmap(Bitmap bitmap, double quality) {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, (int)(quality * 100), stream);
    return stream.toByteArray();
  }

  private SimpleDateFormat dateFormatter;

  // photoId is in format "imageid;imageurl"
  private int getImageId(String photoId) {
    return Integer.parseInt(photoId.substring(0, photoId.indexOf(';') - 1));
  }

  // photoId is in format "imageid;imageurl"
  private String getImageUrl(String photoId) {
    return photoId.substring(photoId.indexOf(';') + 1);
  }

  private PluginResult createPluginResult(PluginResult.Status status, PictureData pictureData) {
    return new PluginResult(status,
      Arrays.asList(
        new PluginResult(status, pictureData.getBytes()),
        new PluginResult(status, pictureData.getMimeType())));
  }

  private class PictureData {
    private byte[] bytes;
    private String mimeType;

    public PictureData(byte[] bytes, String mimeType) {
      this.bytes = bytes;
      this.mimeType = mimeType;
    }

    public byte[] getBytes() { return this.bytes; }
    public String getMimeType() { return this.mimeType; }
  }

}
