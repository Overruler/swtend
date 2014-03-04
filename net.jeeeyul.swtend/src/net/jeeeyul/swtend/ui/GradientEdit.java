package net.jeeeyul.swtend.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.jeeeyul.swtend.SWTExtensions;
import net.jeeeyul.swtend.sam.Procedure1;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

public class GradientEdit extends Canvas {
	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		new GradientEdit(shell);
		shell.pack();
		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	private static Gradient clipboard;

	private Point preferredSize = new Point(100, 25);
	private Gradient selection;

	private boolean itemsInvaldate = false;
	private Point dragBegin = null;
	private boolean lockOrder = false;
	private int state = 0;

	private GradientEditItem selectedItem = null;
	private GradientEditItem hotItem = null;

	private Rectangle dragBeginBounds = null;
	List<GradientEditItem> items = new ArrayList<GradientEditItem>();
	private MenuItem addMenuItem;
	private MenuItem removeMenuItem;
	private Point menuLocation;
	private MenuItem editMenuItem;

	private MenuItem copyMenu;

	private MenuItem pasteMenu;

	public GradientEdit(Composite parent) {
		super(parent, SWT.DOUBLE_BUFFERED);

		createMenu();
		hook();

		Gradient gradient = new Gradient(new HSB(255, 255, 255), new HSB(255, 0, 0));
		gradient.add(1, new ColorStop(new HSB(255, 255, 0), 50));
		setSelection(gradient, false);

		updateMenuEnabilities();
	}

	@Override
	public boolean setFocus() {
		return forceFocus();
	}

	@Override
	public boolean isFocusControl() {
		return super.isFocusControl();
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		return new Point(Math.max(preferredSize.x, wHint), Math.max(preferredSize.y, hHint));
	}

