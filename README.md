# Caramel (Android)
### Android app side of Caramel dog door project

This is a terminal-inspired android app to communicate with the Arduino part of the Caramel project via an HC-05 bluetooth module.
It's specifically intended to be completely modular, so the app gets the commands from the server, and the app doesn't need to update.
As more and more updates happen to the arduino side, the app can more or less stay the same. It relies on the user to pair with the
device first, then enter the password in pairing settings. Once that is achieved, they can use the app. 
