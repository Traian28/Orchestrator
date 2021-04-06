/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.config;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;

/**
 *
 * @author mbochatey
 */
public class Subscriber {
    private String ICCID;
    private String epfCode;
    private String networkAccessMode;
    private String IMSI_t;
    private String MSISDN_t;
    private Integer retryCounter;
    private Timestamp add;
    private String IMEI;
    private Integer Category;
    private String blockedStatus;
    private String IMSI_d;
    private String MSISDN_d;
    private String prevSstCode;
    private String sstCode;
    private Timestamp sstCodeChange;
    private Integer traceLevel;
    private String ki;
    private String transportKey;
    private String electricalProfile;
    private Integer chipBatch;
    private String rechargeMix;
    private String masterKey;
    private Integer filIdInsert;
    private Integer filIdUpdate;
    private Integer filIdLockUnlock;
    private String plnCode;
    private String plcCode;
    private String nwtCode;
    private Timestamp lastUpdate;
    private String imsiRoaming;
    private String origin;
    private String areaCode;
    private String userType;
    private String opc;
    private long   virtualNetwork;
    private long   subApplication;
    private String cardType;
    private String operatorKey;
    private String activationType;
    private String expiration;
    private String batchStatus;
    private String batchBlocked;
    private Integer activationSmConfigTag;
    private String activationSmName;
    
