# jfr-streaming Sample app - fibonacci numbers

This is a maven app that uses jfr-streaming to generate the recording.jfr file. Open this file with JDK Mission Control to visualize the stats.

This app prints first `100000` fibonacci numbers using multi threading, so that all the CPU cores get used, and we get enough action in the output file to visualize.

### Run

To generate the `recording.jfr` file, run the `main()` function inside `App.java`. It will take around 1.5 hours to finish. To make it faster, give a smaller value to `range` in the `App.java` file.