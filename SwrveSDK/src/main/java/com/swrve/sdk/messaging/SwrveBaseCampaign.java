package com.swrve.sdk.messaging;

import com.swrve.sdk.SwrveLogger;

import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.SwrveHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/*
 * Swrve campaign containing messages targeted for the current device and user id.
 */
public abstract class SwrveBaseCampaign {
    protected static final String LOG_TAG = "SwrveMessagingSDK";
    // Default campaign throttle limits
    protected static int DEFAULT_DELAY_FIRST_MESSAGE = 180;
    protected static int DEFAULT_MAX_IMPRESSIONS = 99999;
    protected static int DEFAULT_MIN_DELAY_BETWEEN_MSGS = 60;
    // Random number generator
    protected final SimpleDateFormat timestampFormat = new SimpleDateFormat("HH:mm:ss ZZZZ", Locale.US);
    // Identifies the campaign
    protected int id;
    // SDK controller for this campaign
    protected transient SwrveBase<?, ?> talkController;
    // The state of the campaign that will be kept saved by the SDK
    protected SwrveCampaignState saveableState;
    // Start date of the campaign
    protected Date startDate;
    // End date of the campaign
    protected Date endDate;
    // List of triggers for the campaign
    protected Set<String> triggers;
    // Flag indicating if it is a MessageCenter campaign
    protected boolean messageCenter;
    // MessageCenter subject of the campaign
    protected String subject;
    // Indicates if the campaign serves messages randomly or using round robin
    protected boolean randomOrder;
    // Number of maximum impressions of the campaign
    protected int maxImpressions;
    // Minimum delay we want between messages
    protected int minDelayBetweenMessage;
    // Time we can show the first message after launch
    protected Date showMessagesAfterLaunch;
    // Amount of seconds to wait for the first message
    protected int delayFirstMessage;

    private SwrveBaseCampaign() {
        this.triggers = new HashSet<String>();
    }

    /**
     * Load a campaign from JSON data.
     *
     * @param controller   SwrveTalk object that will manage the data from the campaign.
     * @param campaignData JSON data containing the campaign details.
     * @throws org.json.JSONException
     */
    public SwrveBaseCampaign(SwrveBase<?, ?> controller, JSONObject campaignData) throws JSONException {
        this();
        setId(campaignData.getInt("id"));
        setMessageCenter(campaignData.optBoolean("message_center", false));
        setSubject(campaignData.isNull("subject") ? "" : campaignData.getString("subject"));
        setTalkController(controller);
        SwrveLogger.i(LOG_TAG, "Loading campaign " + getId());

        // Start with an empty state
        this.saveableState = new SwrveCampaignState();

        // Campaign rule defaults
        this.maxImpressions = DEFAULT_MAX_IMPRESSIONS;
        this.minDelayBetweenMessage = DEFAULT_MIN_DELAY_BETWEEN_MSGS;
        this.showMessagesAfterLaunch = SwrveHelper.addTimeInterval(this.talkController.getInitialisedTime(), DEFAULT_DELAY_FIRST_MESSAGE, Calendar.SECOND);

        assignCampaignTriggers(this, campaignData);
        assignCampaignRules(this, campaignData);
        assignCampaignDates(this, campaignData);
    }


    /**
     * @return the campaign id.
     */
    public int getId() {
        return id;
    }

    protected void setId(int id) {
        this.id = id;
    }

    protected void setTalkController(SwrveBase<?, ?> controller) {
        this.talkController = controller;
    }

    /**
     * Used internally to identify campaigns that have been marked as MessageCenter campaigns on the dashboard.
     *
     * @return true if the campaign is an MessageCenter campaign.
     */
    public boolean isMessageCenter() {
        return messageCenter;
    }

    protected void setMessageCenter(boolean messageCenter) {
        this.messageCenter = messageCenter;
    }

    /**
     * @return the name of the campaign.
     */
    public String getSubject() {
        return subject;
    }

    protected void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * @return true if the campaign is active at the given time.
     */
    public boolean isActive(Date now) {
        return checkForStartAndEndDate(now, null);
    }

    /**
     * @return the next message to show.
     */
    public int getNext() {
        return saveableState.next;
    }

    public void setNext(int next) {
        this.saveableState.next = next;
    }

    /**
     * @return the set of triggers for this campaign.
     */
    public Set<String> getTriggers() {
        return triggers;
    }

    protected void setTriggers(Set<String> triggers) {
        this.triggers = triggers;
    }

    /**
     * @return if the campaign serves messages in random order and not round
     * robin.
     */
    public boolean isRandomOrder() {
        return randomOrder;
    }

    protected void setRandomOrder(boolean randomOrder) {
        this.randomOrder = randomOrder;
    }

    /**
     * @return current impressions
     */
    public int getImpressions() {
        return saveableState.impressions;
    }

    public void setImpressions(int impressions) {
        this.saveableState.impressions = impressions;
    }

    /**
     * @return maximum impressions
     */
    public int getMaxImpressions() {
        return maxImpressions;
    }

    public void setMaxImpressions(int maxImpressions) {
        this.maxImpressions = maxImpressions;
    }

    /**
     * @return the campaign start date.
     */
    public Date getStartDate() {
        return startDate;
    }

    protected void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * @return the campaign end date.
     */
    public Date getEndDate() {
        return endDate;
    }

    protected void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    /**
     * Check if the campaign contains messages for the given event.
     *
     * @param eventName
     * @return true if the campaign has this event as a trigger
     */
    public boolean hasElementForEvent(String eventName) {
        String lowerCaseEvent = eventName.toLowerCase(Locale.US);
        return triggers != null && triggers.contains(lowerCaseEvent);
    }

    protected boolean isTooSoonToShowMessageAfterLaunch(Date now) {
        return now.before(showMessagesAfterLaunch);
    }

