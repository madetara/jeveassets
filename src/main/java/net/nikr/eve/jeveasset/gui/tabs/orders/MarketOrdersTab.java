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
package net.nikr.eve.jeveasset.gui.tabs.orders;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.text.StyledDocument;
import net.nikr.eve.jeveasset.Program;
import net.nikr.eve.jeveasset.data.api.accounts.EsiOwner;
import net.nikr.eve.jeveasset.data.api.accounts.OwnerType;
import net.nikr.eve.jeveasset.data.api.my.MyMarketOrder;
import net.nikr.eve.jeveasset.data.api.raw.RawMarketOrder.MarketOrderRange;
import net.nikr.eve.jeveasset.data.settings.ColorEntry;
import net.nikr.eve.jeveasset.data.settings.ColorSettings;
import net.nikr.eve.jeveasset.data.settings.Colors;
import net.nikr.eve.jeveasset.data.settings.Settings;
import net.nikr.eve.jeveasset.data.settings.types.LocationType;
import net.nikr.eve.jeveasset.gui.dialogs.update.TaskDialog;
import net.nikr.eve.jeveasset.gui.dialogs.update.UpdateTask;
import net.nikr.eve.jeveasset.gui.frame.StatusPanel;
import net.nikr.eve.jeveasset.gui.images.Images;
import net.nikr.eve.jeveasset.gui.shared.CopyHandler;
import net.nikr.eve.jeveasset.gui.shared.Formater;
import net.nikr.eve.jeveasset.gui.shared.InstantToolTip;
import net.nikr.eve.jeveasset.gui.shared.Updatable;
import net.nikr.eve.jeveasset.gui.shared.components.JFixedToolBar;
import net.nikr.eve.jeveasset.gui.shared.components.JMainTabPrimary;
import net.nikr.eve.jeveasset.gui.shared.filter.Filter;
import net.nikr.eve.jeveasset.gui.shared.filter.Filter.CompareType;
import net.nikr.eve.jeveasset.gui.shared.filter.Filter.LogicType;
import net.nikr.eve.jeveasset.gui.shared.filter.FilterControl;
import net.nikr.eve.jeveasset.gui.shared.menu.JMenuInfo;
import net.nikr.eve.jeveasset.gui.shared.menu.JMenuUI;
import net.nikr.eve.jeveasset.gui.shared.menu.MenuData;
import net.nikr.eve.jeveasset.gui.shared.menu.MenuManager.TableMenu;
import net.nikr.eve.jeveasset.gui.shared.table.EnumTableColumn;
import net.nikr.eve.jeveasset.gui.shared.table.EnumTableFormatAdaptor;
import net.nikr.eve.jeveasset.gui.shared.table.EventModels;
import net.nikr.eve.jeveasset.gui.shared.table.JAutoColumnTable;
import net.nikr.eve.jeveasset.gui.shared.table.PaddingTableCellRenderer;
import net.nikr.eve.jeveasset.gui.tabs.orders.OutbidProcesser.OutbidProcesserInput;
import net.nikr.eve.jeveasset.gui.tabs.orders.OutbidProcesser.OutbidProcesserOutput;
import net.nikr.eve.jeveasset.i18n.DialoguesUpdate;
import net.nikr.eve.jeveasset.i18n.TabsOrders;
import net.nikr.eve.jeveasset.io.esi.EsiPublicMarketOrdersGetter;
import net.nikr.eve.jeveasset.io.local.MarketLogReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MarketOrdersTab extends JMainTabPrimary {

	private static final Logger LOG = LoggerFactory.getLogger(MarketOrdersTab.class);

	private enum MarketOrdersAction {
		UPDATE,
		AUTO_UPDATE,
		ORDER_TYPE,
		ERROR_LOG,
		ORDER_RANGE
	}

	private static final int SIGNIFICANT_FIGURES = 4;

	private final JAutoColumnTable jTable;
	private final JLabel jSellOrdersTotal;
	private final JLabel jBuyOrdersTotal;
	private final JLabel jEscrowTotal;
	private final JLabel jToCoverTotal;
	private final JButton jUpdate;
	private final JButton jErrors;
	private final JCheckBox jAutoUpdate;
	private final JLabel jSellOrderRangeLast;
	private final JLabel jLastEsiUpdate;
	private final JLabel jLastLogUpdate;
	private final JLabel jClipboard;
	private final JComboBox<MarketOrderRange> jOrderRangeNext;
	private final JComboBox<String> jOrderType;
	private final MarketOrdersErrorDialog jMarketOrdersErrorDialog;
	private final Timer timer;
	private final FileListener fileListener;
	private java.util.Timer updateTimer;
	private static Date lastLogUpdate = null;
	private static String clipboardData = null;

	//Table
	private final MarketOrdersFilterControl filterControl;
	private final EnumTableFormatAdaptor<MarketTableFormat, MyMarketOrder> tableFormat;
	private final DefaultEventTableModel<MyMarketOrder> tableModel;
	private final EventList<MyMarketOrder> eventList;
	private final FilterList<MyMarketOrder> filterList;
	private final DefaultEventSelectionModel<MyMarketOrder> selectionModel;

	public static final String NAME = "marketorders"; //Not to be changed!

	public MarketOrdersTab(final Program program) {
		super(program, NAME, TabsOrders.get().market(), Images.TOOL_MARKET_ORDERS.getIcon(), true);

		ListenerClass listener = new ListenerClass();

		jMarketOrdersErrorDialog = new MarketOrdersErrorDialog(program);

		JFixedToolBar jToolBar = new JFixedToolBar();

		jOrderRangeNext = new JComboBox<>(MarketOrderRange.valuesSorted());
		jOrderRangeNext.setSelectedItem(Settings.get().getOutbidOrderRange());
		jOrderRangeNext.setActionCommand(MarketOrdersAction.ORDER_RANGE.name());
		jOrderRangeNext.addActionListener(listener);
		jToolBar.add(jOrderRangeNext, 95);

		jToolBar.addSpace(1);

		JLabel jOrderRangeNextLabel = new JLabel(Images.MISC_HELP.getIcon());
		jOrderRangeNextLabel.setToolTipText(TabsOrders.get().sellOrderRangeToolTip());
		InstantToolTip.install(jOrderRangeNextLabel);
		jToolBar.add(jOrderRangeNextLabel);

		jToolBar.addSpace(7);

		String[] orderTypes = {TabsOrders.get().updateOutbidFileBuy(), TabsOrders.get().updateOutbidFileSell()};
		jOrderType = new JComboBox<>(orderTypes);
		jOrderType.setSelectedItem(TabsOrders.get().updateOutbidFileBuy());
		jOrderType.setActionCommand(MarketOrdersAction.ORDER_TYPE.name());
		jOrderType.addActionListener(listener);
		jToolBar.add(jOrderType, 95);

		jToolBar.addSpace(1);

		JLabel jOrderTypeLabelLabel = new JLabel(Images.MISC_HELP.getIcon());
		jOrderTypeLabelLabel.setToolTipText(TabsOrders.get().marketLogTypeToolTip());
		InstantToolTip.install(jOrderTypeLabelLabel);
		jToolBar.add(jOrderTypeLabelLabel);

		jToolBar.addSpace(7);

		jErrors = new JButton(TabsOrders.get().logOK(), Images.UPDATE_DONE_ERROR.getIcon());
		jErrors.setActionCommand(MarketOrdersAction.ERROR_LOG.name());
		jErrors.addActionListener(listener);
		jErrors.setDisabledIcon(Images.EDIT_SET.getIcon());
		jErrors.setEnabled(false);
		jToolBar.addButton(jErrors);

		jUpdate = new JButton(TabsOrders.get().updateOutbidEsi(), Images.DIALOG_UPDATE.getIcon());
		jUpdate.setActionCommand(MarketOrdersAction.UPDATE.name());
		jUpdate.addActionListener(listener);
		jToolBar.addButton(jUpdate);

		jAutoUpdate = new JCheckBox(TabsOrders.get().updateOutbidEsiAuto());
		jAutoUpdate.setActionCommand(MarketOrdersAction.AUTO_UPDATE.name());
		jAutoUpdate.addActionListener(listener);
		jToolBar.addButton(jAutoUpdate);

		//Table Format
		tableFormat = new EnumTableFormatAdaptor<>(MarketTableFormat.class);
		//Backend
		eventList = program.getProfileData().getMarketOrdersEventList();
		//Sorting (per column)
		eventList.getReadWriteLock().readLock().lock();
		SortedList<MyMarketOrder> sortedList = new SortedList<>(eventList);
		eventList.getReadWriteLock().readLock().unlock();
		//Filter
		eventList.getReadWriteLock().readLock().lock();
		filterList = new FilterList<>(sortedList);
		eventList.getReadWriteLock().readLock().unlock();
		filterList.addListEventListener(listener);
		//Table Model
		tableModel = EventModels.createTableModel(filterList, tableFormat);
		//Table
		jTable = new JMarketOrdersTable(program, tableModel);
		jTable.setCellSelectionEnabled(true);
		PaddingTableCellRenderer.install(jTable, 1);
		//Sorting
		TableComparatorChooser.install(jTable, sortedList, TableComparatorChooser.MULTIPLE_COLUMN_MOUSE, tableFormat);
		//Selection Model
		selectionModel = EventModels.createSelectionModel(filterList);
		selectionModel.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
		jTable.setSelectionModel(selectionModel);
		jTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int row = jTable.rowAtPoint(e.getPoint());
				int column = jTable.columnAtPoint(e.getPoint());

				if (row < 0 || row > jTable.getRowCount() || column < 0 || column > jTable.getColumnCount()) {
					return;
				}
				Object value = jTable.getValueAt(row, column);
				String columnName = (String) jTable.getTableHeader().getColumnModel().getColumn(column).getHeaderValue();
				if (!columnName.equals(MarketTableFormat.EVE_UI.getColumnName())) {
					return;
				}
				TableCellEditor cellEditor = jTable.getCellEditor();
				if ((value instanceof JButton) && cellEditor != null) {
					JButton jButton = (JButton) value;
					MyMarketOrder marketOrder = tableModel.getElementAt(row);

					ActionListener actionListener = new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							openEve(marketOrder);
						}
					};

					jButton.addActionListener(actionListener);

					cellEditor.addCellEditorListener(new CellEditorListener() {
						@Override
						public void editingStopped(ChangeEvent e) {
							cellEditor.removeCellEditorListener(this);
							jButton.removeActionListener(actionListener);
							jTable.requestFocusInWindow();
						}

						@Override
						public void editingCanceled(ChangeEvent e) {
							cellEditor.removeCellEditorListener(this);
							jButton.removeActionListener(actionListener);
							jTable.requestFocusInWindow();
						}
					});
					jButton.doClick();
				}
			}
		});

		//Listeners
		installTable(jTable);
		//Scroll Panels
		JScrollPane jTableScroll = new JScrollPane(jTable);
		//Table Filter
		Map<String, List<Filter>> defaultFilters = new HashMap<>();
		List<Filter> filter;
		filter = new ArrayList<>();
		filter.add(new Filter(LogicType.AND, MarketTableFormat.ORDER_TYPE, CompareType.EQUALS, TabsOrders.get().buy()));
		filter.add(new Filter(LogicType.AND, MarketTableFormat.STATUS, CompareType.EQUALS, TabsOrders.get().statusActive()));
		defaultFilters.put(TabsOrders.get().activeBuyOrders(), filter);
		filter = new ArrayList<>();
		filter.add(new Filter(LogicType.AND, MarketTableFormat.ORDER_TYPE, CompareType.EQUALS, TabsOrders.get().sell()));
		filter.add(new Filter(LogicType.AND, MarketTableFormat.STATUS, CompareType.EQUALS, TabsOrders.get().statusActive()));
		defaultFilters.put(TabsOrders.get().activeSellOrders(), filter);
		filterControl = new MarketOrdersFilterControl(sortedList, defaultFilters);
		//Menu
		installTableTool(new OrdersTableMenu(), tableFormat, tableModel, jTable, filterControl, MyMarketOrder.class);

		jSellOrdersTotal = StatusPanel.createLabel(TabsOrders.get().totalSellOrders(), Images.ORDERS_SELL.getIcon());
		this.addStatusbarLabel(jSellOrdersTotal);

		jBuyOrdersTotal = StatusPanel.createLabel(TabsOrders.get().totalBuyOrders(), Images.ORDERS_BUY.getIcon());
		this.addStatusbarLabel(jBuyOrdersTotal);

		jEscrowTotal = StatusPanel.createLabel(TabsOrders.get().totalEscrow(), Images.ORDERS_ESCROW.getIcon());
		this.addStatusbarLabel(jEscrowTotal);

		jToCoverTotal = StatusPanel.createLabel(TabsOrders.get().totalToCover(), Images.ORDERS_TO_COVER.getIcon());
		this.addStatusbarLabel(jToCoverTotal);

		jSellOrderRangeLast = StatusPanel.createLabel(TabsOrders.get().sellOrderRangeLastToolTip(), Images.ORDERS_SELL.getIcon());
		this.addStatusbarLabel(jSellOrderRangeLast);
		jSellOrderRangeLast.setText(TabsOrders.get().sellOrderRangeSelcted(Settings.get().getOutbidOrderRange().toString()));

		jClipboard = StatusPanel.createLabel(TabsOrders.get().lastClipboardToolTip(), Images.EDIT_COPY.getIcon());
		this.addStatusbarLabel(jClipboard);
		setClipboardData(TabsOrders.get().none());

		jLastLogUpdate = StatusPanel.createLabel(TabsOrders.get().lastLogUpdateToolTip(), null);
		this.addStatusbarLabel(jLastLogUpdate);

		jLastEsiUpdate = StatusPanel.createLabel(TabsOrders.get().lastEsiUpdateToolTip(), null);
		this.addStatusbarLabel(jLastEsiUpdate);

		updateDates();

		timer = new Timer(1000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateDates();
			}
		});
		timer.start();

		fileListener = new FileListener(program, Settings.get().getOutbidOrderRange());
		fileListener.start();

		layout.setHorizontalGroup(
				layout.createParallelGroup()
						.addComponent(filterControl.getPanel())
						.addComponent(jToolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Integer.MAX_VALUE)
						.addComponent(jTableScroll, 0, 0, Short.MAX_VALUE)
		);
		layout.setVerticalGroup(
				layout.createSequentialGroup()
						.addComponent(filterControl.getPanel())
						.addComponent(jToolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(jTableScroll, 0, 0, Short.MAX_VALUE)
		);
	}

	@Override
	public void clearData() {
		filterControl.clearCache();
	}

	@Override
	public void updateCache() {
		filterControl.createCache();
	}

	@Override
	public Collection<LocationType> getLocations() {
		try {
			eventList.getReadWriteLock().readLock().lock();
			return new ArrayList<>(eventList);
		} finally {
			eventList.getReadWriteLock().readLock().unlock();
		}
	}

	private void schedule() {
		if (jAutoUpdate.isSelected()) {
			long delay = Math.max(0, Settings.get().getPublicMarketOrdersNextUpdate().getTime() - System.currentTimeMillis());
			if (updateTimer != null) { //Cancel old tasks
				updateTimer.cancel();
			}
			updateTimer = new java.util.Timer();
			updateTimer.schedule(new java.util.TimerTask() {
				@Override
				public void run() {
					updateESI();
					updateTimer = null;
				}
			}, delay);
		} else {
			if (updateTimer != null) {
				updateTimer.cancel();
			}
		}
	}

	public synchronized static Date getLastLogUpdate() {
		return lastLogUpdate;
	}

	public synchronized static void setLastLogUpdate() {
		MarketOrdersTab.lastLogUpdate = new Date();
	}

	public synchronized static String getClipboardData() {
		return clipboardData;
	}

	public synchronized static void setClipboardData(String clipboardData) {
		MarketOrdersTab.clipboardData = clipboardData;
	}

	private void openEve(MyMarketOrder marketOrder) {
		OwnerType owner = null;
		if (marketOrder.isCorporation()) {
			//Look for issuing owner
			if (marketOrder.getIssuedBy() != null) {
				for (OwnerType ownerType : program.getProfileData().getOwners().values()) { //Copy = thread safe
					if (marketOrder.getIssuedBy() == ownerType.getOwnerID()) {
						owner = ownerType;
						if (owner.isOpenWindows()) {
							break; //Found valid owner - otherwise keep looking
						}
					}
				}
			}
		} else {
			owner = marketOrder.getOwner();
		}
		//Issuer not found
		if (owner == null) {
			int value = JOptionPane.showConfirmDialog(program.getMainWindow().getFrame(), TabsOrders.get().ownerNotFoundMsg(), TabsOrders.get().ownerNotFoundTitle(), JOptionPane.OK_CANCEL_OPTION);
			if (value != JOptionPane.OK_OPTION) {
				return;
			}
			owner = JMenuUI.selectOwner(program, JMenuUI.EsiOwnerRequirement.OPEN_WINDOW);
			if (owner == null) {
				return;
			}
		}
		//Issuer missing scope
		if (!owner.isOpenWindows()) {
			int value = JOptionPane.showConfirmDialog(program.getMainWindow().getFrame(), TabsOrders.get().ownerInvalidScopeMsg(), TabsOrders.get().ownerInvalidScopeTitle(), JOptionPane.OK_CANCEL_OPTION);
			if (value != JOptionPane.OK_OPTION) {
				return;
			}
			owner = JMenuUI.selectOwner(program, JMenuUI.EsiOwnerRequirement.OPEN_WINDOW);
			if (owner == null) {
				return;
			}
		}
		if (!(owner instanceof EsiOwner)) {
			return;
		}
		EsiOwner esiOwner = (EsiOwner) owner;
		if (marketOrder.isOutbid()) {
			copy(marketOrder, marketOrder.getOutbidPrice());
		}
		JMenuUI.openMarketDetails(program, esiOwner, marketOrder.getTypeID(), false);
	}

	private static void copy(MyMarketOrder marketOrder, Double price) {
		if (price != null) {
			if (marketOrder.isBuyOrder()) {
				price = significantIncrement(price);
			} else { //Sell
				price = significantDecrement(price);
			}
			String copy = Formater.copyFormat(price);
			CopyHandler.toClipboard(copy);
			setClipboardData(copy);
		} else {
			setClipboardData(TabsOrders.get().none());
		}
	}

	private void updateErrorLogButton() {
		jErrors.setEnabled(jMarketOrdersErrorDialog.getDocument().getLength() > 0);
		jErrors.setText(jMarketOrdersErrorDialog.getDocument().getLength() > 0 ? TabsOrders.get().logError() : TabsOrders.get().logOK());
	}

	private void updateDates() {
		Date nextUpdate = Settings.get().getPublicMarketOrdersNextUpdate();
		if (Updatable.isUpdatable(nextUpdate)) {
			jUpdate.setText(TabsOrders.get().updateOutbidEsi());
			jUpdate.setEnabled(!jAutoUpdate.isSelected());
		} else {
			long diff = nextUpdate.getTime() - System.currentTimeMillis();
			if (diff < 1000) {
				jUpdate.setText(TabsOrders.get().updateOutbidWhen("..."));
			} else {
				jUpdate.setText(TabsOrders.get().updateOutbidWhen(Formater.milliseconds(diff, false, false, true, true, true, true)));
			}
			jUpdate.setEnabled(false);
		}
		Date lastEsiUpdate = Settings.get().getPublicMarketOrdersLastUpdate();
		update(jLastEsiUpdate, lastEsiUpdate, Images.MISC_ESI);
		Date logUpdate = getLastLogUpdate();
		update(jLastLogUpdate, logUpdate, Images.FILTER_LOAD);
		jClipboard.setText(getClipboardData());
	}

	private void update(JLabel jLastUpdate, Date lastUpdate, Images images) {
		if (lastUpdate != null) {
			long diff = Math.abs(System.currentTimeMillis() - lastUpdate.getTime());
			long diffMinutes = diff / (60 * 1000) % 60;
			jLastUpdate.setOpaque(true);
			if (diffMinutes < 2) {
				jLastUpdate.setIcon(new IconColorIcon(Colors.STRONG_GREEN.getColor(), images.getImage()));
				ColorSettings.config(jLastUpdate, ColorEntry.GLOBAL_ENTRY_VALID);
			} else if (diffMinutes < 5) {
				jLastUpdate.setIcon(new IconColorIcon(Colors.STRONG_YELLOW.getColor(), images.getImage()));
				ColorSettings.config(jLastUpdate, ColorEntry.GLOBAL_ENTRY_WARNING);
			} else {
				jLastUpdate.setIcon(new IconColorIcon(Colors.STRONG_RED.getColor(), images.getImage()));
				ColorSettings.config(jLastUpdate, ColorEntry.GLOBAL_ENTRY_INVALID);
			}
			jLastUpdate.setText(Formater.milliseconds(diff, false, false, true, true, true, true));
		} else {
			jLastUpdate.setOpaque(false);
			jLastUpdate.setIcon(images.getIcon());
			jLastUpdate.setText(TabsOrders.get().none());
		}
	}

	private void updateESI() {
		timer.stop();
		jUpdate.setText(TabsOrders.get().updateOutbidUpdating());
		jUpdate.setEnabled(false);
		OutbidProcesserInput input = new OutbidProcesserInput(program.getProfileData(), Settings.get().getOutbidOrderRange());
		if (input.getRegionIDs().isEmpty()) {
			LOG.info("no active orders found");
			if (jAutoUpdate.isSelected()) {
				jAutoUpdate.setSelected(false);
			}
			updateDates();
			JOptionPane.showMessageDialog(program.getMainWindow().getFrame(), TabsOrders.get().updateNoActiveMsg(), TabsOrders.get().updateNoActiveTitle(), JOptionPane.PLAIN_MESSAGE);
			return;
		}
		OutbidProcesserOutput output = new OutbidProcesserOutput();
		TaskDialog taskDialog = new TaskDialog(program, new PublicMarkerOrdersUpdateTask(input, output), false, jAutoUpdate.isSelected(), jAutoUpdate.isSelected(), StatusPanel.UpdateType.PUBLIC_MARKET_ORDERS, new TaskDialog.TasksCompletedAdvanced() {
			@Override
			public void tasksCompleted(TaskDialog taskDialog) {
				Settings.lock("Outbid (ESI)");
				Settings.get().setMarketOrdersOutbid(output.getOutbids());
				Settings.unlock("Outbid (ESI)");
				//Update eventlists
				if (!output.getOutbids().isEmpty() || !output.getUpdates().isEmpty()) {
					LOG.info("Updating Orders EventList");
					program.updateMarketOrders(output);
				}
				//Save Settings
				if (!output.getOutbids().isEmpty()) {
					LOG.info("Saving Settings");
					program.saveSettings("Marketlog");
				}
				//Save Profile
				if (!output.getUpdates().isEmpty()) {
					LOG.info("Saving Profile");
					program.saveProfile();
				}
				//Update time again
				timer.start();
				//Schedule next update
				schedule();
				Program.ensureEDT(new Runnable() {
					@Override
					public void run() {
						//Sell Order Range
						jSellOrderRangeLast.setText(TabsOrders.get().sellOrderRangeSelcted(Settings.get().getOutbidOrderRange().toString()));
						//Last Update
						updateDates();
					}
				});
			}

			@Override
			public void tasksHidden(TaskDialog taskDialog) {
				updateErrorLogButton();
			}

			@Override
			public StyledDocument getStyledDocument() {
				return jMarketOrdersErrorDialog.getDocument();
			}
		});
	}

	public static double significantIncrement(double value) {
		return significantChange(value, +1);
	}

	public static double significantDecrement(double value) {
		return significantChange(value, -1);
	}

	private static double significantChange(double value, double change) {
		int significantFigures;
		if (value <= 0.0) {
			return 0.0;
		} else if (value == 0.01) {
			if (change > 0) {
				return 0.02;
			} else {
				return 0.01;
			}
		} else if (value < 0.1) {
			significantFigures = SIGNIFICANT_FIGURES - 3;
		} else if (value < 1.0) {
			significantFigures = SIGNIFICANT_FIGURES - 2;
		} else if (value < 10.0) {
			significantFigures = SIGNIFICANT_FIGURES - 1;
		} else {
			significantFigures = SIGNIFICANT_FIGURES;
		}
		double log10 = Math.log10(value);
		if (Math.round(log10) == log10 && value > 10 && change < 0) { //Power of 10
			significantFigures++;
		}
		double power = Math.pow(10, Math.floor(Math.log10(value)) - significantFigures + 1);
		//double power = Math.pow(10, Math.floor(Math.log10(value)) - significantFigures);
		value = value / power;
		value = value + change;
		value = value * power;
		return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}

	private class OrdersTableMenu implements TableMenu<MyMarketOrder> {

		@Override
		public MenuData<MyMarketOrder> getMenuData() {
			return new MenuData<>(selectionModel.getSelected());
		}

		@Override
		public JMenu getFilterMenu() {
			return filterControl.getMenu(jTable, selectionModel.getSelected());
		}

		@Override
		public JMenu getColumnMenu() {
			return tableFormat.getMenu(program, tableModel, jTable, NAME);
		}

		@Override
		public void addInfoMenu(JComponent jComponent) {
			JMenuInfo.marketOrder(jComponent, selectionModel.getSelected());
		}

		@Override
		public void addToolMenu(JComponent jComponent) {
		}
	}

	private class ListenerClass implements ListEventListener<MyMarketOrder>, ActionListener {

		@Override
		public void listChanged(ListEvent<MyMarketOrder> listChanges) {
			double sellOrdersTotal = 0;
			double buyOrdersTotal = 0;
			double toCoverTotal = 0;
			double escrowTotal = 0;
			for (MyMarketOrder marketOrder : filterList) {
				if (!marketOrder.isBuyOrder()) { //Sell
					sellOrdersTotal += marketOrder.getPrice() * marketOrder.getVolumeRemain();
				} else { //Buy
					buyOrdersTotal += marketOrder.getPrice() * marketOrder.getVolumeRemain();
					escrowTotal += marketOrder.getEscrow();
					toCoverTotal += (marketOrder.getPrice() * marketOrder.getVolumeRemain()) - marketOrder.getEscrow();
				}
			}
			jSellOrdersTotal.setText(Formater.iskFormat(sellOrdersTotal));
			jBuyOrdersTotal.setText(Formater.iskFormat(buyOrdersTotal));
			jToCoverTotal.setText(Formater.iskFormat(toCoverTotal));
			jEscrowTotal.setText(Formater.iskFormat(escrowTotal));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (MarketOrdersAction.UPDATE.name().equals(e.getActionCommand())) {
				updateDates();
				updateESI();
			} else if (MarketOrdersAction.AUTO_UPDATE.name().equals(e.getActionCommand())) {
				if (jAutoUpdate.isSelected()) {
					updateDates();
				}
				schedule();
			} else if (MarketOrdersAction.ORDER_TYPE.name().equals(e.getActionCommand())) {
				String value = jOrderType.getItemAt(jOrderType.getSelectedIndex());
				fileListener.setBuy(TabsOrders.get().updateOutbidFileBuy().equals(value));
			} else if (MarketOrdersAction.ERROR_LOG.name().equals(e.getActionCommand())) {
				jMarketOrdersErrorDialog.setVisible(true);
				updateErrorLogButton();
			} else if (MarketOrdersAction.ORDER_RANGE.name().equals(e.getActionCommand())) {
				MarketOrderRange range = jOrderRangeNext.getItemAt(jOrderRangeNext.getSelectedIndex());
				Settings.lock("Outbid Range");
				Settings.get().setOutbidOrderRange(range);
				Settings.unlock("Outbid Range");
				fileListener.setRange(range);
			}
		}
	}

	private class MarketOrdersFilterControl extends FilterControl<MyMarketOrder> {

		public MarketOrdersFilterControl(EventList<MyMarketOrder> exportEventList, Map<String, List<Filter>> defaultFilters) {
			super(program.getMainWindow().getFrame(),
					NAME,
					eventList,
					exportEventList,
					filterList,
					Settings.get().getTableFilters(NAME),
					defaultFilters
					);
		}

		@Override
		protected Object getColumnValue(final MyMarketOrder marketOrder, final String column) {
			return tableFormat.getColumnValue(marketOrder, column);
		}

		@Override
		protected EnumTableColumn<MyMarketOrder> valueOf(final String column) {
			return tableFormat.valueOf(column);
		}

		@Override
		protected List<EnumTableColumn<MyMarketOrder>> getColumns() {
			return new ArrayList<>(tableFormat.getOrderColumns());
		}

		@Override
		protected List<EnumTableColumn<MyMarketOrder>> getShownColumns() {
			return new ArrayList<>(tableFormat.getShownColumns());
		}

		@Override
		protected void saveSettings(final String msg) {
			program.saveSettings("Market Orders Table: " + msg); //Save Market Order Filters and Export Setttings
		}
	}

	private static class PublicMarkerOrdersUpdateTask extends UpdateTask {

		private final OutbidProcesserInput input;
		private final OutbidProcesserOutput output;

		public PublicMarkerOrdersUpdateTask(OutbidProcesserInput input, OutbidProcesserOutput output) {
			super(DialoguesUpdate.get().publicMarkerOrders());
			this.input = input;
			this.output = output;
		}

		@Override
		public void update() {
			EsiPublicMarketOrdersGetter publicMarketOrdersGetter = new EsiPublicMarketOrdersGetter(this, input, output);
			publicMarketOrdersGetter.run();
		}
	}

	public static class IconColorIcon implements Icon {

		private final Color color;
		private final Image image;

		public IconColorIcon(Color color, Image image) {
			this.color = color;
			this.image = image;
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			Graphics2D g2d = (Graphics2D) g;

			//Background
			g2d.setColor(color);
			g2d.fillRect(x, y, getIconWidth(), getIconHeight());

			//Icon
			g2d.drawImage(image, x+1, y, null);
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

	private static class FileListener extends Thread {

		private final Program program;
		private boolean buy;
		private MarketOrderRange range;

		public FileListener(Program program, MarketOrderRange range) {
			super("FileListener");
			this.program = program;
			this.buy = true;
			this.range = range;
		}

		public synchronized boolean isBuy() {
			return buy;
		}

		public synchronized void setBuy(boolean buy) {
			this.buy = buy;
		}

		public synchronized MarketOrderRange getRange() {
			return range;
		}

		public synchronized void setRange(MarketOrderRange range) {
			this.range = range;
		}

		@Override
		public void run() {
			Path dir = MarketLogReader.getMarketlogsDirectory().toPath();
			WatchService watcher;
			while (true) {
				if (MarketLogReader.getMarketlogsDirectory().exists()) {
					try {
						watcher = FileSystems.getDefault().newWatchService();
						dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
						break;
					} catch (IOException ex) {
						LOG.error(ex.getMessage(), ex);
						return;
					}
				} else {
					try {
						Thread.sleep(15000); //Sleep 15 seconds, then tries again
					} catch (InterruptedException ex1) {
						//No problem
					}
				}
			}
			while (true) {
				WatchKey key;
				try {
					LOG.info("waiting for changes...");
					key = watcher.take();
					LOG.info("change detected");
					for (WatchEvent<?> event : key.pollEvents()) {
						// This key is registered only
						// for ENTRY_CREATE events,
						// but an OVERFLOW event can
						// occur regardless if events
						// are lost or discarded.
						WatchEvent.Kind<?> kind = event.kind();
						if (kind == OVERFLOW) {
							LOG.info("Overflow...");
							continue;
						}
						// The filename is the
						// context of the event.
						Object context = event.context();
						if (context instanceof Path) {
							Path path = (Path) context;
							File file = dir.resolve(path).toFile();
							LOG.info("Starting marketlog file processing for " + file.getName());
							long start = System.currentTimeMillis();
							update(file);
							LOG.info("Marketlog file processing done in " + Formater.milliseconds(System.currentTimeMillis() - start) + " for " + file.getName());
						}
					}
					key.reset();
				} catch (InterruptedException ex) {
					LOG.info("FileListener Interrupted");
				}
			}
		}

		private void update(final File file) {
			OutbidProcesserOutput output = new OutbidProcesserOutput();
			boolean updated = update(file, output);
			if (!updated) {
				return;
			}

			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					LOG.info("Starting marketlog update thread for " + file.getName());
					long start = System.currentTimeMillis();
					LOG.info("Setting setting");
					Settings.lock("Outbids (files)");
					Settings.get().setMarketOrdersOutbid(output.getOutbids());
					Settings.unlock("Outbids (files)");
					//Update eventlists
					if (!output.getOutbids().isEmpty() || !output.getUpdates().isEmpty()) {
						LOG.info("Updating Orders EventList");
						program.updateMarketOrdersWithProgress(output);
					}
					//Save Settings
					if (!output.getOutbids().isEmpty()) {
						LOG.info("Saving Settings");
						program.saveSettings("Marketlog");
					}
					//Save Profile
					if (!output.getUpdates().isEmpty()) {
						LOG.info("Saving Profile");
						program.saveProfile();
					}
					LOG.info("Marketlog update thread done in "  + Formater.milliseconds(System.currentTimeMillis() - start) + " for " + file.getName());
				}
			}, file.getName());
			thread.start();
		}

		private boolean update(final File file, final OutbidProcesserOutput output) {
			OutbidProcesserInput input = new OutbidProcesserInput(program.getProfileData(), Settings.get().getOutbidOrderRange());
			List<MarketLog> marketLogs = MarketLogReader.read(file, input, output);
			if (marketLogs == null || marketLogs.isEmpty()) {
				LOG.info("No marketslogs found");
				return false;
			}
			//Copy to clipart
			MyMarketOrder marketOrderCopy = null;
			for (OwnerType ownerType : program.getProfileData().getOwners().values()) { //Copy = thread safe
				synchronized (ownerType) {
					for (MyMarketOrder marketOrder : ownerType.getMarketOrders()) { //Synchronized on owner = thread safe
						if (!Objects.equals(isBuy(), marketOrder.isBuyOrder())) {
							continue;
						}
						Outbid outbid = output.getOutbids().get(marketOrder.getOrderID());
						if (outbid != null) {
							if (marketOrder.isBuyOrder()) {
								//Outbid and highest buy price
								if (outbid.getPrice() > marketOrder.getPrice() && (marketOrderCopy == null || marketOrder.getPrice() > marketOrderCopy.getPrice()) ) {
									marketOrderCopy = marketOrder;
								}
							} else {
								//Outbid and lowest sell price
								if (outbid.getPrice() < marketOrder.getPrice() &&  (marketOrderCopy == null || marketOrder.getPrice() < marketOrderCopy.getPrice())) {
									marketOrderCopy = marketOrder;
								}
							}
						}
					}
				}
			}
			if (marketOrderCopy != null) {
				LOG.info("Found matching order");
				Outbid outbid = output.getOutbids().get(marketOrderCopy.getOrderID());
				if (outbid != null) { //Better safe than sorry
					copy(marketOrderCopy, outbid.getPrice());
					setLastLogUpdate();
					return true;
				}
			}
			//Nothing found - going region wide
			Double price = null;
			for (MarketLog marketLog : marketLogs) {
				if (!Objects.equals(isBuy(), marketLog.getBid())) {
					continue;
				}
				if (marketLog.getJumps() > OutbidProcesser.getRange(getRange())) {
					continue;
				}
				if (price == null) {
					price = marketLog.getPrice();
				} else {
					if (marketLog.getBid()) {
						price = Math.max(price, marketLog.getPrice());
					} else { //Sell
						price = Math.min(price, marketLog.getPrice());
					}
				}
			}
			if (price != null) {
				LOG.info("Found region price");
				if (isBuy()) {
					price = significantIncrement(price);
				} else { //Sell
					price = significantDecrement(price);
				}
				String copy = Formater.copyFormat(price);
				CopyHandler.toClipboard(copy);
				setLastLogUpdate();
				setClipboardData(copy);
				return true;
			}
			LOG.info("No price found....");
			setClipboardData(TabsOrders.get().none());
			return false;
		}
	}
}
