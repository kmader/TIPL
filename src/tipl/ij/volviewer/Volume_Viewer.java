package tipl.ij.volviewer;

/*

 * Volume Viewer 2.01
 * 01.12.2012
 * 
 * (C) Kai Uwe Barthel
 */

import ij.IJ;
import ij.ImageJ;
//import ij.ImageJ;
import ij.ImagePlus;
import ij.Macro;
import ij.Prefs;
import ij.WindowManager;
import ij.macro.Interpreter;
import ij.plugin.PlugIn;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
//import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.StringTokenizer;

import javax.swing.JFrame;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.formats.VirtualAim;
import tipl.ij.ImageStackToTImg;
import tipl.ij.TImgToImagePlus;
import tipl.util.ArgumentParser;
import tipl.util.ITIPLPluginIn;
import tipl.util.TImgTools;

public final class Volume_Viewer implements PlugIn,ITIPLPluginIn {

	private final static String version = "2.01"; 
	private Control control;
	private JFrame frame;	

	final float[]   a1_R = new float[256];
	final float[][] a2_R = new float[256][128];
	final float[][] a3_R = new float[256][128];

	Volume vol = null;
	Cube cube = null;
	LookupTable lookupTable = null;
	private Transform tr = null;
	private Transform trLight = null;

	ImagePlus imp;
	Gui gui;
	Gradient gradientLUT, gradient2, gradient3, gradient4;
	TFrgb tf_rgb = null;
	TFalpha1 tf_a1 = null;
	TFalpha2 tf_a2 = null;
	TFalpha3 tf_a3 = null;
	TFalpha4 tf_a4 = null;

