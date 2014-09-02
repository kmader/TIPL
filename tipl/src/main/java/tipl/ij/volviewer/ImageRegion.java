package tipl.ij.volviewer;

/*
 * Volume Viewer 2.0
 * 27.11.2012
 * 
 * (C) Kai Uwe Barthel
 */

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.io.SaveDialog;
import ij.process.ImageProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

class ImageRegion extends JPanel {
    private static final long serialVersionUID = 1L;

    private Control control;

    private Pic pic;
    private Image image;
    private int width;
    private int height;

    private TextField[] textField = null;
    private Line[] lines = null;
    private Line[] clipLines = null;

    private Color planeColor = Color.lightGray;

    private Font font0 = new Font("Sans", Font.PLAIN, 18);
    private Font font1 = new Font("Sans", Font.PLAIN, 12);

    private int plotNumber = 1;

    public ImageRegion(Control control) {
        this.control = control;
    }

    /**
     * Write the output image in the given path and save the command to reproduce it as a pl.txt
     *
     * @param outImage
     * @param path
     * @param procLog  command to reproduce the image
     */
    public static synchronized void saveWithLog(ImagePlus outImage, String path, String procLog) {
        IJ.save(outImage, path);

        IJ.saveString(procLog, path + ".pl.log"); //TODO write image
    }

    public void setPlaneColor(Color color) {
        planeColor = color;
    }

    public int[] getValues(int xm, int ym) {
        int[] vals = pic.getValuesfromSlices(xm, ym);
        return vals;
    }

    public void newText(int n) {
        textField = new TextField[n];
        for (int i = 0; i < textField.length; i++)
            textField[i] = new TextField();
    }

    public void setText(String text, int i, int posx, int posy, int z, Color color) {
        textField[i] = new TextField(text, color, posx, posy, z);
    }

    public void setText(String text, int i, int posx, int posy, int z, Color color, int fontNr) {
        textField[i] = new TextField(text, color, posx, posy, z, fontNr);
    }

    public void setText(String text, int i, Color color) {
        textField[i].setText(text);
        textField[i].setColor(color);
    }

    public void setText(String text, int i) {
        textField[i].setText(text);
    }

    public void setTextPos(int i, double xpos, double ypos, int z) {
        textField[i].setXpos(xpos);
        textField[i].setYpos(ypos);
        textField[i].setZ(z);
    }

    public void newLines(int n) {
        lines = new Line[n];
    }

    public void newClipLine(int n) {
        clipLines = new Line[n];
    }

    public void setLine(int i, int x1, int y1, int x2, int y2, int z, Color color) {
        lines[i] = new Line(x1, y1, x2, y2, z, color);
    }

    public void setClipLine(int i, int x1, int y1, int x2, int y2, int z, Color color) {
        clipLines[i] = new Line(x1, y1, x2, y2, z, color);
    }

    public void setPic(Pic pic) {
        this.pic = pic;
        height = pic.getHeight();
        width = pic.getWidth();
        image = pic.getImage();
    }

    public void setImage(Image image) {
        this.image = image;
    }

    void findLines(Cube cube, double scaledDist, int depthV) {

        //cube.setColor(0xFFFF0000);
        cube.findIntersections(scaledDist);

        double[][] iS = cube.getInterSections();
        iS[0][0] = iS[1][0] = -1;

        Color color = Color.cyan;

        cube.findSliceIntersectionsXY(scaledDist);

        if (iS[1][0] != -1) {
            int x1 = (int) (pic.xo + iS[0][0] / pic.xd);
            int x2 = (int) (pic.xo + iS[1][0] / pic.xd);
            int y1 = (int) (pic.yo_xy + iS[0][1] / pic.yd);
            int y2 = (int) (pic.yo_xy + iS[1][1] / pic.yd);

            setLine(0, x1, y1, x2, y2, -1, color);
        } else
            setLine(0, 0, 0, 0, 0, 1, color);

        iS[0][0] = iS[1][0] = -1;
        color = Color.green;
        cube.findSliceIntersectionsYZ(scaledDist);

        if (iS[1][0] != -1) {
            int x1 = (int) (pic.xo + iS[0][1] / pic.yd);
            int x2 = (int) (pic.xo + iS[1][1] / pic.yd);
            int y1 = (int) (pic.yo_yz + (depthV - iS[0][2]) / pic.zd);
            int y2 = (int) (pic.yo_yz + (depthV - iS[1][2]) / pic.zd);

            setLine(1, x1, y1, x2, y2, -1, color);
        } else
            setLine(1, 0, 0, 0, 0, 1, color);

        iS[0][0] = iS[1][0] = -1;
        color = Color.red;
        cube.findIntersections_xz(scaledDist);

        if (iS[1][0] != -1) {
            int x1 = (int) (pic.xo + iS[0][0] / pic.xd);
            int x2 = (int) (pic.xo + iS[1][0] / pic.xd);
            int y1 = (int) (pic.yo_xz + (depthV - iS[0][2]) / pic.zd);
            int y2 = (int) (pic.yo_xz + (depthV - iS[1][2]) / pic.zd);

            setLine(2, x1, y1, x2, y2, -1, color);
        } else
            setLine(2, 0, 0, 0, 0, 1, color);
    }

