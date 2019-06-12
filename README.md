# MonetizeApp

MonetizeApp is an Android library for monetizing your application.

Step 1: Add the JitPack repository to your build file
```
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```

Step 2: Add the dependency
```
dependencies {
  implementation 'com.github.monetizemyapps:MonetizeMyApps:latest.version'
}
```

Step 3: Init the library inside Application class
```
override fun onCreate() {
  ...
  MonetizeMyApp.init(applicationContext)
}
```

Step 4: Supply monetize_app_key in Manifest
```
<meta-data 
  android:name="monetize_app_key"
  android:value="YOUR_APP_KEY"/>
```

## License
[APACHE 2.0](https://choosealicense.com/licenses/apache-2.0/)