	private boolean batch = false;
	protected static ImageJ ijcore=null;
	public Volume_Viewer() {
		if (ijcore==null) ijcore=new ImageJ(ImageJ.NO_SHOW); // open the ImageJ window to see images and results
		
		// This should be created at the very beginning
		control = new Control(this);
		control.xloc=100;
		control.yloc=50;
	}
	@Override
	public boolean execute() {
		
		batch=true;
		assert(internalImage!=null);
		run("");
		return true;
	}
	@Override
	public boolean execute(String actionToExecute)
			throws IllegalArgumentException {
		// TODO Implement Method
		throw new IllegalArgumentException(this+" is not implemented yet!");
	}
	@Override
	public boolean execute(String actionToExecute, Object objectToUse)
			throws IllegalArgumentException {
		// TODO Implement Method
		throw new IllegalArgumentException(this+" is not implemented yet!");
	}
	@Override
	public Object getInfo(String request) {
		// TODO Implement Method
		throw new IllegalArgumentException(this+" is not implemented yet!");
	}
	@Override
	public String getPluginName() {
		return "Volume_Viewer";
	}
	@Override
	public String getProcLog() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public ArgumentParser setParameter(String inp) {
		return setParameter(new ArgumentParser(inp.split(" ")),"");
	}
	@Override
	public void setParameter(String parameterName, Object parameterValue)
			throws IllegalArgumentException {
		// TODO Auto-generated method stub

	}
	public static String join(String[] a,String delim) {return Arrays.asList(a).toString().replaceAll(", ", delim).replaceAll("^\\[|\\]$", "");}
	public static String join(String[] a) {return join(a,", ");}
	protected String snapshotPath="";
	protected boolean customRange=false;
	protected int crMin=0,crMax=0;
	@Override
	public ArgumentParser setParameter(ArgumentParser p, String prefix) {
		// TODO Update help descriptions
		batch = p.getOptionBoolean(prefix+"batch", batch,"Run in batch mode");
		customRange = p.getOptionBoolean(prefix+"usecr", customRange,"Use custom ranges");
		crMin = p.getOptionInt(prefix+"crmin", crMin,"Minimum value");
		crMax = p.getOptionInt(prefix+"crmax", crMax,"Maximum value");
		control.xloc = p.getOptionInt(prefix+"xloc", control.xloc,"x location");
		control.yloc = p.getOptionInt(prefix+"yloc", control.yloc,"y location");
		control.showTF = p.getOptionBoolean(prefix+"showTF",true, "Show the Transfer function");
		control.renderMode =  p.getOptionInt(prefix+"renderMode", control.renderMode,"mode to render in :"+join(Control.renderName));
		control.interpolationMode =p.getOptionInt(prefix+"interpolationMode", control.interpolationMode," mode ot use for interpolation: "+join(Control.interpolationName));
		control.backgroundColor =  new Color(p.getOptionInt(prefix+"backgroundColor", control.backgroundColor.getRGB(),"Background Color"));
		control.lutNr = p.getOptionInt(prefix+"lutNr", control.lutNr,"look up table number: "+join(Control.lutName));
		control.zAspect =  p.getOptionFloat(prefix+"zAspect", control.zAspect,"z aspect ratio");
		control.sampling = p.getOptionFloat(prefix+"sampling", control.sampling,"sampling of image");
		control.dist = p.getOptionFloat(prefix+"dist", control.dist,"distance to slice through the sample");
		control.showAxes = p.getOptionBoolean(prefix+"showAxes", control.showAxes,"Show the axes");
		control.showSlices = p.getOptionBoolean(prefix+"showSlices", control.showSlices,"Show the slices");
		control.showClipLines = p.getOptionBoolean(prefix+"showClipLines", control.showClipLines,"show the clip lines");
		control.scale = p.getOptionFloat(prefix+"scale", control.scale,"how much to scale the image");
		control.degreeX = p.getOptionFloat(prefix+"degreeX", control.degreeX,"degree of rotation in x");
		control.degreeY = p.getOptionFloat(prefix+"degreeY", control.degreeY,"degree of rotation in y");
		control.degreeZ = p.getOptionFloat(prefix+"degreeZ", control.degreeZ,"degree of rotation in z");
		control.alphaMode = p.getOptionInt(prefix+"alphaMode", control.alphaMode,"alpha mode to use");
		control.windowWidthImageRegion = p.getOptionInt(prefix+"windowWidthImageRegion", control.windowWidthImageRegion,"width of image region");
		control.windowWidthSlices = p.getOptionInt(prefix+"windowWidthSlices", control.windowWidthSlices,"width of slices region");
		control.windowHeight = p.getOptionInt(prefix+"windowHeight", control.windowHeight,"window height");
		control.useLight = p.getOptionBoolean(prefix+"useLight", control.useLight,"use light (solid/surface rendering");
		control.ambientValue = p.getOptionFloat(prefix+"ambientValue", control.ambientValue,"");
		control.diffuseValue = p.getOptionFloat(prefix+"diffuseValue", control.diffuseValue,"diffuse value");
		control.specularValue = p.getOptionFloat(prefix+"specularValue", control.specularValue,"specular value");
		control.shineValue = p.getOptionFloat(prefix+"shineValue", control.shineValue,"");
		control.objectLightValue =  p.getOptionFloat(prefix+"objectLightValue", control.objectLightValue,"");
		control.lightRed = p.getOptionInt(prefix+"lightRed", control.lightRed,"");
		control.lightGreen = p.getOptionInt(prefix+"lightGreen", control.lightGreen,"");
		control.lightBlue = p.getOptionInt(prefix+"lightBlue", control.lightBlue,"");
		control.snapshot =  p.getOptionBoolean(prefix+"snapshot",control.snapshot,"Take a snapshot");
		snapshotPath=p.getOptionPath(prefix+"output", snapshotPath, "Location to save the output image(s)");
		return p;
	}
	@Override
	public String toString() {
		return this.getClass().getName()+" -input="+internalImage.getPath()+" "+setParameter("").toString();
	}
	@Override
	public void run() {
		run("");
	}
	
	protected TImgRO internalImage=null;
	@Override
	public void LoadImages(TImgRO[] inImages) {
		assert(inImages.length>0);
		assert(inImages.length<2);
		internalImage=inImages[0];
	}
	
