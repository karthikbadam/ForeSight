# Spider Guider 

<img src="https://github.com/karthikbadam/ForeSight/blob/master/images/sg1.jpg">

Spider Guider is a leg-worn haptic strap that can guide you to a location of interest. It consists of three columns of vibration motors (3 motors per column) for feedback in left, right, and forward directions. We created an Android application that tracks the user's GPS location (or virtual movement on a street view), queries Yelp API to get locations of nearest restaurants, and passes the restaurant information to the Arduino on the wearable using Bluetooth. Based on the number of motors that are buzzing (mapped to number of restaurants in each direction), and how many times they are buzzing (mapped to proximity of the restaurants), you can decide which way to go. 

We worked on this as a final project for a [tangible interaction and
computing course](http://cmsc838f-s15.wikispaces.com/) (CMSC838f) at UMD taught by Prof. Jon Froehlich.

The write-up for our project can be found [here](http://cmsc838f-s15.wikispaces.com/Spider+Guider).

Here is an annotated screenshot of the Android application.

<img src="https://github.com/karthikbadam/ForeSight/blob/master/images/sg_androidapp.png">

## Dependencies
  
  * [Android SDK and Android Studio](https://developer.android.com/sdk/index.html)
  * [Arduino IDE](http://www.arduino.cc/en/Main/Software)

## How to run:

  * The Android application can be run by opening the _ForeSight/_ project folder in Android Studio.  
  * The Arduino IDE can be used to open and run the Arduino part.




