package logic;

import java.util.NoSuchElementException;
import java.util.UUID;

import common.Member;
import server.MemberRepository;

public class UserManagement {

    public enum Role {
        RESTAURANT_MANAGER, MEMBER
    }

    static final class ContactInfo {
        private final String phone;
        private final String email;

        ContactInfo(String phone, String email) {
            if ((phone == null || phone.isBlank()) && (email == null || email.isBlank())) {
                throw new IllegalArgumentException("You must provide at least phone or email.");
            }
            this.phone = phone;
            this.email = email;
        }
    }

    private final MemberRepository repo = new MemberRepository();

    public Member getMemberById(String memberId) {
        Member member = repo.getMemberById(memberId); // חייב להחזיר logic.Member
        if (member == null) throw new NoSuchElementException("Member not found: " + memberId);
        return member;
    }

    public Member registerMember(Role caller, String userName, String fullName, String phone, String email) {
        if (caller != Role.RESTAURANT_MANAGER)
            throw new IllegalStateException("Only the manager can register a member");

        if (userName == null || userName.isBlank() || fullName == null || fullName.isBlank())
            throw new IllegalArgumentException("You must provide a username and a fullname.");

        if (repo.usernameExists(userName))
            throw new IllegalStateException("Username already exists: " + userName);

        String memberId = UUID.randomUUID().toString();
        String qrCode = UUID.randomUUID().toString();

        new ContactInfo(phone, email);

        String res = repo.insertMember(memberId, qrCode, userName, fullName, phone, email);
        if (!"OK".equals(res)) throw new RuntimeException(res);

        return new Member(memberId, qrCode, userName, fullName, phone, email);
    }

    public void updateContact(Role caller, String memberId, String newPhone, String newEmail) {
        if (caller != Role.MEMBER)
            throw new IllegalStateException("Only the member can update his contact info");

        new ContactInfo(newPhone, newEmail);

        String res = repo.updateContact(memberId, newPhone, newEmail);
        if (!"OK".equals(res)) throw new RuntimeException(res);
    }
}
