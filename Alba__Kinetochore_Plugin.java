import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.*;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import loci.poi.util.IntList;
import speckles.*;
import uk.ac.rdg.resc.edal.time.AllLeapChronology;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Alba__Kinetochore_Plugin implements PlugIn {

    ImagePlus referenceImage;
    ImagePlus measurementImage;
    String filename;
    String directory;

    public void run(String arg) {

        //Open deltavision file and split the channels
        new WaitForUserDialog("Open Image", "Open Images. SPLIT CHANNELS!").show();
        IJ.run("Bio-Formats Importer");// Open new file

        //Open kinetochore pairs position file
        FileDialog dialog2 = new FileDialog((Frame)null, "Select Speckle File to Open");
        dialog2.setMode(FileDialog.LOAD);
        dialog2.setVisible(true);
        directory = dialog2.getDirectory();
        filename = dialog2.getFile();
        String file = Paths.get(directory,filename).toString();
        IJ.log(file + " chosen.");
        IJ.log("Hello");


        //Set Reference and Measuring Channel (set Names)
        new WaitForUserDialog("Select", "Select the reference channel eg. CENP-C").show();
        referenceImage = WindowManager.getCurrentImage();
        new WaitForUserDialog("Select", "Select the measurement channel eg. Borealin").show();
        measurementImage = WindowManager.getCurrentImage();

        IJ.run(referenceImage, "Z Project...", "projection=[Average Intensity]");
        referenceImage = WindowManager.getCurrentImage();
        IJ.run(referenceImage, "Enhance Contrast", "saturated=0.35");
        referenceImage = WindowManager.getCurrentImage();

        IJ.run(measurementImage, "Z Project...", "projection=[Average Intensity]");
        measurementImage = WindowManager.getCurrentImage();
        IJ.run(measurementImage, "Enhance Contrast", "saturated=0.35");
        measurementImage = WindowManager.getCurrentImage();

        //read the list of x,y pairs from Speckle file
        List<double[]> coordinates = readSpeckleFile(file);
        List<int[]> allPairs = new ArrayList<>();
        IntList pairedValues = new IntList();
        IntList duplicateValues = new IntList();

        RoiManager roiManager = new RoiManager();
        if(roiManager.getCount()!=0){
            roiManager.runCommand("Delete");
        }

        //select area to be analysed
        IJ.setTool("rectangle");
        referenceImage.show();
        roiManager.runCommand(referenceImage,"Show All without labels");
        new WaitForUserDialog("Draw ROIs around area to be analysed, press 't' to save to ROI manager. press OK").show();
        Roi region = roiManager.getRoi(roiManager.getCount()-1);

        //for each coordinate pair find distance to each other pair
        //if the distance is less than 1.5um then save coordinate pair indexes - unless already saved
        for(int i =0; i < (coordinates.size()-1);i++) {
            if (region.contains((int) coordinates.get(i)[0], (int) coordinates.get(i)[1])) {
                for (int j = (i + 1); j < coordinates.size(); j++) {
                    double dist = twoPointDistance(coordinates.get(i), coordinates.get(j));
                    int[] pair = new int[2];
                    if (dist < 38.1927) { //1.5um 25.4615 pixels per micron
                        pair[0] = i;
                        pair[1] = j;
                        allPairs.add(pair);
                        if (pairedValues.contains(j)) {
                            duplicateValues.add(j);
                        }
                        if (pairedValues.contains(i)) {
                            duplicateValues.add(i);
                        }
                        pairedValues.add(j);
                        pairedValues.add(i);
                    }
                }
            }
        }

        List<int[]> finalPairs = new ArrayList<>();

        //remove duplicate pairings
        for(int i = 0; i< allPairs.size();i++ ){
            if(!duplicateValues.contains(allPairs.get(i)[0]) && !duplicateValues.contains(allPairs.get(i)[1]) ){
                finalPairs.add(allPairs.get(i));
            }
        }

        //add pairs to roi manager
        for(int i = 0; i<finalPairs.size();i++){
            int[] points = finalPairs.get(i);
            double[] pointXYone = coordinates.get(points[0]);
            double[] pointXYtwo = coordinates.get(points[1]);
            if (finalPairs.get(i)[0]!=-1){
                Roi pointOne = new PointRoi(pointXYone[0],pointXYone[1]);
                Roi pointTwo = new PointRoi(pointXYtwo[0],pointXYtwo[1]);
                roiManager.addRoi(pointOne);
                roiManager.addRoi(pointTwo);
            }
        }


        Roi[] lineScans = new Roi[finalPairs.size()];

        //for each pair take a line scan of set length in each channel (start with +/- 1um from centroid)

        for(int i = 0; i<finalPairs.size();i++){

            //to set line find angle tan-1(y2-y1)/(x2-x1) and centroid ((x1+x2)/2,(y1+y2)/2)
            // then calculate yStart = yCenter - dist*sin(angle) and yEnd = yCenter + dist*sin(angle)
            // then calculate xStart = xCenter - dist*cos(angle) and xEnd = xCenter + dist*cos(angle)

            double dist = 25.4614; //2um line
            int[] points = finalPairs.get(i);
            double[] XYone = coordinates.get(points[0]);
            double[] XYtwo = coordinates.get(points[1]);
            double angle = Math.tanh((XYtwo[1]-XYone[1])/(XYtwo[0]-XYone[0]));
            double yCentre = (XYtwo[1]+XYone[1])/2;
            double xCentre = (XYtwo[0]+XYone[0])/2;
            double lineStartY = yCentre - dist*Math.sin(angle);
            double lineEndY = yCentre + dist*Math.sin(angle);
            double lineStartX = xCentre - dist*Math.cos(angle);
            double lineEndX = xCentre + dist*Math.cos(angle);

            // make line ROI with calculated Start and end points
            lineScans[i] = new Line(lineStartX,lineStartY,lineEndX,lineEndY);
            roiManager.addRoi(lineScans[i]);
        }

        List<double[]> refOutput = new ArrayList<>();
        List<double[]> measOutput = new ArrayList<>();

        //For each linescan
        //Apply ROI to each window
        //Take linescan
        for(int i=0; i < lineScans.length;i++) {
            referenceImage.setRoi(lineScans[i]);
            ProfilePlot profileRef = new ProfilePlot(referenceImage);
            double[] refProfile = profileRef.getProfile();
            refOutput.add(refProfile);
            measurementImage.setRoi(lineScans[i]);
            ProfilePlot profilePlot = new ProfilePlot(measurementImage);
            double[] measProfile = profilePlot.getProfile();
            measOutput.add(measProfile);
        }

        double[] averageRef = new double[refOutput.get(1).length];
        double[] averageMeas = new double[refOutput.get(1).length];
        double[] distances = new double[refOutput.get(1).length];

        //calculate averages in reference and measurement channel
        for (int j = 0; j<refOutput.get(1).length;j++){
            for (int i = 0; i<refOutput.size();i++){
                averageRef[j]=averageRef[j]+refOutput.get(i)[j];
                averageMeas[j]=averageMeas[j]+measOutput.get(i)[j];
            }
            averageRef[j]=averageRef[j]/refOutput.size();
            averageMeas[j]=averageMeas[j]/measOutput.size();
            distances[j]=j*2000/refOutput.get(1).length;
        }


        //output linescan to text file
        roiManager.runCommand("Save", Paths.get( directory , filename+".zip").toString());
        makeResultsFile(refOutput,measOutput,averageRef,averageMeas, distances);

    }

    private double twoPointDistance(double[] pointOne, double[] pointTwo){
        double x = pointOne[0]-pointTwo[0];
        double y = pointOne[1]-pointTwo[1];
        double dist = Math.sqrt((x*x)+(y*y));
        return dist;
    }

    private List<double[]> readSpeckleFile(String file){
        double[] scale = new double[2];
        List<double[]> coordinateList = new ArrayList<>();


        try{
            BufferedReader bufferedReader= new BufferedReader(new FileReader(file));
            String line;
            while ((line=bufferedReader.readLine()) != null){
                if(!line.contains("#")){
                    String[] data = line.split("\t");
                    double[] coordinates = new double[2];
                    coordinates[0] = Double.parseDouble(data[0]);
                    coordinates[1] = Double.parseDouble(data[1]);
                    coordinateList.add(coordinates);
                }
            }
        }catch(IOException ex) {
            System.out.println("File Not Found");
            return coordinateList;
        }

        return coordinateList;
    }

    private void makeResultsFile(List<double[]> refOutput, List<double[]> measOutput, double[] averageRef, double[] averageMeas, double[] distances){

        String CreateName = Paths.get( directory , filename+"_linescans.txt").toString();
        File resultsFile = new File(CreateName);


        int i = 1;
        while (resultsFile.exists()){
            CreateName= Paths.get( directory , filename+"_linescans_"+i+".txt").toString();
            resultsFile = new File(CreateName);
            IJ.log(CreateName);
            i++;
        }
        try{
            FileWriter fileWriter = new FileWriter(CreateName,true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.newLine();
            bufferedWriter.write("File= " + filename);
            bufferedWriter.newLine();
            bufferedWriter.write("Reference Channel:");
            bufferedWriter.newLine();
            bufferedWriter.write("Dist (nm) , "+ Arrays.toString(distances).substring(1,Arrays.toString(distances).length()-1));
            bufferedWriter.newLine();
            for(int j=0; j<refOutput.size();j++) {
                bufferedWriter.write((j+1)+" ," + Arrays.toString(refOutput.get(j)).substring(1,Arrays.toString(refOutput.get(j)).length()-1));
                bufferedWriter.newLine();
            }
            bufferedWriter.write("Average ," + Arrays.toString(averageRef).substring(1,Arrays.toString(averageRef).length()-1));
            bufferedWriter.newLine();
            bufferedWriter.newLine();
            bufferedWriter.write("Measurement Channel:");
            bufferedWriter.newLine();
            bufferedWriter.write("Dist (nm) , "+ Arrays.toString(distances).substring(1,Arrays.toString(distances).length()-1));
            bufferedWriter.newLine();
            for(int j=0; j<refOutput.size();j++) {
                bufferedWriter.write((j+1)+" ," + Arrays.toString(measOutput.get(j)).substring(1,Arrays.toString(measOutput.get(j)).length()-1));
                bufferedWriter.newLine();
            }
            bufferedWriter.write("Average ," + Arrays.toString(averageMeas).substring(1,Arrays.toString(averageMeas).length()-1));
            bufferedWriter.close();
        }
        catch(IOException ex) {
            System.out.println(
                    "Error writing to file '" + CreateName + "'");
        }
    }

    }


