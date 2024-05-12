package com.bbn.marti.util.spring;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bbn.marti.config.Network;
import com.bbn.marti.remote.config.CoreConfigFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.GenericFilterBean;

import com.bbn.marti.logging.AuditLogUtil;
import com.bbn.marti.remote.exception.MissionDeletedException;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.MissionRole;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.util.CommonUtil;
import com.google.common.base.Strings;

@Order(0)
public class MissionRoleAssignmentRequestHolderFilterBean extends GenericFilterBean {
	private static final Logger logger = LoggerFactory.getLogger(MissionRoleAssignmentRequestHolderFilterBean.class);

	@Autowired
	private RequestHolderBean requestBean;

	@Autowired
	private MissionService missionService;

	@Autowired
	private CommonUtil martiUtil;

	private final String apiMissions = "/api/missions/";
	private final String copMissions = "/api/cops/";

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		requestBean.setRequest(servletRequest);
		requestBean.setMissionRole(null);

		HttpServletRequest req = (HttpServletRequest) servletRequest;
		HttpServletResponse resp = (HttpServletResponse) servletResponse;

		if (servletRequest.getLocalPort() != 8080
				&& CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().isEnableHSTS()) {
			resp.setHeader("Strict-Transport-Security", "max-age=63072000; includeSubDomains");
		}

		if (CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().isAllowAllOrigins()) {
			resp.setHeader("Access-Control-Allow-Origin", "*");
			resp.setHeader("Access-Control-Allow-Headers", "*");
			resp.setHeader("Access-Control-Allow-Methods", "*");
		} else {
			CorsHeaders.checkAndApplyCorsForConnector(req, resp);
		}

		String path = req.getRequestURI();
		String apiPath = apiMissions;
		int missionStart = path.indexOf(apiPath);

		if (missionStart == -1) {
			missionStart = path.indexOf(copMissions);
			apiPath = copMissions;
		}

		if (logger.isDebugEnabled()) {

			// NB this can act as a request logger
			logger.debug("path: " + path);
		}

		if (CoreConfigFacade.getInstance().getRemoteConfiguration().getLogging() != null &&
				CoreConfigFacade.getInstance().getRemoteConfiguration().getLogging().isAuditLoggingEnabled()) {
			AuditLogUtil.setMdc(req, resp);
			logger.info("doFilter request path: " + path);
		}

		if (missionStart != -1) {

			boolean missionCreate = false;

			int missionEnd = path.indexOf("/", missionStart + apiPath.length());
			if (missionEnd == -1) {
				missionEnd = path.length();

				if (req.getMethod().equals("PUT") || req.getMethod().equals("POST")
						|| (req.getMethod().equals("OPTIONS") && CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().isAllowAllOrigins())) {
					missionCreate = true;
				}
			}

			String missionName = path.substring(missionStart + apiPath.length(), missionEnd);
			if (missionName != null && !missionName.isEmpty()) {

				missionName = missionService.trimName(missionName);
				missionName = URLDecoder.decode(missionName, "UTF-8");

				//
				// ignore /missions endpoints that don't refer to an individual mission
				//
				if (missionName.compareTo("all") != 0
						&& 	missionName.compareTo("logs") != 0
						&& 	missionName.compareTo("invitations") != 0
						&& 	missionName.compareTo("hierarchy") != 0) {

					String missionGuid = null;

					try {
						String[] parts = path.split("/", 0);

						logger.debug("path parts: {}", parts.length);

						if (parts.length > 5) {
							missionGuid = parts[5];
						}
					} catch (Exception e) {
						logger.warn("error getting mission guid from path", e);
					}

					logger.debug("mission guid {}", missionGuid);

					//
					// get the mission
					//
					Mission mission = null;

					logger.debug("mission name (can be guid) : {}", missionName);

					try {

						// for guid case, the missonName looks like 'guid' here
						if (missionName != null && missionName.toLowerCase().equals("guid") && !Strings.isNullOrEmpty(missionGuid)) {

							logger.debug("getting mission by guid {}", missionGuid);

							UUID missionUuid = UUID.fromString(missionGuid);

							mission = missionService.getMissionNoContentByGuid(missionUuid, martiUtil.getGroupVectorBitString(req.getSession().getId()));
						} else {

							logger.debug("getting mission by name {}", missionName);

							mission = missionService.getMissionNoContent(missionName, martiUtil.getGroupVectorBitString(req.getSession().getId()));
						}

						MissionRole role = missionService.getRoleForRequest(mission, req);

						if (logger.isDebugEnabled()) {
							logger.debug("assigned role: " + role);
						}

						requestBean.setMissionRole(role);

						req.setAttribute(MissionRole.class.getName(), role);

						if (CoreConfigFacade.getInstance().getRemoteConfiguration().getVbm().isEnabled()) {
							if (!missionService.validateAccess(mission, req)) {
								((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_NOT_FOUND);
								return;
							}
						}

					} catch (NotFoundException nfe) {
						if (logger.isDebugEnabled()) {
							logger.debug("mission " + missionName + " not found - not assigning role");
						}

						if (!missionCreate) {
							((HttpServletResponse)servletResponse).setStatus(HttpServletResponse.SC_NOT_FOUND);
							return;
						}
					} catch (MissionDeletedException mde) {
						logger.warn("attempt to access a deleted mission : " + missionName);

						if (!missionCreate) {
							((HttpServletResponse)servletResponse).setStatus(HttpServletResponse.SC_GONE);
							return;
						}
					} catch (IllegalArgumentException e) {

						if (CoreConfigFacade.getInstance().getRemoteConfiguration().getLogging().isAuditLoggingEnabled()) {
							logger.error("invalid mission UUID: " + missionGuid);
						}

						throw new IllegalArgumentException("invalid mission UUID");
					} catch (Exception e) {
						logger.warn("exception assigning mission role", e);
					}
				}
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("RequestHolderFilterBean doFilter: " + servletRequest);
		}

		try {
			filterChain.doFilter(servletRequest, servletResponse);
		} finally {
			MDC.clear();
		}
	}
}