    /**
     * get the current snapshot as an imageplus object (more useful for future processing
     *
     * @return filename
     */
    protected synchronized ImagePlus getImageAsImagePlus() {
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        paint(bufferedImage.createGraphics());

        Graphics2D g2d = bufferedImage.createGraphics();
        Color backgroundColor = control.backgroundColor;
        if (backgroundColor.getRed() + backgroundColor.getGreen() + backgroundColor.getGreen() > 3 * 128)
            g2d.setColor(Color.black);
        else
            g2d.setColor(Color.white);

        g2d.drawString("TIPL Volume Viewer", width - 100, height - 10);
        g2d.dispose();

        String s = "Volume_Viewer_" + plotNumber;
        //create new image
        ImagePlus plotImage = NewImage.createRGBImage(s, width, height, 1, NewImage.FILL_BLACK);
        ImageProcessor ip = plotImage.getProcessor();

        int[] pixels = (int[]) ip.getPixels();
        //put the old image data into the new imageplus
        bufferedImage.getRGB(0, 0, width, height, pixels, 0, width);
        return plotImage;

    }

    /**
     * Save the current image as a file (for when the tool is not used inside imageJ
     *
     * @param path path to save to
     */
    public synchronized void saveToImageFile(String path, String procLog) {
        ImagePlus plotImage = getImageAsImagePlus();
        saveWithLog(plotImage, path, procLog);
    }

    public synchronized void saveToImageFile(String procLog) {
        ImagePlus plotImage = getImageAsImagePlus();
        SaveDialog sd = new SaveDialog("Save snapshot as...", plotImage.getTitle(), ".tiff");
        String fileName = sd.getDirectory() + sd.getFileName();
        System.out.println(this + ": Saving as " + fileName);

        saveWithLog(plotImage, fileName, procLog);

    }

    public synchronized void saveToImage() {

        ImagePlus plotImage = getImageAsImagePlus();

        plotImage.show();
        plotImage.updateAndDraw();

        plotNumber++;
    }


    //-------------------------------------------------------------------
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setBackground(planeColor);
        g2.clearRect(0, 0, width, height);

        g2.setFont(font0);

        if (textField != null && control.showAxes == true)
            for (TextField aTextField : textField) {
                if (aTextField != null)
                    if (aTextField.getZ() > 0) {
                        g2.setColor(aTextField.getColor());
                        g2.drawString(aTextField.getText(),(float) aTextField.getXpos(),(float) aTextField.getYpos());
                    }
            }

        if (lines != null)
            for (Line line : lines) {
                if (line != null)
                    if (line.getZ() > 0) {
                        g2.setColor(line.getColor());
                        //g2.setColor(Color.yellow);
                        g2.drawLine(line.getX1(), line.getY1(), line.getX2(), line.getY2());
                    }
            }

        if (image != null)
            g2.drawImage(image, 0, 0, width, height, this);

        if (lines != null)
            for (Line line : lines) {
                if (line != null)
                    if (line.getZ() <= 0) {
                        g2.setColor(line.getColor());
                        g2.drawLine(line.getX1(), line.getY1(), line.getX2(), line.getY2());
                    }
            }

        if (clipLines != null && control.showClipLines == true)
            for (Line clipLine : clipLines) {
                if (clipLine != null) {
                    g2.setColor(clipLine.getColor());
                    g2.drawLine(clipLine.getX1(), clipLine.getY1(), clipLine.getX2(), clipLine.getY2());
                }
            }

        if (textField != null && control.showAxes == true)
            for (TextField aTextField : textField) {
                if (aTextField != null)
                    if (aTextField.getZ() <= 0) {
                        if (aTextField.getFontNr() == 1)
                            g2.setFont(font1);

                        g2.setColor(aTextField.getColor());
                        g2.drawString(aTextField.getText(),(float) aTextField.getXpos(), (float) aTextField.getYpos());
                    }
            }
    }

    public void update(Graphics g) {
        paintComponent(g);
    }

    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public Dimension getMinimumSize() {
        return new Dimension(width, height);
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}