	private void createMenu() {
		Menu menu = new Menu(this);

		menu.addListener(SWT.Show, new Listener() {
			@Override
			public void handleEvent(Event event) {
				menuLocation = toControl(getDisplay().getCursorLocation());
			}
		});

		this.setMenu(menu);
		addMenuItem = new MenuItem(menu, SWT.PUSH);
		addMenuItem.setText("Add");
		addMenuItem.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				insertNewColorStop(menuLocation.x);
			}
		});

		removeMenuItem = new MenuItem(menu, SWT.PUSH);
		removeMenuItem.setText("Remove");
		removeMenuItem.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				removeSelectedItem();
			}

		});

		new MenuItem(menu, SWT.SEPARATOR);

		editMenuItem = new MenuItem(menu, SWT.PUSH);
		editMenuItem.setText("Edit...");
		editMenuItem.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				editItem(selectedItem);
			}
		});

		new MenuItem(menu, SWT.SEPARATOR);
		copyMenu = new MenuItem(menu, SWT.PUSH);
		copyMenu.setText("Copy Gradient");
		copyMenu.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				clipboard = selection.getCopy();
			}
		});

		pasteMenu = new MenuItem(menu, SWT.PUSH);
		pasteMenu.setText("Paste Gradient");
		pasteMenu.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				setSelection(clipboard.getCopy(), true);
			}
		});

		menu.addListener(SWT.Show, new Listener() {

			@Override
			public void handleEvent(Event event) {
				updateMenuEnabilities();

			}
		});
	}

	private void removeSelectedItem() {
		if (selectedItem != null && items.size() > 2) {
			selection.remove(selectedItem.getData());
			rebuildItems();
			notifyListeners(SWT.Modify, new Event());
		}
	}

	private void draw(GC gc) {
		if (itemsInvaldate) {
			rebuildItems();
		}

		drawBar(gc);

		for (GradientEditItem item : items) {
			item.draw(gc);
		}
	}

	private void drawBar(GC gc) {
		Rectangle barArea = getBarArea();
		int offset = 0;
		int gradientWidth = 0;

		for (int i = 0; i <= selection.size(); i++) {
			ColorStop from = (i > 0) ? selection.get(i - 1) : selection.get(i);
			ColorStop to = (i < selection.size()) ? selection.get(i) : selection.get(i - 1);
			Color fromColor = new Color(getDisplay(), from.color.toRGB());
			Color toColor = new Color(getDisplay(), to.color.toRGB());
			gradientWidth = (i < selection.size()) ? (int) (barArea.width * (to.percent / 100d) - offset) : barArea.width - offset;
			gc.setForeground(fromColor);
			gc.setBackground(toColor);
			gc.fillGradientRectangle(barArea.x + offset, barArea.y, gradientWidth, barArea.height, false);

			fromColor.dispose();
			toColor.dispose();
			offset += gradientWidth;
		}

		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		gc.drawLine(barArea.x, barArea.y, barArea.x + barArea.width, barArea.y);
		gc.drawLine(barArea.x, barArea.y, barArea.x, barArea.y + barArea.height);

		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
		gc.drawLine(barArea.x, barArea.y + barArea.height, barArea.x + barArea.width, barArea.y + barArea.height);
		gc.drawLine(barArea.x + barArea.width, barArea.y, barArea.x + barArea.width, barArea.y + barArea.height);
	}

	private Rectangle getBarArea() {
		return SWTExtensions.INSTANCE.shrink(getClientArea(), 0, 15, 0, 0);
	}

	@Override
	public Rectangle getClientArea() {
		Point size = getSize();
		return SWTExtensions.INSTANCE.shrink(new Rectangle(0, 0, size.x, size.y), 5, 0, 6, 1);
	}

	private GradientEditItem getItemAt(Event event) {
		return getItemAt(event.x, event.y);
	}

	private GradientEditItem getItemAt(int x, int y) {
		GradientEditItem target = null;
		List<GradientEditItem> reverseOrder = new ArrayList<GradientEditItem>(items);
		Collections.reverse(reverseOrder);
		for (GradientEditItem each : reverseOrder) {
			if (SWTExtensions.INSTANCE.contains(each.bounds, x, y)) {
				target = each;
				break;
			}
		}
		return target;
	}

	private GradientEditItem getItemFor(ColorStop stop) {
		for (GradientEditItem each : items) {
			if (each.getData() == stop) {
				return each;
			}
		}
		return null;
	}

	public Gradient getSelection() {
		return selection;
	}

	private void handleDoubleClick(Event event) {
		GradientEditItem item = getItemAt(event);
		if (item != null) {
			editItem(item);
		}

		else {
			insertNewColorStop(event.x);
		}
	}

	protected void editItem(GradientEditItem item) {
		final ColorStop colorStop = (ColorStop) item.getData();
		HSB original = colorStop.color;

		final ColorPicker picker = new ColorPicker(getShell());
		picker.setSelection(colorStop.color);

		picker.setContinuosSelectionHandler(new Procedure1<HSB>() {
			@Override
			public void apply(HSB t) {
				colorStop.color = picker.getSelection();
				redraw();
				notifyListeners(SWT.Modify, new Event());
			}
		});

		if (picker.open() == IDialogConstants.OK_ID) {
			colorStop.color = picker.getSelection();
		} else {
			colorStop.color = original;
		}
		redraw();
		notifyListeners(SWT.Modify, new Event());
	}

	private void handleMouseDown(Event event) {
		GradientEditItem target = getItemAt(event);
		setSelectedItem(target);

		if (state == 0 && event.button == 1) {
			if (target != null) {
				dragBegin = new Point(event.x, event.y);
				dragBeginBounds = new Rectangle(target.bounds.x, target.bounds.y, target.bounds.width, target.bounds.height);
				state = 1;
			}
		}
	}

	private void updateMenuEnabilities() {
		addMenuItem.setEnabled(selectedItem == null);
		removeMenuItem.setEnabled(selectedItem != null && items.size() > 2);
		editMenuItem.setEnabled(selectedItem != null);
		pasteMenu.setEnabled(clipboard != null);
	}

	private void handleMouseMove(Event event) {
		if (state == 1) {
			Point delta = new Point(dragBegin.x - event.x, dragBegin.y - event.y);
			Rectangle newBounds = SWTExtensions.INSTANCE.getTranslated(dragBeginBounds, -delta.x, 0);
			newBounds.x = Math.max(getBarArea().x - GradientEditItem.SIZE.x / 2, newBounds.x);
			newBounds.x = Math.min(getBarArea().x - GradientEditItem.SIZE.x / 2 + getBarArea().width, newBounds.x);
			selectedItem.bounds = newBounds;
			ColorStop colorStop = (ColorStop) selectedItem.getData();

			colorStop.percent = (int) (((newBounds.x - getBarArea().x + GradientEditItem.SIZE.x / 2) / (double) getBarArea().width) * 100 + .5);
			if (lockOrder) {
				int index = selection.indexOf(colorStop);
				if (index > 0) {
					GradientEditItem prevItem = getItemFor(selection.get(index - 1));
					ColorStop prevStop = (ColorStop) prevItem.getData();
					newBounds.x = Math.max(prevItem.bounds.x, newBounds.x);
					colorStop.percent = Math.max(prevStop.percent, colorStop.percent);
				}
				if (index < items.size() - 1) {
					GradientEditItem nextItem = getItemFor(selection.get(index + 1));
					ColorStop nextStop = (ColorStop) nextItem.getData();
					newBounds.x = Math.min(nextItem.bounds.x, newBounds.x);
					colorStop.percent = Math.min(nextStop.percent, colorStop.percent);
				}
			}

			if (!lockOrder) {
				sort();
			}

			notifyListeners(SWT.Modify, new Event());

			redraw();
		}

		else if (state == 0) {
			GradientEditItem target = getItemAt(event);
			setHotItem(target);
		}
	}

	private void handleMouseUp(Event event) {
		if (state == 1) {
			dragBegin = null;
			dragBeginBounds = null;
			state = 0;
			layoutItems();
			notifyListeners(SWT.Modify, new Event());
		}
	}

	private void hook() {
		addListener(SWT.FocusOut, new Listener() {
			@Override
			public void handleEvent(Event event) {
				redraw();
			}
		});
		addListener(SWT.FocusIn, new Listener() {
			@Override
			public void handleEvent(Event event) {
				redraw();
			}
		});
		addListener(SWT.KeyDown, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.character == SWT.DEL || event.character == SWT.BS) {
					removeSelectedItem();
				}
			}
		});
		addListener(SWT.Paint, new Listener() {
			@Override
			public void handleEvent(Event event) {
				draw(event.gc);
			}
		});

		addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event event) {
				layoutItems();
			}
		});

		addListener(SWT.MouseDown, new Listener() {
			@Override
			public void handleEvent(Event event) {
				handleMouseDown(event);
			}
		});

		addListener(SWT.MouseMove, new Listener() {
			@Override
			public void handleEvent(Event event) {
				handleMouseMove(event);

			}
		});

		addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				handleMouseUp(event);

			}
		});

		addListener(SWT.MouseDoubleClick, new Listener() {
			@Override
			public void handleEvent(Event event) {
				handleDoubleClick(event);
			}
		});
	}

	private void insertNewColorStop(int x) {
		int percent = toPercent(x);
		ColorStop from = selection.get(0);
		ColorStop to = null;

		for (int i = 0; i < selection.size(); i++) {
			ColorStop each = selection.get(i);
			if (each.percent > percent) {
				break;
			} else {
				from = each;
			}
		}

		for (int i = selection.size() - 1; i >= 0; i--) {
			ColorStop each = selection.get(i);
			if (each.percent < percent) {
				break;
			} else {
				to = each;
			}
		}

		int insertIndex = 0;
		if (to != null) {
			insertIndex = selection.indexOf(to);
		} else {
			to = selection.get(selection.size() - 1);
			insertIndex = selection.size();
		}

		HSB newHSB = from.color.getCopy();
		ColorStop model = new ColorStop(newHSB.mixWith(to.color, 0.5f), percent);
		selection.add(insertIndex, model);

		GradientEditItem newItem = new GradientEditItem(this);
		newItem.setData(model);
		setSelectedItem(newItem);
		layoutItems();

		notifyListeners(SWT.Modify, new Event());
	}

	public boolean isLockOrder() {
		return lockOrder;
	}

	void layoutItems() {
		for (GradientEditItem each : items) {
			ColorStop colorStop = (ColorStop) each.getData();
			Rectangle barArea = getBarArea();
			int x = barArea.x + (int) (barArea.width * (colorStop.percent / 100d)) - GradientEditItem.SIZE.x / 2;
			each.bounds = new Rectangle(x, barArea.y - 15, GradientEditItem.SIZE.x, GradientEditItem.SIZE.y);
		}
		redraw();
	}

	private void rebuildItems() {
		GradientEditItem[] itemArray = items.toArray(new GradientEditItem[items.size()]);
		for (GradientEditItem eachItem : itemArray) {
			eachItem.dispose();
		}

		if (selection != null) {
			for (ColorStop each : selection) {
				GradientEditItem newItem = new GradientEditItem(this);
				newItem.setData(each);
			}
		}

		layoutItems();
	}

	private void setHotItem(GradientEditItem target) {
		if (target == hotItem) {
			return;
		}

		if (hotItem != null) {
			hotItem.state ^= SWT.HOT;
		}
		hotItem = target;
		if (hotItem != null) {
			hotItem.state |= SWT.HOT;
		}

		redraw();
	}

	public void setLockOrder(boolean lockOrder) {
		this.lockOrder = lockOrder;
	}

	private void setSelectedItem(GradientEditItem item) {
		if (selectedItem == item) {
			return;
		}

		if (selectedItem != null) {
			selectedItem.state ^= SWT.SELECTED;
		}
		selectedItem = item;

		if (selectedItem != null) {
			selectedItem.state |= SWT.SELECTED;
			items.remove(selectedItem);
			items.add(selectedItem);
		}

		redraw();
	}

	public void setSelection(Gradient selection, boolean notify) {
		if (this.selection == selection) {
			return;
		}
		this.selection = selection;

		rebuildItems();

		if (notify) {
			notifyListeners(SWT.Modify, new Event());
		}
	}

	public void setSelection(Gradient selection) {
		setSelection(selection, false);
	}

	private void sort() {
		Collections.sort(selection, new Comparator<ColorStop>() {
			@Override
			public int compare(ColorStop a, ColorStop b) {
				return a.percent - b.percent;
			}
		});
		redraw();
	}

	private int toPercent(int x) {
		Rectangle barArea = getBarArea();
		int result = (int) ((x - barArea.x) / (double) barArea.width * 100 + 0.5);
		result = Math.max(result, 0);
		result = Math.min(result, 100);
		return result;
	}

}
