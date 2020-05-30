package utils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public class PaytmResponseModel {
	
	public String MID;
	public String TXNID; 
	public UUID   ORDERID; 
	public String BANKTXNID; 
	public String TXNAMOUNT; 
	public String CURRENCY;
	public String STATUS; 
	public String RESPCODE; 
	public String RESPMSG; 
	public Date   TXNDATE; 
	public String GATEWAYNAME; 
	public String BANKNAME; 
	public String PAYMENTMODE;
	public String CHECKSUMHASH;
	public String TXNTYPE;
	
	
	
	
	public String getMID() {
		return MID;
	}
	public void setMID(String mID) {
		MID = mID;
	}
	public String getTXNID() {
		return TXNID;
	}
	public void setTXNID(String tXNID) {
		TXNID = tXNID;
	}
	public String getBANKTXNID() {
		return BANKTXNID;
	}
	public void setBANKTXNID(String bANKTXNID) {
		BANKTXNID = bANKTXNID;
	}
	public String getTXNAMOUNT() {
		return TXNAMOUNT;
	}
	public void setTXNAMOUNT(String tXNAMOUNT) {
		TXNAMOUNT = tXNAMOUNT;
	}
	public String getCURRENCY() {
		return CURRENCY;
	}
	public void setCURRENCY(String cURRENCY) {
		CURRENCY = cURRENCY;
	}
	public String getSTATUS() {
		return STATUS;
	}
	public void setSTATUS(String sTATUS) {
		STATUS = sTATUS;
	}
	public String getRESPCODE() {
		return RESPCODE;
	}
	public void setRESPCODE(String rESPCODE) {
		RESPCODE = rESPCODE;
	}
	public String getRESPMSG() {
		return RESPMSG;
	}
	public void setRESPMSG(String rESPMSG) {
		RESPMSG = rESPMSG;
	}
	public String getGATEWAYNAME() {
		return GATEWAYNAME;
	}
	public void setGATEWAYNAME(String gATEWAYNAME) {
		GATEWAYNAME = gATEWAYNAME;
	}
	public String getBANKNAME() {
		return BANKNAME;
	}
	public void setBANKNAME(String bANKNAME) {
		BANKNAME = bANKNAME;
	}
	public String getPAYMENTMODE() {
		return PAYMENTMODE;
	}
	public void setPAYMENTMODE(String pAYMENTMODE) {
		PAYMENTMODE = pAYMENTMODE;
	}
	public UUID getORDERID() {
		return ORDERID;
	}
	public void setORDERID(UUID oRDERID) {
		ORDERID = oRDERID;
	}
	public Date getTXNDATE() {
		return TXNDATE;
	}
	public void setTXNDATE(Date tXNDATE) {
		TXNDATE = tXNDATE;
	}
	public String getCHECKSUMHASH() {
		return CHECKSUMHASH;
	}
	public void setCHECKSUMHASH(String cHECKSUMHASH) {
		CHECKSUMHASH = cHECKSUMHASH;
	}
	public String getTXNTYPE() {
		return TXNTYPE;
	}
	public void setTXNTYPE(String tXNTYPE) {
		TXNTYPE = tXNTYPE;
	}
	@Override
	public String toString() {
		return "PaytmResponseModel [MID=" + MID + ", TXNID=" + TXNID + ", ORDERID=" + ORDERID + ", BANKTXNID="
				+ BANKTXNID + ", TXNAMOUNT=" + TXNAMOUNT + ", CURRENCY=" + CURRENCY + ", STATUS=" + STATUS
				+ ", RESPCODE=" + RESPCODE + ", RESPMSG=" + RESPMSG + ", TXNDATE=" + TXNDATE + ", GATEWAYNAME="
				+ GATEWAYNAME + ", BANKNAME=" + BANKNAME + ", PAYMENTMODE=" + PAYMENTMODE + ", CHECKSUMHASH="
				+ CHECKSUMHASH + ", TXNTYPE=" + TXNTYPE + "]";
	}
	
	
	 

}
