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

package net.nikr.eve.jeveasset;

import apple.dts.samplecode.osxadapter.OSXAdapter;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.text.DefaultEditorKit;
import net.nikr.eve.jeveasset.data.api.accounts.OwnerType;
import net.nikr.eve.jeveasset.data.api.my.MyAccountBalance;
import net.nikr.eve.jeveasset.data.api.my.MyAsset;
import net.nikr.eve.jeveasset.data.api.my.MyContract;
import net.nikr.eve.jeveasset.data.api.my.MyContractItem;
import net.nikr.eve.jeveasset.data.api.my.MyIndustryJob;
import net.nikr.eve.jeveasset.data.api.my.MyJournal;
import net.nikr.eve.jeveasset.data.api.my.MyMarketOrder;
import net.nikr.eve.jeveasset.data.api.my.MyTransaction;
import net.nikr.eve.jeveasset.data.profile.ProfileData;
import net.nikr.eve.jeveasset.data.profile.ProfileManager;
import net.nikr.eve.jeveasset.data.sde.MyLocation;
import net.nikr.eve.jeveasset.data.sde.StaticData;
import net.nikr.eve.jeveasset.data.settings.AssetAddedData;
import net.nikr.eve.jeveasset.data.settings.Settings;
import net.nikr.eve.jeveasset.data.settings.TrackerData;
import net.nikr.eve.jeveasset.data.settings.tag.TagUpdate;
import net.nikr.eve.jeveasset.gui.dialogs.AboutDialog;
import net.nikr.eve.jeveasset.gui.dialogs.account.AccountManagerDialog;
import net.nikr.eve.jeveasset.gui.dialogs.profile.ProfileDialog;
import net.nikr.eve.jeveasset.gui.dialogs.settings.SettingsDialog;
import net.nikr.eve.jeveasset.gui.dialogs.settings.UserLocationSettingsPanel;
import net.nikr.eve.jeveasset.gui.dialogs.settings.UserNameSettingsPanel;
import net.nikr.eve.jeveasset.gui.dialogs.settings.UserPriceSettingsPanel;
import net.nikr.eve.jeveasset.gui.dialogs.update.StructureUpdateDialog;
import net.nikr.eve.jeveasset.gui.dialogs.update.UpdateDialog;
import net.nikr.eve.jeveasset.gui.frame.MainMenu.MainMenuAction;
import net.nikr.eve.jeveasset.gui.frame.MainWindow;
import net.nikr.eve.jeveasset.gui.frame.StatusPanel;
import net.nikr.eve.jeveasset.gui.frame.StatusPanel.UpdateType;
import net.nikr.eve.jeveasset.gui.images.Images;
import net.nikr.eve.jeveasset.gui.shared.Updatable;
import net.nikr.eve.jeveasset.gui.shared.components.JLockWindow;
import net.nikr.eve.jeveasset.gui.shared.components.JMainTab;
import net.nikr.eve.jeveasset.gui.tabs.assets.AssetsTab;
import net.nikr.eve.jeveasset.gui.tabs.contracts.ContractsTab;
import net.nikr.eve.jeveasset.gui.tabs.items.ItemsTab;
import net.nikr.eve.jeveasset.gui.tabs.jobs.IndustryJobsTab;
import net.nikr.eve.jeveasset.gui.tabs.journal.JournalTab;
import net.nikr.eve.jeveasset.gui.tabs.loadout.LoadoutsTab;
import net.nikr.eve.jeveasset.gui.tabs.materials.MaterialsTab;
import net.nikr.eve.jeveasset.gui.tabs.orders.MarketOrdersTab;
import net.nikr.eve.jeveasset.gui.tabs.overview.OverviewTab;
import net.nikr.eve.jeveasset.gui.tabs.reprocessed.ReprocessedTab;
import net.nikr.eve.jeveasset.gui.tabs.routing.RoutingTab;
import net.nikr.eve.jeveasset.gui.tabs.stockpile.StockpileTab;
import net.nikr.eve.jeveasset.gui.tabs.tracker.TrackerTab;
import net.nikr.eve.jeveasset.gui.tabs.transaction.TransactionTab;
import net.nikr.eve.jeveasset.gui.tabs.tree.TreeTab;
import net.nikr.eve.jeveasset.gui.tabs.values.DataSetCreator;
import net.nikr.eve.jeveasset.gui.tabs.values.ValueRetroTab;
import net.nikr.eve.jeveasset.gui.tabs.values.ValueTableTab;
import net.nikr.eve.jeveasset.i18n.GuiFrame;
import net.nikr.eve.jeveasset.i18n.GuiShared;
import net.nikr.eve.jeveasset.data.settings.ContractPriceManager;
import net.nikr.eve.jeveasset.gui.tabs.jobs.IndustrySlotsTab;
import net.nikr.eve.jeveasset.gui.tabs.orders.OutbidProcesser.OutbidProcesserOutput;
import net.nikr.eve.jeveasset.io.online.PriceDataGetter;
import net.nikr.eve.jeveasset.io.online.Updater;
import net.nikr.eve.jeveasset.io.shared.DesktopUtil;
import net.nikr.eve.jeveasset.io.shared.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Program implements ActionListener {
	private static final Logger LOG = LoggerFactory.getLogger(Program.class);

	private enum ProgramAction {
		TIMER
	}
	//Major.Minor.Bugfix [Release Candidate n] [BETA n] [DEV BUILD #n];
	public static final String PROGRAM_VERSION = "6.9.4 DEV BUILD 1";
	public static final String PROGRAM_NAME = "jEveAssets";
	public static final String PROGRAM_HOMEPAGE = "https://eve.nikr.net/jeveasset";
	public static final boolean PROGRAM_DEV_BUILD = false;

	private static boolean debug = false;
	private static boolean forceUpdate = false;
	private static boolean forceNoUpdate = false;
	private static boolean portable = false;
	private static boolean lazySave = false;

	//Height
	private static int height = 22; //Defaults to 22

	//GUI
	private MainWindow mainWindow;

	//Dialogs
	private AccountManagerDialog accountManagerDialog;
	private AboutDialog aboutDialog;
	private ProfileDialog profileDialog;
	private SettingsDialog settingsDialog;
	private UpdateDialog updateDialog;

	//Tabs
	private ValueRetroTab valueRetroTab;
	private ValueTableTab valueTableTab;
	private MaterialsTab materialsTab;
	private LoadoutsTab loadoutsTab;
	private RoutingTab routingTab;
	private MarketOrdersTab marketOrdersTab;
	private JournalTab journalTab;
	private TransactionTab transactionsTab;
	private IndustryJobsTab industryJobsTab;
	private IndustrySlotsTab industrySlotsTab;
	private AssetsTab assetsTab;
	private OverviewTab overviewTab;
	private StockpileTab stockpileTab;
	private ItemsTab itemsTab;
	private TrackerTab trackerTab;
	private ReprocessedTab reprocessedTab;
	private ContractsTab contractsTab;
	private TreeTab treeTab;
	private StructureUpdateDialog structureUpdateDialog;

	//Misc
	private Updater updater;
	private Timer timer;
	private Updatable updatable;

	private final Map<String, JMainTab> jMainTabs = new HashMap<>();

	//Data
	private final ProfileData profileData;
	private final ProfileManager profileManager;
	private final PriceDataGetter priceDataGetter;
	private final String localData;

	public Program() {
		if (debug) {
			LOG.debug("Force Update: {} Force No Update: {}", forceUpdate, forceNoUpdate);
			DetectEdtViolationRepaintManager.install();
		}
		if (PROGRAM_DEV_BUILD) {
			portable = true;
		}

	//Data
		SplashUpdater.setText("Loading DATA");
		LOG.info("DATA Loading...");
		FileUtil.autoImportFileUtil();
		StaticData.load();
		Settings.load();
		TrackerData.load();
		AssetAddedData.load();
		ContractPriceManager.load();

		initLookAndFeel(Settings.get().getColorSettings().getLookAndFeelClass());

		calcButtonsHeight(); //Must be done after setting the LAF

		updater = new Updater();
		localData = updater.getLocalData();
		if (!PROGRAM_DEV_BUILD) {
			update();
		}

		profileManager = new ProfileManager();
		profileManager.searchProfile();
		profileManager.loadActiveProfile();
		profileData = new ProfileData(profileManager);
		//Can not update profile data now - list needs to be empty doing creation...
		priceDataGetter = new PriceDataGetter();
		priceDataGetter.load();
	//Timer
		timer = new Timer(15000, this); //Once a minute
		timer.setActionCommand(ProgramAction.TIMER.name());
	//Updatable
		updatable = new Updatable(this);
	//GUI
		SplashUpdater.setText("Loading GUI");
		LOG.info("GUI Loading:");
		LOG.info("Loading: Images");
		Images.preload();
		LOG.info("Loading: Main Window");
		mainWindow = new MainWindow(this);
		SplashUpdater.setProgress(50);
	//Tools
		LOG.info("Loading: Assets Tab");
		assetsTab = new AssetsTab(this);
		mainWindow.addTab(assetsTab);
		SplashUpdater.setProgress(52);
		LOG.info("Loading: Tree Tab");
		treeTab = new TreeTab(this);
		SplashUpdater.setProgress(54);
		LOG.info("Loading: Industry Jobs Tab");
		industryJobsTab = new IndustryJobsTab(this);
		LOG.info("Loading: Industry Slot Tab");
		industrySlotsTab = new IndustrySlotsTab(this);
		LOG.info("Loading: Industry Plot Tab");
		//FIXME - - > IndustryPlotTab - remove or ?
		//industryPlotTab = new IndustryPlotTab(this);
		SplashUpdater.setProgress(56);
		LOG.info("Loading: Market Orders Tab");
		marketOrdersTab = new MarketOrdersTab(this);
		SplashUpdater.setProgress(58);
		LOG.info("Loading: Journal Tab");
		journalTab = new JournalTab(this);
		SplashUpdater.setProgress(60);
		LOG.info("Loading: Transactions Tab");
		transactionsTab = new TransactionTab(this);
		SplashUpdater.setProgress(62);
		LOG.info("Loading: Materials Tab");
		materialsTab = new MaterialsTab(this);
		SplashUpdater.setProgress(64);
		LOG.info("Loading: Ship Loadouts Tab");
		loadoutsTab = new LoadoutsTab(this);
		SplashUpdater.setProgress(66);
		LOG.info("Loading: Values Tab");
		valueRetroTab = new ValueRetroTab(this);
		valueTableTab = new ValueTableTab(this);
		SplashUpdater.setProgress(68);
		LOG.info("Loading: Routing Tab");
		routingTab = new RoutingTab(this);
		SplashUpdater.setProgress(70);
		LOG.info("Loading: Overview Tab");
		overviewTab = new OverviewTab(this);
		SplashUpdater.setProgress(72);
		LOG.info("Loading: Stockpile Tab");
		stockpileTab = new StockpileTab(this);
		SplashUpdater.setProgress(74);
		LOG.info("Loading: Items Tab");
		itemsTab = new ItemsTab(this);
		SplashUpdater.setProgress(76);
		LOG.info("Loading: Tracker Tab");
		trackerTab = new TrackerTab(this);
		SplashUpdater.setProgress(78);
		LOG.info("Loading: Reprocessed Tab");
		reprocessedTab = new ReprocessedTab(this);
		SplashUpdater.setProgress(80);
		LOG.info("Loading: Contracts Tab");
		contractsTab = new ContractsTab(this);
		SplashUpdater.setProgress(82);
	//Dialogs
		LOG.info("Loading: Account Manager Dialog");
		accountManagerDialog = new AccountManagerDialog(this);
		SplashUpdater.setProgress(84);
		LOG.info("Loading: About Dialog");
		aboutDialog = new AboutDialog(this);
		SplashUpdater.setProgress(86);
		LOG.info("Loading: Profiles Dialog");
		profileDialog = new ProfileDialog(this);
		SplashUpdater.setProgress(88);
		LOG.info("Loading: Update Dialog");
		updateDialog = new UpdateDialog(this);
		SplashUpdater.setProgress(90);
		LOG.info("Loading: Options Dialog");
		settingsDialog = new SettingsDialog(this);
		SplashUpdater.setProgress(96);
		LOG.info("Loading: Structure UpdateDialog");
		structureUpdateDialog = new StructureUpdateDialog(this);
	//GUI Done
		LOG.info("GUI loaded");
	//Updating data...
		LOG.info("Updating data...");
		updateEventLists(); //Update price
		macOsxCode();
	//Open Tools
		for (String title : Settings.get().getShowTools()) {
			for (JMainTab jMainTab : jMainTabs.values()) {
				if (title.equals(jMainTab.getTitle())) {
					mainWindow.addTab(jMainTab, false);
				}
			}
		}
		SplashUpdater.setProgress(100);
		LOG.info("Showing GUI");
		mainWindow.show();
		SplashUpdater.hide();
		//Start timer
		timerTicked();
		LOG.info("Startup Done");
		if (debug) {
			LOG.info("Show Debug Warning");
			JOptionPane.showMessageDialog(mainWindow.getFrame(), "WARNING: Debug is enabled", "Debug", JOptionPane.WARNING_MESSAGE);
		}
		if (PROGRAM_DEV_BUILD) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(), "WARNING: This is a dev build\r\n\r\nNotes:\r\n- Always run portable\r\n- Settings and profiles are cloned\r\n- Does not check for updates\r\n- Expect bugs!", "DEV BUILD", JOptionPane.WARNING_MESSAGE);
		}
		if (Settings.get().isSettingsLoadError()) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(), GuiShared.get().errorLoadingSettingsMsg(), GuiShared.get().errorLoadingSettingsTitle(), JOptionPane.ERROR_MESSAGE);
		}
		profileManager.showProfileLoadErrorWarning(mainWindow.getFrame());
		if (profileManager.getOwnerTypes().isEmpty()) {
			LOG.info("Show Account Manager");
			accountManagerDialog.setVisible(true);
		}
	}

	/**
	 *
	 * @param load does nothing except change the signature.
	 */
	protected Program(final boolean load) {
		profileData = null;
		profileManager = null;
		priceDataGetter = null;
		localData = null;
	}

	private void initLookAndFeel(String lookAndFeel) {
		initLookAndFeel(lookAndFeel, true);
	}

	private void initLookAndFeel(String lookAndFeel, boolean tryDefault) {
		//Allow users to overwrite LaF
		if (System.getProperty("swing.defaultlaf") != null) {
			return;
		}
		//lookAndFeel = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
		//lookAndFeel = UIManager.getSystemLookAndFeelClassName(); //System
		//lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName(); //Java
		//lookAndFeel = "javax.swing.plaf.nimbus.NimbusLookAndFeel"; //Nimbus
		//lookAndFeel = "javax.swing.plaf.metal.MetalLookAndFeel"; //Metal
		//lookAndFeel = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel"; //GTK+
		//lookAndFeel = "com.sun.java.swing.plaf.motif.MotifLookAndFeel"; //CDE/Motif

		//flatlaf
		//lookAndFeel = "com.formdev.flatlaf.FlatLightLaf"; //Flat Light
		//lookAndFeel = "com.formdev.flatlaf.FlatDarkLaf"; //Flat Dark
		//lookAndFeel = "com.formdev.flatlaf.FlatIntelliJLaf"; //Flat IntelliJ
		//lookAndFeel = "com.formdev.flatlaf.FlatDarculaLaf"; //Flat Darcula
		
		try {
			UIManager.setLookAndFeel(lookAndFeel);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
			LOG.error(ex.getMessage(), ex);
			//In case the settings is using an unsupported look and feel
			//Try system look and feel
			if (tryDefault) {
				initLookAndFeel(UIManager.getSystemLookAndFeelClassName(), false);
			}
		}
		setMacKeys(); // This must be performed immediately after the LaF has been set

		//Make sure we have nice window decorations.
		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);
	}

	private void setMacKeys() {
		// Ensure Max OSX key bindings are useable for copy, cut, paste, and select all
		addMacKeysText(UIManager.get("EditorPane.focusInputMap"));
		addMacKeysText(UIManager.get("FormattedTextField.focusInputMap"));
		addMacKeysText(UIManager.get("PasswordField.focusInputMap"));
		addMacKeysText(UIManager.get("TextField.focusInputMap"));
		addMacKeysText(UIManager.get("TextPane.focusInputMap"));
		addMacKeysText(UIManager.get("TextArea.focusInputMap"));
		addMacKeysMisc(UIManager.get("Table.ancestorInputMap"));
		addMacKeysMisc(UIManager.get("Tree.focusInputMap"));
		addMacKeysMisc(UIManager.get("List.focusInputMap"));
	}

	private void addMacKeysText(Object object) {
		if (object instanceof InputMap) { //Better safe than sorry
			InputMap inputMap = (InputMap) object;
			inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
			inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
			inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
			inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.META_DOWN_MASK), DefaultEditorKit.selectAllAction);
		}
	}

	private void addMacKeysMisc(Object object) {
		if (object instanceof InputMap) { //Better safe than sorry
			InputMap inputMap = (InputMap) object;
			inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), "copy");
			inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), "cut");
			inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), "parse");
			inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.META_DOWN_MASK), "selectAll");
		}
	}

	public static int getButtonsHeight() {
		return height;
	}

	private void calcButtonsHeight() {
		height = Math.max(height, new JComboBox<>().getPreferredSize().height);
		height = Math.max(height, new JTextField().getPreferredSize().height);
		height = Math.max(height, new JButton().getPreferredSize().height);
	}

	public static int getButtonsWidth() {
		return 90;
	}

	public void addMainTab(final String toolName, final JMainTab jMainTab) {
		JMainTab old = jMainTabs.put(toolName, jMainTab);
		if (old != null) {
			throw new RuntimeException("toolName: " + toolName + " is duplicated");
		}
	}

	public Map<String, JMainTab> getMainTabs() {
		return jMainTabs;
	}

	private void timerTicked() {
		if (!timer.isRunning()) {
			timer.start();
		}
		boolean isUpdatable = updatable.isUpdatable();
		this.getStatusPanel().timerTicked(isUpdatable);
		this.getMainWindow().getMenu().timerTicked(isUpdatable, StructureUpdateDialog.structuresUpdatable(this));
	}

	public final void update() {
		updater.update(Program.PROGRAM_VERSION, localData, Settings.get().getProxyData());
	}

	public boolean checkProgramUpdate() {
		return updater.checkProgramUpdate(Program.PROGRAM_VERSION);
	}

	public boolean checkDataUpdate() {
		return updater.checkDataUpdate(localData);
	}

	public final void updateMarketOrdersWithProgress(OutbidProcesserOutput output) {
		JLockWindow jLockWindow = new JLockWindow(getMainWindow().getFrame());
		jLockWindow.show(GuiShared.get().updating(), new JLockWindow.LockWorker() {
			@Override
			public void task() {
				updateEventLists(null, null, null, output);
			}

			@Override
			public void gui() { }
		});
	}

	public final void updateMarketOrders(OutbidProcesserOutput output) {
		updateEventLists(null, null, null, output);
	}

	public final void updateLocations(Set<Long> locationIDs) {
		JLockWindow jLockWindow = new JLockWindow(getMainWindow().getFrame());
		jLockWindow.show(GuiShared.get().updating(), new JLockWindow.LockWorker() {
			@Override
			public void task() {
				updateEventLists(null, locationIDs, null, null);
			}

			@Override
			public void gui() { }
		});
	}

	public final void updatePrices(Set<Integer> typeIDs) {
		JLockWindow jLockWindow = new JLockWindow(getMainWindow().getFrame());
		jLockWindow.show(GuiShared.get().updating(), new JLockWindow.LockWorker() {
			@Override
			public void task() {
				updateEventLists(null, null, typeIDs, null);
			}

			@Override
			public void gui() { }
		});
	}

	public final void updateNames(Set<Long> itemIDs) {
		JLockWindow jLockWindow = new JLockWindow(getMainWindow().getFrame());
		jLockWindow.show(GuiShared.get().updating(), new JLockWindow.LockWorker() {
			@Override
			public void task() {
				updateEventLists(itemIDs, null, null, null);
			}

			@Override
			public void gui() { }
		});
	}

	public final void updateEventListsWithProgress() {
		updateEventListsWithProgress(getMainWindow().getFrame());
	}

	public final void updateEventListsWithProgress(final Window parent) {
		JLockWindow jLockWindow = new JLockWindow(parent);
		jLockWindow.show(GuiShared.get().updating(), new JLockWindow.LockWorker() {
			@Override
			public void task() {
				updateEventLists();
			}

			@Override
			public void gui() { }
		});
	}

	public final void updateEventLists() {
		updateEventLists(null, null, null, null);
	}

	private synchronized void updateEventLists(Set<Long> itemIDs, Set<Long> locationIDs, Set<Integer> typeIDs, OutbidProcesserOutput output) {
		LOG.info("Updating EventList");
		for (JMainTab jMainTab : mainWindow.getTabs()) {
			ensureEDT(new Runnable() {
				@Override
				public void run() {
					jMainTab.beforeUpdateData();
				}
			});
		}
		if (output != null) {
			profileData.updateMarketOrders(output);
		} else if (itemIDs != null) {
			profileData.updateNames(itemIDs);
		} else if (locationIDs != null) {
			profileData.updateLocations(locationIDs);
		} else if (typeIDs != null) {
			profileData.updatePrice(typeIDs);
		} else {
			profileData.updateEventLists();
		}
		if (locationIDs != null) { //Update locations
			for (JMainTab jMainTab : mainWindow.getTabs()) {
				ensureEDT(new Runnable() {
					@Override
					public void run() {
						jMainTab.updateLocations(locationIDs);
					}
				});
			}
		} else if (typeIDs != null) { //Update prices
			for (JMainTab jMainTab : mainWindow.getTabs()) {
				ensureEDT(new Runnable() {
					@Override
					public void run() {
						jMainTab.updatePrices(typeIDs);
					}
				});
			}
		} else if (itemIDs != null) { //Update names
			for (JMainTab jMainTab : mainWindow.getTabs()) {
				ensureEDT(new Runnable() {
					@Override
					public void run() {
						jMainTab.updateNames(itemIDs);
					}
				});
			}
		} else { //Full update
			for (JMainTab jMainTab : mainWindow.getTabs()) {
				ensureEDT(new Runnable() {
					@Override
					public void run() {
						jMainTab.updateData();
					}
				});
			}
		}
		for (JMainTab jMainTab : mainWindow.getTabs()) {
			ensureEDT(new Runnable() {
				@Override
				public void run() {
					jMainTab.afterUpdateData();
					jMainTab.updateCache();
				}
			});
		}
		ensureEDT(new Runnable() {
			@Override
			public void run() {
				if (stockpileTab != null) {
					stockpileTab.updateStockpileDialog();
				}
			}
		});
		
		ensureEDT(new Runnable() {
			@Override
			public void run() {
				timerTicked();
			}
		});
		ensureEDT(new Runnable() {
			@Override
			public void run() {
				updateTableMenu();
			}
		});
	}

	public static void ensureEDT(Runnable runnable) {
		if (SwingUtilities.isEventDispatchThread()) {
			runnable.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(runnable);
			} catch (InterruptedException | InvocationTargetException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	/**
	 * Save Settings ASAP
	 * @param msg Who is saving what?
	 */
	public void saveSettings(final String msg) {
		if (!lazySave && !Settings.ignoreSave()) {
			Settings.saveStart();
			Thread thread = new SaveSettings(msg, this);
			thread.start();
		}
	}

	private void doSaveSettings(final String msg) {
		LOG.info("Saving Settings: " + msg);
		Settings.lock("Table (Column/Width/Resize) and Window Settings"); //Lock for Table (Column/Width/Resize) and Window Settings
		mainWindow.updateSettings();
		for (JMainTab jMainTab : jMainTabs.values()) {
			jMainTab.saveSettings();
		}
		Settings.unlock("Table (Column/Width/Resize) and Window Settings"); //Unlock for Table (Column/Width/Resize) and Window Settings
		Settings.saveSettings();
	}

	public void saveSettingsAndProfile() {
		if (lazySave) {
			doSaveSettings("API Update");
		} else {
			saveSettings("API Update");
			Settings.waitForEmptySaveQueue();
		}
		saveProfile();
	}

	public synchronized void saveProfile() {
		LOG.info("Saving Profile");
		profileManager.saveProfile();
	}

	/**
	 * Used by macOsxCode() - should not be changed
	 */
	public void exit() {
		if (getStatusPanel().updateInProgress() > 0) {
			int value = JOptionPane.showConfirmDialog(getMainWindow().getFrame(),  GuiFrame.get().exitMsg(getStatusPanel().updateInProgress()), GuiFrame.get().exitTitle(getStatusPanel().updateInProgress()), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (value != JOptionPane.OK_OPTION) {
				return;
			}
		}
		getStatusPanel().cancelUpdates();
		saveExit();
		LOG.info("Running shutdown hook(s) and exiting...");
		System.exit(0);
	}

	/**
	 * Used by macOsxCode() - should not be renamed
	 */
	public void saveExit() {
		if (lazySave) {
			doSaveSettings("Exit");
		} else {
			LOG.info("Waiting for save queue to finish...");
			Settings.waitForEmptySaveQueue();
		}
		TrackerData.waitForEmptySaveQueue();
	}

	/**
	 * Used by macOsxCode() - should not be renamed
	 */
	public void showAbout() {
		aboutDialog.setVisible(true);
	}

	/**
	 * Used by macOsxCode() - should not be renamed
	 */
	public void showSettings() {
		settingsDialog.setVisible(true);
	}

	public String getProgramDataVersion() {
		return localData;
	}

	private void macOsxCode() {
		if (FileUtil.onMac()) {
			try {
				OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("saveExit", (Class[]) null));
				OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("showAbout", (Class[]) null));
				OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod("showSettings", (Class[]) null));
			} catch (NoSuchMethodException ex) {
				LOG.error("NoSuchMethodException: " + ex.getMessage(), ex);
			} catch (SecurityException ex) {
				LOG.error("SecurityException: " + ex.getMessage(), ex);
			}
		}
	}

	public MainWindow getMainWindow() {
		return mainWindow;
	}

	public AssetsTab getAssetsTab() {
		return assetsTab;
	}

	public IndustrySlotsTab getIndustrySlotsTab() {
		return industrySlotsTab;
	}

	public OverviewTab getOverviewTab() {
		return overviewTab;
	}

	public TreeTab getTreeTab() {
		return treeTab;
	}

	public TransactionTab getTransactionsTab() {
		return transactionsTab;
	}

	public StatusPanel getStatusPanel() {
		return this.getMainWindow().getStatusPanel();
	}

	public UserNameSettingsPanel getUserNameSettingsPanel() {
		if (settingsDialog != null) {
			return settingsDialog.getUserNameSettingsPanel();
		} else {
			return null;
		}
	}

	public UserPriceSettingsPanel getUserPriceSettingsPanel() {
		if (settingsDialog != null) {
			return settingsDialog.getUserPriceSettingsPanel();
		} else {
			return null;
		}
	}

	public UserLocationSettingsPanel getUserLocationSettingsPanel() {
		if (settingsDialog != null) {
			return settingsDialog.getUserLocationSettingsPanel();
		} else {
			return null;
		}
	}

	public StockpileTab getStockpileTool() {
		return stockpileTab;
	}
	public ReprocessedTab getReprocessedTab() {
		return reprocessedTab;
	}
	public RoutingTab getRoutingTab() {
		return routingTab;
	}

	public TrackerTab getTrackerTab() {
		return trackerTab;
	}

	public ValueTableTab getValueTableTab() {
		return valueTableTab;
	}

	public ProfileData getProfileData() {
		return profileData;
	}

	public List<MyAsset> getAssetList() {
		return profileData.getAssetsList();
	}
	public List<MyContract> getContractList() {
		return profileData.getContractList();
	}
	public List<MyContractItem> getContractItemList() {
		return profileData.getContractItemList();
	}
	public List<MyIndustryJob> getIndustryJobsList() {
		return profileData.getIndustryJobsList();
	}
	public List<MyMarketOrder> getMarketOrdersList() {
		return profileData.getMarketOrdersList();
	}
	public List<MyJournal> getJournalList() {
		return profileData.getJournalList();
	}
	public List<MyTransaction> getTransactionsList() {
		return profileData.getTransactionsList();
	}
	public List<MyAccountBalance> getAccountBalanceList() {
		return profileData.getAccountBalanceList();
	}
	public List<String> getOwnerNames(boolean all) {
		return profileData.getOwnerNames(all);
	}
	public Map<Long, OwnerType> getOwners() {
		return profileData.getOwners();
	}
	public List<OwnerType> getOwnerTypes() {
		return profileManager.getOwnerTypes();
	}
	public ProfileManager getProfileManager() {
		return profileManager;
	}
	public PriceDataGetter getPriceDataGetter() {
		return priceDataGetter;
	}
	public void createTrackerDataPoint() {
		DataSetCreator.createTrackerDataPoint(profileData, Settings.getNow());
		TrackerData.save("Added", true);
		ensureEDT(new Runnable() {
			@Override
			public void run() {
				trackerTab.updateData();
			}
		});
	}

	public static boolean isDebug() {
		return debug;
	}

	public static void setDebug(final boolean debug) {
		Program.debug = debug;
	}

	public static boolean isForceNoUpdate() {
		return forceNoUpdate;
	}

	public static void setForceNoUpdate(final boolean forceNoUpdate) {
		Program.forceNoUpdate = forceNoUpdate;
	}

	public static boolean isForceUpdate() {
		return forceUpdate;
	}

	public static void setForceUpdate(final boolean forceUpdate) {
		Program.forceUpdate = forceUpdate;
	}

	public static void setPortable(final boolean portable) {
		Program.portable = portable;
	}

	public static void setLazySave(final boolean lazySave) {
		Program.lazySave = lazySave;
	}

	public static boolean isPortable() {
		return portable;
	}

	public void updateStructures(Set<MyLocation> locations) {
		structureUpdateDialog.show(locations);
	}

	/**
	 * Called when Tags are changed.
	 */
	public void updateTags() {
		JLockWindow jLockWindow = new JLockWindow(getMainWindow().getFrame());
		jLockWindow.show(GuiShared.get().updating(), new JLockWindow.LockWorker() {
			@Override
			public void task() {
				for (JMainTab mainTab : jMainTabs.values()) {
					if (mainTab instanceof TagUpdate) {
						mainTab.updateCache();
					}
				}
			}
			@Override
			public void gui() {
				for (JMainTab mainTab : jMainTabs.values()) {
					if (mainTab instanceof TagUpdate) {
						TagUpdate tagUpdate = (TagUpdate) mainTab;
						tagUpdate.updateTags();
					}
				}
			}
		});
	}
	/**
	 * Called when Overview Groups are changed.
	 */
	public void overviewGroupsChanged() {
		routingTab.overviewGroupsChanged();
	}

	/**
	 * Called when the table menu needs update.
	 */
	public void updateTableMenu() {
		this.getMainWindow().getSelectedTab().updateTableMenu();
	}

	/**
	 * Called when the active tab is change (close/open/change).
	 */
	public void tabChanged() {
		getStatusPanel().tabChanged();
		updateTableMenu();
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
	//Tools
		if (MainMenuAction.VALUES.name().equals(e.getActionCommand())) {
			mainWindow.addTab(valueRetroTab);
		} else if (MainMenuAction.VALUE_TABLE.name().equals(e.getActionCommand())) {
			mainWindow.addTab(valueTableTab);
		} else if (MainMenuAction.MATERIALS.name().equals(e.getActionCommand())) {
			mainWindow.addTab(materialsTab);
		} else if (MainMenuAction.LOADOUTS.name().equals(e.getActionCommand())) {
			mainWindow.addTab(loadoutsTab);
		} else if (MainMenuAction.MARKET_ORDERS.name().equals(e.getActionCommand())) {
			mainWindow.addTab(marketOrdersTab);
		} else if (MainMenuAction.JOURNAL.name().equals(e.getActionCommand())) {
			mainWindow.addTab(journalTab);
		} else if (MainMenuAction.TRANSACTION.name().equals(e.getActionCommand())) {
			mainWindow.addTab(transactionsTab);
		} else if (MainMenuAction.INDUSTRY_JOBS.name().equals(e.getActionCommand())) {
			mainWindow.addTab(industryJobsTab);
		} else if (MainMenuAction.INDUSTRY_SLOT.name().equals(e.getActionCommand())) {
			mainWindow.addTab(industrySlotsTab);
		} else if (MainMenuAction.OVERVIEW.name().equals(e.getActionCommand())) {
			mainWindow.addTab(overviewTab);
		} else if (MainMenuAction.ROUTING.name().equals(e.getActionCommand())) {
			mainWindow.addTab(routingTab);
		} else if (MainMenuAction.STOCKPILE.name().equals(e.getActionCommand())) {
			mainWindow.addTab(stockpileTab);
		} else if (MainMenuAction.ITEMS.name().equals(e.getActionCommand())) {
			mainWindow.addTab(itemsTab);
		} else if (MainMenuAction.TRACKER.name().equals(e.getActionCommand())) {
			mainWindow.addTab(trackerTab);
			trackerTab.checkAll();
		} else if (MainMenuAction.REPROCESSED.name().equals(e.getActionCommand())) {
			mainWindow.addTab(reprocessedTab);
		} else if (MainMenuAction.CONTRACTS.name().equals(e.getActionCommand())) {
			mainWindow.addTab(contractsTab);
		} else if (MainMenuAction.TREE.name().equals(e.getActionCommand())) {
			mainWindow.addTab(treeTab);
		} else if (MainMenuAction.ACCOUNT_MANAGER.name().equals(e.getActionCommand())) { //Settings
			accountManagerDialog.setVisible(true);
		} else if (MainMenuAction.PROFILES.name().equals(e.getActionCommand())) {
			profileDialog.setVisible(true);
		} else if (MainMenuAction.OPTIONS.name().equals(e.getActionCommand())) {
			showSettings();
		} else if (MainMenuAction.UPDATE.name().equals(e.getActionCommand())) { //Update
			updateDialog.setVisible(true);
		} else if (MainMenuAction.UPDATE_STRUCTURE.name().equals(e.getActionCommand())) {
			if (getStatusPanel().updateing(UpdateType.STRUCTURE)) {
				JOptionPane.showMessageDialog(getMainWindow().getFrame(), GuiFrame.get().updatingInProgressMsg(), GuiFrame.get().updatingInProgressTitle(), JOptionPane.PLAIN_MESSAGE);
			} else {
				updateStructures(null);
			}
		} else if (MainMenuAction.ABOUT.name().equals(e.getActionCommand())) { //Others
			showAbout();
		} else if (MainMenuAction.SEND_BUG_REPORT.name().equals(e.getActionCommand())) {
			DesktopUtil.browse("https://github.com/GoldenGnu/jeveassets/issues/new", this);
		} else if (MainMenuAction.README.name().equals(e.getActionCommand())) { //External Files
			DesktopUtil.open(FileUtil.getPathReadme(), this);
		} else if (MainMenuAction.LICENSE.name().equals(e.getActionCommand())) {
			DesktopUtil.open(FileUtil.getPathLicense(), this);
		} else if (MainMenuAction.CREDITS.name().equals(e.getActionCommand())) {
			DesktopUtil.open(FileUtil.getPathCredits(), this);
		} else if (MainMenuAction.CHANGELOG.name().equals(e.getActionCommand())) {
			DesktopUtil.open(FileUtil.getPathChangeLog(), this);
		} else if (MainMenuAction.LINK_GITHUB.name().equals(e.getActionCommand())) { //Links
			DesktopUtil.browse("https://github.com/GoldenGnu/jeveassets/issues/new", this);
		} else if (MainMenuAction.LINK_FAQ.name().equals(e.getActionCommand())) {
			DesktopUtil.browse("https://jeveassets.nikr.net/faq", this);
		} else if (MainMenuAction.LINK_FORUM.name().equals(e.getActionCommand())) {
			DesktopUtil.browse("https://forums.eveonline.com/t/13255/", this);
		} else if (MainMenuAction.EXIT_PROGRAM.name().equals(e.getActionCommand())) { //Exit
			exit();
		} else if (ProgramAction.TIMER.name().equals(e.getActionCommand())) { //Ticker
			timerTicked();
		}
	}

	private static class SaveSettings extends Thread {

		private static int counter = 0;

		private final String msg;
		private final Program program;
		private final int id;

		public SaveSettings(String msg, Program program) {
			super("Save Settings " + counter++ + ": " + msg);
			this.msg = msg;
			this.program = program;
			this.id = counter;
		}

		@Override
		public void run() {
			long before = System.currentTimeMillis();

			program.doSaveSettings(msg);

			Settings.saveEnd();

			long after = System.currentTimeMillis();

			LOG.debug("Settings saved in: " + (after - before) + "ms");
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 67 * hash + this.id;
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final SaveSettings other = (SaveSettings) obj;
			return this.id == other.id;
		}
	}
}
