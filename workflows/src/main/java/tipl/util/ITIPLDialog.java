/**
 *
 */
package tipl.util;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;

/**
 * An interface for interacting with GUIs so that AWT, GWT, Pivot, and JavaFX can be used for
 * setting parameters
 *
 * @author mader
 */
public interface ITIPLDialog extends Serializable {
    /**
     * Set the wrapping (automatically flowing into new panels or whatever)
     *
     * @param canWrap
     */
    public void setWrapping(boolean canWrap);

    /**
     * Get the wrapping (automatically flowing into new panels or whatever)
     */
    public boolean getWrapping();

    /**
     * Adds a message consisting of one or more lines of text, which will be displayed using the
     * specified font and color.
     */
    public void addMessage(final String text, final String font, final String color);

    /**
     * Add a choice menu
     *
     * @param label
     * @param items
     * @param defaultItem
     * @return the handle of the control
     */
    public ITIPLDialog.GUIControl addChoice(final String label, final String helpText,
                                            final String[] items,
                                            final String defaultItem);

    /**
     * Adds a checkbox.
     *
     * @param label        the label
     * @param defaultValue the initial state
     */
    public ITIPLDialog.GUIControl appendCheckbox(final String label, final String helpText,
                                                 final boolean defaultValue);

    /**
     * Adds a numeric field. The first word of the label must be unique or command recording will
     * not work.
     *
     * @param label        the label
     * @param defaultValue value to be initially displayed
     * @param digits       number of digits to right of decimal point
     */
    public ITIPLDialog.GUIControl appendNumericField(final String label, final String helpText,
                                                     final double defaultValue, final int digits);

    /**
     * Adds a slider (scroll bar) to the dialog box. Floating point values will be used if
     * (maxValue-minValue)<=5.0 and either minValue or maxValue are non-integer.
     *
     * @param label        the label
     * @param minValue     the minimum value of the slider
     * @param maxValue     the maximum value of the slider
     * @param defaultValue the initial value of the slider
     */
    public ITIPLDialog.GUIControl appendSlider(final String label, final String helpText,
                                               double minValue,
                                               double maxValue, double defaultValue);

    /**
     * Adds an 8 column text field.
     *
     * @param label       the label
     * @param defaultText the text initially displayed
     */
    public ITIPLDialog.GUIControl appendStringField(final String label, final String helpText,
                                                    final String defaultText);

    /**
     * Adds a path control (since on many platforms it will involve open / save dialog, etc)
     *
     * @param label       the label
     * @param defaultText the default value for the path
     */
    public ITIPLDialog.GUIControl appendPathField(final String label, final String helpText,
                                                  final TypedPath defaultText);

    /**
     * A task to be run before the window is closed often used to push values forward
     *
     * @param curTask
     */
    public void addDisposalTasks(Runnable curTask);

    public void showDialog();

    /**
     * Returns true if the user clicked on "Cancel".
     */
    public boolean wasCanceled();

    /**
     * Create a new panel to write into
     *
     * @param newLayerName
     */
    public void createNewLayer(String newLayerName);

    /**
     * Resize the dialog after all components have been added (if needed)
     */
    public void pack();

    /**
     * An interface for making passing back and forth arguments somewhat safer
     *
     * @author mader
     */
    public static interface DialogInteraction {
        public String getKey(String keyName);

        public void setKey(String keyName, String newValue);
    }


    /**
     * Interface to allow easy reading and writing the values from controls
     *
     * @author mader
     */
    public static interface GUIControl {
        public String getValueAsString();

        public void setValueFromString(String newValue);

        public void setValueCallback(ArgumentList.ArgumentCallback iv);
    }


    /**
     * A simple interface to have a callback when something happens (for example preview is
     * clicked)
     *
     * @author mader
     */
    public static abstract class CallbackGUIControlWithMouse implements
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
