package logic;

import java.io.Serializable;

public class Member implements Serializable {
    private static final long serialVersionUID = 1L;

    private String memberId;
    private String qrCode;
    private String userName;
    private String fullName;
    private String phone;
    private String email;

    public Member(String memberId, String qrCode, String userName, String fullName, String phone, String email) {
    	
        this.memberId = memberId;
        this.qrCode = qrCode;
        this.userName = userName;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
    }

    public String getMemberId() { return memberId; }
    public String getQrCode() { return qrCode; }
    public String getUserName() { return userName; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
}
