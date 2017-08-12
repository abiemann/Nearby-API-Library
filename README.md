# Nearby-API-Library
(an Improved Nearby Message demo)


Introduction
------------
- This code is intended to be a more useful version of what has been made publicly available to developers at https://github.com/googlesamples/android-nearby/tree/master/messages/NearbyDevices
- I have only spent a few hours on this project to explore the Nearby API, so this project is not intended to become anything that I will maintain

Known Problem
-------------
Even though the Nearby API and the view have been separated in this code, they are nontheless coupled by the use of `.enableAutoManage(activity, this)`
Therefore, whatever will pause/stop an Activity will stop the Nearby API from discovering devices.

API Key
-------
You will need an API key to use the Nearby API's.
Please see https://developers.google.com/nearby/messages/android/get-started
