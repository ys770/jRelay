# jRelay Instructions to Claude



### UI

* Simplicity: The UI should be simple and straight foward, and fully DPAD compatible. Avoid cards and overlaying views. Stack everything neatly in vertical linear layouts.
* Every activity should be scrollable, using ScrollView as root layout.
* Add android:clickable="true" and android:focusable="true" to relevant UI items for full DPAD compatibility.
* Use highlighting to indicate focus.
* Avoid emojis and other "AI"like styling.
* Keep the UI simple and neat.
* Many of our users have very small screens and we want the app to look and feel natural on these screens in addition to full size screens. 



### Architecture

* We are using Android Java and XML for UI. Do not use jetpack. Do not use kotlin.
* We are trying to keep the app small by minimizing library dependencies. Keep to vanilla android java. Avoid androidx.



### Version Control

* Fully document all changes in neat bullets to VERSION.md. Do not make any changes without documenting.
* Increment versioning with every change using x.x scheme. For major changes, increment the major number, for minor changes, increment minor number.
* Git commit before any changes, with a short but descriptive commit message. Do not make any changes ever without first committing.

