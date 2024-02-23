package com.bbn.cot.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.config.DataFeed;
import com.bbn.marti.config.GeospatialFilter;
import com.bbn.marti.config.GeospatialFilter.BoundingBox;
import com.bbn.marti.feeds.DataFeedService;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.InputMetric;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.service.SubmissionService;
import com.bbn.marti.service.SubscriptionStore;
import com.bbn.marti.sync.model.MinimalMission;
import com.bbn.marti.sync.model.MinimalMissionFeed;
import com.bbn.marti.sync.service.DistributedDataFeedCotService;
import com.bbn.marti.util.GeomUtils;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.bbn.marti.remote.util.SpringContextBeanForApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Strings;

import tak.server.Constants;
import com.bbn.marti.remote.config.CoreConfigFacade;
import tak.server.cot.CotEventContainer;
import tak.server.federation.FigFederateSubscription;
import tak.server.feeds.DataFeedDTO;

/*
 *  
 */
public class DataFeedFilter {
	private static final Logger logger = LoggerFactory.getLogger(DataFeedFilter.class);
	
	private static DataFeedFilter instance = null;
	
	@Autowired
	private DataFeedService dataFeedService;

	@Autowired
	private GroupManager groupManager;

	@Autowired
	private ObjectMapper mapper;
		
	public static DataFeedFilter getInstance() {
		if (instance == null) {
			synchronized (DataFeedFilter.class) {
				if (instance == null) {
					instance = SpringContextBeanForApi.getSpringContext().getBean(DataFeedFilter.class);
				}
			}
		}
		return instance;
	}

