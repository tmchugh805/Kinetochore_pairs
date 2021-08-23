# Kinetochore_pairs

Image J Plugin which will find kinetochore pairs in chromosome spreads and take linescans in two channels. Takes a 3 colour image file (scale: 25.4615 pixels per micron) and a kinetochore position spreadsheet created using the speckle trakerJ (1) software as input. Asks the user to select a reference and measurement channel and an ROI around the kinetochores of interest. Pairs kinetochores based on nearest distance with a max distance between paired kinetochores of 1.5 microns. Draws a 2 micron line ROIs through the pairs centered on the midpoint of the kinetochore pair. Takes linescans in both the reference channel and the measurement channel which are output to a textfile.

(1)) M.B. Smith1, E. Karatekin2,3, A. Gohlke3, H. Mizuno4, N. Watanabe3, and D. Vavylonis1, Biophys J, 101:1794-1804 (2011)
