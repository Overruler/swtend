package net.jeeeyul.swtend.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public class ColorWell extends Canvas {
	private HSB selection = new HSB();

	public ColorWell(Composite parent, int style) {
		super(parent, style | SWT.BORDER);
		addListener(SWT.Paint, new Listener() {
			@Override
			public void handleEvent(Event event) {
				onPaint(event);
			}
		});
	}

	public HSB getSelection() {
		return selection;
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		return new Point(30, 20);
	}

	protected void onPaint(Event event) {
		if (selection == null) {
			return;
		}

		Color color = new Color(getDisplay(), selection.toRGB());
		event.gc.setBackground(color);
		event.gc.fillRectangle(getClientArea());
		color.dispose();

	}

	public void setSelection(HSB selection, boolean notify) {
		this.selection = selection;
		redraw();
		if (notify) {
			Event event = new Event();
			event.widget = this;
			notifyListeners(SWT.Selection, event);
		}
	}

	public void setSelection(HSB selection) {
		setSelection(selection, true);
	}

}
