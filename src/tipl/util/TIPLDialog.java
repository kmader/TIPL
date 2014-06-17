/**
 *
 */
package tipl.util;

import ij.IJ;
import ij.Macro;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GUI;
import ij.gui.HTMLDialog;
import ij.gui.MultiLineLabel;
import ij.macro.Interpreter;
import ij.macro.MacroRunner;
import ij.plugin.ScreenGrabber;
import ij.plugin.frame.Recorder;
import ij.util.Tools;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Vector;

/**
 * Mader: I have modified this class since the ImageJ version does not return
 * components and has all fields as private making subclassing a huge pain in
 * the ass
 * <p/>
 * <p/>
 * This class is a customizable modal dialog box. Here is an example TIPLDialog
 * with one string field and two numeric fields:
 * <p/>
 * <pre>
 * public class Generic_Dialog_Example implements PlugIn {
 * 	static String title = &quot;Example&quot;;
 * 	static int width = 512, height = 512;
 *
 * 	public void run(String arg) {
 * 		TIPLDialog gd = new TIPLDialog(&quot;New Image&quot;);
 * 		gd.addStringField(&quot;Title: &quot;, title);
 * 		gd.addNumericField(&quot;Width: &quot;, width, 0);
 * 		gd.addNumericField(&quot;Height: &quot;, height, 0);
 * 		gd.showDialog();
 * 		if (gd.wasCanceled())
 * 			return;
 * 		title = gd.getNextString();
 * 		width = (int) gd.getNextNumber();
 * 		height = (int) gd.getNextNumber();
 * 		IJ.newImage(title, &quot;8-bit&quot;, width, height, 1);
 *    }
 * }
 * </pre>
 * <p/>
 * To work with macros, the first word of each component label must be unique.
 * If this is not the case, add underscores, which will be converted to spaces
 * when the dialog is displayed. For example, change the checkbox labels
 * "Show Quality" and "Show Residue" to "Show_Quality" and "Show_Residue".
 */