	public static void main(String args[]) {
		
		Volume_Viewer vv = new Volume_Viewer();
		ArgumentParser cArgs=new ArgumentParser(args);
		
		String inpath=cArgs.getOptionPath("input", "", "Image to be opened");
		
		vv.setParameter(cArgs,"");
		cArgs.checkForInvalid();
		TImg inData=TImgTools.ReadTImg(inpath);
		vv.LoadImages(new TImgRO[] {inData});
		vv.run(cArgs.toString());
		
		vv.waitForClose();
	}

	/**
	 * standard access from tipl based tools
	 * @param args
	 * @param inTImg TImgRO to be rendered
	 */
	public void tiplShowView(TImgRO inTImg) {
		LoadImages(new TImgRO[] {inTImg});
		run("");
		waitForClose();
	}
	/**
	 * run the plugin with a starting image
	 * @param args commands for plugin
	 * @param inImp the image to use
	 */
	public void run(String args) {
		if (internalImage==null) internalImage=ImageStackToTImg.FromImagePlus(WindowManager.getCurrentImage());
		
		imp=TImgToImagePlus.MakeImagePlus(internalImage);

		if (imp == null  || !(imp.getStackSize() > 1)) {
			IJ.showMessage("Stack required");
			return;
		}
		if(imp.getType()==ImagePlus.COLOR_RGB) 	// Check for RGB stack.
			control.isRGB = true;
		if (customRange) {
			vol = Volume.create(control, this,crMin,crMax);
		} else {
			vol = Volume.create(control, this);
		}

		lookupTable = new LookupTable(control, this);
		lookupTable.readLut();

		cube = new Cube(control, vol.widthV, vol.heightV, vol.depthV);
		cube.setSlicePositions(control.positionFactorX, control.positionFactorY, control.positionFactorZ, control.zAspect);

		tr = new Transform(control, control.windowWidthImageRegion, control.windowHeight, vol.xOffa, vol.yOffa, vol.zOffa);	
		tr.setScale(control.scale);
		tr.setZAspect(control.zAspect);
		setRotation(control.degreeX, control.degreeY, control.degreeZ);
		initializeTransformation();
		cube.setTransform(tr);
		cube.setTextPositions(control.scale, control.zAspect);
		trLight = new Transform(control, -1, -1, 0, 0, 0);
		trLight.initializeTransformation();

		gradientLUT = new Gradient(control, this, 256, 18);

		gui = new Gui(control, this);
		gui.makeGui();
		gui.newDisplayMode();

		lookupTable.setLut();
		lookupTable.orig();

		if (Interpreter.isBatchMode()) 
			batch = true;

		if (batch) {
			do {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} while (!control.isReady);
			if (snapshotPath.length()>0) gui.imageRegion.saveToImageFile(snapshotPath, this.toString());
			else gui.imageRegion.saveToImage();
			cleanup();
		}
		else {
			frame = new JFrame("3D Preview " +  version + " ");
			frame.setLocation(control.xloc,control.yloc);
			frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					if (control.snapshot)
						gui.imageRegion.saveToImage();
					writePrefs();
					cleanup();
					frame.dispose();
				}
			});
			frame.setVisible(true);
			frame.getContentPane().add(gui);
			gui.requestFocus();
			frame.pack(); 
			frame.validate();

			// add component/resize listener
			frame.addComponentListener(new ComponentAdapter(){
				public void componentResized(ComponentEvent event) {
					buildFrame();
				}
			});

		}
	}
	/** 
	 * wait for the frame to close
	 */
	public void waitForClose() {
		TImgToImagePlus.waitForFrameClose(frame);
		cleanup();
	}

	void reset() {
		tf_rgb = null;
		tf_a1 = null;
		tf_a2 = null;
		tf_a3 = null;
		tf_a4 = null;
		control.reset();

		buildFrame();
	}

	void buildFrame() {
		Insets insets = frame.getInsets();				
		int ww = frame.getWidth() - insets.left - insets.right;
		int wh = frame.getHeight() - insets.bottom - insets.top;

		int h  = wh - (gui.upperButtonPanel.getHeight() + gui.lowerButtonPanel.getHeight());
		if (h < control.windowMinHeight) h = control.windowMinHeight;
		Dimension dim = gui.picSlice.getSliceViewSize((int) (0.25*ww), h-130);
		int wl = dim.width;
		int transferPanelWidth = (control.showTF) ? gui.transferFunctionPanel.getPreferredSize().width : 0;
		int wr = ww - (wl + control.windowWidthSliderRegion + transferPanelWidth);
		if (wr < 480)  {
			int diff = 480 - wr;
			wr = 480;
			wl -= diff;
			if (wl < 200) wl = 200;
		}

		if (control.windowHeight > 0 && ww > 0) {
			control.windowHeight  = h;
			control.windowWidthSlices = wl;
			control.windowWidthImageRegion = wr;

			frame.getContentPane().remove(gui);

			tr = new Transform(control, control.windowWidthImageRegion, control.windowHeight, vol.xOffa, vol.yOffa, vol.zOffa);	
			tr.setScale(control.scale);
			tr.setZAspect(control.zAspect);
			setRotation(control.degreeX, control.degreeY, control.degreeZ);
			initializeTransformation();
			cube.setTransform(tr);

			gui = new Gui(control, this);
			gui.makeGui();
			frame.getContentPane().add(gui);
			frame.pack();	
			gui.requestFocus();
			gui.newDisplayMode();
		}
	}
	/*
	 * just remove all the variables by setting them to null and 'force' a garbage collect 
	 */
	private void cleanup() {

		vol.data3D = null;

		vol.grad3D = null;

		vol.mean3D = null;
		vol.diff3D = null;

		vol.col_3D = null;
		vol.aPaint_3D = null;  	
		vol.aPaint_3D2 = null;  	

		vol.nx_3D = null;
		vol.ny_3D = null;
		vol.nz_3D = null;

		vol.histValGrad = null; 
		vol.histMeanDiff = null; 
		vol.histVal =  null;			
		vol = null;

		gui.pic = null;
		gui.picSlice = null;
		gui = null;

		cube = null;
		lookupTable = null;
		tr = null;
		trLight = null;

		imp = null;
		gradientLUT = gradient2 = gradient3 = gradient4 = null;
		tf_rgb = null;
		tf_a1 = null;
		tf_a2 = null;
		tf_a3 = null;
		tf_a4 = null;

		control = null;

		System.gc();
	}

	private void readPrefsOLD() {

		control.xloc = (int) Prefs.get("VolumeViewer.xloc", 100);
		control.yloc = (int) Prefs.get("VolumeViewer.yloc", 50);
		control.showTF = Prefs.get("VolumeViewer.showTF", true);

		control.renderMode = (int) Prefs.get("VolumeViewer.renderMode", control.renderMode);
		control.interpolationMode = (int) Prefs.get("VolumeViewer.interpolationMode", control.interpolationMode);
		control.backgroundColor =  new Color((int)Prefs.get("VolumeViewer.backgroundColor", control.backgroundColor.getRGB()));
		control.lutNr = (int) Prefs.get("VolumeViewer.lutNr", control.lutNr);
		//control.zAspect = (float) Prefs.get("VolumeViewer.zAspect", control.zAspect);
		control.sampling = (float) Prefs.get("VolumeViewer.sampling", control.sampling);
		control.dist = (float) Prefs.get("VolumeViewer.dist", control.dist);
		control.showAxes = Prefs.get("VolumeViewer.showAxes", control.showAxes);
		control.showSlices = Prefs.get("VolumeViewer.showSlices", control.showSlices);
		control.showClipLines = Prefs.get("VolumeViewer.showClipLines", control.showClipLines);
		control.scale = (float) Prefs.get("VolumeViewer.scale", control.scale);
		control.degreeX = (float) Prefs.get("VolumeViewer.degreeX", control.degreeX);
		control.degreeY = (float) Prefs.get("VolumeViewer.degreeY", control.degreeY);
		control.degreeZ = (float) Prefs.get("VolumeViewer.degreeZ", control.degreeZ);
		control.alphaMode = (int) Prefs.get("VolumeViewer.alphaMode", control.alphaMode);
		control.windowWidthImageRegion = (int) Prefs.get("VolumeViewer.windowWidthImageRegion", control.windowWidthImageRegion);
		control.windowWidthSlices = (int) Prefs.get("VolumeViewer.windowWidthSlices", control.windowWidthSlices);
		control.windowHeight = (int) Prefs.get("VolumeViewer.windowHeight", control.windowHeight);
		control.useLight = Prefs.get("VolumeViewer.useLight", control.useLight);
		control.ambientValue = (float) Prefs.get("VolumeViewer.ambientValue", control.ambientValue);
		control.diffuseValue = (float) Prefs.get("VolumeViewer.diffuseValue", control.diffuseValue);
		control.specularValue = (float) Prefs.get("VolumeViewer.specularValue", control.specularValue);
		control.shineValue = (float) Prefs.get("VolumeViewer.shineValue", control.shineValue);
		control.objectLightValue = (float) Prefs.get("VolumeViewer.objectLightValue", control.objectLightValue);
		control.lightRed = (int) Prefs.get("VolumeViewer.lightRed", control.lightRed);
		control.lightGreen = (int) Prefs.get("VolumeViewer.lightGreen", control.lightGreen);
		control.lightBlue = (int) Prefs.get("VolumeViewer.lightBlue", control.lightBlue);
	}

	private void writePrefs() {
		Prefs.set("VolumeViewer.xloc", frame.getLocation().x);
		Prefs.set("VolumeViewer.yloc", frame.getLocation().y);
		Prefs.set("VolumeViewer.showTF", true);

		Prefs.set("VolumeViewer.renderMode", control.renderMode);
		Prefs.set("VolumeViewer.interpolationMode", control.interpolationMode);
		Prefs.set("VolumeViewer.backgroundColor", control.backgroundColor.getRGB());
		Prefs.set("VolumeViewer.lutNr", control.lutNr);
		//Prefs.set("VolumeViewer.zAspect", control.zAspect);
		Prefs.set("VolumeViewer.sampling", control.sampling);
		Prefs.set("VolumeViewer.dist", control.dist);
		Prefs.set("VolumeViewer.showAxes", control.showAxes);
		Prefs.set("VolumeViewer.showSlices", control.showSlices);
		Prefs.set("VolumeViewer.showClipLines", control.showClipLines);
		Prefs.set("VolumeViewer.scale", control.scale);
		Prefs.set("VolumeViewer.degreeX", control.degreeX);
		Prefs.set("VolumeViewer.degreeY", control.degreeY);
		Prefs.set("VolumeViewer.degreeZ", control.degreeZ);
		Prefs.set("VolumeViewer.alphaMode", control.alphaMode);
		Prefs.set("VolumeViewer.windowWidthImageRegion", control.windowWidthImageRegion);
		Prefs.set("VolumeViewer.windowWidthSlices", control.windowWidthSlices);
		Prefs.set("VolumeViewer.windowHeight", control.windowHeight);
		Prefs.set("VolumeViewer.useLight", control.useLight);
		Prefs.set("VolumeViewer.ambientValue", control.ambientValue);
		Prefs.set("VolumeViewer.diffuseValue", control.diffuseValue);
		Prefs.set("VolumeViewer.specularValue", control.specularValue);
		Prefs.set("VolumeViewer.shineValue", control.shineValue);
		Prefs.set("VolumeViewer.objectLightValue", control.objectLightValue);
		Prefs.set("VolumeViewer.lightRed", control.lightRed);
		Prefs.set("VolumeViewer.lightGreen", control.lightGreen);
		Prefs.set("VolumeViewer.lightBlue", control.lightBlue);
	}


	void setRotation(float degreeX, float degreeY, float degreeZ) {
		tr.setView(Math.toRadians(degreeX), Math.toRadians(degreeY), Math.toRadians(degreeZ));
		updateGuiSpinners();
	}

	void initializeTransformation() {	
		tr.initializeTransformation();
		cube.transformCorners(tr);
		updateGuiSpinners();
	}

	void setScale() {
		tr.setScale(control.scale);
	}

	void changeRotation(int xStart, int yStart, int xAct, int yAct, int width) {
		tr.setMouseMovement(xStart, yStart, xAct, yAct, width);
		updateGuiSpinners();
	}

	void changeRotationLight(int xStart, int yStart, int xAct, int yAct, int width) {
		trLight.setMouseMovement(xStart, yStart, xAct, yAct, width);
	}

	void setZAspect() {
		gui.updateDistSlider();
		tr.setZAspect(control.zAspect);
		if (control.zAspect == 0)
			control.zAspect = 0.01f;

		cube.setTextPositions(control.scale, control.zAspect);
	}	

	void changeTranslation(int dx, int dy) {
		tr.setMouseMovementOffset(dx, dy);	
		updateGuiSpinners();
	}

	void updateGuiSpinners() {

		control.degreeX = tr.getDegreeX();
		control.degreeY = tr.getDegreeY();
		control.degreeZ = tr.getDegreeZ();	

		if (!control.spinnersAreChanging && gui != null) 
			gui.setSpinners();	

		cube.transformCorners(tr);
	}

	private boolean getMacroParameters(String st) {		// read macro parameters
		String[] paramStrings   = {
				"display_mode=",
				"interpolation=",
				"bg_r=",
				"bg_g=",
				"bg_b=",
				"lut=",
				"z-aspect=",
				"sampling=",
				"dist=",
				"axes=",
				"slices=",
				"clipping",
				"scale=",
				"angle_x=",
				"angle_y=",
				"angle_z=",
				"alphamode=",
				"width=",
				"height=",
				"useLight=",
				"ambientValue=",
				"diffuseValue=",
				"specularValue=",
				"shineValue=",
				"objectLightValue=",
				"lightRed=",
				"lightGreen=",
				"lightBlue=",
				"snapshot="
		};

		float[] paramVals = {
				control.renderMode,
				control.interpolationMode,
				control.backgroundColor.getRed(),
				control.backgroundColor.getGreen(),
				control.backgroundColor.getBlue(),
				control.lutNr,
				control.zAspect,
				control.sampling,
				control.dist,
				(control.showAxes == true) ? 1 : 0,
						(control.showSlices == true) ? 1 : 0,
								(control.showClipLines == true) ? 1 : 0,
										control.scale,
										control.degreeX,
										control.degreeY,
										control.degreeZ,
										control.alphaMode,
										control.windowWidthImageRegion,
										control.windowHeight,
										(control.useLight == true) ? 1 : 0,
												control.ambientValue,
												control.diffuseValue,
												control.specularValue,
												control.shineValue,
												control.objectLightValue,
												control.lightRed,
												control.lightGreen,
												control.lightBlue,
												(control.snapshot == true) ? 1 : 0
		};
		boolean distWasSet = false;
		try {
			if (st != null) {
				StringTokenizer ex1; // Declare StringTokenizer Objects
				ex1 = new StringTokenizer(st); //Split on Space (default)

				String str;
				while (ex1.hasMoreTokens()) {
					str = ex1.nextToken();
					boolean valid = false;
					for (int j = 0; j<paramStrings.length; j++) {
						String pattern = paramStrings[j];
						if (str.lastIndexOf(pattern) > -1) { 
							int pos = str.lastIndexOf(pattern) + pattern.length();
							paramVals[j] = Float.parseFloat(str.substring(pos));
							valid = true;
							if (j==8)
								distWasSet = true;
						}
					}
					if (!valid) {
						IJ.error("Unkown macro parameter for the VolumeViewer plugin:\n" + 
								" \n"+
								str + " \n"+
								" \n"+
								"Valid parameters are: defaultValue type (range) \n"+
								"display_mode=0 	int (0 .. 4)\n"+
								"interpolation=1	int (0 .. 3)\n"+
								"bg_r=0  bg_g=52  bg_b=101	int int (0 .. 255)\n"+
								"lut=0				int (0 .. 4)\n"+
								"z-aspect=1 	float  (!= 0)\n"+
								"sampling=1		float ( > 0) \n"+
								"dist=0			float\n"+
								"axes=1			int (0,1)\n"+
								"slices=0		int (0,1)\n"+
								"clipping=0		int (0,1)\n"+
								"scale=1		float (> 0.25, < 128) \n"+
								"angle_x=115  angle_y=41  angle_z=17 	float (0 .. 360)\n"+
								"alphamode=0	int (0 .. 3)\n"+
								"width=500		int (>= 500)\n"+
								"height=660		int (>= 630)\n" +	
								"useLight=0		int (0,1)\n"+
								"ambientValue=0.5	float (0 .. 1)\n"+
								"diffuseValue=0.5	float (0 .. 1)\n"+
								"specularValue=0.5	float (0 .. 1)\n"+
								"shineValue=17		float (0 .. 200)\n"+
								"objectLightValue=0.5	float (0 .. 2)\n"+
								"lightRed=255  lightGreen=128  lightBlue=0	int (0 .. 255)\n"+
								"snapshot=0		int (0,1)"
								);
						return false;
					}	
				}
			}
		} catch (NumberFormatException e1) {
			IJ.error("Error in macro parameter list");
			return false;
		}


		control = new Control(this);
		control.distWasSet = distWasSet;
		control.renderMode =  (int) Math.min(4, Math.max(0, paramVals[0]));
		control.interpolationMode = (int) Math.min(3, Math.max(0, paramVals[1]));
		control.backgroundColor = new Color((int) paramVals[2], (int) paramVals[3], (int) paramVals[4]);
		control.lutNr =        (int) Math.min(4, Math.max(0, paramVals[5]));
		control.zAspect =            paramVals[6];
		control.sampling =           (paramVals[7] > 0) ? paramVals[7] : 1;
		control.dist =         		 paramVals[8];
		control.showAxes =    ((int) paramVals[9] == 0)? false : true;
		control.showSlices =  ((int) paramVals[10] == 0)? false : true;
		control.showClipLines=((int) paramVals[11] == 0)? false : true;
		control.scale =              Math.max(0.25f, Math.min(128, paramVals[12]));
		control.degreeX =      (int) paramVals[13];
		control.degreeY =      (int) paramVals[14];
		control.degreeZ =      (int) paramVals[15];
		control.alphaMode =    (int) paramVals[16];
		control.windowWidthImageRegion = Math.max(control.windowWidthImageRegion, (int) paramVals[17]);
		control.windowHeight =  Math.max(control.windowHeight, (int) paramVals[18]);
		control.useLight= 	   ((int) paramVals[19] == 0)? false : true;
		control.ambientValue=   Math.max(0f, Math.min(1f, paramVals[20]));
		control.diffuseValue= 	Math.max(0f, Math.min(1f, paramVals[21]));
		control.specularValue=  Math.max(0f, Math.min(1f, paramVals[22]));
		control.shineValue= 	Math.max(0f, Math.min(200f, paramVals[23]));
		control.objectLightValue=Math.max(0f, Math.min(200f, paramVals[24]));
		control.lightRed=		(int) Math.max(0, Math.min(255, paramVals[25]));
		control.lightGreen=		(int) Math.max(0, Math.min(255, paramVals[26]));
		control.lightBlue=		(int) Math.max(0, Math.min(255, paramVals[27]));
		control.snapshot= 	    ((int) paramVals[28] == 0)? false : true;

		control.scaledDist = control.dist*control.scale;

		return true;
	}



	public float[] trScreen2Vol(float xS, float yS, float zS) {
		return tr.trScreen2Vol(xS, yS, zS);
	}
	public float[] trScreen2Volume(float[] xyzS) {
		return tr.trScreen2Vol(xyzS[0], xyzS[1], xyzS[2]);
	}

	public float[] trVolume2Screen(float[] xyzV) {
		return tr.trVol2Screen(xyzV[0], xyzV[1], xyzV[2]);
	}
	public float[] trVolume2Screen(float xV, float yV, float zV) {
		return tr.trVol2Screen(xV, yV, zV);
	}

	public float[] trLightScreen2Vol(float xS, float yS, float zS) {
		return trLight.trScreen2Vol(xS, yS, zS);
	}
	public float[] trLightVolume2Screen(float xV, float yV, float zV) {
		return trLight.trVol2Screen(xV, yV, zV);
	}
	

}	