    protected boolean isTooSoonToShowMessageAfterDelay(Date now) {
        return (saveableState.showMessagesAfterDelay != null && now.before(saveableState.showMessagesAfterDelay));
    }

    protected void logAndAddReason(Map<Integer, String> campaignReasons, String reason) {
        if (campaignReasons != null) {
            campaignReasons.put(id, reason);
        }
        SwrveLogger.i(LOG_TAG, reason);
    }

    /**
     * Amount of seconds to wait for first message.
     *
     * @return time in seconds
     */
    public int getDelayFirstMessage() {
        return delayFirstMessage;
    }

    protected void assignCampaignTriggers(SwrveBaseCampaign campaign, JSONObject campaignData) throws JSONException {
        JSONArray jsonTriggers = campaignData.getJSONArray("triggers");
        for (int i = 0, j = jsonTriggers.length(); i < j; i++) {
            String trigger = jsonTriggers.getString(i);
            campaign.getTriggers().add(trigger.toLowerCase(Locale.US));
        }
    }

    protected void assignCampaignRules(SwrveBaseCampaign campaign, JSONObject campaignData) throws JSONException {
        JSONObject rules = campaignData.getJSONObject("rules");
        campaign.setRandomOrder(rules.getString("display_order").equals("random"));

        if (rules.has("dismiss_after_views")) {
            int totalImpressions = rules.getInt("dismiss_after_views");
            setMaxImpressions(totalImpressions);
        }

        if (rules.has("delay_first_message")) {
            int delay = rules.getInt("delay_first_message");
            this.delayFirstMessage = delay;
            this.showMessagesAfterLaunch = SwrveHelper.addTimeInterval(this.talkController.getInitialisedTime(), this.delayFirstMessage, Calendar.SECOND);
        }

        if (rules.has("min_delay_between_messages")) {
            this.minDelayBetweenMessage = rules.getInt("min_delay_between_messages");
        }
    }

    protected void assignCampaignDates(SwrveBaseCampaign campaign, JSONObject campaignData) throws JSONException {
        campaign.setStartDate(new Date(campaignData.getLong("start_date")));
        campaign.setEndDate(new Date(campaignData.getLong("end_date")));
    }

    /**
     * Increment impressions by one.
     */
    public void incrementImpressions() {
        this.saveableState.impressions++;
    }

    /**
     * Ensures a new message cannot be shown until now + minDelayBetweenMessage
     */
    protected void setMessageMinDelayThrottle() {
        Date now = this.talkController.getNow();
        this.saveableState.showMessagesAfterDelay = SwrveHelper.addTimeInterval(now, this.minDelayBetweenMessage, Calendar.SECOND);
        this.talkController.setMessageMinDelayThrottle();
    }

    protected boolean checkForStartAndEndDate(Date now, Map<Integer, String> campaignReasons) {
        if (startDate.after(now)) {
            logAndAddReason(campaignReasons, "Campaign " + id + " has not started yet");
            return false;
        }
        if (endDate.before(now)) {
            logAndAddReason(campaignReasons, "Campaign " + id + " has finished");
            return false;
        }
        return true;
    }

    protected boolean checkCampaignLimits(String event, Date now, Map<Integer, String> campaignReasons, int elementCount, String elementName) {
        if (!hasElementForEvent(event)) {
            SwrveLogger.i(LOG_TAG, "There is no trigger in " + id + " that matches " + event);
            return false;
        }

        if (elementCount == 0) {
            logAndAddReason(campaignReasons, "No " + elementName + "s in campaign " + id);
            return false;
        }

        if (!checkForStartAndEndDate(now, campaignReasons)) {
            return false;
        }

        if (saveableState.impressions >= maxImpressions) {
            logAndAddReason(campaignReasons, "{Campaign throttle limit} Campaign " + id + " has been shown " + maxImpressions + " times already");
            return false;
        }

        // Ignore delay after launch throttle limit for auto show messages
        if (!event.equalsIgnoreCase(talkController.getAutoShowEventTrigger()) && isTooSoonToShowMessageAfterLaunch(now)) {
            logAndAddReason(campaignReasons, "{Campaign throttle limit} Too soon after launch. Wait until " + timestampFormat.format(showMessagesAfterLaunch));
            return false;
        }

        if (isTooSoonToShowMessageAfterDelay(now)) {
            logAndAddReason(campaignReasons, "{Campaign throttle limit} Too soon after last " + elementName + ". Wait until " + timestampFormat.format(saveableState.showMessagesAfterDelay));
            return false;
        }

        return true;
    }

    /**
     * Used internally to set the status of the campaign.
     *
     * @param status new status of the campaign
     */
    public void setStatus(SwrveCampaignState.Status status) {
        this.saveableState.status = status;
    }

    /**
     * Get the status of the campaign.
     *
     * @return status of the campaign
     */
    public SwrveCampaignState.Status getStatus() {
        return saveableState.status;
    }

    /**
     * Used by sublcasses to inform that the campaign was displayed.
     */
    public void messageWasShownToUser() {
        setStatus(SwrveCampaignState.Status.Seen);
        incrementImpressions();
        setMessageMinDelayThrottle();
    }

    public abstract boolean supportsOrientation(SwrveOrientation orientation);

    /**
     * Determine if the assets for this campaign have been downloaded.
     * @return if the assets are ready
     */
    public abstract boolean areAssetsReady();

    /**
     * Obtain the serializable state of the campaign.
     * @return the serializable state of the campaign.
     */
    public SwrveCampaignState getSaveableState() {
        return saveableState;
    }

    /**
     * Set the previous state of this campaign.
     * @param saveableState
     */
    public void setSaveableState(SwrveCampaignState saveableState) {
        this.saveableState = saveableState;
    }
}
