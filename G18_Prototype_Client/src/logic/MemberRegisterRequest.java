package logic;

import java.io.Serializable;

public class MemberRegisterRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String userName;
    private final String fullName;
    private final String phone;
    private final String email;

    public MemberRegisterRequest(String userName, String fullName, String phone, String email) {
        this.userName = userName;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
    }

    public String getUserName() { return userName; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }

    @Override
    public String toString() {
        return "MemberRegisterRequest{userName='" + userName + "', fullName='" + fullName + "'}";
    }
}
