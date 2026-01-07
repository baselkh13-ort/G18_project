package logic;

import java.io.Serializable;

public class UpdateMemberContactRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String memberId;
    private final String phone;
    private final String email;

    public UpdateMemberContactRequest(String memberId, String phone, String email) {
        this.memberId = memberId;
        this.phone = phone;
        this.email = email;
    }

    public String getMemberId() { return memberId; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }

    @Override
    public String toString() {
        return "UpdateMemberContactRequest{memberId='" + memberId + "'}";
    }
}
