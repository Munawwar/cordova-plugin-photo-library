Parts are based on

- https://github.com/ryouaki/Cordova-Plugin-Photos
- https://github.com/subitolabs/cordova-gallery-api 

# TODO

- Add Android support
- iOS: Handle cases where image returned by requestImageDataForAsset is null
- iOS: It seems to ignore png files
- Browser platform: Separate to multiple files
- Browser platform: Compile plugin with webpack
- Android: caching mechanism like [this one](https://developer.android.com/training/displaying-bitmaps/cache-bitmap.html) can be helpful

# References

## Android relevant documentation

https://developer.android.com/reference/org/json/JSONObject.html
https://developer.android.com/reference/android/provider/MediaStore.Images.Media.html
https://developer.android.com/reference/android/provider/MediaStore.Images.Thumbnails.html
https://developer.android.com/reference/android/graphics/BitmapFactory.Options.html
https://developer.android.com/reference/android/media/ThumbnailUtils.html