	public void filter(CotEventContainer cot, DataFeed dataFeed) {
		
		if (logger.isDebugEnabled()) {
			logger.debug("Calling filter for dataFeed: {}, {}", dataFeed.getName(), dataFeed.getUuid());
		}

		if (cot != null && dataFeed != null) {
			
			cot.setContext(Constants.DATA_FEED_KEY, dataFeed);
			cot.setContext(Constants.DATA_FEED_UUID_KEY, dataFeed.getUuid());
			cot.setContext(Constants.ARCHIVE_EVENT_KEY, dataFeed.isArchive());
			
			Element detailElem = cot.getDocument().getRootElement().element("detail");
			if (detailElem == null) {
				detailElem = DocumentHelper.makeElement(cot.getDocument().getRootElement(), "detail");
            }

			Element sourceElement = DocumentHelper.makeElement(detailElem, "source");
			sourceElement.addAttribute("type", "dataFeed");
			sourceElement.addAttribute("name", dataFeed.getName());
			sourceElement.addAttribute("uid", dataFeed.getUuid());
			
			dataFeed.getTag().forEach(tag-> {
				Element tagElement = DocumentHelper.createElement("tag");
				tagElement.addText(tag);
				sourceElement.add(tagElement);
			});

			if (dataFeed.getFiltergroup() != null && !dataFeed.getFiltergroup().isEmpty()) {
				NavigableSet<Group> groups = new ConcurrentSkipListSet<>();
				dataFeed.getFiltergroup().forEach(groupName ->
						groups.add(groupManager.hydrateGroup(new Group(groupName, Direction.IN))));

				if (logger.isDebugEnabled()) {
					logger.debug("Setting groups on datafeed cot: group count {} groups: {}", groups.size(), groups);
				}

				cot.setContext(Constants.GROUPS_KEY, groups);
			}

			// submit data feed message for in memory caching			
			DistributedDataFeedCotService.getInstance().cacheDataFeedEvent(dataFeed, cot);
						
			// if vbm is enabled, only broker messages to clients subscribed to a mission that is linked to this data feed	
			// this is achieved by adding an explicit endpoint for the cot, meaning it won't hit implicit brokering
			if (CoreConfigFacade.getInstance().getRemoteConfiguration().getVbm().isEnabled()) {

				String dataFeedUuid = (String) cot.getContextValue(Constants.DATA_FEED_UUID_KEY);
				
				if (logger.isTraceEnabled()) {
					logger.trace("data.feed.uuid value from message:" + dataFeedUuid);
				}
				
				List<MinimalMission> feedMissions = new ArrayList<>();
				
				// get missions associated with this data feed
				// deserialize the mission from JSON to address pokey binary marshaller
				try {
					for (String missionJson : MessagingDependencyInjectionProxy.getInstance().missionService().getMinimalMissionsJsonForDataFeed(dataFeedUuid)) {
						if (logger.isDebugEnabled()) {
							logger.debug("missionJson: {}", missionJson);

						}
						MinimalMission m = mapper.readValue(missionJson, MinimalMission.class);
						feedMissions.add(m);
					}
				} catch (JsonProcessingException e) {
					throw new TakException(e);
				}
								
				if (logger.isDebugEnabled()) {
					logger.debug("deserialized " + feedMissions.size() + " feed missions from JSON");
					logger.debug("CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getMissionCopTool(): {}", CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getMissionCopTool());
				}
				
				// if there was a vbm match and we're mission federating, pass the data feed message to each fig client
				boolean vbmMatch = feedMissions.stream().anyMatch(m -> CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getMissionCopTool().equals(m.getTool().toLowerCase()));
				if (vbmMatch && isMissionDataFeedFederation()) {
					
					DataFeedDTO dataFeedDao = dataFeedService.getDataFeedByUid(dataFeed.getUuid());
					
					if (dataFeedDao.getFederated()) { // whether or not to federate per datafeed

						if (logger.isDebugEnabled()) {
							logger.debug("Sending datafeed {} to federation", dataFeed.getUuid());
						}

						// submit data feed message to each fig federate.		
						SubscriptionStore.getInstanceFederatedSubscriptionManager().getFederateSubscriptions().forEach(fedSub -> {
							if (fedSub instanceof FigFederateSubscription) {
								FigFederateSubscription figSub = (FigFederateSubscription) fedSub;
								try {
									figSub.submit(cot);
								} catch (Exception e) {
									logger.error("Could not submit data feed Cot to Fig Sub", e);
								}
							}
						});
					}else {
						if (logger.isDebugEnabled()) {
							logger.debug("Not sending datafeed {} to federation", dataFeed.getUuid());
						}
					}
					
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Not sending datafeed {} to federation because vbmMatch: {}, isMissionDataFeedFederation: {}", dataFeed.getUuid(), vbmMatch, isMissionDataFeedFederation());
					}
				}
				
				List<MinimalMissionFeed> missionFeeds = new ArrayList<>();

				try {
					for (String missionFeedJson : MessagingDependencyInjectionProxy.getInstance().missionService().getMinimalMissionFeedsJsonForDataFeed(dataFeedUuid)) {
						if (logger.isDebugEnabled()) {
							logger.debug("missionFeedJson: {}", missionFeedJson);

						}
						MinimalMissionFeed mf = mapper.readValue(missionFeedJson, MinimalMissionFeed.class);
						missionFeeds.add(mf);
					}
				} catch (JsonProcessingException e) {
					throw new TakException(e);
				}
				
				handleVbmMissions(cot, feedMissions, missionFeeds);
			}
		}
	}
	
	public void filterFederatedDataFeed(CotEventContainer cot) {
		String dataFeedUuid = (String) cot.getContextValue(Constants.DATA_FEED_UUID_KEY);
		DataFeedDTO dataFeed = dataFeedService.getDataFeedByUid(dataFeedUuid);

		// submit data feed message for in memory caching			
		DistributedDataFeedCotService.getInstance().cacheDataFeedEvent(dataFeed.toInput(), cot);

		// if vbm is enabled and we're mission federating, only broker messages to clients subscribed to a mission that is linked to this data feed
		// otherwise, this message will continue through federation brokering
		if (isVbm() && isMissionDataFeedFederation()) {
			if (dataFeed != null && dataFeed.getFederated()) {
				// update fed feed counter
				InputMetric metric = SubmissionService.getInstance().getInputMetric(dataFeed.getName());
				metric.getReadsReceived().getAndIncrement();

				cot.setContext(Constants.DATA_FEED_KEY, metric.getInput());
				cot.setContext(Constants.ARCHIVE_EVENT_KEY, dataFeed.getArchive());
				cot.setContext(Constants.DO_NOT_BROKER_KEY, dataFeed.getArchiveOnly());

				List<MinimalMission> feedMissions = new ArrayList<>();

				// get missions associated with this data feed
				// deserialize the mission from JSON to address pokey binary marshaller
				try {
					for (String minimalMissionJson : MessagingDependencyInjectionProxy.getInstance().missionService().getMinimalMissionsJsonForDataFeed(dataFeedUuid)) {
						
						if (logger.isTraceEnabled()) {
							logger.trace("minimalMissionJson: {}", minimalMissionJson);
						}
						
						MinimalMission m = mapper.readValue(minimalMissionJson, MinimalMission.class);
						feedMissions.add(m);
					}
				} catch (JsonProcessingException e) {
					throw new TakException(e);
				}
				
				if (logger.isDebugEnabled()) {
					logger.debug("deserialized {} minimalMissions", feedMissions.size());
				}
				
				List<MinimalMissionFeed> missionFeeds = new ArrayList<>();

				try {
					for (String missionFeedJson : MessagingDependencyInjectionProxy.getInstance().missionService().getMinimalMissionFeedsJsonForDataFeed(dataFeedUuid)) {
						if (logger.isDebugEnabled()) {
							logger.debug("missionFeedJson: {}", missionFeedJson);

						}
						MinimalMissionFeed mf = mapper.readValue(missionFeedJson, MinimalMissionFeed.class);
						missionFeeds.add(mf);
					}
				} catch (JsonProcessingException e) {
					throw new TakException(e);
				}
 
				handleVbmMissions(cot, feedMissions, missionFeeds);
			}
		}
	}

	// this method will stop the cot message from propagating to all clients through brokering by adding the EXPLICIT_FEED_UID_KEY key.
	// (this key will force explicit brokering rather than implicit)		
	private void handleVbmMissions(CotEventContainer cot, List<MinimalMission> feedMissions, List<MinimalMissionFeed> missionFeeds) {

		if (logger.isDebugEnabled()) {
			logger.debug("feedMissions.size: {}", feedMissions.size());
			logger.debug("missionFeeds.size: {}", missionFeeds.size());
		}	
		
		// figure out which missions should filter CoT based on bounding box
		List<String> feedMissionNamesInCotBbox = new ArrayList<>();		
		for (MinimalMission mission : feedMissions) {
			// no bbox, allow mission				
			if (Strings.isNullOrEmpty(mission.getBbox()) && Strings.isNullOrEmpty(mission.getBoundingPolygon())) {
				feedMissionNamesInCotBbox.add(mission.getName());
			} 
			// use polygon over bbox					
			else if (!Strings.isNullOrEmpty(mission.getBoundingPolygon())) {
				Polygon polygon = GeomUtils.postgisBoundingPolygonToPolygon(mission.getBoundingPolygon());
				// valid bounding box
				if (polygon != null) {
					double latitude = Double.parseDouble(cot.getLat());
			        double longitude = Double.parseDouble(cot.getLon());
			        
					// if we received back non null, cot passed the filter
					if (GeomUtils.polygonContainsCoordinate(polygon, latitude, longitude)) {
						feedMissionNamesInCotBbox.add(mission.getName());
					} 
				}
			} 
			// fallback to bbox						
			else {
				BoundingBox boundingBox = getBoundingBoxFromBboxString(mission.getBbox());
				// valid bounding box
				if (boundingBox != null) {
					GeospatialFilter gf = new GeospatialFilter();
					gf.getBoundingBox().add(boundingBox);
					GeospatialEventFilter gef = new GeospatialEventFilter(gf, true, false, false);
					CotEventContainer c = gef.filter(cot);
					// if we received back non null, cot passed the filter
					if (c != null) {
						feedMissionNamesInCotBbox.add(mission.getName());
					} 
				}
			}
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("feedMissionNamesInCotBbox.size: {}", feedMissionNamesInCotBbox.size());
		}	
		
		// Filter as specified in MissionFeed
		List<String> missionNamesAfterFiltering = new ArrayList<>(); 
		for (MinimalMissionFeed missionFeed: missionFeeds) {
			if (!feedMissionNamesInCotBbox.contains(missionFeed.getMissionName())) {
				continue; 
			}
			
			// Filter by cotTypes
			boolean isMatchCotTypes = false;
			if (missionFeed.getFilterCotTypes() == null || missionFeed.getFilterCotTypes().isEmpty()) { // no filter by cot type
				isMatchCotTypes = true;
			} else {
				for (String filterCotType: missionFeed.getFilterCotTypes()) {
					isMatchCotTypes = isCotTypeMatch(cot.getType(), filterCotType);
					if (isMatchCotTypes) {
						break;
					}
				}
			}
			if (!isMatchCotTypes) {
				continue;
			}
			
			// Filter by polygon
			boolean isMatchPolygon = false;
			if (Strings.isNullOrEmpty(missionFeed.getFilterPolygon())) { // no filter by Polygon
				isMatchPolygon = true;
			} else {
				Polygon polygon = GeomUtils.postgisBoundingPolygonToPolygon(missionFeed.getFilterPolygon());
				// valid bounding box
				if (polygon != null) {
					double latitude = Double.parseDouble(cot.getLat());
			        double longitude = Double.parseDouble(cot.getLon());
			        
					// if we received back non null, cot passed the filter
					if (GeomUtils.polygonContainsCoordinate(polygon, latitude, longitude)) {
						isMatchPolygon = true;
					} 
				} else {
					logger.error("missionFeed filterPolygon is invalid: {}", missionFeed.getFilterPolygon());
					isMatchPolygon = true; // not filter out the cot message if the polygon filter is invalid.
				}
			}
			
			if (isMatchPolygon) {
				missionNamesAfterFiltering.add(missionFeed.getMissionName());
			}
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("missionNamesAfterFiltering.size: {}", missionNamesAfterFiltering.size());
		}
		
		// Collect all the mission subscriber uids for valid feed missions
		List<String> feedMissionClients = missionNamesAfterFiltering
			.stream()
			.map(missionName -> SubscriptionStore.getInstance().getLocalUidsByMission(missionName)) 
			.flatMap(clientUids -> clientUids.stream())
			.distinct()
			.collect(Collectors.toList());
		
		if (logger.isDebugEnabled()) {
			logger.debug("feedMissionClients.size: {}", feedMissionClients.size());
		}
				
		// by adding explicit UIDs, this CoT event will go into explicit brokering rather than implicit				
		cot.setContextValue(StreamingEndpointRewriteFilter.EXPLICIT_FEED_UID_KEY, feedMissionClients);
	}
	
	public static boolean isCotTypeMatch(String cotType, String filterCotType) {
		// exact match
		if (cotType.equalsIgnoreCase(filterCotType)) { 
			return true;
		}
		// deal with wild card case (only support 1 wild card)
		if (filterCotType.contains("*")) {
			int index = filterCotType.indexOf("*");
			String prefix = filterCotType.substring(0, index);
			String suffix = filterCotType.substring(index + 1);
			if (cotType.startsWith(prefix) && cotType.endsWith(suffix)) {
				return true;
			}
		}
		return false;
	}

	// compute bbox from string and cache it for instant lookup next time
	BoundingBox getBoundingBoxFromBboxString(String bbox) {

		BoundingBox cachedBoundingBox = cache().getIfPresent(bbox);
		if (cachedBoundingBox != null) {
			return cachedBoundingBox;
		}

		BoundingBox boundingBox = GeomUtils.getBoundingBoxFromBboxString(bbox);

		cache().put(bbox, boundingBox);

		return boundingBox;
	}

	private Cache<String, BoundingBox> cache;
	private Cache<String, BoundingBox> cache() {
		if (cache == null) {
			synchronized (this) {
				if (cache == null) {
					Caffeine<Object, Object> builder = Caffeine.newBuilder();
					cache = builder.maximumSize(100).build();
				}
			}
		}
		return cache;
	}
	
	private boolean isVbm() {
		return CoreConfigFacade.getInstance().getRemoteConfiguration().getVbm().isEnabled();
	}
	
	private boolean isMissionDataFeedFederation() {
		return CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation()
				&& CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowDataFeedFederation();
	}

}
