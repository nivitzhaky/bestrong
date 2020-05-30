package utils;
/**
 * Created by gshah on 7/31/17.
 */

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;


import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 * 
 * @author gshah Sample Content // "goal":"goal2", "reason":"reason2",
 *         "details":"details", // "image":"details","video":"details"}
 *         {"campaign_id":1, "orderId":"123", "txnAmount":"1234" }
 * 
 */

public class PaymentResponse {
	private long id;

	@JsonBackReference
	private Campaign campaign;

	private PaymentRequest paymentRequest;

	

	
//	@GeneratedValue(strategy = GenerationType.AUTO)
	private UUID orderId;

    private BigDecimal txnAmount;

	private String transactionId;
	
	private String bankTransactionId;

	private String currency;

    private String status;

    private String responseCode;

	private String customerId;

    private String mobileNo;

    private String emailId;

	private String orderDetails;

	private String responseMessage;

	private Date transactionDate;

	private String transactionType;

	private String gateWayName;

	private String bankName;

	private String paymentMode;

	private Date createdAt;

	private Date updatedAt;

	private String checksumHash;

	private Boolean isValidchecksumHash;
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public Campaign getCampaign() {
		return campaign;
	}
	public void setCampaign(Campaign campaign) {
		this.campaign = campaign;
	}
	public PaymentRequest getPaymentRequest() {
		return paymentRequest;
	}
	public void setPaymentRequest(PaymentRequest paymentRequest) {
		this.paymentRequest = paymentRequest;
	}
	public UUID getOrderId() {
		return orderId;
	}
	public void setOrderId(UUID orderId) {
		this.orderId = orderId;
	}
	public BigDecimal getTxnAmount() {
		return txnAmount;
	}
	public void setTxnAmount(BigDecimal txnAmount) {
		this.txnAmount = txnAmount;
	}
	public String getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	public String getBankTransactionId() {
		return bankTransactionId;
	}
	public void setBankTransactionId(String bankTransactionId) {
		this.bankTransactionId = bankTransactionId;
	}
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getResponseCode() {
		return responseCode;
	}
	public void setResponseCode(String responseCode) {
		this.responseCode = responseCode;
	}
	public String getCustomerId() {
		return customerId;
	}
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}
	public String getMobileNo() {
		return mobileNo;
	}
	public void setMobileNo(String mobileNo) {
		this.mobileNo = mobileNo;
	}
	public String getEmailId() {
		return emailId;
	}
	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}
	public String getOrderDetails() {
		return orderDetails;
	}
	public void setOrderDetails(String orderDetails) {
		this.orderDetails = orderDetails;
	}
	public String getResponseMessage() {
		return responseMessage;
	}
	public void setResponseMessage(String responseMessage) {
		this.responseMessage = responseMessage;
	}
	public Date getTransactionDate() {
		return transactionDate;
	}
	public void setTransactionDate(Date transactionDate) {
		this.transactionDate = transactionDate;
	}
	public String getGateWayName() {
		return gateWayName;
	}
	public void setGateWayName(String gateWayName) {
		this.gateWayName = gateWayName;
	}
	public String getBankName() {
		return bankName;
	}
	public void setBankName(String bankName) {
		this.bankName = bankName;
	}
	public String getPaymentMode() {
		return paymentMode;
	}
	public void setPaymentMode(String paymentMode) {
		this.paymentMode = paymentMode;
	}
	public Date getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
	public Date getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
	public String getTransactionType() {
		return transactionType;
	}
	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}
	public String getChecksumHash() {
		return checksumHash;
	}
	public void setChecksumHash(String checksumHash) {
		this.checksumHash = checksumHash;
	}
	public Boolean getIsValidchecksumHash() {
		return isValidchecksumHash;
	}
	public void setIsValidchecksumHash(Boolean isValidchecksumHash) {
		this.isValidchecksumHash = isValidchecksumHash;
	}
	
	
//	{ "campaign_id":1, "paymentRequest_id":1,"txnAmount":"100","transactionId":"transaction1","bankTransactionId":"bankTransaction1","currency":"USD","status":"SUCCESS", "customerId":"customer2", "mobileNo":"831-214-6646",  "emailId":"timepass2@gmail.com","checksumHash":"not shared"}
//	@ManyToOne
//	@JoinColumn(name = "name")
//	private User user;


}
