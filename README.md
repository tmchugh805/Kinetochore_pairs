# Kinetochore_pairs

Image J Plugin which will find kinetochore pairs in chromosome spreads and take linescans in two channels. Takes a 2 (or more) colour Deltavision image file (scale: 40nm per pixel) and a kinetochore position spreadsheet created using the Speckle TrakerJ (1) software as input. Asks the user to select a reference and measurement channel and an ROI around the kinetochores of interest. Pairs kinetochores based on nearest distance with a max distance between paired kinetochores of 1.5 microns. Draws a 2 micron line ROIs through the pairs centered on the midpoint of the kinetochore pair. Takes linescans in both the reference channel and the measurement channel which are output to a textfile.

INSTALLATION

Save the Alba_Kinetochore_Plugin.class in the plugins folder of your ImageJ installation. ImageJ should be version 1.5 or later and should have BioFormats installed. Alba Kinetochore Plugin should appear in the Plugins menu.

USAGE
1. Before using the plugin find kinetochores using Speckle TrackerJ and save a position data file in the same folder as your image file. Installation and usage instructions for Speckle TrackerJ can be found at https://www.lehigh.edu/~div206/speckletrackerj/index.html 
2. Select Alba Kinetochore Plugin from the Plugins drop down menu, when prompted select file image file to be opened 
3. When the Bioformats menu appears only the split channels box should be selected
4. Click on the image which you will use as your reference channel, click OK.
5. Click on the image which you will use as your measurement channel, click OK.
6. Draw a box ROI around the kinetochores you want to analyse, press 'T' on the keyboard to add to the ROI manager then click OK.
7. The plugin will run, a text file 'filename_speckles_linescans.txt' and a zipped ROI file 'filename_speckles' will save in your image directory.


(1) M.B. Smith1, E. Karatekin2,3, A. Gohlke3, H. Mizuno4, N. Watanabe3, and D. Vavylonis1, Biophys J, 101:1794-1804 (2011)
