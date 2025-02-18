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

package net.nikr.eve.jeveasset.io.local;

import java.io.File;
import java.net.Proxy;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.nikr.eve.jeveasset.data.api.raw.RawMarketOrder.MarketOrderRange;
import net.nikr.eve.jeveasset.data.settings.ColorEntry;
import net.nikr.eve.jeveasset.data.settings.ColorSettings;
import net.nikr.eve.jeveasset.data.settings.ContractPriceManager.ContractPriceSettings;
import net.nikr.eve.jeveasset.data.settings.CopySettings;
import net.nikr.eve.jeveasset.data.settings.ExportSettings;
import net.nikr.eve.jeveasset.data.settings.MarketOrdersSettings;
import net.nikr.eve.jeveasset.data.settings.PriceDataSettings;
import net.nikr.eve.jeveasset.data.settings.ProxyData;
import net.nikr.eve.jeveasset.data.settings.ReprocessSettings;
import net.nikr.eve.jeveasset.data.settings.RouteResult;
import net.nikr.eve.jeveasset.data.settings.RoutingSettings;
import net.nikr.eve.jeveasset.data.settings.Settings;
import net.nikr.eve.jeveasset.data.settings.Settings.SettingFlag;
import net.nikr.eve.jeveasset.data.settings.TrackerData;
import net.nikr.eve.jeveasset.data.settings.UserItem;
import net.nikr.eve.jeveasset.data.settings.tag.Tag;
import net.nikr.eve.jeveasset.data.settings.tag.TagID;
import net.nikr.eve.jeveasset.gui.shared.filter.Filter;
import net.nikr.eve.jeveasset.gui.shared.menu.JFormulaDialog.Formula;
import net.nikr.eve.jeveasset.gui.shared.menu.JMenuJumps.Jump;
import net.nikr.eve.jeveasset.gui.shared.table.EnumTableFormatAdaptor.ResizeMode;
import net.nikr.eve.jeveasset.gui.shared.table.EnumTableFormatAdaptor.SimpleColumn;
import net.nikr.eve.jeveasset.gui.shared.table.View;
import net.nikr.eve.jeveasset.gui.tabs.orders.Outbid;
import net.nikr.eve.jeveasset.gui.tabs.overview.OverviewGroup;
import net.nikr.eve.jeveasset.gui.tabs.overview.OverviewLocation;
import net.nikr.eve.jeveasset.gui.tabs.routing.SolarSystem;
import net.nikr.eve.jeveasset.gui.tabs.stockpile.Stockpile;
import net.nikr.eve.jeveasset.gui.tabs.stockpile.Stockpile.StockpileFilter;
import net.nikr.eve.jeveasset.gui.tabs.stockpile.Stockpile.StockpileItem;
import net.nikr.eve.jeveasset.gui.tabs.tracker.TrackerDate;
import net.nikr.eve.jeveasset.gui.tabs.tracker.TrackerNote;
import net.nikr.eve.jeveasset.gui.tabs.tracker.TrackerSkillPointFilter;
import net.nikr.eve.jeveasset.io.shared.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class SettingsWriter extends AbstractXmlWriter {

	private static final Logger LOG = LoggerFactory.getLogger(SettingsWriter.class);

	private SettingsWriter() { }

	public static boolean save(final Settings settings, final String filename) {
		if (!new File(FileUtil.getPathTrackerData()).exists()) { //Make sure the tracker data is saved
			TrackerData.save("Saving Settings", true);
		}
		SettingsWriter writer = new SettingsWriter();
		return writer.write(settings, filename);
	}

	public static boolean saveStockpiles(final List<Stockpile> stockpiles, final String filename) {
		SettingsWriter writer = new SettingsWriter();
		return writer.writeStockpiles(stockpiles, filename);
	}

	private boolean writeStockpiles(final List<Stockpile> stockpiles, final String filename) {
		Document xmldoc;
		try {
			xmldoc = getXmlDocument("settings");
		} catch (XmlException ex) {
			LOG.error("Stockpile not saved " + ex.getMessage(), ex);
			return false;
		}

		writeStockpiles(xmldoc, stockpiles, true);
		try {
			writeXmlFile(xmldoc, filename, false);
		} catch (XmlException ex) {
			LOG.error("Stockpile not saved " + ex.getMessage(), ex);
			return false;
		}
		LOG.info("Stockpile saved");
		return true;
	}

	public static boolean saveRoutes(final Map<String, RouteResult> routes, final String filename) {
		SettingsWriter writer = new SettingsWriter();
		return writer.writeRoutes(routes, filename);
	}

	private boolean writeRoutes(final Map<String, RouteResult> routes, final String filename) {
		Document xmldoc;
		try {
			xmldoc = getXmlDocument("settings");
		} catch (XmlException ex) {
			LOG.error("Stockpile not saved " + ex.getMessage(), ex);
			return false;
		}
		Element routingNode = xmldoc.createElementNS(null, "routingsettings");
		xmldoc.getDocumentElement().appendChild(routingNode);
		writeRoutes(xmldoc, routingNode, routes);
		try {
			writeXmlFile(xmldoc, filename, false);
		} catch (XmlException ex) {
			LOG.error("Stockpile not saved " + ex.getMessage(), ex);
			return false;
		}
		LOG.info("Stockpile saved");
		return true;
	}

	private boolean write(final Settings settings, final String filename) {
		Document xmldoc;
		try {
			xmldoc = getXmlDocument("settings");
		} catch (XmlException ex) {
			LOG.error("Settings not saved " + ex.getMessage(), ex);
			return false;
		}
		//Add version number
		setAttribute(xmldoc.getDocumentElement(), "version", SettingsReader.SETTINGS_VERSION);

		writeAssetSettings(xmldoc, settings);
		writeStockpileGroups(xmldoc, settings);
		writeStockpiles(xmldoc, settings.getStockpiles(), false);
		writeOverviewGroups(xmldoc, settings.getOverviewGroups());
		writeReprocessSettings(xmldoc, settings.getReprocessSettings());
		writeWindow(xmldoc, settings);
		writeProxy(xmldoc, settings.getProxyData());
		writePriceDataSettings(xmldoc, settings.getPriceDataSettings());
		writeContractPriceSettings(xmldoc, settings.getContractPriceSettings());
		writeFlags(xmldoc, settings.getFlags());
		writeUserPrices(xmldoc, settings.getUserPrices());
		writeUserItemNames(xmldoc, settings.getUserItemNames());
		writeEveNames(xmldoc, settings.getEveNames());
		writeTableFilters(xmldoc, settings.getTableFilters());
		writeCurrentTableFilters(xmldoc, settings.getCurrentTableFilters(), settings.getCurrentTableFiltersShown());
		writeTableColumns(xmldoc, settings.getTableColumns());
		writeTableColumnsWidth(xmldoc, settings.getTableColumnsWidth());
		writeTablesResize(xmldoc, settings.getTableResize());
		writeTablesViews(xmldoc, settings.getTableViews());
		writeTablesJumps(xmldoc, settings.getTableJumps());
		writeTablesFormula(xmldoc, settings.getTableFormulas());
		writeExportSettings(xmldoc, settings.getExportSettings(), settings.getCopySettings());
		writeTrackerNotes(xmldoc, settings.getTrackerSettings().getNotes());
		writeTrackerFilters(xmldoc, settings.getTrackerSettings().getFilters(), settings.getTrackerSettings().isSelectNew(), settings.getTrackerSettings().getSkillPointFilters());
		writeTrackerSettings(xmldoc, settings);
		writeOwners(xmldoc, settings.getOwners(), settings.getOwnersNextUpdate());
		writeTags(xmldoc, settings.getTags());
		writeRoutingSettings(xmldoc, settings.getRoutingSettings());
		writeMarketOrderOutbid(xmldoc, settings.getPublicMarketOrdersNextUpdate(), settings.getPublicMarketOrdersLastUpdate(), settings.getOutbidOrderRange(), settings.getMarketOrdersOutbid());
		writeMarketOrdersSettings(xmldoc, settings.getMarketOrdersSettings());
		writeShowTool(xmldoc, settings.getShowTools(), settings.isSaveToolsOnExit());
		writeColorSettings(xmldoc, settings.getColorSettings());
		writeFactionWarfareSystemOwners(xmldoc, settings);
		try {
			writeXmlFile(xmldoc, filename, true);
		} catch (XmlException ex) {
			LOG.error("Settings not saved " + ex.getMessage(), ex);
			return false;
		}
		LOG.info("Settings saved");
		return true;
	}

	private void writeFactionWarfareSystemOwners(Document xmldoc, Settings settings) {
		Element FactionWarfareSystemOwnersNode = xmldoc.createElementNS(null, "factionwarfaresystemowners");
		xmldoc.getDocumentElement().appendChild(FactionWarfareSystemOwnersNode);
		setAttribute(FactionWarfareSystemOwnersNode, "factionwarfarenextupdate", settings.getFactionWarfareNextUpdate());
		for (Map.Entry<Long, String> entry : settings.getFactionWarfareSystemOwners().entrySet()) {
			Element colorNode = xmldoc.createElementNS(null, "system");
			setAttribute(colorNode, "system", entry.getKey());
			setAttributeOptional(colorNode, "faction", entry.getValue());
			FactionWarfareSystemOwnersNode.appendChild(colorNode);
		}
	}

	private void writeColorSettings(Document xmldoc, ColorSettings colorSettings) {
		Element colorSettingsNode = xmldoc.createElementNS(null, "colorsettings");
		xmldoc.getDocumentElement().appendChild(colorSettingsNode);
		setAttributeOptional(colorSettingsNode, "theme", colorSettings.getColorTheme().getType());
		setAttribute(colorSettingsNode, "lookandfeel", colorSettings.getLookAndFeelClass());
		for (ColorEntry colorEntry : ColorEntry.values()) {
			Element colorNode = xmldoc.createElementNS(null, "color");
			setAttribute(colorNode, "name", colorEntry);
			setAttributeOptional(colorNode, "background", colorSettings.getBackground(colorEntry));
			setAttributeOptional(colorNode, "foreground", colorSettings.getForeground(colorEntry));
			colorSettingsNode.appendChild(colorNode);
		}
	}

	private void writeShowTool(Document xmldoc, List<String> showTools, boolean saveToolsOnExit) {
		Element showToolsNode = xmldoc.createElementNS(null, "showtools");
		xmldoc.getDocumentElement().appendChild(showToolsNode);
		setAttribute(showToolsNode, "saveonexit", saveToolsOnExit);
		setAttribute(showToolsNode, "show", showTools);
	}

	private void writeMarketOrderOutbid(Document xmldoc, Date publicMarketOrdersNextUpdate, Date publicMarketOrdersLastUpdate, MarketOrderRange outbidOrderRange, Map<Long, Outbid> marketOrdersOutbid) {
		Element marketOrderOutbidNode = xmldoc.createElementNS(null, "marketorderoutbid");
		xmldoc.getDocumentElement().appendChild(marketOrderOutbidNode);
		setAttribute(marketOrderOutbidNode, "nextupdate", publicMarketOrdersNextUpdate);
		setAttributeOptional(marketOrderOutbidNode, "lastupdate", publicMarketOrdersLastUpdate);
		setAttribute(marketOrderOutbidNode, "outbidorderrange", outbidOrderRange);
		for (Map.Entry<Long, Outbid> entry : marketOrdersOutbid.entrySet()) {
			Element outbidNode = xmldoc.createElementNS(null, "outbid");
			setAttribute(outbidNode, "id", entry.getKey());
			setAttribute(outbidNode, "price", entry.getValue().getPrice());
			setAttribute(outbidNode, "count", entry.getValue().getCount());
			marketOrderOutbidNode.appendChild(outbidNode);
		}
	}

	private void writeRoutingSettings(Document xmldoc, RoutingSettings routingSettings) {
		Element routingNode = xmldoc.createElementNS(null, "routingsettings");
		xmldoc.getDocumentElement().appendChild(routingNode);
		setAttribute(routingNode, "securitymaximum", routingSettings.getSecMax());
		setAttribute(routingNode, "securityminimum", routingSettings.getSecMin());
		for (long systemID : routingSettings.getAvoid().keySet()) {
			Element systemNode = xmldoc.createElementNS(null, "routingsystem");
			setAttribute(systemNode, "id", systemID);
			routingNode.appendChild(systemNode);
		}
		for (Map.Entry<String, Set<Long>> entry : routingSettings.getPresets().entrySet()) {
			Element presetNode = xmldoc.createElementNS(null, "routingpreset");
			setAttribute(presetNode, "name", entry.getKey());
			routingNode.appendChild(presetNode);
			for (Long systemID : entry.getValue()) {
				Element systemNode = xmldoc.createElementNS(null, "presetsystem");
				setAttribute(systemNode, "id", systemID);
				presetNode.appendChild(systemNode);
			}
		}
		writeRoutes(xmldoc, routingNode, routingSettings.getRoutes());
	}

	private void writeRoutes(Document xmldoc, Element routingNode, Map<String, RouteResult> routes) {
		for (Map.Entry<String, RouteResult> entry : routes.entrySet()) {
			Element routeNode = xmldoc.createElementNS(null, "route");
			RouteResult routeResult = entry.getValue();
			setAttribute(routeNode, "name", entry.getKey());
			setAttribute(routeNode, "waypoints", routeResult.getWaypoints());
			setAttribute(routeNode, "algorithmname", routeResult.getAlgorithmName());
			setAttribute(routeNode, "algorithmtime",routeResult.getAlgorithmTime());
			setAttribute(routeNode, "jumps", routeResult.getJumps());
			setAttribute(routeNode, "avoid", routeResult.getAvoid());
			setAttribute(routeNode, "security", routeResult.getSecurity());
			routingNode.appendChild(routeNode);
			for (List<SolarSystem> systems : routeResult.getRoute()) {
				Element systemsNode = xmldoc.createElementNS(null, "routesystems");
				for (SolarSystem system : systems) {
					Element systemNode = xmldoc.createElementNS(null, "routesystem");
					setAttribute(systemNode, "systemid", system.getSystemID());
					systemsNode.appendChild(systemNode);
				}
				List<SolarSystem> stations = routeResult.getStations().get(systems.get(0).getSystemID());
				if (stations != null) {
					for (SolarSystem station : stations) {
						Element stationNode = xmldoc.createElementNS(null, "routestation");
						setAttribute(stationNode, "stationid", station.getLocationID());
						systemsNode.appendChild(stationNode);
					}
				}
				routeNode.appendChild(systemsNode);
			}
		}
	}

	private void writeTags(Document xmldoc, Map<String, Tag> tags) {
		Element tagsNode = xmldoc.createElementNS(null, "tags");
		xmldoc.getDocumentElement().appendChild(tagsNode);
		for (Tag tag : tags.values()) {
			Element tagNode = xmldoc.createElementNS(null, "tag");
			setAttribute(tagNode, "name", tag.getName());
			setAttribute(tagNode, "background", tag.getColor().getBackgroundHtml());
			setAttribute(tagNode, "foreground", tag.getColor().getForegroundHtml());
			tagsNode.appendChild(tagNode);
			for (TagID tagID : tag.getIDs()) {
				Element tagIdNode = xmldoc.createElementNS(null, "tagid");
				setAttribute(tagIdNode, "tool", tagID.getTool());
				setAttribute(tagIdNode, "id", tagID.getID());
				tagNode.appendChild(tagIdNode);
			}
		}
	}

	private void writeOwners(final Document xmldoc, final Map<Long, String> owners, final Map<Long, Date> ownersNextUpdate) {
		Element trackerDataNode = xmldoc.createElementNS(null, "owners");
		xmldoc.getDocumentElement().appendChild(trackerDataNode);
		for (Map.Entry<Long, String> entry : owners.entrySet()) {
			Element ownerNode = xmldoc.createElementNS(null, "owner");
			setAttribute(ownerNode, "name", entry.getValue());
			setAttribute(ownerNode, "id", entry.getKey());
			setAttributeOptional(ownerNode, "date", ownersNextUpdate.get(entry.getKey()));
			trackerDataNode.appendChild(ownerNode);
		}
	}

	private void writeTrackerFilters(final Document xmldoc, final Map<String, Boolean> trackerFilters, boolean selectNew, Map<String, TrackerSkillPointFilter> trackerSkillPointFilters) {
		Element trackerDataNode = xmldoc.createElementNS(null, "trackerfilters");
		xmldoc.getDocumentElement().appendChild(trackerDataNode);
		setAttribute(trackerDataNode, "selectnew", selectNew);
		for (Map.Entry<String, Boolean> entry : trackerFilters.entrySet()) {
			Element ownerNode = xmldoc.createElementNS(null, "trackerfilter");
			setAttribute(ownerNode, "id", entry.getKey());
			setAttribute(ownerNode, "selected", entry.getValue());
			trackerDataNode.appendChild(ownerNode);
		}
		for (Map.Entry<String, TrackerSkillPointFilter> entry : trackerSkillPointFilters.entrySet()) {
			Element ownerNode = xmldoc.createElementNS(null, "skillpointfilters");
			TrackerSkillPointFilter filter = entry.getValue();
			setAttribute(ownerNode, "id", entry.getKey());
			setAttribute(ownerNode, "selected", filter.isEnabled());
			setAttribute(ownerNode, "mimimum", filter.getMinimum());
			trackerDataNode.appendChild(ownerNode);
		}
	}

	private void writeTrackerSettings(final Document xmldoc,  Settings settings) {
		Element trackerSettingsNode = xmldoc.createElementNS(null, "trackersettings");
		xmldoc.getDocumentElement().appendChild(trackerSettingsNode);
		setAttribute(trackerSettingsNode, "allprofiles", settings.getTrackerSettings().isAllProfiles());
		setAttribute(trackerSettingsNode, "charactercorporations", settings.getTrackerSettings().isCharacterCorporations());
		setAttributeOptional(trackerSettingsNode, "selectedowners", settings.getTrackerSettings().getSelectedOwners());
		setAttributeOptional(trackerSettingsNode, "fromdate", settings.getTrackerSettings().getFromDate());
		setAttributeOptional(trackerSettingsNode, "todate", settings.getTrackerSettings().getToDate());
		setAttribute(trackerSettingsNode, "displaytype", settings.getTrackerSettings().getDisplayType());
		setAttribute(trackerSettingsNode, "includezero", settings.getTrackerSettings().isIncludeZero());
		setAttribute(trackerSettingsNode, "showoptions", settings.getTrackerSettings().getShowOptions());
	}

	private void writeTrackerNotes(final Document xmldoc, final Map<TrackerDate, TrackerNote> trackerNotes) {
		Element notesNode = xmldoc.createElementNS(null, "trackernotes");
		xmldoc.getDocumentElement().appendChild(notesNode);
		for (Map.Entry<TrackerDate, TrackerNote> entry : trackerNotes.entrySet()) {
			Element noteNode = xmldoc.createElementNS(null, "trackernote");
			setAttribute(noteNode, "note", entry.getValue().getNote());
			setAttribute(noteNode, "date", entry.getKey().getDate());
			notesNode.appendChild(noteNode);
		}
	}

	/***
	 * Write setting for table filters to the xml settings document 'tablefilters' element.
	 *
	 * @param xmldoc Settings document to write to.
	 * @param tableFilters Saved filters to be written to the document zero to many for each table.
	 */
	private void writeTableFilters(final Document xmldoc, final Map<String, Map<String, List<Filter>>> tableFilters) {
		Element tablefiltersNode = xmldoc.createElementNS(null, "tablefilters");
		xmldoc.getDocumentElement().appendChild(tablefiltersNode);
		for (Map.Entry<String, Map<String, List<Filter>>> entry : tableFilters.entrySet()) {
			Element nameNode = xmldoc.createElementNS(null, "table");
			setAttribute(nameNode, "name", entry.getKey());
			tablefiltersNode.appendChild(nameNode);
			for (Map.Entry<String, List<Filter>> filters : entry.getValue().entrySet()) {
				Element filterNode = xmldoc.createElementNS(null, "filter");
				setAttribute(filterNode, "name", filters.getKey());
				nameNode.appendChild(filterNode);
				writeFilters(xmldoc, filterNode, filters);
			}
		}
	}

	/***
	 * Write setting for current table filters to the xml settings document 'currnettablefilters' element.
	 *
	 * @param xmldoc Settings document to write to.
	 * @param tableFilters Current filters to be written to the document one per table.
	 * @param tableFiltersShow Current filters visibility state to be written to the document one per table.
	 */
	private void writeCurrentTableFilters(final Document xmldoc, final Map<String, List<Filter>> tableFilters, final Map<String, Boolean> tableFiltersShow) {
		Element currenttablefiltersNode = xmldoc.createElementNS(null, "currenttablefilters");
		xmldoc.getDocumentElement().appendChild(currenttablefiltersNode);
		for (Map.Entry<String, List<Filter>> filters : tableFilters.entrySet()) {
			Element nameNode = xmldoc.createElementNS(null, "table");
			setAttribute(nameNode, "name", filters.getKey());
			currenttablefiltersNode.appendChild(nameNode);
			Element filterNode = xmldoc.createElementNS(null, "filter");
			setAttribute(filterNode, "show", tableFiltersShow.getOrDefault(filters.getKey(), true));
			nameNode.appendChild(filterNode);
			writeFilters(xmldoc, filterNode, filters);
		}
	}

	/***
	 * Write settings for individual filters rows to the xml settings document.
	 *
	 * @param xmldoc Settings document to write to.
	 * @param parentNode Node of the xml document to write the filter to.
	 * @param filters Filter to be written to the document row by row.
	 */
	private void writeFilters(final Document xmldoc, final Element parentNode, final Map.Entry<String, List<Filter>> filters) {
		for (Filter filter : filters.getValue()) {
			Element childNode = xmldoc.createElementNS(null, "row");
			setAttribute(childNode, "group", filter.getGroup());
			setAttribute(childNode, "text", filter.getText());
			setAttribute(childNode, "column", filter.getColumn().name());
			setAttribute(childNode, "compare", filter.getCompareType());
			setAttribute(childNode, "logic", filter.getLogic());
			setAttribute(childNode, "enabled", filter.isEnabled());
			parentNode.appendChild(childNode);
		}
	}

	private void writeTableColumns(final Document xmldoc, final Map<String, List<SimpleColumn>> tableColumns) {
		Element tablecolumnsNode = xmldoc.createElementNS(null, "tablecolumns");
		xmldoc.getDocumentElement().appendChild(tablecolumnsNode);
		for (Map.Entry<String, List<SimpleColumn>> entry : tableColumns.entrySet()) {
			Element nameNode = xmldoc.createElementNS(null, "table");
			setAttribute(nameNode, "name", entry.getKey());
			tablecolumnsNode.appendChild(nameNode);
			for (SimpleColumn column : entry.getValue()) {
				Element node = xmldoc.createElementNS(null, "column");
				setAttribute(node, "name", column.getEnumName());
				setAttribute(node, "shown", column.isShown());
				nameNode.appendChild(node);
			}
		}
	}

	private void writeTableColumnsWidth(final Document xmldoc, final Map<String, Map<String, Integer>> tableColumnsWidth) {
		Element tablecolumnsNode = xmldoc.createElementNS(null, "tablecolumnswidth");
		xmldoc.getDocumentElement().appendChild(tablecolumnsNode);
		for (Map.Entry<String, Map<String, Integer>> table : tableColumnsWidth.entrySet()) {
			Element nameNode = xmldoc.createElementNS(null, "table");
			setAttribute(nameNode, "name", table.getKey());
			tablecolumnsNode.appendChild(nameNode);
			for (Map.Entry<String, Integer> column : table.getValue().entrySet()) {
				Element node = xmldoc.createElementNS(null, "column");
				setAttribute(node, "column", column.getKey());
				setAttribute(node, "width", column.getValue());
				nameNode.appendChild(node);
			}
		}
	}

	private void writeTablesResize(final Document xmldoc, final Map<String, ResizeMode> tableColumns) {
		Element tablecolumnsNode = xmldoc.createElementNS(null, "tableresize");
		xmldoc.getDocumentElement().appendChild(tablecolumnsNode);
		for (Map.Entry<String, ResizeMode> entry : tableColumns.entrySet()) {
			Element nameNode = xmldoc.createElementNS(null, "table");
			setAttribute(nameNode, "name", entry.getKey());
			setAttribute(nameNode, "resize", entry.getValue());
			tablecolumnsNode.appendChild(nameNode);
		}
	}

	private void writeTablesViews(final Document xmldoc, final Map<String, Map<String ,View>> tableViews) {
		Element tableviewsNode = xmldoc.createElementNS(null, "tableviews");
		xmldoc.getDocumentElement().appendChild(tableviewsNode);
		for (Map.Entry<String, Map<String ,View>> entry : tableViews.entrySet()) {
			Element nameNode = xmldoc.createElementNS(null, "viewtool");
			setAttribute(nameNode, "tool", entry.getKey());
			tableviewsNode.appendChild(nameNode);
			for (View view : entry.getValue().values()) {
				Element tableviewNode = xmldoc.createElementNS(null, "view");
				setAttribute(tableviewNode, "name", view.getName());
				nameNode.appendChild(tableviewNode);
				for (SimpleColumn column : view.getColumns()) {
					Element viewColumnNode = xmldoc.createElementNS(null, "viewcolumn");
					setAttribute(viewColumnNode, "name", column.getEnumName());
					setAttribute(viewColumnNode, "shown", column.isShown());
					tableviewNode.appendChild(viewColumnNode);
				}
			}
			
		}
	}

	private void writeTablesFormula(final Document xmldoc, final Map<String, List<Formula>> formulas) {
		Element tableFormulasNode = xmldoc.createElementNS(null, "tableformulas");
		xmldoc.getDocumentElement().appendChild(tableFormulasNode);
		for (Map.Entry<String, List<Formula>> entry : formulas.entrySet()) {
			Element formulasNode = xmldoc.createElementNS(null, "formulas");
			setAttribute(formulasNode, "tool", entry.getKey());
			tableFormulasNode.appendChild(formulasNode);
			for (Formula formula : entry.getValue()) {
				Element formulaNode = xmldoc.createElementNS(null, "formula");
				setAttribute(formulaNode, "name", formula.getColumnName());
				setAttribute(formulaNode, "expression", formula.getOriginalExpression());
				setAttributeOptional(formulaNode, "index", formula.getIndex());
				formulasNode.appendChild(formulaNode);
			}
		}
	}

	private void writeTablesJumps(final Document xmldoc, final Map<String, List<Jump>> jumps) {
		Element tableJumpsNode = xmldoc.createElementNS(null, "tablejumps");
		xmldoc.getDocumentElement().appendChild(tableJumpsNode);
		for (Map.Entry<String, List<Jump>> entry : jumps.entrySet()) {
			Element jumpsNode = xmldoc.createElementNS(null, "jumps");
			setAttribute(jumpsNode, "tool", entry.getKey());
			tableJumpsNode.appendChild(jumpsNode);
			for (Jump jump : entry.getValue()) {
				Element jumpNode = xmldoc.createElementNS(null, "jump");
				setAttribute(jumpNode, "systemid", jump.getSystemID());
				setAttributeOptional(jumpNode, "index", jump.getIndex());
				jumpsNode.appendChild(jumpNode);
			}
		}
	}

	private void writeAssetSettings(final Document xmldoc, final Settings settings) {
		Element parentNode = xmldoc.createElementNS(null, "assetsettings");
		xmldoc.getDocumentElement().appendChild(parentNode);
		setAttribute(parentNode, "maximumpurchaseage", settings.getMaximumPurchaseAge());
		setAttribute(parentNode, "transactionprofitprice", settings.getTransactionProfitPrice());
		setAttribute(parentNode, "transactionprofitmargin", settings.getTransactionProfitMargin());
	}

	private void writeStockpileGroups(final Document xmldoc, final Settings settings) {
		Element parentNode = xmldoc.createElementNS(null, "stockpilegroups");
		xmldoc.getDocumentElement().appendChild(parentNode);
		setAttribute(parentNode, "stockpilegroup2", settings.getStockpileColorGroup2());
		setAttribute(parentNode, "stockpilegroup3", settings.getStockpileColorGroup3());
	}

	/**
	 * -!- `!´ IMPORTANT `!´ -!-
	 * StockpileDataWriter and StockpileDataReader needs to be updated too - on any changes!!!
	 */
	private void writeStockpiles(final Document xmldoc, final List<Stockpile> stockpiles, boolean export) {
		Element parentNode = xmldoc.createElementNS(null, "stockpiles");
		xmldoc.getDocumentElement().appendChild(parentNode);
		for (Stockpile strockpile : stockpiles) {
			//STOCKPILE
			Element strockpileNode = xmldoc.createElementNS(null, "stockpile");
			setAttribute(strockpileNode, "name", strockpile.getName());
			if (!export) { //Risk of collision, better to generate a new one on import
				setAttribute(strockpileNode, "id", strockpile.getId());
			}
			setAttribute(strockpileNode, "multiplier", strockpile.getMultiplier());
			//ITEMS
			for (StockpileItem item : strockpile.getItems()) {
				if (item.getItemTypeID() != 0) { //Ignore Total
					Element itemNode = xmldoc.createElementNS(null, "item");
					if (!export) { //Risk of collision, better to generate a new one on import
						setAttribute(itemNode, "id", item.getID());
					}
					setAttribute(itemNode, "typeid", item.getItemTypeID());
					setAttribute(itemNode, "minimum", item.getCountMinimum());
					setAttribute(itemNode, "runs", item.isRuns());
					strockpileNode.appendChild(itemNode);
				}
			}
			//SUBPILES
			for (Map.Entry<Stockpile, Double> entry : strockpile.getSubpiles().entrySet()) {
				Element itemNode = xmldoc.createElementNS(null, "subpile");
				itemNode.setAttributeNS(null, "name", entry.getKey().getName());
				itemNode.setAttributeNS(null, "minimum", String.valueOf(entry.getValue()));
				strockpileNode.appendChild(itemNode);
			}
			//FILTERS
			for (StockpileFilter filter : strockpile.getFilters()) {
				Element locationNode = xmldoc.createElementNS(null, "stockpilefilter");
				setAttribute(locationNode, "locationid", filter.getLocation().getLocationID());
				setAttribute(locationNode, "sellingcontracts", filter.isSellingContracts());
				setAttribute(locationNode, "soldcontracts", filter.isSoldContracts());
				setAttribute(locationNode, "buyingcontracts", filter.isBuyingContracts());
				setAttribute(locationNode, "boughtcontracts", filter.isBoughtContracts());
				setAttribute(locationNode, "exclude", filter.isExclude());
				setAttributeOptional(locationNode, "singleton", filter.isSingleton());
				setAttribute(locationNode, "inventory", filter.isAssets());
				setAttribute(locationNode, "sellorders", filter.isSellOrders());
				setAttribute(locationNode, "buyorders", filter.isBuyOrders());
				setAttribute(locationNode, "buytransactions", filter.isBuyTransactions());
				setAttribute(locationNode, "selltransactions", filter.isSellTransactions());
				setAttribute(locationNode, "jobs", filter.isJobs());
				strockpileNode.appendChild(locationNode);
				for (Long ownerID : filter.getOwnerIDs()) {
					Element ownerNode = xmldoc.createElementNS(null, "owner");
					setAttribute(ownerNode, "ownerid", ownerID);
					locationNode.appendChild(ownerNode);
				}
				for (StockpileFilter.StockpileContainer container : filter.getContainers()) {
					Element containerNode = xmldoc.createElementNS(null, "container");
					setAttribute(containerNode, "container", container.getContainer());
					setAttribute(containerNode, "includecontainer", container.isIncludeContainer());
					locationNode.appendChild(containerNode);
				}
				for (Integer flagID : filter.getFlagIDs()) {
					Element flagNode = xmldoc.createElementNS(null, "flag");
					setAttribute(flagNode, "flagid", flagID);
					locationNode.appendChild(flagNode);
				}
			}
			parentNode.appendChild(strockpileNode);
		}
	}

	private void writeOverviewGroups(final Document xmldoc, final Map<String, OverviewGroup> overviewGroups) {
		Element parentNode = xmldoc.createElementNS(null, "overview");
		xmldoc.getDocumentElement().appendChild(parentNode);
		for (Map.Entry<String, OverviewGroup> entry : overviewGroups.entrySet()) {
			OverviewGroup overviewGroup = entry.getValue();
			Element node = xmldoc.createElementNS(null, "group");
			setAttribute(node, "name", overviewGroup.getName());
			parentNode.appendChild(node);
			for (OverviewLocation location : overviewGroup.getLocations()) {
				Element nodeLocation = xmldoc.createElementNS(null, "location");
				setAttribute(nodeLocation, "name", location.getName());
				setAttribute(nodeLocation, "type", location.getType());
				node.appendChild(nodeLocation);
			}
		}
	}

	private void writeUserItemNames(final Document xmldoc, final Map<Long, UserItem<Long, String>> userPrices) {
		Element parentNode = xmldoc.createElementNS(null, "itemmames");
		xmldoc.getDocumentElement().appendChild(parentNode);
		for (Map.Entry<Long, UserItem<Long, String>> entry : userPrices.entrySet()) {
			UserItem<Long, String> userItemName = entry.getValue();
			Element node = xmldoc.createElementNS(null, "itemname");
			setAttribute(node, "name", userItemName.getValue());
			setAttribute(node, "typename", userItemName.getName());
			setAttribute(node, "itemid", userItemName.getKey());
			parentNode.appendChild(node);
		}
	}

	private void writeEveNames(final Document xmldoc, final Map<Long, String> eveNames) {
		Element parentNode = xmldoc.createElementNS(null, "evenames");
		xmldoc.getDocumentElement().appendChild(parentNode);
		for (Map.Entry<Long, String> entry : eveNames.entrySet()) {
			Element node = xmldoc.createElementNS(null, "evename");
			setAttribute(node, "name", entry.getValue());
			setAttribute(node, "itemid", entry.getKey());
			parentNode.appendChild(node);
		}
	}

	private void writeReprocessSettings(final Document xmldoc, final ReprocessSettings reprocessSettings) {
		Element parentNode = xmldoc.createElementNS(null, "reprocessing");
		xmldoc.getDocumentElement().appendChild(parentNode);
		setAttribute(parentNode, "refining", reprocessSettings.getReprocessingLevel());
		setAttribute(parentNode, "efficiency", reprocessSettings.getReprocessingEfficiencyLevel());
		setAttribute(parentNode, "processing", reprocessSettings.getScrapmetalProcessingLevel());
		setAttribute(parentNode, "station", reprocessSettings.getStation());
	}

	private void writeWindow(final Document xmldoc, final Settings settings) {
		Element parentNode = xmldoc.createElementNS(null, "window");
		xmldoc.getDocumentElement().appendChild(parentNode);
		setAttribute(parentNode, "x", settings.getWindowLocation().x);
		setAttribute(parentNode, "y", settings.getWindowLocation().y);
		setAttribute(parentNode, "height", settings.getWindowSize().height);
		setAttribute(parentNode, "width", settings.getWindowSize().width);
		setAttribute(parentNode, "maximized", settings.isWindowMaximized());
		setAttribute(parentNode, "autosave", settings.isWindowAutoSave());
		setAttribute(parentNode, "alwaysontop", settings.isWindowAlwaysOnTop());
	}

	private void writeUserPrices(final Document xmldoc, final Map<Integer, UserItem<Integer, Double>> userPrices) {
		Element parentNode = xmldoc.createElementNS(null, "userprices");
		xmldoc.getDocumentElement().appendChild(parentNode);
		for (Map.Entry<Integer, UserItem<Integer, Double>> entry : userPrices.entrySet()) {
			UserItem<Integer, Double> userPrice = entry.getValue();
			Element node = xmldoc.createElementNS(null, "userprice");
			setAttribute(node, "name", userPrice.getName());
			setAttribute(node, "price", userPrice.getValue());
			setAttribute(node, "typeid", userPrice.getKey());
			parentNode.appendChild(node);
		}
	}
	private void writePriceDataSettings(final Document xmldoc, final PriceDataSettings priceDataSettings) {
		Element parentNode = xmldoc.createElementNS(null, "marketstat");
		setAttribute(parentNode, "defaultprice", priceDataSettings.getPriceType());
		setAttribute(parentNode, "defaultreprocessedprice", priceDataSettings.getPriceReprocessedType());
		setAttribute(parentNode, "pricesource", priceDataSettings.getSource());
		setAttribute(parentNode, "locationid", priceDataSettings.getLocationID());
		setAttribute(parentNode, "type", priceDataSettings.getLocationType());
		xmldoc.getDocumentElement().appendChild(parentNode);
	}

	private void writeContractPriceSettings(final Document xmldoc, final ContractPriceSettings contractPriceSettings) {
		Element parentNode = xmldoc.createElementNS(null, "contractpricesettings");
		setAttribute(parentNode, "includeprivate", contractPriceSettings.isIncludePrivate());
		setAttribute(parentNode, "defaultbpc", contractPriceSettings.isDefaultBPC());
		setAttribute(parentNode, "mode", contractPriceSettings.getContractPriceMode());
		setAttribute(parentNode, "sec", contractPriceSettings.getSecurityString());
		setAttribute(parentNode, "feedback", contractPriceSettings.isFeedback());
		setAttribute(parentNode, "feedbackasked", contractPriceSettings.isFeedbackAsked());
		xmldoc.getDocumentElement().appendChild(parentNode);
	}

	private void writeMarketOrdersSettings(final Document xmldoc, final MarketOrdersSettings marketOrdersSettings) {
		Element parentNode = xmldoc.createElementNS(null, "marketorderssettings");
		setAttribute(parentNode, "expirewarndays", marketOrdersSettings.getExpireWarnDays());
		setAttribute(parentNode, "remainingwarnpercent", marketOrdersSettings.getRemainingWarnPercent());
		xmldoc.getDocumentElement().appendChild(parentNode);
	}

	private void writeFlags(final Document xmldoc, final Map<SettingFlag, Boolean> flags) {
		Element parentNode = xmldoc.createElementNS(null, "flags");
		xmldoc.getDocumentElement().appendChild(parentNode);
		for (Map.Entry<SettingFlag, Boolean> entry : flags.entrySet()) {
			Element node = xmldoc.createElementNS(null, "flag");
			setAttribute(node, "key", entry.getKey());
			setAttribute(node, "enabled", entry.getValue());
			parentNode.appendChild(node);
		}
	}

	private void writeProxy(final Document xmldoc, final ProxyData proxy) {
		if (proxy.getType() != Proxy.Type.DIRECT) { // Only adds proxy tag if there is anything to save... (To prevent an error when the proxy tag doesn't have any attributes)
			Element node = xmldoc.createElementNS(null, "proxy");
			setAttribute(node, "address", proxy.getAddress());
			setAttribute(node, "port", proxy.getPort());
			setAttribute(node, "type", proxy.getType());
			if (proxy.isAuth()) {
				setAttribute(node, "username", proxy.getUsername());
				setAttribute(node, "password", proxy.getPassword());
			}
			xmldoc.getDocumentElement().appendChild(node);
		}
	}

	private void writeExportSettings(final Document xmldoc, final Map<String, ExportSettings> exportSettings, final CopySettings copySettings) {
		Element node = xmldoc.createElementNS(null, "exports");
		//Copy
		setAttribute(node, "copy", copySettings.getCopyDecimalSeparator());

		for(Map.Entry<String, ExportSettings> exportSetting : exportSettings.entrySet()) {
			//Common
			Element exportNode = xmldoc.createElementNS(null, "export");
			setAttribute(exportNode, "name", exportSetting.getKey());
			setAttributeOptional(exportNode, "exportformat", exportSetting.getValue().getExportFormat());
			setAttributeOptional(exportNode, "filename", exportSetting.getValue().getFilename());
			setAttributeOptional(exportNode, "columnselection", exportSetting.getValue().getColumnSelection());
			setAttributeOptional(exportNode, "viewname", exportSetting.getValue().getViewName());
			setAttributeOptional(exportNode, "filterselection", exportSetting.getValue().getFilterSelection());
			setAttributeOptional(exportNode, "filtername", exportSetting.getValue().getFilterName());
			node.appendChild(exportNode);

			if (exportSetting.getValue().getTableExportColumns() != null &&
					!exportSetting.getValue().getTableExportColumns().isEmpty()) {

				Element tableNode = xmldoc.createElementNS(null, "table");
				for (String column : exportSetting.getValue().getTableExportColumns()) {
					Element columnNode = xmldoc.createElementNS(null, "column");
					setAttribute(columnNode, "name", column);
					tableNode.appendChild(columnNode);
				}
				exportNode.appendChild(tableNode);
			}

			//CSV
			Element csvNode = xmldoc.createElementNS(null, "csv");
			setAttribute(csvNode, "decimal", exportSetting.getValue().getCsvDecimalSeparator());
			setAttribute(csvNode, "field", exportSetting.getValue().getFieldDelimiter());
			setAttribute(csvNode, "line", exportSetting.getValue().getLineDelimiter());
			exportNode.appendChild(csvNode);

			//SQL
			Element sqlNode = xmldoc.createElementNS(null, "sql");
			setAttribute(sqlNode, "tablename", exportSetting.getValue().getTableName());
			setAttribute(sqlNode, "createtable", exportSetting.getValue().isCreateTable());
			setAttribute(sqlNode, "droptable", exportSetting.getValue().isDropTable());
			setAttribute(sqlNode, "extendedinserts", exportSetting.getValue().isExtendedInserts());
			exportNode.appendChild(sqlNode);

			//Html
			Element htmlNode = xmldoc.createElementNS(null, "html");
			setAttribute(htmlNode, "styled", exportSetting.getValue().isHtmlStyled());
			setAttribute(htmlNode, "igb", exportSetting.getValue().isHtmlIGB());
			setAttribute(htmlNode, "repeatheader", exportSetting.getValue().getHtmlRepeatHeader());
			exportNode.appendChild(htmlNode);

		}
		xmldoc.getDocumentElement().appendChild(node);
	}
}