public class TIPLDialog extends Dialog implements ActionListener, TextListener,
        FocusListener, ItemListener, KeyListener, AdjustmentListener,
        WindowListener {
    /**
     * begin old code *
     */

    public static final int MAX_SLIDERS = 25;
    final protected static MouseListener emptyMouseListener = new MouseListener() {
        @Override
        public void mouseClicked(final MouseEvent arg0) {
        }

        @Override
        public void mouseEntered(final MouseEvent arg0) {
        }

        @Override
        public void mouseExited(final MouseEvent arg0) {
        }

        @Override
        public void mousePressed(final MouseEvent arg0) {
        }

        @Override
        public void mouseReleased(final MouseEvent arg0) {
        }

    };
    private final static String previewRunning = "wait...";
    private final GridBagLayout grid;
    private final GridBagConstraints c;
    private final boolean macro;
    private final String macroOptions;
    private final String previewLabel = " Preview";
    protected Vector numberField, stringField, checkbox, choice, slider,
            radioButtonGroup;
    protected TextArea textArea1, textArea2;
    protected Vector defaultValues, defaultText;
    protected Component theLabel;
    private Button cancel, okay, no, help;
    private String okLabel = "  OK  ";
    private String cancelLabel = "Cancel";
    private String helpLabel = "Help";
    private boolean wasCanceled, wasOKed;
    private int y;
    private int nfIndex, sfIndex, cbIndex, choiceIndex, textAreaIndex,
            radioButtonIndex;
    private boolean firstNumericField = true;
    private boolean invalidNumber;
    private String errorMessage;
    private boolean firstPaint = true;
    private Hashtable labels;
    private int topInset, leftInset, bottomInset;
    private boolean customInsets;
    private int[] sliderIndexes;
    private double[] sliderScales;
    private Checkbox previewCheckbox; // the "Preview" Checkbox, if any
    private Vector dialogListeners; // the Objects to notify on user input
    private boolean recorderOn; // whether recording is allowed
    private boolean yesNoCancel;
    private char echoChar;
    private boolean hideCancelButton;
    private boolean centerDialog = true;
    private String helpURL;
    private String yesLabel, noLabel;

    /**
     * Creates a new TIPLDialog with the specified title. Uses the current image
     * image window as the parent frame or the ImageJ frame if no image windows
     * are open. Dialog parameters are recorded by ImageJ's command recorder but
     * this requires that the first word of each label be unique.
     */
    public TIPLDialog(final String title) {
        this(title,
                WindowManager.getCurrentImage() != null ? WindowManager
                        .getCurrentImage().getWindow()
                        : IJ.getInstance() != null ? IJ.getInstance()
                        : new Frame());
    }

    /**
     * Creates a new TIPLDialog using the specified title and parent frame.
     */
    public TIPLDialog(final String title, final Frame parent) {
        super(parent == null ? new Frame() : parent, title, true);
        // super(title,parent);
        if (Prefs.blackCanvas) {
            setForeground(SystemColor.controlText);
            setBackground(SystemColor.control);
        }
        // if (IJ.isLinux())
        // setBackground(new Color(238, 238, 238));
        grid = new GridBagLayout();
        c = new GridBagConstraints();
        setLayout(grid);
        macroOptions = Macro.getOptions();
        macro = macroOptions != null;
        addKeyListener(this);
        addWindowListener(this);
    }

    public static GUIControl asGUI(final Checkbox f) {
        return new CallbackGUIControlWithMouse() {
            @Override
            public String getValueAsString() {
                return f.getState() ? "true" : "false";
            }

            @Override
            protected void pushMLToObject(final MouseListener curListener) {
                f.addMouseListener(curListener);
            }

        };
    }

    public static GUIControl asGUI(final TextField f) {
        return new CallbackGUIControlWithMouse() {
            @Override
            public String getValueAsString() {
                return f.getText();
            }

            @Override
            protected void pushMLToObject(final MouseListener curListener) {
                f.addMouseListener(curListener);
            }
        };
    }

    void accessTextFields() {
        if (stringField != null) {
            for (int i = 0; i < stringField.size(); i++)
                ((TextField) (stringField.elementAt(i))).getText();
        }
        if (numberField != null) {
            for (int i = 0; i < numberField.size(); i++)
                ((TextField) (numberField.elementAt(i))).getText();
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        final Object source = e.getSource();
        if (source == okay || source == cancel | source == no) {
            wasCanceled = source == cancel;
            wasOKed = source == okay;
            dispose();
        } else if (source == help) {
            if (hideCancelButton) {
                if (helpURL != null && helpURL.equals("")) {
                    notifyListeners(e);
                    return;
                } else {
                    wasOKed = true;
                    dispose();
                }
            }
            showHelp();
        } else
            notifyListeners(e);
    }

    /**
     * Adds a group of checkboxs using a grid layout.
     *
     * @param rows          the number of rows
     * @param columns       the number of columns
     * @param labels        the labels
     * @param defaultValues the initial states
     */
    public void addCheckboxGroup(final int rows, final int columns,
                                 final String[] labels, final boolean[] defaultValues) {
        addCheckboxGroup(rows, columns, labels, defaultValues, null);
    }

    /**
     * Adds a group of checkboxs using a grid layout.
     *
     * @param rows          the number of rows
     * @param columns       the number of columns
     * @param labels        the labels
     * @param defaultValues the initial states
     * @param headings      the column headings Example:
     *                      http://imagej.nih.gov/ij/plugins/multi
     *                      -column-dialog/index.html
     */
    public void addCheckboxGroup(final int rows, final int columns,
                                 final String[] labels, final boolean[] defaultValues,
                                 final String[] headings) {
        final Panel panel = new Panel();
        final int nRows = headings != null ? rows + 1 : rows;
        panel.setLayout(new GridLayout(nRows, columns, 6, 0));
        if (checkbox == null)
            checkbox = new Vector(12);
        if (headings != null) {
            final Font font = new Font("SansSerif", Font.BOLD, 12);
            for (int i = 0; i < columns; i++) {
                if (i > headings.length - 1 || headings[i] == null)
                    panel.add(new Label(""));
                else {
                    final Label label = new Label(headings[i]);
                    label.setFont(font);
                    panel.add(label);
                }
            }
        }
        int i1 = 0;
        final int[] index = new int[labels.length];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                final int i2 = col * rows + row;
                if (i2 >= labels.length)
                    break;
                index[i1] = i2;
                String label = labels[i1];
                if (label == null || label.length() == 0) {
                    final Label lbl = new Label("");
                    panel.add(lbl);
                    i1++;
                    continue;
                }
                if (label.indexOf('_') != -1)
                    label = label.replace('_', ' ');
                final Checkbox cb = new Checkbox(label);
                checkbox.addElement(cb);
                cb.setState(defaultValues[i1]);
                cb.addItemListener(this);
                if (Recorder.record || macro)
                    saveLabel(cb, labels[i1]);
                if (IJ.isLinux()) {
                    final Panel panel2 = new Panel();
                    panel2.setLayout(new BorderLayout());
                    panel2.add("West", cb);
                    panel.add(panel2);
                } else
                    panel.add(cb);
                i1++;
            }
        }
        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = getInsets(10, 0, 0, 0);
        grid.setConstraints(panel, c);
        add(panel);
        y++;
    }

    /**
     * Adds a popup menu.
     *
     * @param label       the label
     * @param items       the menu items
     * @param defaultItem the menu item initially selected
     */
    public void addChoice(final String label, final String[] items,
                          final String defaultItem) {
        String label2 = label;
        if (label2.indexOf('_') != -1)
            label2 = label2.replace('_', ' ');
        final Label theLabel = makeLabel(label2);
        c.gridx = 0;
        c.gridy = y;
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = 1;
        if (choice == null) {
            choice = new Vector(4);
            c.insets = getInsets(5, 0, 5, 0);
        } else
            c.insets = getInsets(0, 0, 5, 0);
        grid.setConstraints(theLabel, c);
        add(theLabel);
        final Choice thisChoice = new Choice();
        thisChoice.addKeyListener(this);
        thisChoice.addItemListener(this);
        for (String item : items) thisChoice.addItem(item);
        thisChoice.select(defaultItem);
        c.gridx = 1;
        c.gridy = y;
        c.anchor = GridBagConstraints.WEST;
        grid.setConstraints(thisChoice, c);
        add(thisChoice);
        choice.addElement(thisChoice);
        if (Recorder.record || macro)
            saveLabel(thisChoice, label);
        y++;
    }

    /**
     * Add an Object implementing the DialogListener interface. This object will
     * be notified by its dialogItemChanged method of input to the dialog. The
     * first DialogListener will be also called after the user has typed 'OK' or
     * if the dialog has been invoked by a macro; it should read all input
     * fields of the dialog. For other listeners, the OK button will not cause a
     * call to dialogItemChanged; the CANCEL button will never cause such a
     * call.
     *
     * @param dl the Object that wants to listen.
     */
    public void addDialogListener(final DialogListener dl) {
        if (dialogListeners == null)
            dialogListeners = new Vector();
        dialogListeners.addElement(dl);
        if (IJ.debugMode)
            IJ.log("TIPLDialog: Listener added: " + dl);
    }

    /**
     * Adds a "Help" button that opens the specified URL in the default browser.
     * With v1.46b or later, displays an HTML formatted message if 'url' starts
     * with "<html>". There is an example at
     * http://imagej.nih.gov/ij/macros/js/DialogWithHelp.js
     */
    public void addHelp(final String url) {
        helpURL = url;
    }

    /**
     * Adds a message consisting of one or more lines of text.
     */
    public void addMessage(final String text) {
        addMessage(text, null, null);
    }

    /**
     * Adds a message consisting of one or more lines of text, which will be
     * displayed using the specified font.
     */
    public void addMessage(final String text, final Font font) {
        addMessage(text, font, null);
    }

    /**
     * Adds a message consisting of one or more lines of text, which will be
     * displayed using the specified font and color.
     */
    public void addMessage(final String text, final Font font, final Color color) {
        theLabel = null;
        if (text.indexOf('\n') >= 0)
            theLabel = new MultiLineLabel(text);
        else
            theLabel = new Label(text);
        // theLabel.addKeyListener(this);
        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = getInsets(text.equals("") ? 0 : 10, 20, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        grid.setConstraints(theLabel, c);
        if (font != null)
            theLabel.setFont(font);
        if (color != null)
            theLabel.setForeground(color);
        add(theLabel);
        c.fill = GridBagConstraints.NONE;
        y++;
    }

    /**
     * Adds a Panel to the dialog.
     */
    public void addPanel(final Panel panel) {
        addPanel(panel, GridBagConstraints.WEST, new Insets(5, 0, 0, 0));
    }

    /**
     * Adds a Panel to the dialog with custom contraint and insets. The defaults
     * are GridBagConstraints.WEST (left justified) and "new Insets(5, 0, 0, 0)"
     * (5 pixels of padding at the top).
     */
    public void addPanel(final Panel panel, final int constraints,
                         final Insets insets) {
        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 2;
        c.anchor = constraints;
        c.insets = insets;
        grid.setConstraints(panel, c);
        add(panel);
        y++;
    }

    /**
     * Adds a radio button group.
     *
     * @param label       group label (or null)
     * @param items       radio button labels
     * @param rows        number of rows
     * @param columns     number of columns
     * @param defaultItem button initially selected
     */
    public void addRadioButtonGroup(String label, final String[] items,
                                    final int rows, final int columns, final String defaultItem) {
        final Panel panel = new Panel();
        final int n = items.length;
        panel.setLayout(new GridLayout(rows, columns, 0, 0));
        final CheckboxGroup cg = new CheckboxGroup();
        for (String item : items) panel.add(new Checkbox(item, cg, item.equals(defaultItem)));
        if (radioButtonGroup == null)
            radioButtonGroup = new Vector();
        radioButtonGroup.addElement(cg);
        final Insets insets = getInsets(5, 10, 0, 0);
        if (label == null || label.equals("")) {
            label = "rbg" + radioButtonGroup.size();
            insets.top += 5;
        } else {
            setInsets(10, insets.left, 0);
            addMessage(label);
            insets.top = 2;
            insets.left += 10;
        }
        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(insets.top, insets.left, 0, 0);
        grid.setConstraints(panel, c);
        add(panel);
        if (Recorder.record || macro)
            saveLabel(cg, label);
        y++;
    }

    /**
     * Adds one or two (side by side) text areas.
     *
     * @param text1 initial contents of the first text area
     * @param text2 initial contents of the second text area or null
     * @param rows  the number of rows
     * @param rows  the number of columns
     */
    public void addTextAreas(final String text1, final String text2,
                             final int rows, final int columns) {
        if (textArea1 != null)
            return;
        final Panel panel = new Panel();
        textArea1 = new TextArea(text1, rows, columns, TextArea.SCROLLBARS_NONE);
        if (IJ.isLinux())
            textArea1.setBackground(Color.white);
        textArea1.addTextListener(this);
        panel.add(textArea1);
        if (text2 != null) {
            textArea2 = new TextArea(text2, rows, columns,
                    TextArea.SCROLLBARS_NONE);
            if (IJ.isLinux())
                textArea2.setBackground(Color.white);
            panel.add(textArea2);
        }
        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = getInsets(15, 20, 0, 0);
        grid.setConstraints(panel, c);
        add(panel);
        y++;
    }

    @Override
    public synchronized void adjustmentValueChanged(final AdjustmentEvent e) {
        final Object source = e.getSource();
        for (int i = 0; i < slider.size(); i++) {
            if (source == slider.elementAt(i)) {
                final Scrollbar sb = (Scrollbar) source;
                final TextField tf = (TextField) numberField
                        .elementAt(sliderIndexes[i]);
                final int digits = sliderScales[i] == 1.0 ? 0 : 2;
                tf.setText("" + IJ.d2s(sb.getValue() / sliderScales[i], digits));
            }
        }
    }

    /**
     * Adds a checkbox.
     *
     * @param label        the label
     * @param defaultValue the initial state
     */
    public GUIControl appendCheckbox(final String label,
                                     final boolean defaultValue) {
        return appendCheckbox(label, defaultValue, false);
    }

    /**
     * Adds a checkbox; does not make it recordable if isPreview is true. With
     * isPreview true, the checkbox can be referred to as previewCheckbox from
     * hereon.
     */
    private GUIControl appendCheckbox(final String label,
                                      final boolean defaultValue, final boolean isPreview) {
        String label2 = label;
        if (label2.indexOf('_') != -1)
            label2 = label2.replace('_', ' ');
        if (checkbox == null) {
            checkbox = new Vector(4);
            c.insets = getInsets(15, 20, 0, 0);
        } else
            c.insets = getInsets(0, 20, 0, 0);
        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        final Checkbox cb = new Checkbox(label2);
        grid.setConstraints(cb, c);
        cb.setState(defaultValue);
        cb.addItemListener(this);
        cb.addKeyListener(this);
        add(cb);
        checkbox.addElement(cb);
        // ij.IJ.write("addCheckbox: "+ y+" "+cbIndex);
        if (!isPreview && (Recorder.record || macro)) // preview checkbox is not
            // recordable
            saveLabel(cb, label);
        if (isPreview)
            previewCheckbox = cb;
        y++;
        return asGUI(cb);
    }

    /**
     * Adds a numeric field. The first word of the label must be unique or
     * command recording will not work.
     *
     * @param label        the label
     * @param defaultValue value to be initially displayed
     * @param digits       number of digits to right of decimal point
     */
    public GUIControl appendNumericField(final String label,
                                         final double defaultValue, final int digits) {
        return appendNumericField(label, defaultValue, digits, 6, null);
    }

    /**
     * Adds a numeric field. The first word of the label must be unique or
     * command recording will not work.
     *
     * @param label        the label
     * @param defaultValue value to be initially displayed
     * @param digits       number of digits to right of decimal point
     * @param columns      width of field in characters
     * @param units        a string displayed to the right of the field
     */
    public GUIControl appendNumericField(final String label,
                                         final double defaultValue, final int digits, int columns,
                                         final String units) {
        String label2 = label;
        if (label2.indexOf('_') != -1)
            label2 = label2.replace('_', ' ');
        final Label theLabel = makeLabel(label2);
        c.gridx = 0;
        c.gridy = y;
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = 1;
        if (firstNumericField)
            c.insets = getInsets(5, 0, 3, 0);
        else
            c.insets = getInsets(0, 0, 3, 0);
        grid.setConstraints(theLabel, c);
        add(theLabel);
        if (numberField == null) {
            numberField = new Vector(5);
            defaultValues = new Vector(5);
            defaultText = new Vector(5);
        }
        if (IJ.isWindows())
            columns -= 2;
        if (columns < 1)
            columns = 1;
        final TextField tf = new TextField(IJ.d2s(defaultValue, digits),
                columns);
        if (IJ.isLinux())
            tf.setBackground(Color.white);
        tf.addActionListener(this);
        tf.addTextListener(this);
        tf.addFocusListener(this);
        tf.addKeyListener(this);
        numberField.addElement(tf);
        defaultValues.addElement(new Double(defaultValue));
        defaultText.addElement(tf.getText());
        c.gridx = 1;
        c.gridy = y;
        c.anchor = GridBagConstraints.WEST;
        tf.setEditable(true);
        // if (firstNumericField) tf.selectAll();
        firstNumericField = false;
        if (units == null || units.equals("")) {
            grid.setConstraints(tf, c);
            add(tf);
        } else {
            final Panel panel = new Panel();
            panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            panel.add(tf);
            panel.add(new Label(" " + units));
            grid.setConstraints(panel, c);
            add(panel);
        }
        if (Recorder.record || macro)
            saveLabel(tf, label);
        y++;
        return asGUI(tf);
    }

    /**
     * Adds a slider (scroll bar) to the dialog box. Floating point values will
     * be used if (maxValue-minValue)<=5.0 and either minValue or maxValue are
     * non-integer.
     *
     * @param label        the label
     * @param minValue     the minimum value of the slider
     * @param maxValue     the maximum value of the slider
     * @param defaultValue the initial value of the slider
     */
    public GUIControl appendSlider(final String label, double minValue,
                                   double maxValue, double defaultValue) {
        int columns = 4;
        int digits = 0;
        double scale = 1.0;
        if ((maxValue - minValue) <= 5.0
                && (minValue != (int) minValue || maxValue != (int) maxValue || defaultValue != (int) defaultValue)) {
            scale = 20.0;
            minValue *= scale;
            maxValue *= scale;
            defaultValue *= scale;
            digits = 2;
        }
        String label2 = label;
        if (label2.indexOf('_') != -1)
            label2 = label2.replace('_', ' ');
        final Label theLabel = makeLabel(label2);
        c.gridx = 0;
        c.gridy = y;
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = 1;
        c.insets = new Insets(0, 0, 3, 0);
        grid.setConstraints(theLabel, c);
        add(theLabel);

        if (slider == null) {
            slider = new Vector(5);
            sliderIndexes = new int[MAX_SLIDERS];
            sliderScales = new double[MAX_SLIDERS];
        }
        final Scrollbar s = new Scrollbar(Scrollbar.HORIZONTAL,
                (int) defaultValue, 1, (int) minValue, (int) maxValue + 1);
        GUI.fix(s);

        slider.addElement(s);
        s.addAdjustmentListener(this);
        s.setUnitIncrement(1);

        if (numberField == null) {
            numberField = new Vector(5);
            defaultValues = new Vector(5);
            defaultText = new Vector(5);
        }
        if (IJ.isWindows())
            columns -= 2;
        if (columns < 1)
            columns = 1;
        final TextField tf = new TextField(
                IJ.d2s(defaultValue / scale, digits), columns);
        if (IJ.isLinux())
            tf.setBackground(Color.white);
        tf.addActionListener(this);
        tf.addTextListener(this);
        tf.addFocusListener(this);
        tf.addKeyListener(this);
        numberField.addElement(tf);
        sliderIndexes[slider.size() - 1] = numberField.size() - 1;
        sliderScales[slider.size() - 1] = scale;
        defaultValues.addElement(new Double(defaultValue / scale));
        defaultText.addElement(tf.getText());
        tf.setEditable(true);
        final Panel panel = new Panel();
        final GridBagLayout pgrid = new GridBagLayout();
        final GridBagConstraints pc = new GridBagConstraints();
        panel.setLayout(pgrid);
        // label
        // pc.insets = new Insets(5, 0, 0, 0);
        // pc.gridx = 0; pc.gridy = 0;
        // pc.gridwidth = 1;
        // pc.anchor = GridBagConstraints.EAST;
        // pgrid.setConstraints(theLabel, pc);
        // panel.add(theLabel);
        // slider
        pc.gridx = 0;
        pc.gridy = 0;
        pc.gridwidth = 1;
        pc.ipadx = 75;
        pc.anchor = GridBagConstraints.WEST;
        pgrid.setConstraints(s, pc);
        panel.add(s);
        pc.ipadx = 0; // reset
        // text field
        pc.gridx = 1;
        pc.insets = new Insets(5, 5, 0, 0);
        pc.anchor = GridBagConstraints.EAST;
        pgrid.setConstraints(tf, pc);
        panel.add(tf);

        grid.setConstraints(panel, c);
        c.gridx = 1;
        c.gridy = y;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 0, 0);
        grid.setConstraints(panel, c);
        add(panel);
        y++;
        if (Recorder.record || macro)
            saveLabel(tf, label);
        return asGUI(tf);
    }

    /**
     * Adds an 8 column text field.
     *
     * @param label       the label
     * @param defaultText the text initially displayed
     */
    public GUIControl appendStringField(final String label,
                                        final String defaultText) {
        return appendStringField(label, defaultText, 8);
    }

    /**
     * Adds a text field.
     *
     * @param label       the label
     * @param defaultText text initially displayed
     * @param columns     width of the text field
     */
    public GUIControl appendStringField(final String label,
                                        final String defaultText, final int columns) {
        String label2 = label;
        if (label2.indexOf('_') != -1)
            label2 = label2.replace('_', ' ');
        final Label theLabel = makeLabel(label2);
        c.gridx = 0;
        c.gridy = y;
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = 1;
        final boolean custom = customInsets;
        if (stringField == null) {
            stringField = new Vector(4);
            c.insets = getInsets(5, 0, 5, 0);
        } else
            c.insets = getInsets(0, 0, 5, 0);
        grid.setConstraints(theLabel, c);
        add(theLabel);
        if (custom) {
            if (stringField.size() == 0)
                c.insets = getInsets(5, 0, 5, 0);
            else
                c.insets = getInsets(0, 0, 5, 0);
        }
        final TextField tf = new TextField(defaultText, columns);
        if (IJ.isLinux())
            tf.setBackground(Color.white);
        tf.setEchoChar(echoChar);
        echoChar = 0;
        tf.addActionListener(this);
        tf.addTextListener(this);
        tf.addFocusListener(this);
        tf.addKeyListener(this);
        c.gridx = 1;
        c.gridy = y;
        c.anchor = GridBagConstraints.WEST;
        grid.setConstraints(tf, c);
        tf.setEditable(true);
        add(tf);
        stringField.addElement(tf);
        if (Recorder.record || macro)
            saveLabel(tf, label);
        y++;
        return asGUI(tf);
    }

    /**
     * Display dialog centered on the primary screen?
     */
    public void centerDialog(final boolean b) {
        centerDialog = b;
    }

    /**
     * Make this a "Yes No Cancel" dialog.
     */
    public void enableYesNoCancel() {
        enableYesNoCancel(" Yes ", " No ");
    }

    /**
     * Make this a "Yes No Cancel" dialog with custom labels. Here is an
     * example:
     * <p/>
     * <pre>
     * TIPLDialog gd = new TIPLDialog(&quot;YesNoCancel Demo&quot;);
     * gd.addMessage(&quot;This is a custom YesNoCancel dialog&quot;);
     * gd.enableYesNoCancel(&quot;Do something&quot;, &quot;Do something else&quot;);
     * gd.showDialog();
     * if (gd.wasCanceled())
     * 	IJ.log(&quot;User clicked 'Cancel'&quot;);
     * else if (gd.wasOKed())
     * 	IJ.log(&quot;User clicked 'Yes'&quot;);
     * else
     * 	IJ.log(&quot;User clicked 'No'&quot;);
     * </pre>
     */
    public void enableYesNoCancel(final String yesLabel, final String noLabel) {
        this.yesLabel = yesLabel;
        this.noLabel = noLabel;
        yesNoCancel = true;
    }

    @Override
    public void focusGained(final FocusEvent e) {
        final Component c = e.getComponent();
        if (c instanceof TextField)
            ((TextField) c).selectAll();
    }

    @Override
    public void focusLost(final FocusEvent e) {
        final Component c = e.getComponent();
        if (c instanceof TextField)
            ((TextField) c).select(0, 0);
    }

    /**
     * Returns references to the "OK" ("Yes"), "Cancel", and if present, "No"
     * buttons as an array.
     */
    public Button[] getButtons() {
        final Button[] buttons = new Button[3];
        buttons[0] = okay;
        buttons[1] = cancel;
        buttons[2] = no;
        return buttons;
    }

    /**
     * Returns the Vector containing the Checkboxes.
     */
    public Vector getCheckboxes() {
        return checkbox;
    }

    /**
     * Returns the Vector containing the Choices.
     */
    public Vector getChoices() {
        return choice;
    }

    private String getChoiceVariable(String item) {
        item = item.substring(1);
        final Interpreter interp = Interpreter.getInstance();
        final String s = interp != null ? interp.getStringVariable(item) : null;
        if (s != null)
            item = s;
        return item;
    }

    /**
     * Returns an error message if getNextNumber was unable to convert a string
     * into a number, otherwise, returns null.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public Insets getInsets() {
        final Insets i = super.getInsets();
        return new Insets(i.top + 10, i.left + 10, i.bottom + 10, i.right + 10);
    }

    Insets getInsets(final int top, final int left, final int bottom,
                     final int right) {
        if (customInsets) {
            customInsets = false;
            return new Insets(topInset, leftInset, bottomInset, 0);
        } else
            return new Insets(top, left, bottom, right);
    }

    /**
     * Returns a reference to the Label or MultiLineLabel created by the last
     * addMessage() call, or null if addMessage() was not called.
     */
    public Component getMessage() {
        return theLabel;
    }

    /**
     * Returns the state of the next checkbox.
     */
    public boolean getNextBoolean() {
        if (checkbox == null)
            return false;
        final Checkbox cb = (Checkbox) (checkbox.elementAt(cbIndex));
        if (recorderOn)
            recordCheckboxOption(cb);
        boolean state = cb.getState();
        if (macro) {
            final String label = (String) labels.get(cb);
            final String key = Macro.trimKey(label);
            state = isMatch(macroOptions, key + " ");
        }
        cbIndex++;
        return state;
    }

    /**
     * Returns the selected item in the next popup menu.
     */
    public String getNextChoice() {
        if (choice == null)
            return "";
        final Choice thisChoice = (Choice) (choice.elementAt(choiceIndex));
        String item = thisChoice.getSelectedItem();
        if (macro) {
            final String label = (String) labels.get(thisChoice);
            item = Macro.getValue(macroOptions, label, item);
            if (item != null && item.startsWith("&")) // value is macro variable
                item = getChoiceVariable(item);
        }
        if (recorderOn)
            recordOption(thisChoice, item);
        choiceIndex++;
        return item;
    }

    /**
     * Returns the index of the selected item in the next popup menu.
     */
    public int getNextChoiceIndex() {
        if (choice == null)
            return -1;
        final Choice thisChoice = (Choice) (choice.elementAt(choiceIndex));
        int index = thisChoice.getSelectedIndex();
        if (macro) {
            final String label = (String) labels.get(thisChoice);
            final String oldItem = thisChoice.getSelectedItem();
            final int oldIndex = thisChoice.getSelectedIndex();
            String item = Macro.getValue(macroOptions, label, oldItem);
            if (item != null && item.startsWith("&")) // value is macro variable
                item = getChoiceVariable(item);
            thisChoice.select(item);
            index = thisChoice.getSelectedIndex();
            if (index == oldIndex && !item.equals(oldItem)) {
                // is value a macro variable?
                final Interpreter interp = Interpreter.getInstance();
                final String s = interp != null ? interp
                        .getStringVariable(item) : null;
                if (s == null)
                    IJ.error(getTitle(), "\"" + item
                            + "\" is not a valid choice for \"" + label + "\"");
                else
                    item = s;
            }
        }
        if (recorderOn) {
            final String item = thisChoice.getSelectedItem();
            if (!(item.equals("*None*") && getTitle().equals("Merge Channels")))
                recordOption(thisChoice, thisChoice.getSelectedItem());
        }
        choiceIndex++;
        return index;
    }

    /**
     * Returns the contents of the next numeric field, or NaN if the field does
     * not contain a number.
     */
    public double getNextNumber() {
        if (numberField == null)
            return -1.0;
        final TextField tf = (TextField) numberField.elementAt(nfIndex);
        String theText = tf.getText();
        String label = null;
        if (macro) {
            label = (String) labels.get(tf);
            theText = Macro.getValue(macroOptions, label, theText);
            // IJ.write("getNextNumber: "+label+"  "+theText);
        }
        final String originalText = (String) defaultText.elementAt(nfIndex);
        final double defaultValue = ((Double) (defaultValues.elementAt(nfIndex)))
                .doubleValue();
        double value;
        if (theText.equals(originalText))
            value = defaultValue;
        else {
            final Double d = getValue(theText);
            if (d != null)
                value = d.doubleValue();
            else {
                // Is the value a macro variable?
                if (theText.startsWith("&"))
                    theText = theText.substring(1);
                final Interpreter interp = Interpreter.getInstance();
                value = interp != null ? interp.getVariable2(theText)
                        : Double.NaN;
                if (Double.isNaN(value)) {
                    invalidNumber = true;
                    errorMessage = "\"" + theText + "\" is an invalid number";
                    value = Double.NaN;
                    if (macro) {
                        IJ.error("Macro Error",
                                "Numeric value expected in run() function\n \n"
                                        + "   Dialog box title: \""
                                        + getTitle() + "\"\n" + "   Key: \""
                                        + label.toLowerCase(Locale.US) + "\"\n"
                                        + "   Value or variable name: \""
                                        + theText + "\"");
                    }
                }
            }
        }
        if (recorderOn)
            recordOption(tf, trim(theText));
        nfIndex++;
        return value;
    }

    /**
     * Returns the selected item in the next radio button group.
     */
    public String getNextRadioButton() {
        if (radioButtonGroup == null)
            return null;
        final CheckboxGroup cg = (CheckboxGroup) (radioButtonGroup
                .elementAt(radioButtonIndex));
        radioButtonIndex++;
        final Checkbox checkbox = cg.getSelectedCheckbox();
        String item = "null";
        if (checkbox != null)
            item = checkbox.getLabel();
        if (macro) {
            final String label = (String) labels.get(cg);
            item = Macro.getValue(macroOptions, label, item);
        }
        if (recorderOn)
            recordOption(cg, item);
        return item;
    }

    /**
     * Returns the contents of the next text field.
     */
    public String getNextString() {
        String theText;
        if (stringField == null)
            return "";
        final TextField tf = (TextField) (stringField.elementAt(sfIndex));
        theText = tf.getText();
        if (macro) {
            final String label = (String) labels.get(tf);
            theText = Macro.getValue(macroOptions, label, theText);
            if (theText != null
                    && (theText.startsWith("&") || label.toLowerCase(Locale.US)
                    .startsWith(theText))) {
                // Is the value a macro variable?
                if (theText.startsWith("&"))
                    theText = theText.substring(1);
                final Interpreter interp = Interpreter.getInstance();
                final String s = interp != null ? interp
                        .getVariableAsString(theText) : null;
                if (s != null)
                    theText = s;
            }
        }
        if (recorderOn) {
            String s = theText;
            if (s != null && Character.isLetter(s.charAt(0))
                    && s.charAt(1) == ':' && s.charAt(2) == '\\')
                s = s.replaceAll("\\\\", "\\\\\\\\"); // replace "\" with "\\"
            // in Windows file paths
            recordOption(tf, s);
        }
        sfIndex++;
        return theText;
    }

    /**
     * Returns the contents of the next textarea.
     */
    public String getNextText() {
        String text;
        if (textAreaIndex == 0 && textArea1 != null) {
            // textArea1.selectAll();
            text = textArea1.getText();
            textAreaIndex++;
            if (macro)
                text = Macro.getValue(macroOptions, "text1", text);
            if (recorderOn) {
                String text2 = text;
                final String cmd = Recorder.getCommand();
                if (cmd != null && cmd.equals("Convolve...")) {
                    text2 = text.replaceAll("\n", "\\\\n");
                    if (!text.endsWith("\n"))
                        text2 = text2 + "\\n";
                } else
                    text2 = text.replace('\n', ' ');
                Recorder.recordOption("text1", text2);
            }
        } else if (textAreaIndex == 1 && textArea2 != null) {
            textArea2.selectAll();
            text = textArea2.getText();
            textAreaIndex++;
            if (macro)
                text = Macro.getValue(macroOptions, "text2", text);
            if (recorderOn)
                Recorder.recordOption("text2", text.replace('\n', ' '));
        } else
            text = null;
        return text;
    }

    /**
     * Returns the Vector containing the numeric TextFields.
     */
    public Vector getNumericFields() {
        return numberField;
    }

    /**
     * Returns a reference to the Preview Checkbox.
     */
    public Checkbox getPreviewCheckbox() {
        return previewCheckbox;
    }

    /**
     * Returns the Vector containing the sliders (Scrollbars).
     */
    public Vector getSliders() {
        return slider;
    }

    /**
     * Returns the Vector containing the string TextFields.
     */
    public Vector getStringFields() {
        return stringField;
    }

    /**
     * Returns a reference to textArea1.
     */
    public TextArea getTextArea1() {
        return textArea1;
    }

    /**
     * Returns a reference to textArea2.
     */
    public TextArea getTextArea2() {
        return textArea2;
    }

    protected Double getValue(final String text) {
        Double d;
        try {
            d = new Double(text);
        } catch (final NumberFormatException e) {
            d = null;
        }
        return d;
    }

    /**
     * No not display "Cancel" button.
     */
    public void hideCancelButton() {
        hideCancelButton = true;
    }

    /**
     * Returns true if one or more of the numeric fields contained an invalid
     * number. Must be called after one or more calls to getNextNumber().
     */
    public boolean invalidNumber() {
        final boolean wasInvalid = invalidNumber;
        invalidNumber = false;
        return wasInvalid;
    }

    // Returns true if s2 is in s1 and not in a bracketed literal (e.g.,
    // "[literal]")
    boolean isMatch(final String s1, String s2) {
        if (s1.startsWith(s2))
            return true;
        s2 = " " + s2;
        final int len1 = s1.length();
        final int len2 = s2.length();
        boolean match, inLiteral = false;
        char c;
        for (int i = 0; i < len1 - len2 + 1; i++) {
            c = s1.charAt(i);
            if (inLiteral && c == ']')
                inLiteral = false;
            else if (c == '[')
                inLiteral = true;
            if (c != s2.charAt(0) || inLiteral
                    || (i > 1 && s1.charAt(i - 1) == '='))
                continue;
            match = true;
            for (int j = 0; j < len2; j++) {
                if (s2.charAt(j) != s1.charAt(i + j)) {
                    match = false;
                    break;
                }
            }
            if (match)
                return true;
        }
        return false;
    }

    @Override
    public void itemStateChanged(final ItemEvent e) {
        notifyListeners(e);
    }

    @Override
    public void keyPressed(final KeyEvent e) {
        final int keyCode = e.getKeyCode();
        IJ.setKeyDown(keyCode);
        if (keyCode == KeyEvent.VK_ENTER && textArea1 == null) {
            wasOKed = true;
            if (IJ.isMacOSX() && IJ.isJava15())
                accessTextFields();
            dispose();
        } else if (keyCode == KeyEvent.VK_ESCAPE) {
            wasCanceled = true;
            dispose();
            IJ.resetEscape();
        } else if (keyCode == KeyEvent.VK_W
                && (e.getModifiers() & Toolkit.getDefaultToolkit()
                .getMenuShortcutKeyMask()) != 0) {
            wasCanceled = true;
            dispose();
        }
    }

    @Override
    public void keyReleased(final KeyEvent e) {
        final int keyCode = e.getKeyCode();
        IJ.setKeyUp(keyCode);
        final int flags = e.getModifiers();
        final boolean control = (flags & InputEvent.CTRL_MASK) != 0;
        final boolean meta = (flags & InputEvent.META_MASK) != 0;
        final boolean shift = (flags & InputEvent.SHIFT_MASK) != 0;
        if (keyCode == KeyEvent.VK_G && shift && (control || meta))
            new ScreenGrabber().run("");
    }

    @Override
    public void keyTyped(final KeyEvent e) {
    }

    private Label makeLabel(String label) {
        if (IJ.isMacintosh())
            label += " ";
        return new Label(label);
    }

    public void NonBlockingShow() {
        final Panel buttons = new Panel();

        buttons.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
        cancel = new Button(cancelLabel);
        cancel.addActionListener(this);
        cancel.addKeyListener(this);
        if (yesNoCancel) {
            okLabel = yesLabel;
            no = new Button(noLabel);
            no.addActionListener(this);
            no.addKeyListener(this);
        }
        okay = new Button(okLabel);
        okay.addActionListener(this);
        okay.addKeyListener(this);
        final boolean addHelp = helpURL != null;
        if (addHelp) {
            help = new Button(helpLabel);
            help.addActionListener(this);
            help.addKeyListener(this);
        }
        if (IJ.isMacintosh()) {
            if (addHelp)
                buttons.add(help);
            if (yesNoCancel)
                buttons.add(no);
            if (!hideCancelButton)
                buttons.add(cancel);
            buttons.add(okay);
        } else {
            buttons.add(okay);
            if (yesNoCancel)
                buttons.add(no);
            ;
            if (!hideCancelButton)
                buttons.add(cancel);
            if (addHelp)
                buttons.add(help);
        }
        c.gridx = 0;
        c.gridy = y;
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = 2;
        c.insets = new Insets(15, 0, 0, 0);
        grid.setConstraints(buttons, c);
        add(buttons);
        if (IJ.isMacintosh())
            setResizable(false);
        pack();
        setup();
        if (centerDialog)
            GUI.center(this);
        setVisible(true);
    }

    /**
     * Notify any DialogListeners of changes having occurred If a listener
     * returns false, do not call further listeners and disable the OK button
     * and preview Checkbox (if it exists). For PlugInFilters, this ensures that
     * the PlugInFilterRunner, which listens as the last one, is not called if
     * the PlugInFilter has detected invalid parameters. Thus, unnecessary
     * calling the run(ip) method of the PlugInFilter for preview is avoided in
     * that case.
     */
    private void notifyListeners(final AWTEvent e) {
        if (dialogListeners == null)
            return;
        boolean everythingOk = true;
        for (int i = 0; everythingOk && i < dialogListeners.size(); i++)
            try {
                resetCounters();
                // if
                // (!((DialogListener)dialogListeners.elementAt(i)).dialogItemChanged(this,
                // e))
                everythingOk = false;
            } // disable further listeners if false (invalid parameters)
            // returned
            catch (final Exception err) { // for exceptions, don't cover the
                // input by a window but
                IJ.beep(); // show them at in the "Log"
                IJ.log("ERROR: " + err + "\nin DialogListener of "
                        + dialogListeners.elementAt(i) + "\nat "
                        + (err.getStackTrace()[0]) + "\nfrom "
                        + (err.getStackTrace()[1])); // requires Java 1.4
            }
        final boolean workaroundOSXbug = IJ.isMacOSX() && !okay.isEnabled()
                && everythingOk;
        if (previewCheckbox != null)
            previewCheckbox.setEnabled(everythingOk);
        okay.setEnabled(everythingOk);
        if (workaroundOSXbug)
            repaint(); // OSX 10.4 bug delays update of enabled until the next
        // input
    }

    @Override
    public void paint(final Graphics g) {
        super.paint(g);
        if (firstPaint) {
            if (numberField != null && IJ.isMacOSX()) {
                // work around for bug on Intel Macs that caused 1st field to be
                // un-editable
                final TextField tf = (TextField) (numberField.elementAt(0));
                tf.setEditable(false);
                tf.setEditable(true);
            }
            if (numberField == null && stringField == null)
                okay.requestFocus();
            firstPaint = false;
        }
    }

    public double parseDouble(String s) {
        if (s == null)
            return Double.NaN;
        double value = Tools.parseDouble(s);
        if (Double.isNaN(value)) {
            if (s.startsWith("&"))
                s = s.substring(1);
            final Interpreter interp = Interpreter.getInstance();
            value = interp != null ? interp.getVariable2(s) : Double.NaN;
        }
        return value;
    }

    /**
     * Used by PlugInFilterRunner to provide visable feedback whether preview is
     * running or not by switching from "Preview" to "wait..."
     */
    public void previewRunning(final boolean isRunning) {
        if (previewCheckbox != null) {
            previewCheckbox.setLabel(isRunning ? previewRunning : previewLabel);
            if (IJ.isMacOSX())
                repaint(); // workaround OSX 10.4 refresh bug
        }
    }

    private void recordCheckboxOption(final Checkbox cb) {
        final String label = (String) labels.get(cb);
        if (label != null) {
            if (cb.getState()) // checked
                Recorder.recordOption(label);
            else if (Recorder.getCommandOptions() == null)
                Recorder.recordOption(" ");
        }
    }

    private void recordOption(final Object component, String value) {
        final String label = (String) labels.get(component);
        if (value.equals(""))
            value = "[]";
        Recorder.recordOption(label, value);
    }

    /**
     * Reset the counters before reading the dialog parameters
     */
    private void resetCounters() {
        nfIndex = 0; // prepare for readout
        sfIndex = 0;
        cbIndex = 0;
        choiceIndex = 0;
        textAreaIndex = 0;
        invalidNumber = false;
    }

    private void saveLabel(final Object component, String label) {
        if (labels == null)
            labels = new Hashtable();
        if (label.length() > 0) {
            if (label.charAt(0) == ' ')
                label = label.trim();
            labels.put(component, label);
        }
    }

    /**
     * Sets a replacement label for the "Cancel" button.
     */
    public void setCancelLabel(final String label) {
        cancelLabel = label;
    }

    /**
     * Sets the echo character for the next string field.
     */
    public void setEchoChar(final char echoChar) {
        this.echoChar = echoChar;
    }

    /**
     * Sets a replacement label for the "Help" button.
     */
    public void setHelpLabel(final String label) {
        helpLabel = label;
    }

    /**
     * Set the insets (margins), in pixels, that will be used for the next
     * component added to the dialog.
     * <p/>
     * <pre>
     * 	    Default insets:
     * 	        addMessage: 0,20,0 (empty string) or 10,20,0
     * 	        addCheckbox: 15,20,0 (first checkbox) or 0,20,0
     * 	        addCheckboxGroup: 10,0,0
     * 	        addRadioButtonGroup: 5,10,0
     * 	        addNumericField: 5,0,3 (first field) or 0,0,3
     * 	        addStringField: 5,0,5 (first field) or 0,0,5
     * 	        addChoice: 5,0,5 (first field) or 0,0,5
     * </pre>
     */
    public void setInsets(final int top, final int left, final int bottom) {
        topInset = top;
        leftInset = left;
        bottomInset = bottom;
        customInsets = true;
    }

    /**
     * Sets a replacement label for the "OK" button.
     */
    public void setOKLabel(final String label) {
        okLabel = label;
    }

    protected void setup() {
    }

    /**
     * Displays this dialog box.
     */
    public void showDialog() {
        if (macro) {
            dispose();
            recorderOn = Recorder.record && Recorder.recordInMacros;
        } else {
            // if (pfr!=null) // prepare preview (not in macro mode): tell the
            // PlugInFilterRunner to listen
            // pfr.setDialog(this);
            NonBlockingShow();
            recorderOn = Recorder.record;
            IJ.wait(50); // work around for Sun/WinNT bug
        }
        /*
         * For plugins that read their input only via dialogItemChanged, call it
		 * at least once
		 */
        if (!wasCanceled && dialogListeners != null
                && dialogListeners.size() > 0) {
            resetCounters();
            // ((DialogListener)dialogListeners.elementAt(0)).dialogItemChanged(this,null);
            recorderOn = false;
        }
        resetCounters();
    }

    void showHelp() {
        if (helpURL.startsWith("<html>"))
            new HTMLDialog(this, "", helpURL);
        else {
            final String macro = "run('URL...', 'url=" + helpURL + "');";
            new MacroRunner(macro);
        }
    }

    @Override
    public void textValueChanged(final TextEvent e) {
        notifyListeners(e);
        if (slider == null)
            return;
        final Object source = e.getSource();
        for (int i = 0; i < slider.size(); i++) {
            final int index = sliderIndexes[i];
            if (source == numberField.elementAt(index)) {
                final TextField tf = (TextField) numberField.elementAt(index);
                final double value = Tools.parseDouble(tf.getText());
                if (!Double.isNaN(value)) {
                    final Scrollbar sb = (Scrollbar) slider.elementAt(i);
                    sb.setValue((int) (value * sliderScales[i]));
                }
                // IJ.log(i+" "+tf.getText());
            }
        }
    }

    private String trim(String value) {
        if (value.endsWith(".0"))
            value = value.substring(0, value.length() - 2);
        if (value.endsWith(".00"))
            value = value.substring(0, value.length() - 3);
        return value;
    }

    /**
     * Returns true if the user clicked on "Cancel".
     */
    public boolean wasCanceled() {
        if (wasCanceled)
            Macro.abort();
        return wasCanceled;
    }

    /**
     * Returns true if the user has clicked on "OK" or a macro is running.
     */
    public boolean wasOKed() {
        return wasOKed || macro;
    }

    @Override
    public void windowActivated(final WindowEvent e) {
    }

    @Override
    public void windowClosed(final WindowEvent e) {
    }

    @Override
    public void windowClosing(final WindowEvent e) {
        wasCanceled = true;
        dispose();
    }

    @Override
    public void windowDeactivated(final WindowEvent e) {
    }

    @Override
    public void windowDeiconified(final WindowEvent e) {
    }

    @Override
    public void windowIconified(final WindowEvent e) {
    }

    @Override
    public void windowOpened(final WindowEvent e) {
    }

    /**
     * Interface to allow easy reading the values from controls
     *
     * @author mader
     */
    protected static interface GUIControl {
        public String getValueAsString();

        public void setValueCallback(ArgumentList.ArgumentCallback iv);
    }

    protected static abstract class CallbackGUIControlWithMouse implements
            GUIControl {
        protected MouseListener getMouseListener(
                final ArgumentList.ArgumentCallback iv) {
            return new MouseListener() {
                @Override
                public void mouseClicked(final MouseEvent arg0) {
                    iv.valueSet(getValueAsString());
                }

                @Override
                public void mouseEntered(final MouseEvent arg0) {
                }

                @Override
                public void mouseExited(final MouseEvent arg0) {
                }

                @Override
                public void mousePressed(final MouseEvent arg0) {
                }

                @Override
                public void mouseReleased(final MouseEvent arg0) {
                }
            };
        }

        protected abstract void pushMLToObject(MouseListener curListener);

        // protected ArgumentList.ArgumentCallback
        // curCallback=ArgumentList.emptyCallback;
        // protected MouseListener curMouseListener=emptyMouseListener;
        @Override
        public void setValueCallback(final ArgumentList.ArgumentCallback iv) {
            pushMLToObject(getMouseListener(iv));

        }
    }

}