	public String getICCID() {
		return ICCID;
	}
	public void setICCID(String iCCID) {
		ICCID = iCCID;
	}
	public String getEpfCode() {
		return epfCode;
	}
	public void setEpfCode(String epfCode) {
		this.epfCode = epfCode;
	}
	public String getNetworkAccessMode() {
		return networkAccessMode;
	}
	public void setNetworkAccessMode(String networkAccessMode) {
		this.networkAccessMode = networkAccessMode;
	}
	public String getIMSI_t() {
		return IMSI_t;
	}
	public void setIMSI_t(String iMSI_t) {
		IMSI_t = iMSI_t;
	}
	public String getMSISDN_t() {
		return MSISDN_t;
	}
	public void setMSISDN_t(String mSISDN_t) {
		MSISDN_t = mSISDN_t;
	}
	public Integer getRetryCounter() {
		return retryCounter;
	}
	public void setRetryCounter(Integer retryCounter) {
		this.retryCounter = retryCounter;
	}
	public Timestamp getAdd() {
		return add;
	}
	public void setAdd(Timestamp add) {
		this.add = add;
	}
	public String getIMEI() {
		return IMEI;
	}
	public void setIMEI(String iMEI) {
		IMEI = iMEI;
	}
	public Integer getCategory() {
		return Category;
	}
	public void setCategory(Integer category) {
		Category = category;
	}
	public String getBlockedStatus() {
		return blockedStatus;
	}
	public void setBlockedStatus(String blockedStatus) {
		this.blockedStatus = blockedStatus;
	}
	public String getIMSI_d() {
		return IMSI_d;
	}
	public void setIMSI_d(String iMSI_d) {
		IMSI_d = iMSI_d;
	}
	public String getMSISDN_d() {
		return MSISDN_d;
	}
	public void setMSISDN_d(String mSISDN_d) {
		MSISDN_d = mSISDN_d;
	}
	public String getPrevSstCode() {
		return prevSstCode;
	}
	public void setPrevSstCode(String prevSstCode) {
		this.prevSstCode = prevSstCode;
	}
	public String getSstCode() {
		return sstCode;
	}
	public void setSstCode(String sstCode) {
		this.sstCode = sstCode;
	}
	public Timestamp getSstCodeChange() {
		return sstCodeChange;
	}
	public void setSstCodeChange(Timestamp sstCodeChange) {
		this.sstCodeChange = sstCodeChange;
	}
	public Integer getTraceLevel() {
		return traceLevel;
	}
	public void setTraceLevel(Integer traceLevel) {
		this.traceLevel = traceLevel;
	}
	public String getKi() {
		return ki;
	}
	public void setKi(String ki) {
		this.ki = ki;
	}
	public String getTransportKey() {
		return transportKey;
	}
	public void setTransportKey(String transportKey) {
		this.transportKey = transportKey;
	}
	public String getElectricalProfile() {
		return electricalProfile;
	}
	public void setElectricalProfile(String electricalProfile) {
		this.electricalProfile = electricalProfile;
	}
	public Integer getChipBatch() {
		return chipBatch;
	}
	public void setChipBatch(Integer chipBatch) {
		this.chipBatch = chipBatch;
	}
	public String getRechargeMix() {
		return rechargeMix;
	}
	public void setRechargeMix(String rechargeMix) {
		this.rechargeMix = rechargeMix;
	}
	public String getMasterKey() {
		return masterKey;
	}
	public void setMasterKey(String masterKey) {
		this.masterKey = masterKey;
	}
	public Integer getFilIdInsert() {
		return filIdInsert;
	}
	public void setFilIdInsert(Integer filIdInsert) {
		this.filIdInsert = filIdInsert;
	}
	public Integer getFilIdUpdate() {
		return filIdUpdate;
	}
	public void setFilIdUpdate(Integer filIdUpdate) {
		this.filIdUpdate = filIdUpdate;
	}
	public Integer getFilIdLockUnlock() {
		return filIdLockUnlock;
	}
	public void setFilIdLockUnlock(Integer filIdLockUnlock) {
		this.filIdLockUnlock = filIdLockUnlock;
	}
	public String getPlnCode() {
		return plnCode;
	}
	public void setPlnCode(String plnCode) {
		this.plnCode = plnCode;
	}
	public String getPlcCode() {
		return plcCode;
	}
	public void setPlcCode(String plcCode) {
		this.plcCode = plcCode;
	}
	public String getNwtCode() {
		return nwtCode;
	}
	public void setNwtCode(String nwtCode) {
		this.nwtCode = nwtCode;
	}
	public Timestamp getLastUpdate() {
		return lastUpdate;
	}
	public void setLastUpdate(Timestamp lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
	public String getImsiRoaming() {
		return imsiRoaming;
	}
	public void setImsiRoaming(String imsiRoaming) {
		this.imsiRoaming = imsiRoaming;
	}
	public String getOrigin() {
		return origin;
	}
	public void setOrigin(String origin) {
		this.origin = origin;
	}
	public String getAreaCode() {
		return areaCode;
	}
	public void setAreaCode(String areaCode) {
		this.areaCode = areaCode;
	}
	public String getUserType() {
		return userType;
	}
	public void setUserType(String userType) {
		this.userType = userType;
	}
	public String getOpc() {
		return opc;
	}
	public void setOpc(String opc) {
		this.opc = opc;
	}
	public long getVirtualNetwork() {
		return virtualNetwork;
	}
	public void setVirtualNetwork(long virtualNetwork) {
		this.virtualNetwork = virtualNetwork;
	}
	public long getSubApplication() {
		return subApplication;
	}
	public void setSubApplication(long subApplication) {
		this.subApplication = subApplication;
	}
	public String getCardType() {
		return cardType;
	}
	public void setCardType(String cardType) {
		this.cardType = cardType;
	}
	public String getOperatorKey() {
		return operatorKey;
	}
	public void setOperatorKey(String operatorKey) {
		this.operatorKey = operatorKey;
	}
	public String getExpiration() {
		return expiration;
	}
	public void setExpiration(String expiration) {
		this.expiration = expiration;
	}
	public String getBatchBlocked() {
		return batchBlocked;
	}
	public void setBatchBlocked(String batchBlocked) {
		this.batchBlocked = batchBlocked;
	}
	
    public boolean isExpired() {
		if (this.expiration == null) return false;
		try {
			LocalDate today = LocalDate.now();
			LocalDate expirationDay = LocalDate.parse(this.expiration, DateTimeFormatter.BASIC_ISO_DATE);
			return today.isAfter(expirationDay);
		}
		catch (DateTimeParseException e) {
			return false;
		}
    }     
    
	public String getBatchStatus() {
		return batchStatus;
	}
	public void setBatchStatus(String batchStatus) {
		this.batchStatus = batchStatus;
	}

    public boolean isBatchProccesedOk() {
    	return (this.batchStatus.equals("BATCH_PROCESSED_OK"));
    }                                    
    public boolean isBatchBlocked() {
    	return(this.batchBlocked.equals("T"));
    }
    
	public String getActivationType() {
		return this.activationType;
	}
	public void setActivationType(String activationType) {
		this.activationType = activationType;
	}
	
	public String getActivationSmName() {
		return activationSmName;
	}
	public void setActivationSmName(String activationSmName) {
		this.activationSmName = activationSmName;
	}

	public Integer getActivationSmConfigTag() {
		return activationSmConfigTag;
	}
	public void setActivationSmConfigTag(Integer activationSmConfigTag) {
		this.activationSmConfigTag = activationSmConfigTag;
	}
}
