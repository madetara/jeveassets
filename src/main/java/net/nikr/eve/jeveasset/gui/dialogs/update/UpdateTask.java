/*
 * Copyright 2009-2021 Contributors (see credits.txt)
 *
 * This file is part of jEveAssets.
 *
 * jEveAssets is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * jEveAssets is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jEveAssets; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package net.nikr.eve.jeveasset.gui.dialogs.update;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.SwingWorker;
import javax.swing.text.*;
import net.nikr.eve.jeveasset.gui.dialogs.update.TaskDialog.ErrorListener;
import net.nikr.eve.jeveasset.gui.images.Images;
import net.nikr.eve.jeveasset.gui.shared.Formater;
import net.nikr.eve.jeveasset.i18n.DialoguesUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class UpdateTask extends SwingWorker<Void, Void> {
	private static final Logger LOG = LoggerFactory.getLogger(UpdateTask.class);

	private Icon icon;
	private final JLabel jText;
	private final JButton jShow;
	private final List<LogClass> log;
	private final String name;
	private Integer totalProgress = null;

	private boolean warning = false;
	private boolean error = false;
	private boolean errorShown = false;
	private boolean taskDone = false;
	private boolean pause = false;

	private ErrorListener errorListener;

	public UpdateTask(final String name) {
		this.name = name;
		this.addPropertyChangeListener(new ListenerClass());
		jText = new JLabel(name);
		jText.setIcon(Images.UPDATE_NOT_STARTED.getIcon());
		jText.setFont(jText.getFont().deriveFont(Font.PLAIN));
		jShow = new JButton();
		jShow.setFocusPainted(false);
		jShow.setIcon(Images.MISC_EXPANDED.getIcon());
		jShow.setVisible(false);
		log = Collections.synchronizedList(new ArrayList<>());
	}

	public void addErrorListener(ErrorListener errorListener) {
		removeErrorListener();
		jShow.addActionListener(errorListener);
		jText.addMouseListener(errorListener);
		this.errorListener = errorListener;
	}

	public void removeErrorListener() {
		if (errorListener != null) {
			jShow.removeActionListener(errorListener);
			jText.removeMouseListener(errorListener);
			errorListener = null;
		}
	}

	public void setTotalProgress(final float end, final float done, final int start, final int max) {
		int progress = Math.round(((done / end) * (max - start)) + start);
		if (progress > 100) {
			progress = 100;
		} else if (progress < 0) {
			progress = 0;
		}
		if (totalProgress == null || totalProgress != progress) {
			Integer oldValue = totalProgress;
			totalProgress = progress;
			firePropertyChange("TotalProgress", oldValue, progress);
		}
	}

	public void pause() {
		while (isPause()) {
			synchronized(this) {
				try {
					wait();
				} catch (InterruptedException ex) {
					//No problem
				}
			}
		}
	}

	public synchronized boolean isPause() {
		return pause;
	}

	public synchronized void setPause(boolean pause) {
		this.pause = pause;
		notifyAll();
	}

	public Integer getTotalProgress() {
		return totalProgress;
	}

	public String getName() {
		return name;
	}

	public JLabel getTextLabel() {
		return jText;
	}

	public int getWidth() {
		int width = jText.getPreferredSize().width;
		int plain = jText.getFontMetrics(jText.getFont()).stringWidth(name);
		int bold = jText.getFontMetrics(jText.getFont().deriveFont(Font.BOLD)).stringWidth(name);
		return width + (bold - plain);
	}

	public JButton getShowButton() {
		return jShow;
	}

	public void addError(final String owner, final String msg) {
		log.add(new LogClass(owner, msg));
		error = true;
	}

	public void addWarning(final String owner, final String msg) {
		log.add(new LogClass(owner, msg));
		warning = true;
	}

	public boolean hasLog() {
		return !log.isEmpty();
	}

	public Icon getIcon() {
		if (icon == null) {
			icon = Images.UPDATE_WORKING.getIcon();
		}
		return icon;
	}

	public final void setIcon(Icon icon) {
		this.icon = icon;
	}

	public abstract void update();

	@Override
	public Void doInBackground() {
		setProgress(0);
		update();
		return null;
	}

	@Override
	public void done() {
		try {
			get();
		} catch (CancellationException ex) {
			LOG.info("Update cancelled by user");
		} catch (Exception ex) { //InterruptedException, ExecutionException
			LOG.error(ex.getMessage(), ex);
			throw new RuntimeException(ex);
		}
		taskDone = true;
		setProgress(100);
	}

	public boolean isTaskDone() {
		return taskDone;
	}

	public void setTaskDone(final boolean done) {
		this.taskDone = done;
	}

	protected void setTaskProgress(final int progress) {
		this.setProgress(progress);
	}

	public void insertLog(final JTextPane jError) {
		if (!log.isEmpty()) {
			StyledDocument doc = new DefaultStyledDocument();
			UpdateTask.this.insertLog(doc);
			jError.setDocument(doc);
		}
	}

	public void insertLog(final StyledDocument doc) {
		if (!log.isEmpty()) {
			SimpleAttributeSet errorAttributeSet = new SimpleAttributeSet();
			errorAttributeSet.addAttribute(StyleConstants.CharacterConstants.Foreground, jText.getBackground().darker().darker());

			try {
				boolean first = true;
				synchronized (log) {
					for (LogClass errorClass : log) {
						if (first) {
							first = false;
						} else {
							doc.insertString(doc.getLength(), "\n\r", null);
						}
						doc.insertString(doc.getLength(), errorClass.getOwner(), null);
						doc.insertString(doc.getLength(), "\r\n" + processError(errorClass.getError()), errorAttributeSet);
					}
				}
			} catch (BadLocationException ex) {
				LOG.warn("Ignoring exception: " + ex.getMessage(), ex);
			}
		}
	}

	public void setTaskProgress(final float progressEnd, final float progressNow, final int minimum, final int maximum) {
		int progress = Math.round(((progressNow / progressEnd) * (maximum - minimum)) + minimum);
		if (progress > 100) {
			progress = 100;
		} else if (progress < 0) {
			progress = 0;
		}
		if (progress != getProgress()) {
			setProgress(progress);
		}
	}

	public void resetTaskProgress() {
		setProgress(0);
	}

	public void cancelled() {
		jText.setIcon(Images.UPDATE_CANCELLED.getIcon());
	}

	public void showLog(final boolean b) {
		if (!log.isEmpty()) {
			if (b) {
				errorShown = true;
				jText.setFont(jText.getFont().deriveFont(Font.BOLD));
				jShow.setIcon(Images.MISC_COLLAPSED.getIcon());
				jShow.setSelected(true);
				jShow.requestFocusInWindow();
			} else {
				jText.setFont(jText.getFont().deriveFont(Font.PLAIN));
				jShow.setIcon(Images.MISC_EXPANDED.getIcon());
				jShow.setSelected(false);
				errorShown = false;
			}
		}
	}

	public boolean isErrorShown() {
		return errorShown;
	}

	private String processError(String error) {
		if (error == null) {
			return "";
		}
		Pattern p = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Matcher m = p.matcher(error);
		while (m.find()) {
			int start = m.start();
			int end = m.end();
			String time = error.substring(start, end);
			try {
				Date date = df.parse(time);
				time = Formater.weekdayAndTime(date);
			} catch (ParseException ex) {
				time = error.substring(start, end);
			}
			error = error.substring(0, start) + time + error.substring(end);
			error = error.replace("retry after", "\r\n" + DialoguesUpdate.get().nextUpdate());
		}
		return error;
	}

	private class ListenerClass implements PropertyChangeListener {

		@Override
		public void propertyChange(final PropertyChangeEvent evt) {
			int value = getProgress();
			if (value == 100) {
				if (error) {
					jText.setIcon(Images.UPDATE_DONE_ERROR.getIcon());
					jShow.setVisible(true);
				} else if (isCancelled() || warning) {
					jText.setIcon(Images.UPDATE_DONE_SOME.getIcon());
					jShow.setVisible(true);
				} else {
					jShow.setVisible(false);
					jText.setIcon(Images.UPDATE_DONE_OK.getIcon());
				}
				if (!log.isEmpty()) {
					jText.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				}
			} else {
				jText.setIcon(Images.UPDATE_WORKING.getIcon());
			}
		}
	}

	private static class LogClass {
		private final String owner;
		private final String error;

		public LogClass(String owner, String error) {
			this.owner = owner;
			this.error = error;
		}

		public String getOwner() {
			return owner;
		}

		public String getError() {
			return error;
		}
	}

	public static class EmptyIcon implements Icon {

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			//Nothing to paint
		}

		@Override
		public int getIconWidth() {
			return 16;
		}

		@Override
		public int getIconHeight() {
			return 16;
		}
		
	}
}
